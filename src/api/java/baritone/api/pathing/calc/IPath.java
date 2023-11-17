package baritone.api.pathing.calc;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.movement.IMovement;
import baritone.api.utils.BetterBlockPos;

import java.util.HashSet;
import java.util.List;

public interface IPath {

    List<IMovement> movements();

    List<BetterBlockPos> positions();

    default IPath postProcess() {
        throw new UnsupportedOperationException();
    }

    default int length() {
        return positions().size();
    }

    Goal getGoal();

    int getNumNodesConsidered();

    default BetterBlockPos getSrc() {
        return positions().get(0);
    }

    default BetterBlockPos getDest() {
        List<BetterBlockPos> pos = positions();
        return pos.get(pos.size() - 1);
    }

    default double ticksRemainingFrom(int pathPosition) {
        double sum = 0;
        List<IMovement> movements = movements();
        for (int i = pathPosition; i < movements.size(); i++) {
            sum += movements.get(i).getCost();
        }
        return sum;
    }

    default IPath cutoffAtLoadedChunks(Object bsi) {
        throw new UnsupportedOperationException();
    }

    default IPath staticCutoff(Goal destination) {
        throw new UnsupportedOperationException();
    }

    default void sanityCheck() {
        List<BetterBlockPos> path = positions();
        List<IMovement> movements = movements();
        if (!getSrc().equals(path.get(0))) {
            throw new IllegalStateException("Start node does not equal first path element");
        }
        if (!getDest().equals(path.get(path.size() - 1))) {
            throw new IllegalStateException("End node does not equal last path element");
        }
        if (path.size() != movements.size() + 1) {
            throw new IllegalStateException("Size of path array is unexpected");
        }
        HashSet<BetterBlockPos> seenSoFar = new HashSet<>();
        for (int i = 0; i < path.size() - 1; i++) {
            BetterBlockPos src = path.get(i);
            BetterBlockPos dest = path.get(i + 1);
            IMovement movement = movements.get(i);
            if (!src.equals(movement.getSrc())) {
                throw new IllegalStateException("Path source is not equal to the movement source");
            }
            if (!dest.equals(movement.getDest())) {
                throw new IllegalStateException("Path destination is not equal to the movement destination");
            }
            if (seenSoFar.contains(src)) {
                throw new IllegalStateException("Path doubles back on itself, making a loop");
            }
            seenSoFar.add(src);
        }
    }
}
