package baritone.pathing.calc;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.movement.ActionCosts;
import baritone.api.utils.BetterBlockPos;

public final class PathNode {

    public final int x;
    public final int y;
    public final int z;

    public final double estimatedCostToGoal;

    public double cost;

    public double combinedCost;

    public PathNode previous;

    public int heapPosition;

    public PathNode(int x, int y, int z, Goal goal) {
        this.previous = null;
        this.cost = ActionCosts.COST_INF;
        this.estimatedCostToGoal = goal.heuristic(x, y, z);
        if (Double.isNaN(estimatedCostToGoal)) {
            throw new IllegalStateException(goal + " calculated implausible heuristic");
        }
        this.heapPosition = -1;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean isOpen() {
        return heapPosition != -1;
    }

    @Override
    public int hashCode() {
        return (int) BetterBlockPos.longHash(x, y, z);
    }

    @Override
    public boolean equals(Object obj) {
        final PathNode other = (PathNode) obj;
        return x == other.x && y == other.y && z == other.z;
    }
}
