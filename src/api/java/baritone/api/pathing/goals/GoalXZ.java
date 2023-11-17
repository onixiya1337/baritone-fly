package baritone.api.pathing.goals;

import baritone.api.BaritoneAPI;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.SettingsUtil;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class GoalXZ implements Goal {

    private static final double SQRT_2 = Math.sqrt(2);

    private final int x;

    private final int z;

    public GoalXZ(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public GoalXZ(BetterBlockPos pos) {
        this.x = pos.x;
        this.z = pos.z;
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        return x == this.x && z == this.z;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        int xDiff = x - this.x;
        int zDiff = z - this.z;
        return calculate(xDiff, zDiff);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GoalXZ goal = (GoalXZ) o;
        return x == goal.x && z == goal.z;
    }

    @Override
    public String toString() {
        return String.format(
                "GoalXZ{x=%s,z=%s}",
                SettingsUtil.maybeCensor(x),
                SettingsUtil.maybeCensor(z)
        );
    }

    public static double calculate(double xDiff, double zDiff) {
        double x = Math.abs(xDiff);
        double z = Math.abs(zDiff);
        double straight;
        double diagonal;
        if (x < z) {
            straight = z - x;
            diagonal = x;
        } else {
            straight = x - z;
            diagonal = z;
        }
        diagonal *= SQRT_2;
        return (diagonal + straight) * BaritoneAPI.getSettings().costHeuristic.value;
    }

    public static GoalXZ fromDirection(Vec3 origin, float yaw, double distance) {
        float theta = (float) Math.toRadians(yaw);
        double x = origin.xCoord - MathHelper.sin(theta) * distance;
        double z = origin.zCoord + MathHelper.cos(theta) * distance;
        return new GoalXZ(MathHelper.floor_double(x), MathHelper.floor_double(z));
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }
}
