package baritone.api.pathing.goals;

import net.minecraft.util.BlockPos;

public interface Goal {

    boolean isInGoal(int x, int y, int z);

    double heuristic(int x, int y, int z);

    default boolean isInGoal(BlockPos pos) {
        return isInGoal(pos.getX(), pos.getY(), pos.getZ());
    }

    default double heuristic(BlockPos pos) {
        return heuristic(pos.getX(), pos.getY(), pos.getZ());
    }

    default double heuristic() {
        return 0;
    }
}
