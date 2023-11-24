package baritone.pathing.calc;

import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.movement.IMovement;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Helper;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.Moves;
import baritone.pathing.path.CutoffPath;
import baritone.utils.pathing.PathBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

class Path extends PathBase {

    private final BetterBlockPos start;

    private final BetterBlockPos end;

    private final List<BetterBlockPos> path;

    private final List<Movement> movements;

    private final List<PathNode> nodes;

    private final Goal goal;

    private final int numNodes;

    private final CalculationContext context;

    private volatile boolean verified;

    Path(PathNode start, PathNode end, int numNodes, Goal goal, CalculationContext context) {
        this.start = new BetterBlockPos(start.x, start.y, start.z);
        this.end = new BetterBlockPos(end.x, end.y, end.z);
        this.numNodes = numNodes;
        this.movements = new ArrayList<>();
        this.goal = goal;
        this.context = context;
        PathNode current = end;
        LinkedList<BetterBlockPos> tempPath = new LinkedList<>();
        LinkedList<PathNode> tempNodes = new LinkedList<>();
        while (current != null) {
            tempNodes.addFirst(current);
            tempPath.addFirst(new BetterBlockPos(current.x, current.y, current.z));
            current = current.previous;
        }
        this.path = new ArrayList<>(tempPath);
        this.nodes = new ArrayList<>(tempNodes);
    }

    @Override
    public Goal getGoal() {
        return goal;
    }

    private boolean assembleMovements() {
        if (path.isEmpty() || !movements.isEmpty()) {
            throw new IllegalStateException();
        }
        for (int i = 0; i < path.size() - 1; i++) {
            double cost = nodes.get(i + 1).cost - nodes.get(i).cost;
            Movement move = runBackwards(path.get(i), path.get(i + 1), cost);
            if (move == null) {
                return true;
            } else {
                movements.add(move);
            }
        }
        return false;
    }

    private Movement runBackwards(BetterBlockPos src, BetterBlockPos dest, double cost) {
        for (Moves moves : Moves.values()) {
            Movement move = moves.apply0(context, src);
            if (move.getDest().equals(dest)) {
                move.override(Math.min(move.calculateCost(context), cost));
                return move;
            }
        }
        Helper.HELPER.logDebug("Movement became impossible during calculation " + src + " " + dest + " " + dest.subtract(src));
        return null;
    }

    @Override
    public IPath postProcess() {
        if (verified) {
            throw new IllegalStateException();
        }
        verified = true;
        boolean failed = assembleMovements();

        if (failed) {
            CutoffPath res = new CutoffPath(this, movements().size());
            if (res.movements().size() != movements.size()) {
                throw new IllegalStateException();
            }
            return res;
        }
        sanityCheck();
        return this;
    }

    @Override
    public List<IMovement> movements() {
        if (!verified) {
            throw new IllegalStateException();
        }
        return Collections.unmodifiableList(movements);
    }

    @Override
    public List<BetterBlockPos> positions() {
        return Collections.unmodifiableList(path);
    }

    @Override
    public int getNumNodesConsidered() {
        return numNodes;
    }

    @Override
    public BetterBlockPos getSrc() {
        return start;
    }

    @Override
    public BetterBlockPos getDest() {
        return end;
    }
}
