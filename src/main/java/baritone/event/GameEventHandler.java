package baritone.event;

import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.event.events.*;
import baritone.api.event.listener.IEventBus;
import baritone.api.event.listener.IGameEventListener;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.*;
import baritone.utils.BlockStateInterface;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameEventHandler implements IEventBus {

    private final Baritone baritone;

    private final List<IGameEventListener> listeners = new CopyOnWriteArrayList<>();


    public GameEventHandler(Baritone baritone) {
        this.baritone = baritone;
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.IN) {
            try {
                baritone.bsi = new BlockStateInterface(baritone.getPlayerContext(), true);
            } catch (Exception ex) {
                baritone.bsi = null;
            }
        } else {
            baritone.bsi = null;
        }
        IPlayerContext ctx = baritone.getPlayerContext();
        if (ctx.minecraft().gameSettings.keyBindSneak.isPressed()) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().setGoal(new GoalXZ(ctx.playerFeet().x + 50, ctx.playerFeet().z + 50));
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().findFlyPath(ctx.playerFeet());
        }
        listeners.forEach(l -> l.onTick(event));
    }

    @Override
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        listeners.forEach(l -> l.onPlayerUpdate(event));
    }

    @Override
    public void onSendChatMessage(ChatEvent event) {
        listeners.forEach(l -> l.onSendChatMessage(event));
    }

    @Override
    public void onChunkEvent(ChunkEvent event) {
        listeners.forEach(l -> l.onChunkEvent(event));
    }

    @Override
    public void onBlockChange(BlockChangeEvent event) {
        listeners.forEach(l -> l.onBlockChange(event));
    }

    @Override
    public void onRenderPass(RenderEvent event) {
        listeners.forEach(l -> l.onRenderPass(event));
    }

    @Override
    public void onWorldEvent(WorldEvent event) {
        listeners.forEach(l -> l.onWorldEvent(event));
    }

    @Override
    public void onSendPacket(PacketEvent event) {
        listeners.forEach(l -> l.onSendPacket(event));
    }

    @Override
    public void onReceivePacket(PacketEvent event) {
        listeners.forEach(l -> l.onReceivePacket(event));
    }

    @Override
    public void onPlayerRotationMove(RotationMoveEvent event) {
        listeners.forEach(l -> l.onPlayerRotationMove(event));
    }

    @Override
    public void onPlayerSprintState(SprintStateEvent event) {
        listeners.forEach(l -> l.onPlayerSprintState(event));
    }

    @Override
    public void onBlockInteract(BlockInteractEvent event) {
        listeners.forEach(l -> l.onBlockInteract(event));
    }

    @Override
    public void registerEventListener(IGameEventListener listener) {
        this.listeners.add(listener);
    }
}
