package baritone.api.event.listener;

import baritone.api.event.events.*;

public interface AbstractGameEventListener extends IGameEventListener {

    @Override
    default void onTick(TickEvent event) {}

    @Override
    default void onPlayerUpdate(PlayerUpdateEvent event) {}

    @Override
    default void onSendChatMessage(ChatEvent event) {}

    @Override
    default void onChunkEvent(ChunkEvent event) {}

    @Override
    default void onBlockChange(BlockChangeEvent event) {}

    @Override
    default void onRenderPass(RenderEvent event) {}

    @Override
    default void onWorldEvent(WorldEvent event) {}

    @Override
    default void onSendPacket(PacketEvent event) {}

    @Override
    default void onReceivePacket(PacketEvent event) {}

    @Override
    default void onPlayerRotationMove(RotationMoveEvent event) {}

    @Override
    default void onPlayerSprintState(SprintStateEvent event) {}

    @Override
    default void onBlockInteract(BlockInteractEvent event) {}

    @Override
    default void onPathEvent(PathEvent event) {}
}
