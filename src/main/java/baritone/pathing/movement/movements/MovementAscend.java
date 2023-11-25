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
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

import java.util.Set;

public class MovementAscend extends Movement {

    public MovementAscend(IBaritone baritone, BetterBlockPos from, BetterBlockPos to) {
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
        BetterBlockPos prior = new BetterBlockPos(src.subtract(getDirection()).up());
        return ImmutableSet.of(src, src.up(), dest, prior, prior.up());
    }

    public static double cost(CalculationContext context, int x, int y, int z, int destX, int destZ) {
        IBlockState destOn = context.get(destX, y, destZ);
        if (!MovementHelper.canWalkOn(context, destX, y, destZ, destOn)) {
            return COST_INF;
        }

        IBlockState srcUp2 = context.get(x, y + 2, z);
        if (!MovementHelper.canWalkThrough(context, x, y + 2, z, srcUp2)) {
            return COST_INF;
        }

        if (!MovementHelper.canWalkThrough(context, destX, y + 1, destZ)) {
            return COST_INF;
        }
        if (!MovementHelper.canWalkThrough(context, destX, y + 2, destZ)) {
            return COST_INF;
        }

        IBlockState srcDown = context.get(x, y - 1, z);
        if (srcDown.getBlock() == Blocks.ladder || srcDown.getBlock() == Blocks.vine) {
            return COST_INF;
        }

        boolean jumpingFromBottomSlab = MovementHelper.isBottomSlab(srcDown);
        boolean jumpingToBottomSlab = MovementHelper.isBottomSlab(destOn);
        if (jumpingFromBottomSlab && !jumpingToBottomSlab) {
            return COST_INF;
        }

        double cost;
        if (jumpingToBottomSlab) {
            if (jumpingFromBottomSlab) {
                cost = Math.max(JUMP_ONE_BLOCK_COST, WALK_ONE_BLOCK_COST);
                cost += context.jumpPenalty;
            } else {
                cost = WALK_ONE_BLOCK_COST;
            }
        } else {
            if (destOn.getBlock() == Blocks.soul_sand) {
                cost = WALK_ONE_OVER_SOUL_SAND_COST;
            } else {
                cost = Math.max(JUMP_ONE_BLOCK_COST, WALK_ONE_BLOCK_COST);
            }
            cost += context.jumpPenalty;
        }

        return cost;
    }

    @Override
    public MovementState updateState(MovementState state) {
        if (ctx.playerFeet().y < src.y) {
            return state.setStatus(MovementStatus.UNREACHABLE);
        }

        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        if (ctx.playerFeet().equals(dest) || ctx.playerFeet().equals(dest.add(getDirection().down()))) {
            return state.setStatus(MovementStatus.SUCCESS);
        }

        Vec3 destCenter = VecUtils.getBlockPosCenter(dest).subtract(0, 0.5, 0);
        Rotation target = RotationUtils.calcRotationFromVec3d(ctx.playerHead(),
                destCenter,
                ctx.playerRotations()).withPitch(ctx.playerRotations().getPitch());

        state.setTarget(new MovementState.MovementTarget(target, false));

        Rotation nextRotation = baritone.getLookBehavior().getAimProcessor().interpolate(ctx.playerRotations(), target);
        float deltaYaw = nextRotation.subtract(target).normalize().getYaw();
        if (Math.abs(deltaYaw) <= Baritone.settings().randomLooking.value + Baritone.settings().randomLooking113.value &&
                MovementHelper.isWater(ctx, src)) {
            state.setInput(Input.MOVE_FORWARD, true);
            return state;
        } else {
            MovementHelper.setInputsAccurate(ctx, state, destCenter);
        }
        //TODO: Move towards dest

        IBlockState jumpingOnto = BlockStateInterface.get(ctx, dest.down());
        if (MovementHelper.isBottomSlab(jumpingOnto) && !MovementHelper.isBottomSlab(BlockStateInterface.get(ctx, src.down()))) {
            return state;
        }

        if (Baritone.settings().assumeStep.value || ctx.playerFeet().equals(src.up())) {
            return state;
        }

        int xAxis = Math.abs(src.getX() - dest.getX());
        int zAxis = Math.abs(src.getZ() - dest.getZ());
        double flatDistToNext = xAxis * Math.abs((dest.getX() + 0.5D) - ctx.playerFeetAsVec().xCoord) + zAxis * Math.abs((dest.getZ() + 0.5D) - ctx.playerFeetAsVec().zCoord);
        double sideDist = zAxis * Math.abs((dest.getX() + 0.5D) - ctx.playerFeetAsVec().xCoord) + xAxis * Math.abs((dest.getZ() + 0.5D) - ctx.playerFeetAsVec().zCoord);

        double lateralMotion = xAxis * ctx.playerMotion().zCoord + zAxis * ctx.playerMotion().xCoord;
        if (Math.abs(lateralMotion) > 0.1) {
            return state;
        }

        if (ctx.playerFeetAsVec().yCoord > src.y + 0.5) {
            return state;
        }

        if (headBonkClear()) {
            return state.setInput(Input.JUMP, true);
        }

        if (flatDistToNext > 1.2 || sideDist > 0.2) {
            return state;
        }

        return state.setInput(Input.JUMP, true);
    }

    public boolean headBonkClear() {
        BetterBlockPos startUp = src.up(2);
        for (int i = 0; i < 4; i++) {
            BetterBlockPos check = startUp.offset(EnumFacing.getHorizontal(i));
            if (!MovementHelper.canWalkThrough(ctx, check)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        return state.getStatus() != MovementStatus.RUNNING;
    }
}
