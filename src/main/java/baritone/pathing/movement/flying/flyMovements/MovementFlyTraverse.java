package baritone.pathing.movement.flying.flyMovements;

import baritone.Baritone;
import baritone.api.IBaritone;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.BetterBlockPos;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;

import java.util.Set;

public class MovementFlyTraverse extends Movement {

    public MovementFlyTraverse(IBaritone baritone, BetterBlockPos from, BetterBlockPos to) {
        super(baritone, from, to);
    }

    @Override
    public void reset() {
        super.reset();
    }

    @Override
    public double calculateCost(CalculationContext context) {
        return cost(context, src.x, src.y, src.z, dest.x, dest.z);
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        return ImmutableSet.of(src, dest);
    }

    public static double cost(CalculationContext context, int x, int y, int z, int destX, int destZ) {
        IBlockState pb0 = context.get(destX, y + 1, destZ);
        IBlockState pb1 = context.get(destX, y, destZ);

        if (!MovementHelper.canWalkThrough(context, destX, y, destZ, pb1) && !MovementHelper.isWater(pb1.getBlock())) {
            return COST_INF;
        }

        if (!MovementHelper.canWalkThrough(context, destX, y + 1, destZ, pb0) && !MovementHelper.isWater(pb0.getBlock())) {
            return COST_INF;
        }

        if (!MovementHelper.canWalkThrough(context, destX, y - 1, destZ)) {
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

        BlockPos feet = ctx.playerFeet();
        if (Baritone.settings().overshootTraverse.value && (feet.equals(dest.add(getDirection())) || feet.equals(dest.add(getDirection()).add(getDirection())))) {
            return state.setStatus(MovementStatus.SUCCESS);
        }
        if (feet.equals(dest)) {
            return state.setStatus(MovementStatus.SUCCESS);
        }

        return state;
    }

    @Override
    protected boolean prepared(MovementState state) {
        return true;
    }
}
