package baritone.pathing.path;

import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.movement.ActionCosts;
import baritone.api.pathing.movement.IMovement;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.pathing.path.IPathExecutor;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Helper;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.VecUtils;
import baritone.api.utils.input.Input;
import baritone.behavior.PathingBehavior;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.movements.*;
import baritone.utils.InputOverrideHandler;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

public class PathExecutor implements IPathExecutor, Helper {

    private static final double MAX_MAX_DIST_FROM_PATH = 5;
    private static final double MAX_DIST_FROM_PATH = 4;

    private static final double MAX_TICKS_AWAY = 200;

    private final IPath path;
    private int pathPosition;
    private int ticksAway;
    private int ticksOnCurrent;
    private Double movementCost;
    private Integer costEstimateIndex;
    private boolean failed;
    private boolean sprintNextTick = true;

    private final PathingBehavior behavior;
    private final IPlayerContext ctx;

    public PathExecutor(PathingBehavior behavior, IPath path) {
        this.behavior = behavior;
        this.ctx = behavior.ctx;
        this.path = path;
        this.pathPosition = 0;
    }

    @Override
    public IPath getPath() {
        return path;
    }

    @Override
    public int getPosition() {
        return pathPosition;
    }


    public boolean onTick() {
        if (pathPosition >= path.length() - 1) {
            return true;
        }

        Movement movement = (Movement) path.movements().get(pathPosition);
        BetterBlockPos playerPos = ctx.playerFeet();

        if (!movement.getValidPositions().contains(playerPos)) {
            for (int i = 0; i < pathPosition && i < path.length(); i++) {
                if (((Movement) path.movements().get(i)).getValidPositions().contains(playerPos)) {
                    logDebug("Lagged back " + (pathPosition - i) + " steps, to " + i);
                    int previousPos = pathPosition;
                    pathPosition = i;
                    for (int j = pathPosition; j <= previousPos; j++) {
                        path.movements().get(j).reset();
                    }
                    onChangeInPathPosition();
                    return false;
                }
            }
            for (int i = pathPosition + 3; i < path.length() - 1; i++) {
                if (((Movement) path.movements().get(i)).getValidPositions().contains(playerPos)) {
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

        BlockPos closestPos = closestPathPos();

        if (offPath(closestPos, MAX_DIST_FROM_PATH)) {
            ticksAway++;
            if (ticksAway > MAX_TICKS_AWAY) {
                logDebug("Too far from path for too long. Cancelling path");
                cancel();
                return false;
            }
        } else {
            ticksAway = 0;
        }

        if (offPath(closestPos, MAX_MAX_DIST_FROM_PATH)) {
            logDebug("Too far from path. Cancelling path");
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
            movementCost = movement.getCost();
            for (int i = 1; i < Baritone.settings().costVerificationLookahead.value && pathPosition + i < path.length() - 1; i++) {
                if (((Movement) path.movements().get(pathPosition + i)).calculateCost(behavior.getCalculationContext()) >= ActionCosts.COST_INF && canCancel) {
                    logDebug("Something has changed in the world and a future movement has become impossible. Cancelling.");
                    cancel();
                    return true;
                }
            }
        }

        double currentCost = movement.recalculateCost(behavior.getCalculationContext());
        if (currentCost >= ActionCosts.COST_INF && canCancel) {
            logDebug("Something has changed in the world and this movement has become impossible. Cancelling.");
            cancel();
            return true;
        }

        MovementStatus status = movement.update();

        if (status == MovementStatus.UNREACHABLE || status == MovementStatus.FAILED) {
            logDebug("Movement returns status " + status.toString().toLowerCase() + ". Movement " + movement.toString().toLowerCase());
            cancel();
            return false;
        } else if (status == MovementStatus.SUCCESS) {
            pathPosition++;
            onChangeInPathPosition();
            onTick();
            return true;
        } else {
            if (BaritoneAPI.getSettings().safeMode.value && movement instanceof MovementDescend) {
                ((MovementDescend) movement).forceSafeMode();
            }
            if (decrease()) {
                MovementHelper.decreaseMotion(behavior.baritone.getInputOverrideHandler(), ctx);
            } else {
                sprintNextTick = shouldSprintNextTick();
                if (!sprintNextTick) {
                    ctx.player().setSprinting(false);
                }
            }
            ticksOnCurrent++;

            if (ticksOnCurrent > movementCost + Baritone.settings().movementTimeoutTicks.value) {
                logDebug("This movement has taken too long (" + ticksOnCurrent + " ticks, expected " + movementCost + "). Cancelling.");
                cancel();
                return true;
            }
        }
        return movement.safeToCancel();
    }

    private boolean shouldSprintNextTick() {
        boolean requested = behavior.baritone.getInputOverrideHandler().isInputForcedDown(Input.SPRINT);

        behavior.baritone.getInputOverrideHandler().setInputForceState(Input.SPRINT, false);

        if (!new CalculationContext(behavior.baritone, false).canSprint) {
            return false;
        }

        if (pathPosition >= path.length() - 3) {
            return false;
        }

        if (requested) {
            return true;
        }

        IMovement current = path.movements().get(pathPosition);

        if (current instanceof MovementDescend || current instanceof MovementAscend || current instanceof MovementFall) {
            return false;
        }

        IMovement next = path.movements().get(pathPosition + 1);
        IMovement nextNext = path.movements().get(pathPosition + 2);

        if (current instanceof MovementDiagonal || current instanceof MovementTraverse) {
            if (current.getDest().y != current.getSrc().y) {
                return false;
            }

            if (next instanceof MovementDescend || next instanceof MovementAscend || next instanceof MovementFall) {
                return false;
            }

            if (BaritoneAPI.getSettings().safeMode.value && (nextNext instanceof MovementDescend || nextNext instanceof MovementAscend || nextNext instanceof MovementFall)) {
                return false;
            }


            return true;
        }

        return false;
    }

    private boolean decrease() {
        if (!BaritoneAPI.getSettings().safeMode.value) {
            return false;
        }

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
            return ctx.playerFeet().equals(current.getSrc()) && ctx.playerMotion().lengthVector() > 0.15;
        }

        if (pathPosition < path.length() - 3) {
            if (ctx.playerMotion().lengthVector() < 0.15) {
                return false;
            }

            IMovement next = path.movements().get(pathPosition + 1);

            if (current.getDirection().equals(next.getDirection())) {
                return false;
            }

            if (next instanceof MovementAscend || next instanceof MovementDescend) {
                return true;
            }

            if (next instanceof MovementDiagonal) {
                if (next.getDest().y != next.getSrc().y) {
                    return true;
                }
            }

            if (BaritoneAPI.getSettings().safeMode.value) {
                IMovement nextNext = path.movements().get(pathPosition + 2);

                if (next.getDirection().equals(nextNext.getDirection())) {
                    return false;
                }

                if (nextNext instanceof MovementAscend || nextNext instanceof MovementDescend) {
                    return true;
                }

                if (nextNext instanceof MovementDiagonal) {
                    return nextNext.getDest().y != nextNext.getSrc().y;
                }
            }
            return false;
        }

        return ctx.playerMotion().lengthVector() > 0.2;
    }

    private BlockPos closestPathPos() {
        double best = Double.MAX_VALUE;
        BlockPos bestPos = null;
        for (IMovement movement : path.movements()) {
            for (BlockPos pos : ((Movement) movement).getValidPositions()) {
                double dist = VecUtils.entityDistanceToCenter(ctx.player(), pos);
                if (dist < best) {
                    best = dist;
                    bestPos = pos;
                }
            }
        }

        return bestPos;
    }

    private boolean offPath(BlockPos closestPos, double leniency) {
        double distanceFromPath = VecUtils.entityDistanceToCenter(ctx.player(), closestPos);
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

    public boolean finished() {
        return pathPosition >= path.length() - 1;
    }

    public boolean failed() {
        return failed;
    }

    public boolean isSprinting() {
        return sprintNextTick;
    }
}
