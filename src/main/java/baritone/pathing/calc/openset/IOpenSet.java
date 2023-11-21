package baritone.pathing.calc.openset;

import baritone.pathing.calc.PathNode;

public interface IOpenSet {

    void insert(PathNode node);

    boolean isEmpty();

    PathNode removeLowest();

    void update(PathNode node);
}