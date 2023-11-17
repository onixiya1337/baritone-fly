package baritone.api.behavior;

import baritone.api.behavior.look.IAimProcessor;
import baritone.api.utils.Rotation;

public interface ILookBehavior {

    void updateTarget(Rotation rotation, boolean blockInteract);

    IAimProcessor getAimProcessor();
}
