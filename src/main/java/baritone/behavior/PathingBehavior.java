package baritone.behavior;

import baritone.Baritone;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.event.events.RenderEvent;
import baritone.api.event.events.SprintStateEvent;
import baritone.api.event.events.TickEvent;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Helper;
import baritone.api.utils.PathCalculationResult;
import baritone.api.utils.interfaces.IGoalRenderPos;
import baritone.pathing.calc.AStarPathFinder;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.path.PathExecutor;
import baritone.utils.PathRenderer;
import net.minecraft.util.BlockPos;

import java.util.Objects;
import java.util.Optional;

public class PathingBehavior extends Behavior implements IPathingBehavior, Helper {

    private AStarPathFinder inProgress;

    private PathExecutor current;

    private PathExecutor next;

    private Goal goal;
    private CalculationContext context;

    private final Object pathCalcLock = new Object();

    private final Object pathPlanLock = new Object();

    private boolean safeToCancel;

    private BetterBlockPos expectedSegmentStart;

    public PathingBehavior(Baritone baritone) {
        super(baritone);
    }

    @Override
    public void setGoal(Goal goal) {
        this.goal = goal;
    }

    @Override
    public Goal getGoal() {
        return goal;
    }

    @Override
    public boolean isPathing() {
        return this.current != null;
    }

    @Override
    public void cancelEverything() {
        current = null;
        next = null;
        baritone.getInputOverrideHandler().clearAllKeys();
    }

    @Override
    public PathExecutor getCurrent() {
        return current;
    }

    @Override
    public Optional<AbstractNodeCostSearch> getInProgress() {
        return Optional.ofNullable(inProgress);
    }

    public CalculationContext secretInternalGetCalculationContext() {
        return context;
    }

    @Override
    public void findPath(BetterBlockPos start) {
        context = new CalculationContext(baritone, true);
        long primaryTimeout;
        long failureTimeout;
        if (current == null) {
            primaryTimeout = Baritone.settings().primaryTimeoutMS.value;
            failureTimeout = Baritone.settings().failureTimeoutMS.value;
        } else {
            primaryTimeout = Baritone.settings().planAheadPrimaryTimeoutMS.value;
            failureTimeout = Baritone.settings().planAheadFailureTimeoutMS.value;
        }
        AbstractNodeCostSearch pathfinder = createPathfinder(start, goal, current == null ? null : current.getPath(), context);
        if (!Objects.equals(pathfinder.getGoal(), goal)) {
            logDebug("Simplifying " + goal.getClass() + " to GoalXZ due to distance");
        }

        Baritone.getExecutor().execute(() -> {
            logDebug("Starting to search for path from " + start + " to " + goal);

            PathCalculationResult calcResult = pathfinder.calculate(primaryTimeout, failureTimeout);
            synchronized (pathPlanLock) {
                Optional<PathExecutor> executor = calcResult.getPath().map(p -> new PathExecutor(PathingBehavior.this, p));
                if (current == null) {
                    if (executor.isPresent()) {
                        current = executor.get();
                    }
                } else {
                    if (next == null) {
                        if (executor.isPresent()) {
                            next = executor.get();
                        }
                    } else {
                        logDirect("Warning: PathingBehaivor illegal state! Discarding invalid path!");
                    }
                }
                synchronized (pathCalcLock) {
                    inProgress = null;
                }
            }
        });
    }

    private static AbstractNodeCostSearch createPathfinder(BlockPos start, Goal goal, IPath previous, CalculationContext context) {
        Goal transformed = goal;
        if (Baritone.settings().simplifyUnloadedYCoord.value && goal instanceof IGoalRenderPos) {
            BlockPos pos = ((IGoalRenderPos) goal).getGoalPos();
            if (!context.bsi.worldContainsLoadedChunk(pos.getX(), pos.getZ())) {
                transformed = new GoalXZ(pos.getX(), pos.getZ());
            }
        }
        return new AStarPathFinder(start.getX(), start.getY(), start.getZ(), transformed, context);
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT) {
            return;
        }

        if (current == null) {
            return;
        }

        safeToCancel = current.onTick();

        if (current.failed() || current.finished()) {
            logDebug("Finished");
            current = null;
            if (goal == null || goal.isInGoal(ctx.playerFeet())) {
                logDebug("Finished. At " + goal);

                next = null;
                if (Baritone.settings().disconnectOnArrival.value) {
                    ctx.world().sendQuittingDisconnectingPacket();
                }
                return;
            }
            if (next != null && !next.getPath().positions().contains(ctx.playerFeet()) &&
                    !next.getPath().positions().contains(expectedSegmentStart)) {
                logDebug("Next path does not contain current position" + next.getPath().getSrc() + ", " + ctx.playerFeet());
                next = null;
            }

            if (next != null) {
                logDebug("Continuing on to planned next path");
                current = next;
                next = null;
                current.onTick();
                return;
            }
            return;
        }

        if (next != null && current.getPath().getDest().equals(next.getPath().getDest())) {
            next = null;
        }

        synchronized (pathCalcLock) {
            if (inProgress != null) {
                return;
            }

            if (next != null) {
                return;
            }

            if (goal == null || goal.isInGoal(current.getPath().getDest())) {
                return;
            }

            Optional<Double> ticksRemainingInSegment = ticksRemainingInSegment(false);

            if (ticksRemainingInSegment.isPresent() && ticksRemainingInSegment.get() < Baritone.settings().planningTickLookahead.value) {
                logDebug("Path almost over. Planning ahead");

                findPath(current.getPath().getDest());
            }
        }
    }

    @Override
    public void onPlayerSprintState(SprintStateEvent event) {
        if (isPathing()) {
            event.setState(current.isSprinting());
        }
    }

    @Override
    public void onRenderPass(RenderEvent event) {
        PathRenderer.render(event, this);
    }

    public CalculationContext getCalculationContext() {
        return this.context;
    }
}
