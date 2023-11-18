package baritone.pathing.movement.movements;

import baritone.Baritone;
import baritone.api.IBaritone;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.RotationUtils;
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
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import java.util.Set;

public class MovementDescend extends Movement {

    private int numTicks = 0;
    public boolean forceSafeMode = false;

    public MovementDescend(IBaritone baritone, BetterBlockPos from, BetterBlockPos to) {
        super(baritone, from, to);
    }

    @Override
    public void reset() {
        super.reset();
        numTicks = 0;
        forceSafeMode = false;
    }

    public void forceSafeMode() {
        forceSafeMode = true;
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
        return ImmutableSet.of(src, dest.up(), dest);
    }

    public static void cost(CalculationContext context, int x, int y, int z, int destX, int destZ, MutableMoveResult res) {
        IBlockState destDown = context.get(destX, y - 1, destZ);
        if (!MovementHelper.canWalkThrough(context, destX, y - 1, z, destDown)) {
            return;
        }

        if (!MovementHelper.canWalkThrough(context, destX, y, destZ)) {
            return;
        }

        if (!MovementHelper.canWalkThrough(context, destX, y + 1, destZ)) {
            return;
        }

        Block fromDown = context.get(x, y - 1, z).getBlock();
        if (fromDown == Blocks.ladder || fromDown == Blocks.vine) {
            return;
        }

        IBlockState below = context.get(destX, y - 2, destZ);
        if (MovementHelper.canWalkThrough(context, destX, y - 2, destZ, below)) {
            dynamicFallCost(context, x, y, z, destX, destZ, below, res);
            return;
        }

        if (destDown.getBlock() == Blocks.ladder || destDown.getBlock() == Blocks.vine) {
            return;
        }

        double cost = WALK_OFF_BLOCK_COST;
        if (fromDown == Blocks.soul_sand) {
            cost *= WALK_ONE_OVER_SOUL_SAND_COST / WALK_ONE_BLOCK_COST;
        }
        cost += Math.max(FALL_N_BLOCKS_COST[1], CENTER_AFTER_FALL_COST);
        res.x = destX;
        res.y = y - 1;
        res.z = destZ;
        res.cost = cost;
    }

    public static void dynamicFallCost(CalculationContext context, int x, int y, int z, int destX, int destZ, IBlockState below, MutableMoveResult res) {
        if (!MovementHelper.canWalkThrough(context, destX, y - 2, destZ, below)) {
            return;
        }
        double cost = 0;
        int effectiveStartHeight = y;
        for (int fallHeight = 3; true; fallHeight++) {
            int newY = y - fallHeight;
            if (newY < 0) {
                return;
            }
            IBlockState ontoBlock = context.get(destX, newY, destZ);
            int unprotectedFallHeight = fallHeight - (y - effectiveStartHeight);
            double tentativeCost = WALK_OFF_BLOCK_COST + FALL_N_BLOCKS_COST[unprotectedFallHeight] + cost;
            if (MovementHelper.isWater(ontoBlock.getBlock())) {
                if (!MovementHelper.canWalkThrough(context, destX, newY, destZ, ontoBlock)) {
                    return;
                }
                if (context.assumeWalkOnWater) {
                    return;
                }
                if (MovementHelper.isFlowing(destX, newY, destZ, ontoBlock, context.bsi)) {
                    return;
                }
                if (!MovementHelper.canWalkOn(context, destX, newY - 1, destZ)) {
                    return;
                }

                res.x = destX;
                res.y = newY;
                res.z = destZ;
                res.cost = tentativeCost;
                return;
            }

            if (unprotectedFallHeight <= 11 && (ontoBlock.getBlock() == Blocks.vine || ontoBlock.getBlock() == Blocks.ladder)) {
                cost += FALL_N_BLOCKS_COST[unprotectedFallHeight - 1];
                cost += LADDER_DOWN_ONE_COST;
                effectiveStartHeight = newY;
                continue;
            }
            if (MovementHelper.canWalkThrough(context, destX, newY, destZ, ontoBlock)) {
                continue;
            }

            if (!MovementHelper.canWalkOn(context, destX, newY, destZ, ontoBlock)) {
                return;
            }

            if (MovementHelper.isBottomSlab(ontoBlock)) {
                return;
            }

            if (unprotectedFallHeight <= context.maxFallHeightNoWater + 1) {
                res.x = destX;
                res.y = newY + 1;
                res.z = destZ;
                res.cost = tentativeCost;
            }
            return;
        }
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        BlockPos playerFeet = ctx.playerFeet();
        BlockPos fakeDest = new BlockPos(dest.getX() * 2 - src.getX(), dest.getY(), dest.getZ() * 2 - src.getZ());
        if ((playerFeet.equals(dest) || playerFeet.equals(fakeDest)) && (MovementHelper.isLiquid(ctx, dest) || ctx.playerFeetAsVec().yCoord - dest.getY() < 0.5)) {
            return state.setStatus(MovementStatus.SUCCESS);
        }

        if (safeMode()) {
            double destX = (src.getX() + 0.5) * 0.17 + (dest.getX() + 0.5) * 0.83;
            double destZ = (src.getZ() + 0.5) * 0.17 + (dest.getZ() + 0.5) * 0.83;

            Vec3 destCenter = new Vec3(destX, dest.getY(), destZ);
            MovementHelper.rotate(ctx, state, src, destCenter);
            MovementHelper.setInputs(ctx, state, destCenter);

            //TODO:  Move towards destX, dest.getY(), destZ
            return state;
        }

        double diffX = ctx.playerFeetAsVec().xCoord - (dest.getX() + 0.5);
        double diffZ = ctx.playerFeetAsVec().zCoord - (dest.getZ() + 0.5);
        double ab = Math.sqrt(diffX * diffX + diffZ * diffZ);
        double x = ctx.playerFeetAsVec().xCoord - (src.getX() + 0.5);
        double z = ctx.playerFeetAsVec().zCoord - (src.getZ() + 0.5);
        double fromStart = Math.sqrt(x * x + z * z);
        if (!playerFeet.equals(dest) || ab > 0.25) {
            if (numTicks++ < 20 && fromStart < 1.25) {
                Vec3 destCenter = VecUtils.getBlockPosCenter(fakeDest);
                MovementHelper.rotate(ctx, state, src, destCenter);
                MovementHelper.setInputs(ctx, state, destCenter);
                //TODO:  move towards fake dest
            } else {
                Vec3 destCenter = VecUtils.getBlockPosCenter(dest);
                MovementHelper.rotate(ctx, state, src, destCenter);
                MovementHelper.setInputs(ctx, state, destCenter);
                //TODO:  move towards dest
            }
        }

        return state;
    }

    public boolean safeMode() {
        if (forceSafeMode) {
            return true;
        }
        BlockPos into = dest.subtract(src.down()).add(dest);
        if (skipToAscend()) {
            return true;
        }
        for (int y = 0; y <= 2; y++) {
            if (MovementHelper.avoidWalkingInto(BlockStateInterface.getBlock(ctx, into.up(y)))) {
                return true;
            }
        }
        return false;
    }

    public boolean skipToAscend() {
        BlockPos into = dest.subtract(src.down()).add(dest);
        return !MovementHelper.canWalkThrough(ctx, new BetterBlockPos(into)) && MovementHelper.canWalkThrough(ctx, new BetterBlockPos(into).up()) && MovementHelper.canWalkThrough(ctx, new BetterBlockPos(into).up(2));
    }
}
