package baritone.pathing.movement.movements;

import baritone.api.utils.BetterBlockPos;
import baritone.pathing.movement.CalculationContext;
import net.minecraft.util.BlockPos;

public class MovementPillar {

    public static BlockPos getAgainst(CalculationContext context, BetterBlockPos vine) {
        if (context.get(vine.north()).getBlock().isBlockNormalCube()) {
            return vine.north();
        }
        if (context.get(vine.south()).getBlock().isBlockNormalCube()) {
            return vine.south();
        }
        if (context.get(vine.east()).getBlock().isBlockNormalCube()) {
            return vine.east();
        }
        if (context.get(vine.west()).getBlock().isBlockNormalCube()) {
            return vine.west();
        }
        return null;
    }
}
