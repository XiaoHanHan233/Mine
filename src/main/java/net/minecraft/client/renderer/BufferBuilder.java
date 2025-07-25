package net.minecraft.client.renderer;

import com.google.common.primitives.Floats;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;

import dev.diona.southside.Southside;
import dev.diona.southside.module.modules.render.Xray;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.optifine.Config;
import net.optifine.SmartAnimations;
import net.optifine.render.RenderEnv;
import net.optifine.shaders.SVertexBuilder;
import net.optifine.util.TextureUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjglx.opengl.GL11;

public class BufferBuilder
{
    private static final Logger LOGGER = LogManager.getLogger();
    private ByteBuffer byteBuffer;
    public IntBuffer rawIntBuffer;
    private ShortBuffer rawShortBuffer;
    public FloatBuffer rawFloatBuffer;
    public int vertexCount;
    private VertexFormatElement vertexFormatElement;
    private int vertexFormatIndex;

    /** None */
    private boolean noColor;
    public int drawMode;
    private double xOffset;
    private double yOffset;
    private double zOffset;
    private VertexFormat vertexFormat;
    private boolean isDrawing;
    private BlockRenderLayer blockLayer = null;
    private boolean[] drawnIcons = new boolean[256];
    private TextureAtlasSprite[] quadSprites = null;
    private TextureAtlasSprite[] quadSpritesPrev = null;
    private TextureAtlasSprite quadSprite = null;
    public SVertexBuilder sVertexBuilder;
    public RenderEnv renderEnv = null;
    public BitSet animatedSprites = null;
    public BitSet animatedSpritesCached = new BitSet();
    private boolean modeTriangles = false;
    private ByteBuffer byteBufferTriangles;

    public BufferBuilder(int bufferSizeIn)
    {
        this.byteBuffer = GLAllocation.createDirectByteBuffer(bufferSizeIn * 4);
        this.rawIntBuffer = this.byteBuffer.asIntBuffer();
        this.rawShortBuffer = this.byteBuffer.asShortBuffer();
        this.rawFloatBuffer = this.byteBuffer.asFloatBuffer();
        SVertexBuilder.initVertexBuilder(this);
    }

    private void growBuffer(int increaseAmount)
    {
        if (MathHelper.roundUp(increaseAmount, 4) / 4 > this.rawIntBuffer.remaining() || this.vertexCount * this.vertexFormat.getSize() + increaseAmount > this.byteBuffer.capacity())
        {
            int i = this.byteBuffer.capacity();
            int j = i + MathHelper.roundUp(increaseAmount, 2097152);
            LOGGER.debug("Needed to grow BufferBuilder buffer: Old size {} bytes, new size {} bytes.", Integer.valueOf(i), Integer.valueOf(j));
            int k = this.rawIntBuffer.position();
            ByteBuffer bytebuffer = GLAllocation.createDirectByteBuffer(j);
            this.byteBuffer.position(0);
            bytebuffer.put(this.byteBuffer);
            bytebuffer.rewind();
            this.byteBuffer = bytebuffer;
            this.rawFloatBuffer = this.byteBuffer.asFloatBuffer();
            this.rawIntBuffer = this.byteBuffer.asIntBuffer();
            this.rawIntBuffer.position(k);
            this.rawShortBuffer = this.byteBuffer.asShortBuffer();
            this.rawShortBuffer.position(k << 1);

            if (this.quadSprites != null)
            {
                TextureAtlasSprite[] atextureatlassprite = this.quadSprites;
                int l = this.getBufferQuadSize();
                this.quadSprites = new TextureAtlasSprite[l];
                System.arraycopy(atextureatlassprite, 0, this.quadSprites, 0, Math.min(atextureatlassprite.length, this.quadSprites.length));
                this.quadSpritesPrev = null;
            }
        }
    }

    public void sortVertexData(float cameraX, float cameraY, float cameraZ)
    {
        int i = this.vertexCount / 4;
        final float[] afloat = new float[i];

        for (int j = 0; j < i; ++j)
        {
            afloat[j] = getDistanceSq(this.rawFloatBuffer, (float)((double)cameraX + this.xOffset), (float)((double)cameraY + this.yOffset), (float)((double)cameraZ + this.zOffset), this.vertexFormat.getIntegerSize(), j * this.vertexFormat.getSize());
        }

        Integer[] ainteger = new Integer[i];

        for (int k = 0; k < ainteger.length; ++k)
        {
            ainteger[k] = k;
        }

        Arrays.sort(ainteger, new Comparator<Integer>()
        {
            public int compare(Integer p_compare_1_, Integer p_compare_2_)
            {
                return Floats.compare(afloat[p_compare_2_.intValue()], afloat[p_compare_1_.intValue()]);
            }
        });
        BitSet bitset = new BitSet();
        int l = this.vertexFormat.getSize();
        int[] aint = new int[l];

        for (int i1 = bitset.nextClearBit(0); i1 < ainteger.length; i1 = bitset.nextClearBit(i1 + 1))
        {
            int j1 = ainteger[i1].intValue();

            if (j1 != i1)
            {
                this.rawIntBuffer.limit(j1 * l + l);
                this.rawIntBuffer.position(j1 * l);
                this.rawIntBuffer.get(aint);
                int k1 = j1;

                for (int l1 = ainteger[j1].intValue(); k1 != i1; l1 = ainteger[l1].intValue())
                {
                    this.rawIntBuffer.limit(l1 * l + l);
                    this.rawIntBuffer.position(l1 * l);
                    IntBuffer intbuffer = this.rawIntBuffer.slice();
                    this.rawIntBuffer.limit(k1 * l + l);
                    this.rawIntBuffer.position(k1 * l);
                    this.rawIntBuffer.put(intbuffer);
                    bitset.set(k1);
                    k1 = l1;
                }

                this.rawIntBuffer.limit(i1 * l + l);
                this.rawIntBuffer.position(i1 * l);
                this.rawIntBuffer.put(aint);
            }

            bitset.set(i1);
        }

        this.rawIntBuffer.limit(this.rawIntBuffer.capacity());
        this.rawIntBuffer.position(this.getBufferSize());

        if (this.quadSprites != null)
        {
            TextureAtlasSprite[] atextureatlassprite = new TextureAtlasSprite[this.vertexCount / 4];
            int i2 = this.vertexFormat.getSize() / 4 * 4;

            for (int j2 = 0; j2 < ainteger.length; ++j2)
            {
                int k2 = ainteger[j2].intValue();
                atextureatlassprite[j2] = this.quadSprites[k2];
            }

            System.arraycopy(atextureatlassprite, 0, this.quadSprites, 0, atextureatlassprite.length);
        }
    }

    public State getVertexState()
    {
        this.rawIntBuffer.rewind();
        int i = this.getBufferSize();
        this.rawIntBuffer.limit(i);
        int[] aint = new int[i];
        this.rawIntBuffer.get(aint);
        this.rawIntBuffer.limit(this.rawIntBuffer.capacity());
        this.rawIntBuffer.position(i);
        TextureAtlasSprite[] atextureatlassprite = null;

        if (this.quadSprites != null)
        {
            int j = this.vertexCount / 4;
            atextureatlassprite = new TextureAtlasSprite[j];
            System.arraycopy(this.quadSprites, 0, atextureatlassprite, 0, j);
        }

        return new State(aint, new VertexFormat(this.vertexFormat), atextureatlassprite);
    }

    public int getBufferSize()
    {
        return this.vertexCount * this.vertexFormat.getIntegerSize();
    }

    private static float getDistanceSq(FloatBuffer floatBufferIn, float x, float y, float z, int integerSize, int offset)
    {
        float f = floatBufferIn.get(offset + integerSize * 0 + 0);
        float f1 = floatBufferIn.get(offset + integerSize * 0 + 1);
        float f2 = floatBufferIn.get(offset + integerSize * 0 + 2);
        float f3 = floatBufferIn.get(offset + integerSize * 1 + 0);
        float f4 = floatBufferIn.get(offset + integerSize * 1 + 1);
        float f5 = floatBufferIn.get(offset + integerSize * 1 + 2);
        float f6 = floatBufferIn.get(offset + integerSize * 2 + 0);
        float f7 = floatBufferIn.get(offset + integerSize * 2 + 1);
        float f8 = floatBufferIn.get(offset + integerSize * 2 + 2);
        float f9 = floatBufferIn.get(offset + integerSize * 3 + 0);
        float f10 = floatBufferIn.get(offset + integerSize * 3 + 1);
        float f11 = floatBufferIn.get(offset + integerSize * 3 + 2);
        float f12 = (f + f3 + f6 + f9) * 0.25F - x;
        float f13 = (f1 + f4 + f7 + f10) * 0.25F - y;
        float f14 = (f2 + f5 + f8 + f11) * 0.25F - z;
        return f12 * f12 + f13 * f13 + f14 * f14;
    }

    public void setVertexState(State state)
    {
        this.rawIntBuffer.clear();
        this.growBuffer(state.getRawBuffer().length * 4);
        this.rawIntBuffer.put(state.getRawBuffer());
        this.vertexCount = state.getVertexCount();
        this.vertexFormat = new VertexFormat(state.getVertexFormat());

        if (state.stateQuadSprites != null)
        {
            if (this.quadSprites == null)
            {
                this.quadSprites = this.quadSpritesPrev;
            }

            if (this.quadSprites == null || this.quadSprites.length < this.getBufferQuadSize())
            {
                this.quadSprites = new TextureAtlasSprite[this.getBufferQuadSize()];
            }

            TextureAtlasSprite[] atextureatlassprite = state.stateQuadSprites;
            System.arraycopy(atextureatlassprite, 0, this.quadSprites, 0, atextureatlassprite.length);
        }
        else
        {
            if (this.quadSprites != null)
            {
                this.quadSpritesPrev = this.quadSprites;
            }

            this.quadSprites = null;
        }
    }

    public void reset()
    {
        this.vertexCount = 0;
        this.vertexFormatElement = null;
        this.vertexFormatIndex = 0;
        this.quadSprite = null;

        if (SmartAnimations.isActive())
        {
            if (this.animatedSprites == null)
            {
                this.animatedSprites = this.animatedSpritesCached;
            }

            this.animatedSprites.clear();
        }
        else if (this.animatedSprites != null)
        {
            this.animatedSprites = null;
        }

        this.modeTriangles = false;
    }

    public void begin(int glMode, VertexFormat format)
    {
        if (this.isDrawing)
        {
            throw new IllegalStateException("Already building!");
        }
        else
        {
            this.isDrawing = true;
            this.reset();
            this.drawMode = glMode;
            this.vertexFormat = format;
            this.vertexFormatElement = format.getElement(this.vertexFormatIndex);
            this.noColor = false;
            this.byteBuffer.limit(this.byteBuffer.capacity());

            if (Config.isShaders())
            {
                SVertexBuilder.endSetVertexFormat(this);
            }

            if (Config.isMultiTexture())
            {
                if (this.blockLayer != null)
                {
                    if (this.quadSprites == null)
                    {
                        this.quadSprites = this.quadSpritesPrev;
                    }

                    if (this.quadSprites == null || this.quadSprites.length < this.getBufferQuadSize())
                    {
                        this.quadSprites = new TextureAtlasSprite[this.getBufferQuadSize()];
                    }
                }
            }
            else
            {
                if (this.quadSprites != null)
                {
                    this.quadSpritesPrev = this.quadSprites;
                }

                this.quadSprites = null;
            }
        }
    }

    public BufferBuilder tex(double u, double v)
    {
        if (this.quadSprite != null && this.quadSprites != null)
        {
            u = (double)this.quadSprite.toSingleU((float)u);
            v = (double)this.quadSprite.toSingleV((float)v);
            this.quadSprites[this.vertexCount / 4] = this.quadSprite;
        }

        int i = this.vertexCount * this.vertexFormat.getSize() + this.vertexFormat.getOffset(this.vertexFormatIndex);

        switch (this.vertexFormatElement.getType())
        {
            case FLOAT:
                this.byteBuffer.putFloat(i, (float)u);
                this.byteBuffer.putFloat(i + 4, (float)v);
                break;

            case UINT:
            case INT:
                this.byteBuffer.putInt(i, (int)u);
                this.byteBuffer.putInt(i + 4, (int)v);
                break;

            case USHORT:
            case SHORT:
                this.byteBuffer.putShort(i, (short)((int)v));
                this.byteBuffer.putShort(i + 2, (short)((int)u));
                break;

            case UBYTE:
            case BYTE:
                this.byteBuffer.put(i, (byte)((int)v));
                this.byteBuffer.put(i + 1, (byte)((int)u));
        }

        this.nextVertexFormatIndex();
        return this;
    }

    public BufferBuilder lightmap(int skyLight, int blockLight)
    {
        int i = this.vertexCount * this.vertexFormat.getSize() + this.vertexFormat.getOffset(this.vertexFormatIndex);

        switch (this.vertexFormatElement.getType())
        {
            case FLOAT:
                this.byteBuffer.putFloat(i, (float)skyLight);
                this.byteBuffer.putFloat(i + 4, (float)blockLight);
                break;

            case UINT:
            case INT:
                this.byteBuffer.putInt(i, skyLight);
                this.byteBuffer.putInt(i + 4, blockLight);
                break;

            case USHORT:
            case SHORT:
                this.byteBuffer.putShort(i, (short)blockLight);
                this.byteBuffer.putShort(i + 2, (short)skyLight);
                break;

            case UBYTE:
            case BYTE:
                this.byteBuffer.put(i, (byte)blockLight);
                this.byteBuffer.put(i + 1, (byte)skyLight);
        }

        this.nextVertexFormatIndex();
        return this;
    }

    /**
     * Set the brightness for the previously stored quad (4 vertices)
     */
    public void putBrightness4(int vertex0, int vertex1, int vertex2, int vertex3)
    {
        int i = (this.vertexCount - 4) * this.vertexFormat.getIntegerSize() + this.vertexFormat.getUvOffsetById(1) / 4;
        int j = this.vertexFormat.getSize() >> 2;
        this.rawIntBuffer.put(i, vertex0);
        this.rawIntBuffer.put(i + j, vertex1);
        this.rawIntBuffer.put(i + j * 2, vertex2);
        this.rawIntBuffer.put(i + j * 3, vertex3);
    }

    public void putPosition(double x, double y, double z)
    {
        int i = this.vertexFormat.getIntegerSize();
        int j = (this.vertexCount - 4) * i;

        for (int k = 0; k < 4; ++k)
        {
            int l = j + k * i;
            int i1 = l + 1;
            int j1 = i1 + 1;
            this.rawIntBuffer.put(l, Float.floatToRawIntBits((float)(x + this.xOffset) + Float.intBitsToFloat(this.rawIntBuffer.get(l))));
            this.rawIntBuffer.put(i1, Float.floatToRawIntBits((float)(y + this.yOffset) + Float.intBitsToFloat(this.rawIntBuffer.get(i1))));
            this.rawIntBuffer.put(j1, Float.floatToRawIntBits((float)(z + this.zOffset) + Float.intBitsToFloat(this.rawIntBuffer.get(j1))));
        }
    }

    /**
     * Gets the position into the vertex data buffer at which the given vertex's color data can be found, in {@code
     * int}s.
     */
    public int getColorIndex(int vertexIndex)
    {
        return ((this.vertexCount - vertexIndex) * this.vertexFormat.getSize() + this.vertexFormat.getColorOffset()) / 4;
    }

    /**
     * Modify the color data of the given vertex with the given multipliers.
     */
    public void putColorMultiplier(float red, float green, float blue, int vertexIndex)
    {
        int i = this.getColorIndex(vertexIndex);
        int j = -1;

        if (!this.noColor)
        {
            j = this.rawIntBuffer.get(i);

            var xray = (Xray) Southside.moduleManager.getModuleByClass(Xray.class);

            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN)
            {
                int k = (int)((float)(j & 255) * red);
                int l = (int)((float)(j >> 8 & 255) * green);
                int i1 = (int)((float)(j >> 16 & 255) * blue);
                j = j & -16777216;
                j = j | i1 << 16 | l << 8 | k;

                if (xray.isEnabled()) j = buildColor(k, l, i1, Math.round(xray.getWorldOpacity().getValue().floatValue() * 255F));
            }
            else
            {
                int j1 = (int)((float)(j >> 24 & 255) * red);
                int k1 = (int)((float)(j >> 16 & 255) * green);
                int l1 = (int)((float)(j >> 8 & 255) * blue);
                j = j & 255;
                j = j | j1 << 24 | k1 << 16 | l1 << 8;

                if (xray.isEnabled()) j = buildColor(j1, k1, l1, Math.round(xray.getWorldOpacity().getValue().floatValue() * 255F));
            }
        }

        this.rawIntBuffer.put(i, j);
    }

    private int buildColor(int red, int green, int blue, int alpha) {
        int color = MathHelper.clamp(alpha, 0, 255) << 24;
        color |= MathHelper.clamp(red, 0, 255) << 16;
        color |= MathHelper.clamp(green, 0, 255) << 8;
        return color |= MathHelper.clamp(blue, 0, 255);
    }

    private void putColor(int argb, int vertexIndex)
    {
        int i = this.getColorIndex(vertexIndex);
        int j = argb >> 16 & 255;
        int k = argb >> 8 & 255;
        int l = argb & 255;
        this.putColorRGBA(i, j, k, l);
    }

    public void putColorRGB_F(float red, float green, float blue, int vertexIndex)
    {
        int i = this.getColorIndex(vertexIndex);
        int j = MathHelper.clamp((int)(red * 255.0F), 0, 255);
        int k = MathHelper.clamp((int)(green * 255.0F), 0, 255);
        int l = MathHelper.clamp((int)(blue * 255.0F), 0, 255);
        this.putColorRGBA(i, j, k, l);
    }

    /**
     * Write the given color data of 4 bytes at the given index into the vertex data buffer, accounting for system
     * endianness.
     */
    public void putColorRGBA(int index, int red, int green, int blue)
    {
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN)
        {
            this.rawIntBuffer.put(index, -16777216 | blue << 16 | green << 8 | red);
        }
        else
        {
            this.rawIntBuffer.put(index, red << 24 | green << 16 | blue << 8 | 255);
        }
    }

    /**
     * Disables color processing.
     */
    public void noColor()
    {
        this.noColor = true;
    }

    public BufferBuilder color(float red, float green, float blue, float alpha)
    {
        return this.color((int)(red * 255.0F), (int)(green * 255.0F), (int)(blue * 255.0F), (int)(alpha * 255.0F));
    }

    public BufferBuilder color(int red, int green, int blue, int alpha)
    {
        if (this.noColor)
        {
            return this;
        }
        else
        {
            int i = this.vertexCount * this.vertexFormat.getSize() + this.vertexFormat.getOffset(this.vertexFormatIndex);

            switch (this.vertexFormatElement.getType())
            {
                case FLOAT:
                    this.byteBuffer.putFloat(i, (float)red / 255.0F);
                    this.byteBuffer.putFloat(i + 4, (float)green / 255.0F);
                    this.byteBuffer.putFloat(i + 8, (float)blue / 255.0F);
                    this.byteBuffer.putFloat(i + 12, (float)alpha / 255.0F);
                    break;

                case UINT:
                case INT:
                    this.byteBuffer.putFloat(i, (float)red);
                    this.byteBuffer.putFloat(i + 4, (float)green);
                    this.byteBuffer.putFloat(i + 8, (float)blue);
                    this.byteBuffer.putFloat(i + 12, (float)alpha);
                    break;

                case USHORT:
                case SHORT:
                    this.byteBuffer.putShort(i, (short)red);
                    this.byteBuffer.putShort(i + 2, (short)green);
                    this.byteBuffer.putShort(i + 4, (short)blue);
                    this.byteBuffer.putShort(i + 6, (short)alpha);
                    break;

                case UBYTE:
                case BYTE:
                    if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN)
                    {
                        this.byteBuffer.put(i, (byte)red);
                        this.byteBuffer.put(i + 1, (byte)green);
                        this.byteBuffer.put(i + 2, (byte)blue);
                        this.byteBuffer.put(i + 3, (byte)alpha);
                    }
                    else
                    {
                        this.byteBuffer.put(i, (byte)alpha);
                        this.byteBuffer.put(i + 1, (byte)blue);
                        this.byteBuffer.put(i + 2, (byte)green);
                        this.byteBuffer.put(i + 3, (byte)red);
                    }
            }

            this.nextVertexFormatIndex();
            return this;
        }
    }

    public void addVertexData(int[] vertexData)
    {
        if (Config.isShaders())
        {
            SVertexBuilder.beginAddVertexData(this, vertexData);
        }

        this.growBuffer(vertexData.length * 4 + this.vertexFormat.getSize());
        this.rawIntBuffer.position(this.getBufferSize());
        this.rawIntBuffer.put(vertexData);
        this.vertexCount += vertexData.length / this.vertexFormat.getIntegerSize();

        if (Config.isShaders())
        {
            SVertexBuilder.endAddVertexData(this);
        }
    }

    public void endVertex()
    {
        ++this.vertexCount;
        this.growBuffer(this.vertexFormat.getSize());
        this.vertexFormatIndex = 0;
        this.vertexFormatElement = this.vertexFormat.getElement(this.vertexFormatIndex);

        if (Config.isShaders())
        {
            SVertexBuilder.endAddVertex(this);
        }
    }

    public BufferBuilder pos(double x, double y, double z)
    {
        if (Config.isShaders())
        {
            SVertexBuilder.beginAddVertex(this);
        }

        int i = this.vertexCount * this.vertexFormat.getSize() + this.vertexFormat.getOffset(this.vertexFormatIndex);

        switch (this.vertexFormatElement.getType())
        {
            case FLOAT:
                this.byteBuffer.putFloat(i, (float)(x + this.xOffset));
                this.byteBuffer.putFloat(i + 4, (float)(y + this.yOffset));
                this.byteBuffer.putFloat(i + 8, (float)(z + this.zOffset));
                break;

            case UINT:
            case INT:
                this.byteBuffer.putInt(i, Float.floatToRawIntBits((float)(x + this.xOffset)));
                this.byteBuffer.putInt(i + 4, Float.floatToRawIntBits((float)(y + this.yOffset)));
                this.byteBuffer.putInt(i + 8, Float.floatToRawIntBits((float)(z + this.zOffset)));
                break;

            case USHORT:
            case SHORT:
                this.byteBuffer.putShort(i, (short)((int)(x + this.xOffset)));
                this.byteBuffer.putShort(i + 2, (short)((int)(y + this.yOffset)));
                this.byteBuffer.putShort(i + 4, (short)((int)(z + this.zOffset)));
                break;

            case UBYTE:
            case BYTE:
                this.byteBuffer.put(i, (byte)((int)(x + this.xOffset)));
                this.byteBuffer.put(i + 1, (byte)((int)(y + this.yOffset)));
                this.byteBuffer.put(i + 2, (byte)((int)(z + this.zOffset)));
        }

        this.nextVertexFormatIndex();
        return this;
    }

    public void putNormal(float x, float y, float z)
    {
        int i = (byte)((int)(x * 127.0F)) & 255;
        int j = (byte)((int)(y * 127.0F)) & 255;
        int k = (byte)((int)(z * 127.0F)) & 255;
        int l = i | j << 8 | k << 16;
        int i1 = this.vertexFormat.getSize() >> 2;
        int j1 = (this.vertexCount - 4) * i1 + this.vertexFormat.getNormalOffset() / 4;
        this.rawIntBuffer.put(j1, l);
        this.rawIntBuffer.put(j1 + i1, l);
        this.rawIntBuffer.put(j1 + i1 * 2, l);
        this.rawIntBuffer.put(j1 + i1 * 3, l);
    }

    private void nextVertexFormatIndex()
    {
        ++this.vertexFormatIndex;
        this.vertexFormatIndex %= this.vertexFormat.getElementCount();
        this.vertexFormatElement = this.vertexFormat.getElement(this.vertexFormatIndex);

        if (this.vertexFormatElement.getUsage() == VertexFormatElement.EnumUsage.PADDING)
        {
            this.nextVertexFormatIndex();
        }
    }

    public BufferBuilder normal(float x, float y, float z)
    {
        int i = this.vertexCount * this.vertexFormat.getSize() + this.vertexFormat.getOffset(this.vertexFormatIndex);

        switch (this.vertexFormatElement.getType())
        {
            case FLOAT:
                this.byteBuffer.putFloat(i, x);
                this.byteBuffer.putFloat(i + 4, y);
                this.byteBuffer.putFloat(i + 8, z);
                break;

            case UINT:
            case INT:
                this.byteBuffer.putInt(i, (int)x);
                this.byteBuffer.putInt(i + 4, (int)y);
                this.byteBuffer.putInt(i + 8, (int)z);
                break;

            case USHORT:
            case SHORT:
                this.byteBuffer.putShort(i, (short)((int)(x * 32767.0F) & 65535));
                this.byteBuffer.putShort(i + 2, (short)((int)(y * 32767.0F) & 65535));
                this.byteBuffer.putShort(i + 4, (short)((int)(z * 32767.0F) & 65535));
                break;

            case UBYTE:
            case BYTE:
                this.byteBuffer.put(i, (byte)((int)(x * 127.0F) & 255));
                this.byteBuffer.put(i + 1, (byte)((int)(y * 127.0F) & 255));
                this.byteBuffer.put(i + 2, (byte)((int)(z * 127.0F) & 255));
        }

        this.nextVertexFormatIndex();
        return this;
    }

    public void setTranslation(double x, double y, double z)
    {
        this.xOffset = x;
        this.yOffset = y;
        this.zOffset = z;
    }

    public void finishDrawing()
    {
        if (!this.isDrawing)
        {
            throw new IllegalStateException("Not building!");
        }
        else
        {
            this.isDrawing = false;
            this.byteBuffer.position(0);
            this.byteBuffer.limit(this.getBufferSize() * 4);
        }
    }

    public ByteBuffer getByteBuffer()
    {
        return this.modeTriangles ? this.byteBufferTriangles : this.byteBuffer;
    }

    public VertexFormat getVertexFormat()
    {
        return this.vertexFormat;
    }

    public int getVertexCount()
    {
        return this.modeTriangles ? this.vertexCount / 4 * 6 : this.vertexCount;
    }

    public int getDrawMode()
    {
        return this.modeTriangles ? 4 : this.drawMode;
    }

    public void putColor4(int argb)
    {
        for (int i = 0; i < 4; ++i)
        {
            this.putColor(argb, i + 1);
        }
    }

    public void putColorRGB_F4(float red, float green, float blue)
    {
        for (int i = 0; i < 4; ++i)
        {
            this.putColorRGB_F(red, green, blue, i + 1);
        }
    }

    public void putSprite(TextureAtlasSprite p_putSprite_1_)
    {
        if (this.animatedSprites != null && p_putSprite_1_ != null && p_putSprite_1_.getAnimationIndex() >= 0)
        {
            this.animatedSprites.set(p_putSprite_1_.getAnimationIndex());
        }

        if (this.quadSprites != null)
        {
            int i = this.vertexCount / 4;
            this.quadSprites[i - 1] = p_putSprite_1_;
        }
    }

    public void setSprite(TextureAtlasSprite p_setSprite_1_)
    {
        if (this.animatedSprites != null && p_setSprite_1_ != null && p_setSprite_1_.getAnimationIndex() >= 0)
        {
            this.animatedSprites.set(p_setSprite_1_.getAnimationIndex());
        }

        if (this.quadSprites != null)
        {
            this.quadSprite = p_setSprite_1_;
        }
    }

    public boolean isMultiTexture()
    {
        return this.quadSprites != null;
    }

    public void drawMultiTexture()
    {
        if (this.quadSprites != null)
        {
            int i = Config.getMinecraft().getTextureMapBlocks().getCountRegisteredSprites();

            if (this.drawnIcons.length <= i)
            {
                this.drawnIcons = new boolean[i + 1];
            }

            Arrays.fill(this.drawnIcons, false);
            int j = 0;
            int k = -1;
            int l = this.vertexCount / 4;

            for (int i1 = 0; i1 < l; ++i1)
            {
                TextureAtlasSprite textureatlassprite = this.quadSprites[i1];

                if (textureatlassprite != null)
                {
                    int j1 = textureatlassprite.getIndexInMap();

                    if (!this.drawnIcons[j1])
                    {
                        if (textureatlassprite == TextureUtils.iconGrassSideOverlay)
                        {
                            if (k < 0)
                            {
                                k = i1;
                            }
                        }
                        else
                        {
                            i1 = this.drawForIcon(textureatlassprite, i1) - 1;
                            ++j;

                            if (this.blockLayer != BlockRenderLayer.TRANSLUCENT)
                            {
                                this.drawnIcons[j1] = true;
                            }
                        }
                    }
                }
            }

            if (k >= 0)
            {
                this.drawForIcon(TextureUtils.iconGrassSideOverlay, k);
                ++j;
            }

            if (j > 0)
            {
                ;
            }
        }
    }

    private int drawForIcon(TextureAtlasSprite p_drawForIcon_1_, int p_drawForIcon_2_)
    {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, p_drawForIcon_1_.glSpriteTextureId);
        int i = -1;
        int j = -1;
        int k = this.vertexCount / 4;

        for (int l = p_drawForIcon_2_; l < k; ++l)
        {
            TextureAtlasSprite textureatlassprite = this.quadSprites[l];

            if (textureatlassprite == p_drawForIcon_1_)
            {
                if (j < 0)
                {
                    j = l;
                }
            }
            else if (j >= 0)
            {
                this.draw(j, l);

                if (this.blockLayer == BlockRenderLayer.TRANSLUCENT)
                {
                    return l;
                }

                j = -1;

                if (i < 0)
                {
                    i = l;
                }
            }
        }

        if (j >= 0)
        {
            this.draw(j, k);
        }

        if (i < 0)
        {
            i = k;
        }

        return i;
    }

    private void draw(int p_draw_1_, int p_draw_2_)
    {
        int i = p_draw_2_ - p_draw_1_;

        if (i > 0)
        {
            int j = p_draw_1_ * 4;
            int k = i * 4;
            GL11.glDrawArrays(this.drawMode, j, k);
        }
    }

    public void setBlockLayer(BlockRenderLayer p_setBlockLayer_1_)
    {
        this.blockLayer = p_setBlockLayer_1_;

        if (p_setBlockLayer_1_ == null)
        {
            if (this.quadSprites != null)
            {
                this.quadSpritesPrev = this.quadSprites;
            }

            this.quadSprites = null;
            this.quadSprite = null;
        }
    }

    private int getBufferQuadSize()
    {
        int i = this.rawIntBuffer.capacity() * 4 / (this.vertexFormat.getIntegerSize() * 4);
        return i;
    }

    public RenderEnv getRenderEnv(IBlockState p_getRenderEnv_1_, BlockPos p_getRenderEnv_2_)
    {
        if (this.renderEnv == null)
        {
            this.renderEnv = new RenderEnv(p_getRenderEnv_1_, p_getRenderEnv_2_);
            return this.renderEnv;
        }
        else
        {
            this.renderEnv.reset(p_getRenderEnv_1_, p_getRenderEnv_2_);
            return this.renderEnv;
        }
    }

    public boolean isDrawing()
    {
        return this.isDrawing;
    }

    public double getXOffset()
    {
        return this.xOffset;
    }

    public double getYOffset()
    {
        return this.yOffset;
    }

    public double getZOffset()
    {
        return this.zOffset;
    }

    public BlockRenderLayer getBlockLayer()
    {
        return this.blockLayer;
    }

    public void putColorMultiplierRgba(float p_putColorMultiplierRgba_1_, float p_putColorMultiplierRgba_2_, float p_putColorMultiplierRgba_3_, float p_putColorMultiplierRgba_4_, int p_putColorMultiplierRgba_5_)
    {
        int i = this.getColorIndex(p_putColorMultiplierRgba_5_);
        int j = -1;

        if (!this.noColor)
        {
            j = this.rawIntBuffer.get(i);

            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN)
            {
                int k = (int)((float)(j & 255) * p_putColorMultiplierRgba_1_);
                int l = (int)((float)(j >> 8 & 255) * p_putColorMultiplierRgba_2_);
                int i1 = (int)((float)(j >> 16 & 255) * p_putColorMultiplierRgba_3_);
                int j1 = (int)((float)(j >> 24 & 255) * p_putColorMultiplierRgba_4_);
                j = j1 << 24 | i1 << 16 | l << 8 | k;
            }
            else
            {
                int k1 = (int)((float)(j >> 24 & 255) * p_putColorMultiplierRgba_1_);
                int l1 = (int)((float)(j >> 16 & 255) * p_putColorMultiplierRgba_2_);
                int i2 = (int)((float)(j >> 8 & 255) * p_putColorMultiplierRgba_3_);
                int j2 = (int)((float)(j & 255) * p_putColorMultiplierRgba_4_);
                j = k1 << 24 | l1 << 16 | i2 << 8 | j2;
            }
        }

        this.rawIntBuffer.put(i, j);
    }

    public void quadsToTriangles()
    {
        if (this.drawMode == 7)
        {
            if (this.byteBufferTriangles == null)
            {
                this.byteBufferTriangles = GLAllocation.createDirectByteBuffer(this.byteBuffer.capacity() * 2);
            }

            if (this.byteBufferTriangles.capacity() < this.byteBuffer.capacity() * 2)
            {
                this.byteBufferTriangles = GLAllocation.createDirectByteBuffer(this.byteBuffer.capacity() * 2);
            }

            int i = this.vertexFormat.getSize();
            int j = this.byteBuffer.limit();
            this.byteBuffer.rewind();
            this.byteBufferTriangles.clear();

            for (int k = 0; k < this.vertexCount; k += 4)
            {
                this.byteBuffer.limit((k + 3) * i);
                this.byteBuffer.position(k * i);
                this.byteBufferTriangles.put(this.byteBuffer);
                this.byteBuffer.limit((k + 1) * i);
                this.byteBuffer.position(k * i);
                this.byteBufferTriangles.put(this.byteBuffer);
                this.byteBuffer.limit((k + 2 + 2) * i);
                this.byteBuffer.position((k + 2) * i);
                this.byteBufferTriangles.put(this.byteBuffer);
            }

            this.byteBuffer.limit(j);
            this.byteBuffer.rewind();
            this.byteBufferTriangles.flip();
            this.modeTriangles = true;
        }
    }

    public void putColorRGBA(int p_putColorRGBA_1_, int p_putColorRGBA_2_, int p_putColorRGBA_3_, int p_putColorRGBA_4_, int p_putColorRGBA_5_)
    {
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN)
        {
            this.rawIntBuffer.put(p_putColorRGBA_1_, p_putColorRGBA_5_ << 24 | p_putColorRGBA_4_ << 16 | p_putColorRGBA_3_ << 8 | p_putColorRGBA_2_);
        }
        else
        {
            this.rawIntBuffer.put(p_putColorRGBA_1_, p_putColorRGBA_2_ << 24 | p_putColorRGBA_3_ << 16 | p_putColorRGBA_4_ << 8 | p_putColorRGBA_5_);
        }
    }

    public boolean isColorDisabled()
    {
        return this.noColor;
    }

    public void putBulkData(ByteBuffer p_putBulkData_1_)
    {
        if (Config.isShaders())
        {
            SVertexBuilder.beginAddVertexData(this, p_putBulkData_1_);
        }

        this.growBuffer(p_putBulkData_1_.limit() + this.vertexFormat.getSize());
        this.byteBuffer.position(this.vertexCount * this.vertexFormat.getSize());
        this.byteBuffer.put(p_putBulkData_1_);
        this.vertexCount += p_putBulkData_1_.limit() / this.vertexFormat.getSize();

        if (Config.isShaders())
        {
            SVertexBuilder.endAddVertexData(this);
        }
    }

    public class State
    {
        private final int[] stateRawBuffer;
        private final VertexFormat stateVertexFormat;
        private TextureAtlasSprite[] stateQuadSprites;

        public State(int[] p_i4_2_, VertexFormat p_i4_3_, TextureAtlasSprite[] p_i4_4_)
        {
            this.stateRawBuffer = p_i4_2_;
            this.stateVertexFormat = p_i4_3_;
            this.stateQuadSprites = p_i4_4_;
        }

        public State(int[] buffer, VertexFormat format)
        {
            this.stateRawBuffer = buffer;
            this.stateVertexFormat = format;
        }

        public int[] getRawBuffer()
        {
            return this.stateRawBuffer;
        }

        public int getVertexCount()
        {
            return this.stateRawBuffer.length / this.stateVertexFormat.getIntegerSize();
        }

        public VertexFormat getVertexFormat()
        {
            return this.stateVertexFormat;
        }
    }
}
