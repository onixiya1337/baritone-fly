package baritone.pathing.movement.movements;

import baritone.api.IBaritone;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.BetterBlockPos;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.BlockStateInterface;
import baritone.utils.pathing.MutableMoveResult;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;

import java.util.HashSet;
import java.util.Set;

public class MovementParkour extends Movement {

    private final EnumFacing direction;
    private final int dist;
    private final boolean ascend;

    private MovementParkour(IBaritone baritone, BetterBlockPos src, int dist, EnumFacing dir, boolean ascend) {
        super(baritone, src, src.offset(dir, dist).up(ascend ? 1 : 0));
        this.direction = dir;
        this.dist = dist;
        this.ascend = ascend;
    }

    public static MovementParkour cost(CalculationContext context, BetterBlockPos src, EnumFacing direction) {
        MutableMoveResult res = new MutableMoveResult();
        cost(context, src.x, src.y, src.z, direction, res);
        int dist = Math.abs(res.x - src.x) + Math.abs(res.z - src.z);
        return new MovementParkour(context.getBaritone(), src, dist, direction, res.y > src.y);
    }

    public static void cost(CalculationContext context, int x, int y, int z, EnumFacing dir, MutableMoveResult res) {
        //TODO
    }


    @Override
    public double calculateCost(CalculationContext context) {
        MutableMoveResult res = new MutableMoveResult();
        cost(context, src.x, src.y, src.z, direction, res);
        if (res.x != dest.x || res.y != dest.y || res.z != dest.z) {
            return COST_INF;
        }
        return res.cost;
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        //TODO
        return new HashSet<>();
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        return state.getStatus() != MovementStatus.RUNNING;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        return state;
    }
}
