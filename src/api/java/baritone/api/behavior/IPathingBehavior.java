package baritone.api.behavior;

import baritone.api.pathing.calc.IPathFinder;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.path.IPathExecutor;
import baritone.api.utils.BetterBlockPos;

import java.util.Optional;

public interface IPathingBehavior extends IBehavior {

    default Optional<Double> ticksRemainingInSegment() {
        return ticksRemainingInSegment(true);
    }

    default Optional<Double> ticksRemainingInSegment(boolean includeCurrentMovement) {
        IPathExecutor current = getCurrent();
        if (current == null) {
            return Optional.empty();
        }
        int start = includeCurrentMovement ? current.getPosition() : current.getPosition() + 1;
        return Optional.of(current.getPath().ticksRemainingFrom(start));
    }

    Goal getGoal();

    void setGoal(Goal goal);

    void cancelEverything();

    void findPath(BetterBlockPos start);

    void findFlyPath(BetterBlockPos start);

    boolean isPathing();

    default boolean hasPath() {
        return getCurrent() != null;
    }

    Optional<? extends IPathFinder> getInProgress();

    IPathExecutor getCurrent();
}
