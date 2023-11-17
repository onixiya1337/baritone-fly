package baritone.api.utils;

import net.minecraft.block.BlockFire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public final class VecUtils {

    private VecUtils() {}

    public static Vec3 calculateBlockCenter(World world, BlockPos pos) {
        IBlockState b = world.getBlockState(pos);
        AxisAlignedBB bbox = b.getBlock().getCollisionBoundingBox(world, pos, b);
        double xDiff = (bbox.minX + bbox.maxX) / 2;
        double yDiff = (bbox.minY + bbox.maxY) / 2;
        double zDiff = (bbox.minZ + bbox.maxZ) / 2;
        if (b.getBlock() instanceof BlockFire) {
            yDiff = 0;
        }
        return new Vec3(
                pos.getX() + xDiff,
                pos.getY() + yDiff,
                pos.getZ() + zDiff
        );
    }

    public static Vec3 getBlockPosCenter(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public static double distanceToCenter(BlockPos pos, double x, double y, double z) {
        double xdiff = pos.getX() + 0.5 - x;
        double ydiff = pos.getY() + 0.5 - y;
        double zdiff = pos.getZ() + 0.5 - z;
        return Math.sqrt(xdiff * xdiff + ydiff * ydiff + zdiff * zdiff);
    }

    public static double entityDistanceToCenter(Entity entity, BlockPos pos) {
        return distanceToCenter(pos, entity.posX, entity.posY, entity.posZ);
    }

    public static double entityFlatDistanceToCenter(Entity entity, BlockPos pos) {
        return distanceToCenter(pos, entity.posX, pos.getY() + 0.5, entity.posZ);
    }
}
