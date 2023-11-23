package baritone.api.behavior.look;

import baritone.api.utils.Rotation;

import java.util.function.Supplier;

public interface IAimProcessor {

    Rotation peekRotation(Rotation rotation);

    Rotation interpolate(Rotation initial, Rotation rotation);

    ITickableAimProcessor fork();
}
