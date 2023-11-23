package baritone.api.behavior.look;

import baritone.api.behavior.ILookBehavior;
import baritone.api.utils.Rotation;

public interface ITickableAimProcessor extends IAimProcessor {

    void tick();

    void advance(int ticks);

    Rotation nextRotation(Rotation initial, Rotation rotation);
}
