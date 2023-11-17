package baritone.api;

import baritone.api.behavior.ILookBehavior;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.event.listener.IEventBus;
import baritone.api.utils.IInputOverrideHandler;
import baritone.api.utils.IPlayerContext;

public interface IBaritone {

    IEventBus getGameEventHandler();

    IInputOverrideHandler getInputOverrideHandler();

    IPlayerContext getPlayerContext();

    IPathingBehavior getPathingBehavior();

    ILookBehavior getLookBehavior();
}
