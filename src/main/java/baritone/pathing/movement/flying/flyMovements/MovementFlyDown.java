package baritone.pathing.movement.flying.flyMovements;

import baritone.api.IBaritone;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.BetterBlockPos;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.state.IBlockState;

import java.util.Set;

public class MovementFlyDown extends Movement {

    public MovementFlyDown(IBaritone baritone, BetterBlockPos start, BetterBlockPos end) {
        super(baritone, start, end);
    }

    @Override
    public double calculateCost(CalculationContext context) {
        return cost(context, src.x, src.y, src.z);
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        return ImmutableSet.of(src, dest);
    }

    public static double cost(CalculationContext context, int x, int y, int z) {
        IBlockState pb0 = context.get(x, y - 1, z);

        if (!MovementHelper.canWalkThrough(context, x, y - 1, z, pb0) && !MovementHelper.isWater(pb0.getBlock())) {
            return COST_INF;
        }

        if (!MovementHelper.canWalkThrough(context, x, y - 2, z)) {
            return 2 * WALK_ONE_BLOCK_COST;
        }

        return WALK_ONE_BLOCK_COST;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        return state;
    }

    @Override
    protected boolean prepared(MovementState state) {
        return true;
    }
}
