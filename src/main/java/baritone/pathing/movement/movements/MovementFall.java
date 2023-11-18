package baritone.pathing.movement.movements;

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
import baritone.utils.pathing.MutableMoveResult;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.MaterialLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class MovementFall extends Movement {

    public MovementFall(IBaritone baritone, BetterBlockPos src, BetterBlockPos dest) {
        super(baritone, src, dest);
    }

    @Override
    public double calculateCost(CalculationContext context) {
        MutableMoveResult result = new MutableMoveResult();
        MovementDescend.cost(context, src.x, src.y, src.z, dest.x, dest.z, result);
        if (result.y != dest.y) {
            return COST_INF;
        }
        return result.cost;
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        Set<BetterBlockPos> set = new HashSet<>();
        set.add(src);
        for (int y = src.y - dest.y; y >= 0; y--) {
            set.add(dest.up(y));
        }
        return set;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        BlockPos playerFeet = ctx.playerFeet();

        state.setTarget(new MovementState.MovementTarget(
                RotationUtils.calcRotationFromVec3d(ctx.playerHead(),
                        VecUtils.getBlockPosCenter(dest),
                        ctx.playerRotations()),
                false));

        IBlockState destState = ctx.world().getBlockState(dest);
        Block destBlock = destState.getBlock();
        boolean isWater = MovementHelper.isWater(destBlock);
        if (playerFeet.equals(dest) && (ctx.playerFeetAsVec().yCoord - playerFeet.getY() < 0.094 || isWater)) {
            if (isWater) {
                if (ctx.playerMotion().yCoord >= 0) {
                    return state.setStatus(MovementStatus.SUCCESS);
                }
            } else {
                return state.setStatus(MovementStatus.SUCCESS);
            }
        }

        Vec3 destCenter = VecUtils.getBlockPosCenter(dest);
        if (Math.abs(ctx.playerFeetAsVec().xCoord + ctx.playerMotion().xCoord - destCenter.xCoord) > 0.1 ||
                Math.abs(ctx.playerFeetAsVec().zCoord + ctx.playerMotion().zCoord - destCenter.zCoord) > 0.1) {
            if (!ctx.player().onGround && Math.abs(ctx.playerMotion().yCoord) > 0.4) {
                state.setInput(Input.SNEAK, true);
            }

            MovementHelper.setInputs(ctx, state, destCenter, getValidPositions());
            //TODO:  Move towards (only keys) dest
        }
        Vec3i avoid = Optional.ofNullable(avoid()).map(EnumFacing::getDirectionVec).orElse(null);
        if (avoid == null) {
            avoid = src.subtract(dest);
        } else {
            double dist = Math.abs(avoid.getX() * (destCenter.xCoord - avoid.getX() / 2.0 - ctx.playerFeetAsVec().xCoord)) + Math.abs(avoid.getZ() * (destCenter.zCoord - avoid.getZ() / 2.0 - ctx.playerFeetAsVec().zCoord));
            if (dist < 0.6) {
                MovementHelper.setInputs(ctx, state, destCenter, getValidPositions());
                //TODO:  move towards (only keys) dest
            } else if (!ctx.player().onGround) {
                state.setInput(Input.SNEAK, false);
            }
        }

        Vec3 destCenterOffset = new Vec3(destCenter.xCoord + 0.125 * avoid.getX(), destCenter.yCoord, destCenter.zCoord + 0.125 * avoid.getZ());
        MovementHelper.rotate(ctx, state, src, destCenterOffset);
        MovementHelper.setInputs(ctx, state, destCenterOffset, getValidPositions());
        //TODO:  move towards destCenterOffset

        return state;
    }

    private EnumFacing avoid() {
        for (int i = 0; i < 15; i++) {
            IBlockState state = ctx.world().getBlockState(ctx.playerFeet().down(i));
            if (state.getBlock() == Blocks.ladder) {
                return state.getValue(BlockLadder.FACING);
            }
        }
        return null;
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        return ctx.playerFeet().equals(src) || state.getStatus() != MovementStatus.RUNNING;
    }
}
