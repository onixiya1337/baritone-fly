package baritone.pathing.movement.movements;

import baritone.Baritone;
import baritone.api.IBaritone;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.VecUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.BlockStateInterface;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import java.util.Optional;
import java.util.Set;

public class MovementTraverse extends Movement {

    public MovementTraverse(IBaritone baritone, BetterBlockPos from, BetterBlockPos to) {
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
    public Set<BetterBlockPos> calculateValidPositions() {
        return ImmutableSet.of(src, dest);
    }

    public static double cost(CalculationContext context, int x, int y, int z, int destX, int destZ) {
        IBlockState pb0 = context.get(destX, y + 1, destZ);
        IBlockState pb1 = context.get(destX, y, destZ);
        IBlockState destOn = context.get(destX, y - 1, destZ);
        IBlockState srcDown = context.get(x, y - 1, z);
        Block srcDownBlock = srcDown.getBlock();

        if (MovementHelper.canWalkOn(context, destX, y - 1, destZ, destOn)) {
            double cost = WALK_ONE_BLOCK_COST;
            boolean water = false;
            if (MovementHelper.isWater(pb0.getBlock())) {
                cost = context.waterWalkSpeed;
                water = true;
            } else {
                if (destOn.getBlock() == Blocks.soul_sand) {
                    cost += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
                } else if (destOn.getBlock() == Blocks.water) {
                    cost += context.walkOnWaterOnePenalty;
                }
                if (srcDownBlock == Blocks.soul_sand) {
                    cost += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
                }
            }

            if (!MovementHelper.canWalkThrough(context, destX, y, destZ, pb1)) {
                return COST_INF;
            }

            if (!MovementHelper.canWalkThrough(context, destX, y + 1, destZ, pb0)) {
                return COST_INF;
            }

            if (!water && context.canSprint) {
                cost *= SPRINT_MULTIPLIER;
            }
            return cost;
        } else {
            return COST_INF;
        }
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        Block fd = BlockStateInterface.get(ctx, src.down()).getBlock();
        boolean ladder = fd == Blocks.ladder || fd == Blocks.vine;

        BetterBlockPos destUp = dest.up();

        IBlockState pb0 = BlockStateInterface.get(ctx, destUp);
        IBlockState pb1 = BlockStateInterface.get(ctx, dest);

        if (pb0.getBlock() instanceof BlockDoor || pb1.getBlock() instanceof BlockDoor) {
            boolean notPassable = pb0.getBlock() instanceof BlockDoor && !MovementHelper.isDoorPassable(ctx, src, dest) || pb1.getBlock() instanceof BlockDoor && !MovementHelper.isDoorPassable(ctx, dest, src);
            boolean canOpen = !(Blocks.iron_door.equals(pb0.getBlock()) || Blocks.iron_door.equals(pb1.getBlock()));

            if (notPassable && canOpen) {
                return state.setTarget(new MovementState.MovementTarget(RotationUtils.calcRotationFromVec3d(ctx.playerHead(), VecUtils.calculateBlockCenter(ctx.world(), destUp), ctx.playerRotations()), true))
                        .setInput(Input.CLICK_RIGHT, true);
            }
        }

        if (pb0.getBlock() instanceof BlockFenceGate || pb1.getBlock() instanceof BlockFenceGate) {
            BlockPos blocked = !MovementHelper.isGatePassable(ctx, destUp, src.up()) ? destUp
                    : !MovementHelper.isGatePassable(ctx, dest, src) ? dest
                    : null;
            if (blocked != null) {
                Optional<Rotation> rotation = RotationUtils.reachable(ctx, blocked);
                if (rotation.isPresent()) {
                    return state.setTarget(new MovementState.MovementTarget(rotation.get(), true)).setInput(Input.CLICK_RIGHT, true);
                }
            }
        }

        BlockPos feet = ctx.playerFeet();
        if (feet.getY() != dest.getY() && !ladder) {
            if (feet.getY() < dest.getY()) {
                return state.setInput(Input.JUMP, true);
            }
            return state;
        }

        IBlockState destDown = BlockStateInterface.get(ctx, dest.down());

        if (feet.equals(dest)) {
            return state.setStatus(MovementStatus.SUCCESS);
        }
        if (Baritone.settings().overshootTraverse.value && (feet.equals(dest.add(getDirection())) || feet.equals(dest.add(getDirection()).add(getDirection())))) {
            return state.setStatus(MovementStatus.SUCCESS);
        }

        Block low = BlockStateInterface.get(ctx, src).getBlock();
        Block high = BlockStateInterface.get(ctx, src.up()).getBlock();
        if (ctx.playerFeetAsVec().yCoord > src.y + 0.1D && !ctx.player().onGround && (low == Blocks.vine || low == Blocks.ladder || high == Blocks.vine || high == Blocks.ladder)) {
            return state;
        }

        BlockPos into = dest.subtract(src).add(dest);
        Block intoBelow = BlockStateInterface.getBlock(ctx, into);
        Block intoAbove = BlockStateInterface.getBlock(ctx, into.up());
        if ((!MovementHelper.isLiquid(ctx, feet) || Baritone.settings().sprintInWater.value) && (!MovementHelper.avoidWalkingInto(intoBelow) || MovementHelper.isWater(intoBelow)) && !MovementHelper.avoidWalkingInto(intoAbove)) {
            state.setInput(Input.SPRINT, true);
        }

        BlockPos against = destUp;

        if (feet.getY() != dest.getY() && ladder && (destDown.getBlock() == Blocks.vine || destDown.getBlock() == Blocks.ladder)) {
            against = destDown.getBlock() == Blocks.vine ? MovementPillar.getAgainst(new CalculationContext(baritone), dest.down()) : dest.offset(destDown.getValue(BlockLadder.FACING).getOpposite());
            if (against == null) {
                logDirect("Unable to climb vines. Consider disabling allowVines.");
                return state.setStatus(MovementStatus.UNREACHABLE);
            }
        }

        //TODO: Move towards against

        Vec3 destCenter = VecUtils.getBlockPosCenter(against).subtract(0, 0.5, 0);
        MovementHelper.rotate(ctx, state, destCenter);
        MovementHelper.setInputs(ctx, state, destCenter);

        return state;
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        BetterBlockPos destDown = dest.down();
        return state.getStatus() != MovementStatus.RUNNING || MovementHelper.canWalkOn(new BlockStateInterface(ctx), destDown.x, destDown.y, destDown.z);
    }


    @Override
    protected boolean prepared(MovementState state) {
        return true;
    }
}
