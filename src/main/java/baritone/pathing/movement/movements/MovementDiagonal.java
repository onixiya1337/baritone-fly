package baritone.pathing.movement.movements;

import baritone.Baritone;
import baritone.api.IBaritone;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.VecUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.BlockStateInterface;
import baritone.utils.pathing.MutableMoveResult;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

import java.util.Set;

public class MovementDiagonal extends Movement {

    private static final double SQRT_2 = Math.sqrt(2);

    public MovementDiagonal(IBaritone baritone, BetterBlockPos start, EnumFacing dir1, EnumFacing dir2, int dy) {
        this(baritone, start, start.offset(dir1), dir2, dy);
    }

    public MovementDiagonal(IBaritone baritone, BetterBlockPos start, BetterBlockPos dir1, EnumFacing dir2, int dy) {
        this(baritone, start, dir1.offset(dir2).up(dy));
    }

    public MovementDiagonal(IBaritone baritone, BetterBlockPos start, BetterBlockPos end) {
        super(baritone, start, end);
    }

    @Override
    public void reset() {
        super.reset();
    }

    @Override
    public double calculateCost(CalculationContext context) {
        MutableMoveResult result = new MutableMoveResult();
        cost(context, src.x, src.y, src.z, dest.x, dest.z, result);
        if (result.y != dest.y) {
            return COST_INF;
        }
        return result.cost;
    }

    @Override
    public Set<BetterBlockPos> calculateValidPositions() {
        BetterBlockPos diagA = new BetterBlockPos(src.x, src.y, dest.z);
        BetterBlockPos diagB = new BetterBlockPos(dest.x, src.y, src.z);
        if (dest.y < src.y) {
            return ImmutableSet.of(src, dest.up(), diagA, diagB, dest, diagA.down(), diagB.down());
        }
        if (dest.y > src.y) {
            return ImmutableSet.of(src, src.up(), diagA, diagB, dest, diagA.up(), diagB.up());
        }
        return ImmutableSet.of(src, dest, diagA, diagB);
    }

    public static void cost(CalculationContext context, int x, int y, int z, int destX, int destZ, MutableMoveResult res) {
        if (!MovementHelper.canWalkThrough(context, destX, y + 1, destZ)) {
            return;
        }

        IBlockState destInto = context.get(destX, y, destZ);
        IBlockState fromDown;
        boolean ascend = false;
        IBlockState destWalkOn;
        boolean descend = false;
        if (!MovementHelper.canWalkThrough(context, destX, y, destZ, destInto)) {
            ascend = true;
            if (!context.allowDiagonalAscend || !MovementHelper.canWalkThrough(context, x, y + 2, z) ||
                    !MovementHelper.canWalkOn(context, destX, y, destZ, destInto) || !MovementHelper.canWalkThrough(context, destX, y + 2, destZ)) {
                return;
            }
            destWalkOn = destInto;
            fromDown = context.get(x, y - 1, z);
        } else {
            destWalkOn = context.get(destX, y - 1, destZ);
            fromDown = context.get(x, y - 1, z);
            if (!MovementHelper.canWalkOn(context, destX, y - 1, destZ, destWalkOn)) {
                descend = true;
                if (!context.allowDiagonalDescend || !MovementHelper.canWalkOn(context, destX, y - 2, destZ) ||
                        !MovementHelper.canWalkThrough(context, destX, y - 1, destZ, destWalkOn)) {
                    return;
                }
            }
        }

        double costMultiplier = WALK_ONE_BLOCK_COST;
        if (destWalkOn.getBlock() == Blocks.soul_sand) {
            costMultiplier += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
        } else if (destWalkOn.getBlock() == Blocks.water) {
            costMultiplier += context.walkOnWaterOnePenalty * SQRT_2;
        }

        Block fromDownBlock = fromDown.getBlock();
        if (fromDownBlock == Blocks.ladder || fromDownBlock == Blocks.vine) {
            return;
        }

        if (fromDownBlock == Blocks.soul_sand) {
            costMultiplier += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
        }


        Block cuttingOver1 = context.get(x, y - 1, destZ).getBlock();
        if (MovementHelper.isLava(cuttingOver1)) {
            return;
        }

        Block cuttingOver2 = context.get(destX, y - 1, z).getBlock();
        if (MovementHelper.isLava(cuttingOver2)) {
            return;
        }


        Block startIn = context.getBlock(x, y, z);
        boolean water = false;
        if (MovementHelper.isWater(startIn) || MovementHelper.isWater(destInto.getBlock())) {
            if (ascend) {
                return;
            }

            costMultiplier = context.waterWalkSpeed;
            water = true;
        }

        IBlockState pb0 = context.get(x, y, destZ);
        IBlockState pb2 = context.get(destX, y, z);
        if (ascend) {
            boolean ATop = MovementHelper.canWalkThrough(context, x, y + 2, destZ);
            boolean AMid = MovementHelper.canWalkThrough(context, x, y + 1, destZ);
            boolean ALow = MovementHelper.canWalkThrough(context, x, y, destZ, pb0);
            boolean BTop = MovementHelper.canWalkThrough(context, destX, y + 2, z);
            boolean BMid = MovementHelper.canWalkThrough(context, destX, y + 1, z);
            boolean BLow = MovementHelper.canWalkThrough(context, destX, y, z, pb2);
            if ((!(ATop && AMid && ALow) && !(BTop && BMid && BLow))
                    || MovementHelper.avoidWalkingInto(pb0.getBlock())
                    || MovementHelper.avoidWalkingInto(pb2.getBlock())
                    || (ATop && AMid && MovementHelper.canWalkOn(context, x, y, destZ, pb0))
                    || (BTop && BMid && MovementHelper.canWalkOn(context, destX, y, z, pb2))
                    || (!ATop && AMid && ALow)
                    || (!BTop && BMid && BLow)) {
                return;
            }
            res.cost = costMultiplier * SQRT_2 + JUMP_ONE_BLOCK_COST;
            res.x = destX;
            res.z = destZ;
            res.y = y + 1;
            return;
        }

        boolean optionA = MovementHelper.canWalkThrough(context, x, y, destZ, pb0);
        boolean optionB = MovementHelper.canWalkThrough(context, destX, y, z, pb2);

        if (!optionA && !optionB) {
            return;
        }

        IBlockState pb1 = context.get(x, y + 1, destZ);
        optionA |= MovementHelper.canWalkThrough(context, x, y + 1, destZ, pb1);
        if (!optionA && !optionB) {
            return;
        }

        IBlockState pb3 = context.get(destX, y + 1, z);
        if (optionA && ((MovementHelper.avoidWalkingInto(pb2.getBlock()) && pb2.getBlock() != Blocks.water) || MovementHelper.avoidWalkingInto(pb3.getBlock()))) {
            return;
        }

        optionB |= MovementHelper.canWalkThrough(context, destX, y + 1, z, pb3);
        if (!optionA && !optionB) {
            return;
        }

        if (optionB && ((MovementHelper.avoidWalkingInto(pb0.getBlock()) && pb0.getBlock() != Blocks.water) || MovementHelper.avoidWalkingInto(pb1.getBlock()))) {
            return;
        }

        boolean aSlab = MovementHelper.isBottomSlab(pb0);
        if (optionA && aSlab) {
            return;
        }

        boolean bSlab = MovementHelper.isBottomSlab(pb2);
        if (optionB && bSlab) {
            return;
        }

        if (!optionA || !optionB) {
            costMultiplier *= SQRT_2 + 1;
            if (startIn == Blocks.ladder || startIn == Blocks.vine) {
                return;
            }
        } else {
            if (context.canSprint && !water) {
                costMultiplier *= SPRINT_MULTIPLIER;
            }
        }
        res.cost = costMultiplier * SQRT_2;
        if (descend) {
            res.cost += Math.max(FALL_N_BLOCKS_COST[1], CENTER_AFTER_FALL_COST);
            res.y = y - 1;
        } else {
            res.y = y;
        }
        res.x = destX;
        res.z = destZ;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        if (ctx.playerFeet().equals(dest)) {
            return state.setStatus(MovementStatus.SUCCESS);
        } else if (!playerInValidPosition() && !(MovementHelper.isLiquid(ctx, src) && getValidPositions().contains(ctx.playerFeet().up()))) {
            return state.setStatus(MovementStatus.UNREACHABLE);
        }
        if (dest.y > src.y && ctx.playerFeetAsVec().yCoord < src.y + 0.1 && ctx.player().isCollidedHorizontally) {
            state.setInput(Input.JUMP, true);
        }
        if (sprint()) {
            state.setInput(Input.SPRINT, true);
        }
        Vec3 destCenter = VecUtils.getBlockPosCenter(dest);
        MovementHelper.rotate(ctx, state, src, destCenter);
        MovementHelper.setInputs(ctx, state, destCenter);

        return state;
    }

    private boolean sprint() {
        return !MovementHelper.isLiquid(ctx, ctx.playerFeet()) || Baritone.settings().sprintInWater.value;
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        return state.getStatus() != MovementStatus.RUNNING;
    }

    @Override
    protected boolean prepared(MovementState state) {
        return true;
    }
}
