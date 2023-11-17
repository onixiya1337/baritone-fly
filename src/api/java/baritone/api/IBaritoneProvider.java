package baritone.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;

import java.util.List;
import java.util.Objects;

public interface IBaritoneProvider {

    IBaritone getPrimaryBaritone();

    List<IBaritone> getAllBaritones();

    default IBaritone getBaritoneForPlayer(EntityPlayerSP player) {
        for (IBaritone baritone : this.getAllBaritones()) {
            if (Objects.equals(player, baritone.getPlayerContext().player())) {
                return baritone;
            }
        }
        return null;
    }

    default IBaritone getBaritoneForMinecraft(Minecraft minecraft) {
        for (IBaritone baritone : this.getAllBaritones()) {
            if (Objects.equals(minecraft, baritone.getPlayerContext().minecraft())) {
                return baritone;
            }
        }
        return null;
    }

    default IBaritone getBaritoneForConnection(NetHandlerPlayClient connection) {
        for (IBaritone baritone : this.getAllBaritones()) {
            final EntityPlayerSP player = baritone.getPlayerContext().player();
            if (player != null && player.sendQueue == connection) {
                return baritone;
            }
        }
        return null;
    }


    IBaritone createBaritone(Minecraft minecraft);

    boolean destroyBaritone(IBaritone baritone);
}
