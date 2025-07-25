//#if MC<=11202
package cc.polyfrost.oneconfig.utils.commands.arguments;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The old player argument parser. Returns an {@link EntityPlayer}.
 * @deprecated Use {@link PlayerArgumentParser} instead.
 */
@Deprecated
public class EntityPlayerArgumentParser extends ArgumentParser<EntityPlayer> {
    @Nullable
    @Override
    public EntityPlayer parse(@NotNull String arg) {
//        Deprecator.markDeprecated();
        List<EntityPlayer> matchingPlayers = getMatchingPlayers(arg, false);
        for (EntityPlayer profile : matchingPlayers) {
            return profile;
        }
        throw new IllegalArgumentException("Player not found");
    }
    // This only returns players in tab list that match, not all players in the current server, hence why this is deprecated.
    private static List<EntityPlayer> getMatchingPlayers(String arg, boolean startsWith) {
        if (Minecraft.getMinecraft().world == null) return Lists.newArrayList();
        return Minecraft.getMinecraft().world.getPlayers(EntityPlayer.class,
                player -> (startsWith ? player.getName().startsWith(arg) : player.getName().equals(arg)));
    }

    @NotNull
    @Override
    public List<String> complete(String current, Parameter parameter) {
        return getMatchingPlayers(current, true).stream().map(EntityPlayer::getName).collect(Collectors.toList());
    }
}
//#endif
