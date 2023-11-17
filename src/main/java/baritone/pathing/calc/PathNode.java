package baritone.pathing.calc;

import baritone.api.utils.BetterBlockPos;

public class PathNode {

    public final int x;
    public final int y;
    public final int z;

    public double hCost;
    public double gCost;
    public double fCost() {
        return hCost + gCost;
    }

    public PathNode parent;

    public PathNode(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public int hashCode() {
        return (int) BetterBlockPos.longHash(x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        final PathNode other = (PathNode) o;
        return x == other.x && y == other.y && z == other.z;
    }

}
