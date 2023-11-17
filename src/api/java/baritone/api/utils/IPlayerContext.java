package baritone.api.utils;

import net.minecraft.block.BlockSlab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface IPlayerContext {

    Minecraft minecraft();

    EntityPlayerSP player();

    IPlayerController playerController();

    World world();

    default Iterable<Entity> entities() {
        return world().getLoadedEntityList();
    }

    default Stream<Entity> entitiesStream() {
        return StreamSupport.stream(entities().spliterator(), false);
    }

    MovingObjectPosition objectMouseOver();

    default BetterBlockPos playerFeet() {
        BetterBlockPos feet = new BetterBlockPos(player().posX, player().posY + 0.1251, player().posZ);
        try {
            if (world().getBlockState(feet).getBlock() instanceof BlockSlab) {
                return feet.up();
            }
        } catch (NullPointerException ignored) {}

        return feet;
    }

    default Vec3 playerFeetAsVec() {
        return new Vec3(player().posX, player().posY, player().posZ);
    }

    default Vec3 playerHead() {
        return new Vec3(player().posX, player().posY + player().getEyeHeight(), player().posZ);
    }

    default Vec3 playerMotion() {
        return new Vec3(player().motionX, player().motionY, player().motionZ);
    }

    BetterBlockPos viewerPos();

    default Rotation playerRotations() {
        return new Rotation(player().rotationYaw, player().rotationPitch);
    }

    static double eyeHeight(boolean ifSneaking) {
        return ifSneaking ? 1.54 : 1.62;
    }

    default Optional<BlockPos> getSelectedBlock() {
        MovingObjectPosition result = objectMouseOver();
        if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return Optional.of(result.getBlockPos());
        }
        return Optional.empty();
    }

    default boolean isLookingAt(BlockPos pos) {
        return getSelectedBlock().equals(Optional.of(pos));
    }
}
