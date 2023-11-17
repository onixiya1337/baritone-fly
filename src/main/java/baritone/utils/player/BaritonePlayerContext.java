package baritone.utils.player;

import baritone.Baritone;
import baritone.api.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;

public class BaritonePlayerContext implements IPlayerContext {

    private final Baritone baritone;
    private final Minecraft mc;
    private final IPlayerController playerController;

    public BaritonePlayerContext(Baritone baritone, Minecraft mc) {
        this.baritone = baritone;
        this.mc = mc;
        this.playerController = new BaritonePlayerController(mc);
    }

    @Override
    public Minecraft minecraft() {
        return this.mc;
    }

    @Override
    public EntityPlayerSP player() {
        return this.mc.thePlayer;
    }

    @Override
    public IPlayerController playerController() {
        return this.playerController;
    }

    @Override
    public WorldClient world() {
        return this.mc.theWorld;
    }

    @Override
    public BetterBlockPos viewerPos() {
        final Entity entity = this.mc.getRenderViewEntity();
        return entity == null ? this.playerFeet() : BetterBlockPos.from(new BlockPos(entity));
    }

    @Override
    public Rotation playerRotations() {
        return this.baritone.getLookBehavior().getEffectiveRotation().orElseGet(IPlayerContext.super::playerRotations);
    }

    @Override
    public MovingObjectPosition objectMouseOver() {
        return RayTraceUtils.rayTraceTowards(player(), playerRotations(), playerController().getBlockReachDistance());
    }
}
