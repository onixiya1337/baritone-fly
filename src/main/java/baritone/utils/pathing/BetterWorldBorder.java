package baritone.utils.pathing;

import net.minecraft.world.border.WorldBorder;

public class BetterWorldBorder {

    private final double minX;
    private final double maxX;
    private final double minZ;
    private final double maxZ;

    public BetterWorldBorder(WorldBorder border) {
        this.minX = border.minX();
        this.maxX = border.maxX();
        this.minZ = border.minZ();
        this.maxZ = border.maxZ();
    }

    public boolean entirelyContains(int x, int z) {
        return x + 1 > minX && x < maxX && z + 1 > minZ && z < maxZ;
    }

    public boolean canPlaceAt(int x, int z) {
        return x > minX && x + 1 < maxX && z > minZ && z + 1 < maxZ;
    }
}
