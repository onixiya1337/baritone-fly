package baritone.pathing.calc;

import baritone.Baritone;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.movement.ActionCosts;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Helper;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Moves;
import baritone.utils.pathing.MutableMoveResult;
import net.minecraft.util.BlockPos;

import java.util.*;

public class AStarPathFinder {

    private final CalculationContext context;
    private final int startX;
    private final int startY;
    private final int startZ;
    private final Goal goal;

    public AStarPathFinder(BetterBlockPos start, Goal goal, CalculationContext context) {
        this.context = context;
        this.startX = start.x;
        this.startY = start.y;
        this.startZ = start.z;
        this.goal = goal;
    }

    public AStarPathFinder(BlockPos start, Goal goal, CalculationContext context) {
        this(new BetterBlockPos(start), goal, context);
    }

    public Optional<IPath> pathFind() {

        Moves[] allMoves = Moves.values();

        MutableMoveResult res = new MutableMoveResult();

        PathNode startNode = new PathNode(startX, startY, startZ);

        List<PathNode> openSet = new ArrayList<>();
        Set<PathNode> closedSet = new HashSet<>();

        openSet.add(startNode);

        int iterations = 0;

        int numEmptyChunk = 0;
        int pathingMaxChunkBorderFetch = Baritone.settings().pathingMaxChunkBorderFetch.value;

        int numNodes = 0;
        int numMovements = 0;

        long startTime = System.currentTimeMillis();

        PathNode current = null;

        while (!openSet.isEmpty() && numEmptyChunk < pathingMaxChunkBorderFetch) {
            iterations++;
            System.out.println(iterations);
            current = openSet.get(0);
            for (int i = 1; i < openSet.size(); i++) {
                if (openSet.get(i).fCost() < current.fCost() || openSet.get(i).fCost() == current.fCost() && openSet.get(i).hCost < current.hCost)
                    current = openSet.get(i);
            }

            openSet.remove(current);
            closedSet.add(current);

            numNodes++;

            if (goal.isInGoal(current.x, current.y, current.z)) {
                Helper.HELPER.logDebug("Took " + (System.currentTimeMillis() - startTime) + "ms, " + numMovements + " movements considered");
                return Optional.of(new Path(startNode, current, numNodes, goal, context));
            }

            for (Moves moves : allMoves) {
                int newX = current.x + moves.xOffset;
                int newZ = current.z + moves.zOffset;

                if ((newX >> 4 != current.x >> 4 || newZ >> 4 != current.z >> 4) && !context.isLoaded(newX, newZ)) {
                    if (!moves.dynamicXZ) {
                        numEmptyChunk++;
                    }
                    continue;
                }

                if (current.y + moves.yOffset > 256 || current.y + moves.yOffset < 0) {
                    continue;
                }

                res.reset();

                moves.apply(context, current.x, current.y, current.z, res);
                numMovements++;

                double actionCost = res.cost;
                if (actionCost >= ActionCosts.COST_INF) {
                    continue;
                }

                if (actionCost <= 0 || Double.isNaN(actionCost)) {
                    throw new IllegalStateException(moves + " calculated implausible cost " + actionCost);
                }

                PathNode neighbour = new PathNode(res.x, res.y, res.z);

                if (closedSet.contains(neighbour))
                    continue;

                double tentativeCost = current.gCost + actionCost;

                if (tentativeCost < neighbour.gCost || !openSet.contains(neighbour)) {
                    neighbour.gCost = tentativeCost;
                    neighbour.hCost = goal.heuristic(neighbour.x, neighbour.y, neighbour.z);
                    neighbour.parent = current;

                    if (!openSet.contains(neighbour))
                        openSet.add(neighbour);

                }
            }
        }

        if (numEmptyChunk >= pathingMaxChunkBorderFetch) {
            Helper.HELPER.logDebug("Path cutoff because of chunk border fetch. Empty chunk count: " + numEmptyChunk + ", Threshold: " + pathingMaxChunkBorderFetch);
            if (current == null) {
                throw new IllegalStateException("Last node at path cutoff is null");
            }
            return Optional.of(new Path(startNode, current, numNodes, goal, context));
        }

        return Optional.empty();
    }
}
