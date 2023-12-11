package baritone.pathing.path;

import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.movement.ActionCosts;
import baritone.api.pathing.movement.IMovement;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.pathing.path.IPathExecutor;
import baritone.api.utils.*;
import baritone.api.utils.input.Input;
import baritone.behavior.PathingBehavior;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.pathing.movement.movements.*;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static baritone.api.pathing.movement.MovementStatus.*;

public class PathExecutor implements IPathExecutor, Helper {

    private static final double MAX_TICKS_AWAY = 200;

    private final IPath path;
    private int pathPosition;
    private int ticksAway;
    private int ticksOnCurrent;
    private Double currentMovementOriginalCostEstimate;
    private Integer costEstimateIndex;
    private boolean failed;

    private final PathingBehavior behavior;
    private final IPlayerContext ctx;

    private boolean sprintNextTick;

    public PathExecutor(PathingBehavior behavior, IPath path) {
        this.behavior = behavior;
        this.ctx = behavior.ctx;
        this.path = path;
        this.pathPosition = 0;
    }

    public boolean onTick() {
        if (pathPosition == path.length() - 1) {
            pathPosition++;
        }
        if (pathPosition >= path.length()) {
            return true;
        }
        Movement movement = (Movement) path.movements().get(pathPosition);
        BetterBlockPos whereAmI = ctx.playerFeet();
        if (!movement.getValidPositions().contains(whereAmI)) {
            for (int i = 0; i < pathPosition && i < path.length(); i++) {
                if (((Movement) path.movements().get(i)).getValidPositions().contains(whereAmI)) {
                    int previousPos = pathPosition;
                    pathPosition = i;
                    for (int j = pathPosition; j <= previousPos; j++) {
                        path.movements().get(j).reset();
                    }
                    onChangeInPathPosition();
                    onTick();
                    return false;
                }
            }
            for (int i = pathPosition + 3; i < path.length() - 1; i++) {
                if (((Movement) path.movements().get(i)).getValidPositions().contains(whereAmI)) {
                    if (i - pathPosition > 2) {
                        logDebug("Skipping forward " + (i - pathPosition) + " steps, to " + i);
                    }
                    pathPosition = i - 1;
                    onChangeInPathPosition();
                    onTick();
                    return false;
                }
            }
        }
        Tuple<Double, BlockPos> status = closestPathPos(path);

        boolean isInJumpBoost = ctx.player().getActivePotionEffects() != null
                && ctx.player().getActivePotionEffect(Potion.jump) != null
                && ctx.player().getActivePotionEffect(Potion.jump).getAmplifier() > 0 && !ctx.player().onGround;

        if (!isInJumpBoost && possiblyOffPath(status, BaritoneAPI.getSettings().maxDistFromPath.value)) {
            ticksAway++;
            System.out.println("FAR AWAY FROM PATH FOR " + ticksAway + " TICKS. Current distance: " + status.getFirst() + ". Threshold: " +  BaritoneAPI.getSettings().maxDistFromPath.value);
            if (ticksAway > MAX_TICKS_AWAY) {
                logDebug("Too far away from path for too long, cancelling path");
                cancel();
                return false;
            }
        } else {
            ticksAway = 0;
        }
        if (!isInJumpBoost && possiblyOffPath(status, BaritoneAPI.getSettings().maxDistFromPath.value)) {
            logDebug("too far from path");
            cancel();
            return false;
        }

        if (pathPosition < path.movements().size() - 1) {
            IMovement next = path.movements().get(pathPosition + 1);
            if (!behavior.baritone.bsi.worldContainsLoadedChunk(next.getDest().x, next.getDest().z)) {
                logDebug("Pausing since destination is at edge of loaded chunks");
                clearKeys();
                return true;
            }
        }
        boolean canCancel = movement.safeToCancel();
        if (costEstimateIndex == null || costEstimateIndex != pathPosition) {
            costEstimateIndex = pathPosition;
            currentMovementOriginalCostEstimate = movement.getCost();
            for (int i = 1; i < Baritone.settings().costVerificationLookahead.value && pathPosition + i < path.length() - 1; i++) {
                if (((Movement) path.movements().get(pathPosition + i)).calculateCost(behavior.secretInternalGetCalculationContext()) >= ActionCosts.COST_INF && canCancel) {
                    logDebug("Something has changed in the world and a future movement has become impossible. Cancelling.");
                    cancel();
                    return true;
                }
            }
        }
        double currentCost = movement.recalculateCost(behavior.secretInternalGetCalculationContext());
        if (currentCost >= ActionCosts.COST_INF && canCancel) {
            logDebug("Something has changed in the world and this movement has become impossible. Cancelling.");
            cancel();
            return true;
        }
        if (!movement.calculatedWhileLoaded() && currentCost - currentMovementOriginalCostEstimate > Baritone.settings().maxCostIncrease.value && canCancel) {
            logDebug("Original cost " + currentMovementOriginalCostEstimate + " current cost " + currentCost + ". Cancelling.");
            cancel();
            return true;
        }
        if (shouldPause()) {
            logDebug("Pausing since current best path is a backtrack");
            clearKeys();
            return true;
        }
        MovementStatus movementStatus = movement.update();
        if (movementStatus == UNREACHABLE || movementStatus == FAILED) {
            logDebug("Movement returns status " + movementStatus);
            cancel();
            return true;
        }
        if (movementStatus == SUCCESS) {
            pathPosition++;
            onChangeInPathPosition();
            onTick();
            return true;
        } else {
            if (BaritoneAPI.getSettings().safeMode.value && movement instanceof MovementDescend) {
                boolean srcSlab = MovementHelper.isBottomSlab(ctx.world().getBlockState(movement.getSrc().down()));
                boolean destSlab = MovementHelper.isBottomSlab(ctx.world().getBlockState(movement.getDest().down()));
                if (srcSlab == destSlab) {
                    ((MovementDescend) movement).forceSafeMode();
                }
            }
            if (decrease()) {
                MovementHelper.decreaseMotion(behavior.baritone.getInputOverrideHandler(), ctx);
            } else {
                sprintNextTick = shouldSprintNextTick();
                if (!sprintNextTick) {
                    ctx.player().setSprinting(false);
                }
                ticksOnCurrent++;
                if (ticksOnCurrent > currentMovementOriginalCostEstimate + Baritone.settings().movementTimeoutTicks.value) {
                    logDebug("This movement has taken too long (" + ticksOnCurrent + " ticks, expected " + currentMovementOriginalCostEstimate + "). Cancelling.");
                    cancel();
                    return true;
                }
            }
        }
        return canCancel;
    }

    private Tuple<Double, BlockPos> closestPathPos(IPath path) {
        double best = -1;
        BlockPos bestPos = null;
        for (IMovement movement : path.movements()) {
            for (BlockPos pos : ((Movement) movement).getValidPositions()) {
                double dist = VecUtils.entityDistanceToCenter(ctx.player(), pos);
                if (dist < best || best == -1) {
                    best = dist;
                    bestPos = pos;
                }
            }
        }
        return new Tuple<>(best, bestPos);
    }

    private boolean shouldPause() {
        Optional<AbstractNodeCostSearch> current = behavior.getInProgress();
        if (!current.isPresent()) {
            return false;
        }
        if (!ctx.player().onGround) {
            return false;
        }
        if (!MovementHelper.canWalkOn(ctx, ctx.playerFeet().down())) {
            return false;
        }
        if (!MovementHelper.canWalkThrough(ctx, ctx.playerFeet()) || !MovementHelper.canWalkThrough(ctx, ctx.playerFeet().up())) {
            return false;
        }
        if (!path.movements().get(pathPosition).safeToCancel()) {
            return false;
        }
        Optional<IPath> currentBest = current.get().bestPathSoFar();
        if (!currentBest.isPresent()) {
            return false;
        }
        List<BetterBlockPos> positions = currentBest.get().positions();
        if (positions.size() < 3) {
            return false;
        }
        positions = positions.subList(1, positions.size());
        return positions.contains(ctx.playerFeet());
    }

    private boolean possiblyOffPath(Tuple<Double, BlockPos> status, double leniency) {
        double distanceFromPath = status.getFirst();
        if (distanceFromPath > leniency) {
            if (path.movements().get(pathPosition) instanceof MovementFall) {
                BlockPos fallDest = path.positions().get(pathPosition + 1);
                return VecUtils.entityFlatDistanceToCenter(ctx.player(), fallDest) >= leniency;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean snipsnapifpossible() {
        if (!ctx.player().onGround && !MovementHelper.isWater(ctx.world().getBlockState(ctx.playerFeet()).getBlock())) {
            return false;
        } else {
            if (ctx.playerMotion().yCoord < -0.1) {
                return false;
            }
        }
        int index = path.positions().indexOf(ctx.playerFeet());
        if (index == -1) {
            return false;
        }
        pathPosition = index;
        clearKeys();
        return true;
    }

    private boolean decrease() {
        IMovement current = path.movements().get(pathPosition);

        if (current instanceof MovementParkour) {
            return false;
        }

        if (current instanceof MovementDiagonal) {
            if (current.getDest().y != current.getSrc().y) {
                return false;
            }
        }

        IPlayerContext ctx = behavior.ctx;

        if (current instanceof MovementAscend || current instanceof MovementDescend) {
            return false; //ctx.playerFeet().equals(current.getSrc()) && ctx.playerMotion().lengthVector() > 0.15;
        }

        if (pathPosition < path.length() - 3) {
            if (ctx.playerMotion().lengthVector() < 0.15) {
                return false;
            }

            IMovement next = path.movements().get(pathPosition + 1);

            if (next instanceof MovementParkour) {
                return true;
            }

            if (current.getDirection().equals(next.getDirection())) {
                return false;
            }
            Vec3 currentDirVec = new Vec3(current.getDirection());
            Vec3 nextDirVec = new Vec3(next.getDirection());
            if (currentDirVec.dotProduct(nextDirVec) <= 0) {
                return true;
            }

            if (next instanceof MovementFall) {
                return true;
            }

            if (next instanceof MovementAscend || next instanceof MovementDescend) {
                boolean srcSlab = MovementHelper.isBottomSlab(ctx.world().getBlockState(next.getSrc().down()));
                boolean destSlab = MovementHelper.isBottomSlab(ctx.world().getBlockState(next.getDest().down()));
                if (srcSlab ^ destSlab) {
                    return true;
                }
            }

            if (next instanceof MovementDiagonal) {
                if (next.getDest().y != next.getSrc().y) {
                    return true;
                }
            }

            if (BaritoneAPI.getSettings().safeMode.value) {
                IMovement nextNext = path.movements().get(pathPosition + 2);

                if (nextNext instanceof MovementParkour) {
                    return true;
                }

                if (next.getDirection().equals(nextNext.getDirection())) {
                    return false;
                }

                if (nextNext instanceof MovementFall) {
                    return true;
                }

                if (nextNext instanceof MovementAscend || nextNext instanceof MovementDescend) {
                    boolean srcSlab = MovementHelper.isBottomSlab(ctx.world().getBlockState(next.getSrc().down()));
                    boolean destSlab = MovementHelper.isBottomSlab(ctx.world().getBlockState(next.getDest().down()));
                    if (srcSlab ^ destSlab) {
                        return true;
                    }
                }

                if (nextNext instanceof MovementDiagonal) {
                    return nextNext.getDest().y != nextNext.getSrc().y;
                }
            }
            return false;
        }

        return ctx.playerMotion().lengthVector() > 0.2;
    }

    private boolean shouldSprintNextTick() {
        boolean requested = behavior.baritone.getInputOverrideHandler().isInputForcedDown(Input.SPRINT);

        behavior.baritone.getInputOverrideHandler().setInputForceState(Input.SPRINT, false);

        if (!new CalculationContext(behavior.baritone, false).canSprint) {
            return false;
        }
        IMovement current = path.movements().get(pathPosition);

        if (current instanceof MovementTraverse && pathPosition < path.length() - 3) {
            IMovement next = path.movements().get(pathPosition + 1);
            if (next instanceof MovementAscend && sprintableAscend(ctx, (MovementTraverse) current, (MovementAscend) next, path.movements().get(pathPosition + 2))) {
                if (skipNow(ctx, current)) {
                    logDebug("Skipping traverse to straight ascend");
                    pathPosition++;
                    onChangeInPathPosition();
                    onTick();
                    behavior.baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, true);
                    return true;
                } else {
                    logDebug("Too far to the side to safely sprint ascend");
                }
            }
        }

        if (requested) {
            return true;
        }

        if (current instanceof MovementDescend) {
            if (((MovementDescend) current).safeMode() && !((MovementDescend) current).skipToAscend()) {
                logDebug("Sprinting would be unsafe");
                return false;
            }

            if (pathPosition < path.length() - 2) {
                IMovement next = path.movements().get(pathPosition + 1);
                if (next instanceof MovementAscend && current.getDirection().up().equals(next.getDirection().down())) {
                    pathPosition++;
                    onChangeInPathPosition();
                    onTick();
                    logDebug("Skipping descend to straight ascend");
                    return true;
                }
                if (canSprintFromDescendInto(ctx, current, next)) {

                    if (next instanceof MovementDescend && pathPosition < path.length() - 3) {
                        IMovement next_next = path.movements().get(pathPosition + 2);
                        if (next_next instanceof MovementDescend && !canSprintFromDescendInto(ctx, next, next_next)) {
                            return false;
                        }

                    }
                    if (ctx.playerFeet().equals(current.getDest())) {
                        pathPosition++;
                        onChangeInPathPosition();
                        onTick();
                    }

                    return true;
                }
            }
        }
        if (current instanceof MovementAscend && pathPosition != 0) {
            IMovement prev = path.movements().get(pathPosition - 1);
            if (prev instanceof MovementDescend && prev.getDirection().up().equals(current.getDirection().down())) {
                BlockPos center = current.getSrc().up();
                if (ctx.playerFeetAsVec().yCoord >= center.getY() - 0.07) {
                    behavior.baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, false);
                    return true;
                }
            }
            if (pathPosition < path.length() - 2 && prev instanceof MovementTraverse && sprintableAscend(ctx, (MovementTraverse) prev, (MovementAscend) current, path.movements().get(pathPosition + 1))) {
                return true;
            }
        }
        if (current instanceof MovementFall) {
            Tuple<Vec3, BlockPos> data = overrideFall((MovementFall) current);
            if (data != null) {
                BetterBlockPos fallDest = new BetterBlockPos(data.getSecond());
                if (!path.positions().contains(fallDest)) {
                    throw new IllegalStateException();
                }
                if (ctx.playerFeet().equals(fallDest)) {
                    pathPosition = path.positions().indexOf(fallDest);
                    onChangeInPathPosition();
                    onTick();
                    return true;
                }
                clearKeys();
                behavior.baritone.getLookBehavior().updateTarget(RotationUtils.calcRotationFromVec3d(ctx.playerHead(), data.getFirst(), ctx.playerRotations()).withPitch(ctx.playerRotations().getPitch()), false);
                MovementState state = new MovementState();
                MovementHelper.setInputsAccurate(ctx, state, data.getFirst());
                for (Map.Entry<Input, Boolean> inputState : state.getInputStates().entrySet()) {
                    behavior.baritone.getInputOverrideHandler().setInputForceState(inputState.getKey(), inputState.getValue());
                }
                return true;
            }
        }
        return false;
    }

    private Tuple<Vec3, BlockPos> overrideFall(MovementFall movement) {
        Vec3i dir = movement.getDirection();
        if (dir.getY() < -3) {
            return null;
        }
        Vec3i flatDir = new Vec3i(dir.getX(), 0, dir.getZ());
        int i;
        outer:
        for (i = pathPosition + 1; i < path.length() - 1 && i < pathPosition + 3; i++) {
            IMovement next = path.movements().get(i);
            if (!(next instanceof MovementTraverse)) {
                break;
            }
            if (!flatDir.equals(next.getDirection())) {
                break;
            }
            for (int y = next.getDest().y; y <= movement.getSrc().y + 1; y++) {
                BlockPos chk = new BlockPos(next.getDest().x, y, next.getDest().z);
                if (!MovementHelper.fullyPassable(ctx, chk)) {
                    break outer;
                }
            }
            if (!MovementHelper.canWalkOn(ctx, next.getDest().down())) {
                break;
            }
        }
        i--;
        if (i == pathPosition) {
            return null;
        }
        double len = i - pathPosition - 0.4;
        return new Tuple<>(
                new Vec3(flatDir.getX() * len + movement.getDest().x + 0.5, movement.getDest().y, flatDir.getZ() * len + movement.getDest().z + 0.5),
                movement.getDest().add(flatDir.getX() * (i - pathPosition), 0, flatDir.getZ() * (i - pathPosition)));
    }

    private static boolean skipNow(IPlayerContext ctx, IMovement current) {
        double offTarget = Math.abs(current.getDirection().getX() * (current.getSrc().z + 0.5D - ctx.playerFeetAsVec().zCoord)) + Math.abs(current.getDirection().getZ() * (current.getSrc().x + 0.5D - ctx.playerFeetAsVec().xCoord));
        if (offTarget > 0.1) {
            return false;
        }
        BlockPos headBonk = current.getSrc().subtract(current.getDirection()).up(2);
        if (MovementHelper.fullyPassable(ctx, headBonk)) {
            return true;
        }
        double flatDist = Math.abs(current.getDirection().getX() * (headBonk.getX() + 0.5D - ctx.playerFeetAsVec().xCoord)) + Math.abs(current.getDirection().getZ() * (headBonk.getZ() + 0.5 - ctx.playerFeetAsVec().zCoord));
        return flatDist > 0.8;
    }

    private static boolean sprintableAscend(IPlayerContext ctx, MovementTraverse current, MovementAscend next, IMovement nextnext) {
        if (!Baritone.settings().sprintAscends.value) {
            return false;
        }
        if (ctx.player().getActivePotionEffect(Potion.jump).getAmplifier() > 0) {
            return false;
        }
        if (!current.getDirection().equals(next.getDirection().down())) {
            return false;
        }
        if (nextnext.getDirection().getX() != next.getDirection().getX() || nextnext.getDirection().getZ() != next.getDirection().getZ()) {
            return false;
        }
        if (!MovementHelper.canWalkOn(ctx, current.getDest().down())) {
            return false;
        }
        if (!MovementHelper.canWalkOn(ctx, next.getDest().down())) {
            return false;
        }
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 3; y++) {
                BlockPos chk = current.getSrc().up(y);
                if (x == 1) {
                    chk = chk.add(current.getDirection());
                }
                if (!MovementHelper.fullyPassable(ctx, chk)) {
                    return false;
                }
            }
        }
        if (MovementHelper.avoidWalkingInto(ctx.world().getBlockState(current.getSrc().up(3)).getBlock())) {
            return false;
        }
        return !MovementHelper.avoidWalkingInto(ctx.world().getBlockState(next.getDest().up(2)).getBlock());
    }

    private static boolean canSprintFromDescendInto(IPlayerContext ctx, IMovement current, IMovement next) {
        if (next instanceof MovementDescend && next.getDirection().equals(current.getDirection())) {
            return true;
        }
        if (!MovementHelper.canWalkOn(ctx, current.getDest().add(current.getDirection()))) {
            return false;
        }
        if (next instanceof MovementTraverse && next.getDirection().equals(current.getDirection())) {
            return true;
        }
        return next instanceof MovementDiagonal && Baritone.settings().allowOvershootDiagonalDescend.value;
    }

    private void onChangeInPathPosition() {
        clearKeys();
        ticksOnCurrent = 0;
    }

    private void clearKeys() {
        behavior.baritone.getInputOverrideHandler().clearAllKeys();
    }

    private void cancel() {
        clearKeys();
        behavior.baritone.getInputOverrideHandler().getBlockBreakHelper().stopBreakingBlock();
        pathPosition = path.length() + 3;
        failed = true;
    }

    @Override
    public int getPosition() {
        return pathPosition;
    }

    public PathExecutor trySplice(PathExecutor next) {
        if (next == null) {
            return cutIfTooLong();
        }
        return SplicedPath.trySplice(path, next.path, false).map(path -> {
            if (!path.getDest().equals(next.getPath().getDest())) {
                throw new IllegalStateException();
            }
            PathExecutor ret = new PathExecutor(behavior, path);
            ret.pathPosition = pathPosition;
            ret.currentMovementOriginalCostEstimate = currentMovementOriginalCostEstimate;
            ret.costEstimateIndex = costEstimateIndex;
            ret.ticksOnCurrent = ticksOnCurrent;
            return ret;
        }).orElseGet(this::cutIfTooLong);
    }

    private PathExecutor cutIfTooLong() {
        if (pathPosition > Baritone.settings().maxPathHistoryLength.value) {
            int cutoffAmt = Baritone.settings().pathHistoryCutoffAmount.value;
            CutoffPath newPath = new CutoffPath(path, cutoffAmt, path.length() - 1);
            if (!newPath.getDest().equals(path.getDest())) {
                throw new IllegalStateException();
            }
            logDebug("Discarding earliest segment movements, length cut from " + path.length() + " to " + newPath.length());
            PathExecutor ret = new PathExecutor(behavior, newPath);
            ret.pathPosition = pathPosition - cutoffAmt;
            ret.currentMovementOriginalCostEstimate = currentMovementOriginalCostEstimate;
            if (costEstimateIndex != null) {
                ret.costEstimateIndex = costEstimateIndex - cutoffAmt;
            }
            ret.ticksOnCurrent = ticksOnCurrent;
            return ret;
        }
        return this;
    }

    @Override
    public IPath getPath() {
        return path;
    }

    public boolean failed() {
        return failed;
    }

    public boolean finished() {
        return pathPosition >= path.length();
    }

    public boolean isSprinting() {
        return sprintNextTick;
    }
}