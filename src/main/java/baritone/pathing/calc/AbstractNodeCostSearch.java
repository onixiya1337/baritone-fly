package baritone.pathing.calc;

import baritone.Baritone;
import baritone.KeepName;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.calc.IPathFinder;
import baritone.api.pathing.goals.Goal;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Helper;
import baritone.api.utils.PathCalculationResult;
import baritone.pathing.movement.CalculationContext;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.Optional;

@KeepName
public abstract class AbstractNodeCostSearch implements IPathFinder, Helper {

    protected final int startX;
    protected final int startY;
    protected final int startZ;

    protected final Goal goal;

    private final CalculationContext context;

    private final Long2ObjectOpenHashMap<PathNode> map;

    protected PathNode startNode;

    protected PathNode mostRecentConsidered;

    protected final PathNode[] bestSoFar = new PathNode[COEFFICIENTS.length];

    private volatile boolean isFinished;

    protected boolean cancelRequested;

    protected static final double[] COEFFICIENTS = {1.5, 2, 2.5, 3, 4, 5, 10};

    protected static final double MIN_DIST_PATH = 5;

    protected static final double MIN_IMPROVEMENT = 0.01;

    AbstractNodeCostSearch(int startX, int startY, int startZ, Goal goal, CalculationContext context) {
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.goal = goal;
        this.context = context;
        this.map = new Long2ObjectOpenHashMap<>(Baritone.settings().pathingMapDefaultSize.value, Baritone.settings().pathingMapLoadFactor.value);
    }

    public void cancel() {
        cancelRequested = true;
    }

    @Override
    public synchronized PathCalculationResult calculate(long primaryTimeout, long failureTimeout) {
        if (isFinished) {
            throw new IllegalStateException("Path finder cannot be reused!");
        }
        cancelRequested = false;
        try {
            IPath path = calculate0(primaryTimeout, failureTimeout).map(IPath::postProcess).orElse(null);
            if (cancelRequested) {
                return new PathCalculationResult(PathCalculationResult.Type.CANCELLATION);
            }
            if (path == null) {
                return new PathCalculationResult(PathCalculationResult.Type.FAILURE);
            }
            int previousLength = path.length();
            path = path.cutoffAtLoadedChunks(context.bsi);
            if (path.length() < previousLength) {
                Helper.HELPER.logDebug("Cutting off path at edge of loaded chunks");
                Helper.HELPER.logDebug("Length decreased by " + (previousLength - path.length()));
            } else {
                Helper.HELPER.logDebug("Path ends within loaded chunks");
            }
            previousLength = path.length();
            path = path.staticCutoff(goal);
            if (path.length() < previousLength) {
                Helper.HELPER.logDebug("Static cutoff " + previousLength + " to " + path.length());
            }
            if (goal.isInGoal(path.getDest())) {
                return new PathCalculationResult(PathCalculationResult.Type.SUCCESS_TO_GOAL, path);
            } else {
                return new PathCalculationResult(PathCalculationResult.Type.SUCCESS_SEGMENT, path);
            }
        } catch (Exception e) {
            Helper.HELPER.logDirect("Pathing exception: " + e);
            e.printStackTrace();
            return new PathCalculationResult(PathCalculationResult.Type.EXCEPTION);
        } finally {
            isFinished = true;
        }
    }

    protected abstract Optional<IPath> calculate0(long primaryTimeout, long failureTimeout);

    protected double getDistFromStartSq(PathNode n) {
        int xDiff = n.x - startX;
        int yDiff = n.y - startY;
        int zDiff = n.z - startZ;
        return xDiff * xDiff + yDiff * yDiff + zDiff * zDiff;
    }

    protected PathNode getNodeAtPosition(int x, int y, int z, long hashCode) {
        PathNode node = map.get(hashCode);
        if (node == null) {
            node = new PathNode(x, y, z, goal);
            map.put(hashCode, node);
        }
        return node;
    }

    @Override
    public Optional<IPath> pathToMostRecentNodeConsidered() {
        return Optional.ofNullable(mostRecentConsidered).map(node -> new Path(startNode, node, 0, goal, context));
    }

    @Override
    public Optional<IPath> bestPathSoFar() {
        return bestSoFar(false, 0);
    }

    protected Optional<IPath> bestSoFar(boolean logInfo, int numNodes) {
        if (startNode == null) {
            return Optional.empty();
        }
        double bestDist = 0;
        for (int i = 0; i < COEFFICIENTS.length; i++) {
            if (bestSoFar[i] == null) {
                continue;
            }
            double dist = getDistFromStartSq(bestSoFar[i]);
            if (dist > bestDist) {
                bestDist = dist;
            }
            if (dist > MIN_DIST_PATH * MIN_DIST_PATH) { // square the comparison since distFromStartSq is squared
                if (logInfo) {
                    if (COEFFICIENTS[i] >= 3) {
                        System.out.println("Warning: cost coefficient is greater than three! Probably means that");
                        System.out.println("the path I found is pretty terrible (like sneak-bridging for dozens of blocks)");
                        System.out.println("But I'm going to do it anyway, because yolo");
                    }
                    System.out.println("Path goes for " + Math.sqrt(dist) + " blocks");
                    logDebug("A* cost coefficient " + COEFFICIENTS[i]);
                }
                return Optional.of(new Path(startNode, bestSoFar[i], numNodes, goal, context));
            }
        }
        if (logInfo) {
            logDebug("Even with a cost coefficient of " + COEFFICIENTS[COEFFICIENTS.length - 1] + ", I couldn't get more than " + Math.sqrt(bestDist) + " blocks");
            logDebug("No path found =(");
            logNotification("No path found =(", true);
        }
        return Optional.empty();
    }

    @Override
    public final boolean isFinished() {
        return isFinished;
    }

    @Override
    public final Goal getGoal() {
        return goal;
    }

    public BetterBlockPos getStart() {
        return new BetterBlockPos(startX, startY, startZ);
    }

    protected int mapSize() {
        return map.size();
    }
}
