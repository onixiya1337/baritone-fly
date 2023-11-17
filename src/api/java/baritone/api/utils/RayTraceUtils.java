package baritone.api.utils;

import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public final class RayTraceUtils {

    private RayTraceUtils() {}

    public static MovingObjectPosition rayTraceTowards(Entity entity, Rotation rotation, double blockReachDistance) {
        return rayTraceTowards(entity, rotation, blockReachDistance, false);
    }

    public static MovingObjectPosition rayTraceTowards(Entity entity, Rotation rotation, double blockReachDistance, boolean wouldSneak) {
        Vec3 start;
        if (wouldSneak) {
            start = inferSneakingEyePosition(entity);
        } else {
            start = entity.getPositionEyes(1.0F);
        }
        Vec3 direction = RotationUtils.calcVec3dFromRotation(rotation);
        Vec3 end = start.addVector(
                direction.xCoord * blockReachDistance,
                direction.yCoord * blockReachDistance,
                direction.zCoord * blockReachDistance
        );
        return entity.worldObj.rayTraceBlocks(start, end, false, false, true);
    }

    public static Vec3 inferSneakingEyePosition(Entity entity) {
        return new Vec3(entity.posX, entity.posY + IPlayerContext.eyeHeight(true), entity.posZ);
    }
}
