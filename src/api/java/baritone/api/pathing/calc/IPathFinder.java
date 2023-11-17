package baritone.api.pathing.calc;

import baritone.api.pathing.goals.Goal;
import baritone.api.utils.PathCalculationResult;

import java.util.Optional;

public interface IPathFinder {

    Goal getGoal();

    PathCalculationResult calculate(long primaryTimeout, long failureTimeout);

    boolean isFinished();

    Optional<IPath> pathToMostRecentNodeConsidered();

    Optional<IPath> bestPathSoFar();
}