package baritone.api.pathing.goals;

import baritone.api.utils.interfaces.IGoalRenderPos;
import baritone.api.utils.SettingsUtil;
import net.minecraft.util.BlockPos;

public class GoalBlock implements Goal, IGoalRenderPos {

    public final int x;

    public final int y;

    public final int z;

    public GoalBlock(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    public GoalBlock(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        return x == this.x && y == this.y && z == this.z;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        int xDiff = x - this.x;
        int yDiff = y - this.y;
        int zDiff = z - this.z;
        return calculate(xDiff, yDiff, zDiff);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GoalBlock goal = (GoalBlock) o;
        return x == goal.x
                && y == goal.y
                && z == goal.z;
    }

    @Override
    public String toString() {
        return String.format(
                "GoalBlock{x=%s,y=%s,z=%s}",
                SettingsUtil.maybeCensor(x),
                SettingsUtil.maybeCensor(y),
                SettingsUtil.maybeCensor(z)
        );
    }

    @Override
    public BlockPos getGoalPos() {
        return new BlockPos(x, y, z);
    }

    public static double calculate(double xDiff, int yDiff, double zDiff) {
        double heuristic = 0;

        heuristic += GoalYLevel.calculate(0, yDiff);

        heuristic += GoalXZ.calculate(xDiff, zDiff);
        return heuristic;
    }
}
