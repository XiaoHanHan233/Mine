package net.minecraft.network;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import de.florianmichael.vialoadingbase.netty.event.CompressionReorderEvent;
import de.florianmichael.viamcp.MCPVLBPipeline;
import de.florianmichael.viamcp.ViaMCP;
import dev.diona.southside.Southside;
import dev.diona.southside.event.PacketType;
import dev.diona.southside.event.events.HigherPacketEvent;
import dev.diona.southside.event.events.PacketEvent;
import dev.diona.southside.event.events.ServerDisconnectedEvent;
import dev.diona.southside.util.player.MovementUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoopGroup;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.TimeoutException;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nullable;
import javax.crypto.SecretKey;

import net.minecraft.util.CryptManager;
import net.minecraft.util.ITickable;
import net.minecraft.util.LazyLoadBase;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class NetworkManager extends SimpleChannelInboundHandler < Packet<? >>
{
    private static final Logger LOGGER = LogManager.getLogger();
    public static final Marker NETWORK_MARKER = MarkerManager.getMarker("NETWORK");
    public static final Marker NETWORK_PACKETS_MARKER = MarkerManager.getMarker("NETWORK_PACKETS", NETWORK_MARKER);
    public static final AttributeKey<EnumConnectionState> PROTOCOL_ATTRIBUTE_KEY = AttributeKey.<EnumConnectionState>valueOf("protocol");
    public static final LazyLoadBase<NioEventLoopGroup> CLIENT_NIO_EVENTLOOP = new LazyLoadBase<NioEventLoopGroup>()
    {
        protected NioEventLoopGroup load()
        {
            return new NioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Client IO #%d").setDaemon(true).build());
        }
    };
    public static final LazyLoadBase<EpollEventLoopGroup> CLIENT_EPOLL_EVENTLOOP = new LazyLoadBase<EpollEventLoopGroup>()
    {
        protected EpollEventLoopGroup load()
        {
            return new EpollEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Epoll Client IO #%d").setDaemon(true).build());
        }
    };
    public static final LazyLoadBase<LocalEventLoopGroup> CLIENT_LOCAL_EVENTLOOP = new LazyLoadBase<LocalEventLoopGroup>()
    {
        protected LocalEventLoopGroup load()
        {
            return new LocalEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Local Client IO #%d").setDaemon(true).build());
        }
    };
    private final EnumPacketDirection direction;
    private final Queue<InboundHandlerTuplePacketListener> outboundPacketsQueue = Queues.<InboundHandlerTuplePacketListener>newConcurrentLinkedQueue();
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    /** The active channel */
    public Channel channel;

    /** The address of the remote party */
    private SocketAddress socketAddress;

    /** The INetHandler instance responsible for processing received packets */
    private INetHandler packetListener;

    /** A String indicating why the network has shutdown. */
    private ITextComponent terminationReason;
    private boolean isEncrypted;
    public boolean disconnected;

    public NetworkManager(EnumPacketDirection packetDirection)
    {
        this.direction = packetDirection;
    }

    public void channelActive(ChannelHandlerContext p_channelActive_1_) throws Exception
    {
        super.channelActive(p_channelActive_1_);
        this.channel = p_channelActive_1_.channel();
        this.socketAddress = this.channel.remoteAddress();

        try
        {
            this.setConnectionState(EnumConnectionState.HANDSHAKING);
        }
        catch (Throwable throwable)
        {
            LOGGER.fatal(throwable);
        }
    }

    /**
     * Sets the new connection state and registers which packets this channel may send and receive
     */
    public void setConnectionState(EnumConnectionState newState)
    {
        this.channel.attr(PROTOCOL_ATTRIBUTE_KEY).set(newState);
        this.channel.config().setAutoRead(true);
        LOGGER.debug("Enabled auto read");
    }

    public void channelInactive(ChannelHandlerContext p_channelInactive_1_) {
        this.closeChannel(new TextComponentTranslation("disconnect.endOfStream", new Object[0]));
    }

    public void exceptionCaught(ChannelHandlerContext p_exceptionCaught_1_, Throwable p_exceptionCaught_2_) {
        TextComponentTranslation textcomponenttranslation;

        if (p_exceptionCaught_2_ instanceof TimeoutException)
        {
            textcomponenttranslation = new TextComponentTranslation("disconnect.timeout", new Object[0]);
        }
        else
        {
            textcomponenttranslation = new TextComponentTranslation("disconnect.genericReason", new Object[] {"Internal Exception: " + p_exceptionCaught_2_});
        }

        LOGGER.debug(textcomponenttranslation.getUnformattedText(), p_exceptionCaught_2_);
        this.closeChannel(textcomponenttranslation);
    }

    protected void channelRead0(ChannelHandlerContext p_channelRead0_1_, Packet<?> p_channelRead0_2_) {
        if (this.channel.isOpen()) {
            try {
                Packet<INetHandler> packet = (Packet<INetHandler>) p_channelRead0_2_;
                packet.processPacket(this.packetListener);
            } catch (ThreadQuickExitException ignored) {}
        }
    }

    /**
     * Sets the NetHandler for this NetworkManager, no checks are made if this handler is suitable for the particular
     * connection state (protocol)
     */
    public void setNetHandler(INetHandler handler)
    {
        Validate.notNull(handler, "packetListener");
        LOGGER.debug("Set listener of {} to {}", this, handler);
        this.packetListener = handler;
    }

    public void sendPacketNoEvent(Packet<?> packetIn) {
        HigherPacketEvent event = new HigherPacketEvent(PacketType.SEND, packetIn);
        Southside.eventBus.post(event);
        if (event.isCancelled()) {
            return;
        }
        
        this.sendPacketNoHigherEvent(packetIn);
    }

    public void sendPacketNoHigherEvent(Packet<?> packetIn) {
//        if (packetIn instanceof CPacketConfirmTransaction && Stuck.isCancelC0F()) {
//            return;
//        }
        if (this.isChannelOpen())
        {
            this.flushOutboundQueue();
            this.dispatchPacket(packetIn, null);
        }
        else
        {
            this.readWriteLock.writeLock().lock();

            try
            {
                this.outboundPacketsQueue.add(new InboundHandlerTuplePacketListener(packetIn, new GenericFutureListener[0]));
            }
            finally
            {
                this.readWriteLock.writeLock().unlock();
            }
        }
    }

    public void sendPacketNoEvent(Packet<?> packetIn, GenericFutureListener <? extends Future <? super Void >> listener, GenericFutureListener <? extends Future <? super Void >> ... listeners)
    {
        HigherPacketEvent event = new HigherPacketEvent(PacketType.SEND, packetIn);
        Southside.eventBus.post(event);
        if (event.isCancelled()) {
            return;
        }

        sendPacketNoHigherEvent(packetIn, listener, listeners);
    }

    public void sendPacketNoHigherEvent(Packet<?> packetIn, GenericFutureListener <? extends Future <? super Void >> listener, GenericFutureListener <? extends Future <? super Void >> ... listeners)
    {
        if (this.isChannelOpen())
        {
            this.flushOutboundQueue();
            this.dispatchPacket(packetIn, ArrayUtils.add(listeners, 0, listener));
        }
        else
        {
            this.readWriteLock.writeLock().lock();

            try
            {
                this.outboundPacketsQueue.add(new InboundHandlerTuplePacketListener(packetIn, ArrayUtils.add(listeners, 0, listener)));
            }
            finally
            {
                this.readWriteLock.writeLock().unlock();
            }
        }
    }

    public void sendPacket(Packet<?> packetIn)
    {
        MovementUtils.onPacket(packetIn);
        PacketEvent event = new PacketEvent(PacketType.SEND, packetIn);
        Southside.eventBus.post(event);
        if (event.isCancelled()) {
            return;
        }
        sendPacketNoEvent(packetIn);
    }

    public void sendPacket(Packet<?> packetIn, GenericFutureListener <? extends Future <? super Void >> listener, GenericFutureListener <? extends Future <? super Void >> ... listeners)
    {
        PacketEvent event = new PacketEvent(PacketType.SEND, packetIn);
        Southside.eventBus.post(event);
        if (event.isCancelled()) {
            return;
        }

        sendPacketNoEvent(packetIn, listener, listeners);
    }

    /**
     * Will commit the packet to the channel. If the current thread 'owns' the channel it will write and flush the
     * packet, otherwise it will add a task for the channel eventloop thread to do that.
     */
    private void dispatchPacket(final Packet<?> inPacket, @Nullable final GenericFutureListener <? extends Future <? super Void >> [] futureListeners)
    {
        final EnumConnectionState enumconnectionstate = EnumConnectionState.getFromPacket(inPacket);
        final EnumConnectionState enumconnectionstate1 = (EnumConnectionState)this.channel.attr(PROTOCOL_ATTRIBUTE_KEY).get();

        if (enumconnectionstate1 != enumconnectionstate)
        {
            LOGGER.debug("Disabled auto read");
            this.channel.config().setAutoRead(false);
        }

        if (this.channel.eventLoop().inEventLoop())
        {
            if (enumconnectionstate != enumconnectionstate1)
            {
                this.setConnectionState(enumconnectionstate);
            }

            ChannelFuture channelfuture = this.channel.writeAndFlush(inPacket);

            if (futureListeners != null)
            {
                channelfuture.addListeners(futureListeners);
            }

            channelfuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }
        else
        {
            this.channel.eventLoop().execute(new Runnable()
            {
                public void run()
                {
                    if (enumconnectionstate != enumconnectionstate1)
                    {
                        NetworkManager.this.setConnectionState(enumconnectionstate);
                    }

                    ChannelFuture channelfuture1 = NetworkManager.this.channel.writeAndFlush(inPacket);

                    if (futureListeners != null)
                    {
                        channelfuture1.addListeners(futureListeners);
                    }

                    channelfuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                }
            });
        }
    }

    /**
     * Will iterate through the outboundPacketQueue and dispatch all Packets
     */
    private void flushOutboundQueue()
    {
        if (this.channel != null && this.channel.isOpen())
        {
            this.readWriteLock.readLock().lock();

            try
            {
                while (!this.outboundPacketsQueue.isEmpty())
                {
                    InboundHandlerTuplePacketListener networkmanager$inboundhandlertuplepacketlistener = this.outboundPacketsQueue.poll();
                    if (networkmanager$inboundhandlertuplepacketlistener != null) {
                        this.dispatchPacket(networkmanager$inboundhandlertuplepacketlistener.packet, networkmanager$inboundhandlertuplepacketlistener.futureListeners);
                    }
                }
            }
            finally
            {
                this.readWriteLock.readLock().unlock();
            }
        }
    }

    /**
     * Checks timeouts and processes all packets received
     */
    public void processReceivedPackets()
    {
        this.flushOutboundQueue();

        if (this.packetListener instanceof ITickable)
        {
            ((ITickable)this.packetListener).update();
        }

        if (this.channel != null)
        {
            this.channel.flush();
        }
    }

    /**
     * Returns the socket address of the remote side. Server-only.
     */
    public SocketAddress getRemoteAddress()
    {
        return this.socketAddress;
    }

    /**
     * Closes the channel, the parameter can be used for an exit message (not certain how it gets sent)
     */
    public void closeChannel(ITextComponent message)
    {
        if (this.channel.isOpen())
        {
//            this.channel.close().awaitUninterruptibly();
            this.channel.close();
            this.terminationReason = message;
        }
    }

    /**
     * True if this NetworkManager uses a memory connection (single player game). False may imply both an active TCP
     * connection or simply no active connection at all
     */
    public boolean isLocalChannel()
    {
        return this.channel instanceof LocalChannel || this.channel instanceof LocalServerChannel;
    }

    /**
     * Create a new NetworkManager from the server host and connect it to the server
     */
    public static NetworkManager createNetworkManagerAndConnect(InetAddress address, int serverPort, boolean useNativeTransport)
    {
        final NetworkManager networkmanager = new NetworkManager(EnumPacketDirection.CLIENTBOUND);
        Class <? extends SocketChannel > oclass;
        LazyLoadBase <? extends EventLoopGroup > lazyloadbase;

        if (Epoll.isAvailable() && useNativeTransport)
        {
            oclass = EpollSocketChannel.class;
            lazyloadbase = CLIENT_EPOLL_EVENTLOOP;
        }
        else
        {
            oclass = NioSocketChannel.class;
            lazyloadbase = CLIENT_NIO_EVENTLOOP;
        }

        new Bootstrap().group(lazyloadbase.getValue()).handler(new ChannelInitializer<>() {
            protected void initChannel(Channel p_initChannel_1_) {
                p_initChannel_1_.config().setOption(ChannelOption.TCP_NODELAY, true);
                p_initChannel_1_.pipeline().addLast("timeout", new ReadTimeoutHandler(30)).addLast("splitter", new NettyVarint21FrameDecoder()).addLast("decoder", new NettyPacketDecoder(EnumPacketDirection.CLIENTBOUND)).addLast("prepender", new NettyVarint21FrameEncoder()).addLast("encoder", new NettyPacketEncoder(EnumPacketDirection.SERVERBOUND)).addLast("packet_handler", networkmanager);
                if (p_initChannel_1_ instanceof SocketChannel && ViaLoadingBase.getInstance().getTargetVersion().getVersion() != ViaMCP.NATIVE_VERSION) {
                    final UserConnection user = new UserConnectionImpl(p_initChannel_1_, true);
                    new ProtocolPipelineImpl(user);

                    p_initChannel_1_.pipeline().addLast(new MCPVLBPipeline(user));
                }
            }
        }).channel(oclass).connect(address, serverPort).syncUninterruptibly();
        return networkmanager;
    }

    /**
     * Prepares a clientside NetworkManager: establishes a connection to the socket supplied and configures the channel
     * pipeline. Returns the newly created instance.
     */
    public static NetworkManager provideLocalClient(SocketAddress address)
    {
        final NetworkManager networkmanager = new NetworkManager(EnumPacketDirection.CLIENTBOUND);
        new Bootstrap().group(CLIENT_LOCAL_EVENTLOOP.getValue()).handler(new ChannelInitializer<>() {
            protected void initChannel(Channel p_initChannel_1_) throws Exception {
                p_initChannel_1_.pipeline().addLast("packet_handler", networkmanager);
            }
        }).channel(LocalChannel.class).connect(address).syncUninterruptibly();
        return networkmanager;
    }

    /**
     * Adds an encoder+decoder to the channel pipeline. The parameter is the secret key used for encrypted communication
     */
    public void enableEncryption(SecretKey key)
    {
        this.isEncrypted = true;
        this.channel.pipeline().addBefore("splitter", "decrypt", new NettyEncryptingDecoder(CryptManager.createNetCipherInstance(2, key)));
        this.channel.pipeline().addBefore("prepender", "encrypt", new NettyEncryptingEncoder(CryptManager.createNetCipherInstance(1, key)));
    }

    public boolean isEncrypted()
    {
        return this.isEncrypted;
    }

    /**
     * Returns true if this NetworkManager has an active channel, false otherwise
     */
    public boolean isChannelOpen()
    {
        return this.channel != null && this.channel.isOpen();
    }

    public boolean hasNoChannel()
    {
        return this.channel == null;
    }

    /**
     * Gets the current handler for processing packets
     */
    public INetHandler getNetHandler()
    {
        return this.packetListener;
    }

    /**
     * If this channel is closed, returns the exit message, null otherwise.
     */
    public ITextComponent getExitMessage()
    {
        return this.terminationReason;
    }

    /**
     * Switches the channel to manual reading modus
     */
    public void disableAutoRead()
    {
        this.channel.config().setAutoRead(false);
    }

    public void setCompressionThreshold(int threshold)
    {
        if (threshold >= 0)
        {
            if (this.channel.pipeline().get("decompress") instanceof NettyCompressionDecoder)
            {
                ((NettyCompressionDecoder)this.channel.pipeline().get("decompress")).setCompressionThreshold(threshold);
            }
            else
            {
                this.channel.pipeline().addBefore("decoder", "decompress", new NettyCompressionDecoder(threshold));
            }

            if (this.channel.pipeline().get("compress") instanceof NettyCompressionEncoder)
            {
                ((NettyCompressionEncoder)this.channel.pipeline().get("compress")).setCompressionThreshold(threshold);
            }
            else
            {
                this.channel.pipeline().addBefore("encoder", "compress", new NettyCompressionEncoder(threshold));
            }
        }
        else
        {
            if (this.channel.pipeline().get("decompress") instanceof NettyCompressionDecoder)
            {
                this.channel.pipeline().remove("decompress");
            }

            if (this.channel.pipeline().get("compress") instanceof NettyCompressionEncoder)
            {
                this.channel.pipeline().remove("compress");
            }
        }
        this.channel.pipeline().fireUserEventTriggered(new CompressionReorderEvent());
    }

    public void handleDisconnection()
    {
        if (this.channel != null && !this.channel.isOpen())
        {
            if (this.getExitMessage() != null)
            {
                final var event = new ServerDisconnectedEvent(this.getExitMessage());
                Southside.eventBus.post(event);
                if (event.isCancelled()) return;
            }

            if (this.disconnected)
            {
                LOGGER.warn("handleDisconnection() called twice");
            }
            else
            {
                this.disconnected = true;

                if (this.getExitMessage() != null)
                {
                    this.getNetHandler().onDisconnect(this.getExitMessage());
                }
                else if (this.getNetHandler() != null)
                {
                    this.getNetHandler().onDisconnect(new TextComponentTranslation("multiplayer.disconnect.generic", new Object[0]));
                }
            }
        }
    }

    static class InboundHandlerTuplePacketListener
    {
        private final Packet<?> packet;
        private final GenericFutureListener <? extends Future <? super Void >> [] futureListeners;

        public InboundHandlerTuplePacketListener(Packet<?> inPacket, GenericFutureListener <? extends Future <? super Void >> ... inFutureListeners)
        {
            this.packet = inPacket;
            this.futureListeners = inFutureListeners;
        }
    }
}
