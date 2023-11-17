package baritone.utils;

import baritone.api.BaritoneAPI;
import baritone.api.event.events.RenderEvent;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IPlayerContext;
import baritone.behavior.PathingBehavior;
import baritone.pathing.path.PathExecutor;
import net.minecraft.entity.Entity;

import java.awt.*;
import java.util.List;

public class PathRenderer implements IRenderer {

    private PathRenderer() {}

    public static void render(RenderEvent event, PathingBehavior behavior) {
        final IPlayerContext ctx = behavior.ctx;
        if (ctx.world() == null) {
            return;
        }

        final float partialTicks = event.getPartialTicks();
        final Goal goal = behavior.getGoal();

        final int thisPlayerDimension = ctx.world().provider.getDimensionId();
        final int currentRenderViewDimension = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().world().provider.getDimensionId();

        if (thisPlayerDimension != currentRenderViewDimension) {
            return;
        }

        //TODO: goal render

        if (!settings.renderPath.value) {
            return;
        }

        PathExecutor current = behavior.getCurrent();
        if (current != null) {
            int renderStart = Math.max(current.getPosition() - 3, 0);
            drawPath(current.getPath(), renderStart, settings.colorCurrentPath.value, settings.fadePath.value,
                    20, 30);
        }
    }

    private static void drawPath(IPath path, int startIndex, Color color, boolean fadeOut, int fadeStart0, int fadeEnd0) {
        IRenderer.startLines(color, settings.pathRenderLineWidthPixels.value, settings.renderPathIgnoreDepth.value);

        int fadeStart = fadeStart0 + startIndex;
        int fadeEnd = fadeEnd0 + startIndex;



        List<BetterBlockPos> positions = path.positions();
        for (int i = startIndex, next; i < positions.size() - 1; i = next) {
            BetterBlockPos start = positions.get(i);
            BetterBlockPos end = positions.get(next = i + 1);

            int dx = end.x - start.x;
            int dy = end.y - start.y;
            int dz = end.z - start.z;

            while (next + 1 < positions.size() && (!fadeOut || next + 1 < fadeStart) &&
                    dx == positions.get(next + 1).x - end.x &&
                    dy == positions.get(next + 1).y - end.y &&
                    dz == positions.get(next + 1).z - end.z) {
                end = positions.get(++next);
            }

            if (fadeOut) {
                float alpha;

                if (i <= fadeStart) {
                    alpha = 0.4F;
                } else {
                    if (i > fadeEnd) {
                        break;
                    }
                    alpha = 0.4F * (1.0F - (float) (i - fadeStart) / (float) (fadeEnd - fadeStart));
                }

                IRenderer.glColor(color, alpha);
            }

            IRenderer.emitLine(start.x + 0.5, start.y + 0.1, start.z + 0.5, end.x + 0.5, end.y + 0.1, end.z + 0.5);
        }

        IRenderer.endLines(settings.renderPathIgnoreDepth.value);
    }

    private static void drawGoal(Entity player, Goal goal, float partialTicks, Color color) {
        drawGoal(player, goal, partialTicks, color, true);
    }

    private static void drawGoal(Entity player, Goal goal, float partialTicks, Color color, boolean setupRenderer) {
        double renderPosX = renderManager.viewerPosX;
        double renderPosY = renderManager.viewerPosY;
        double renderPosZ = renderManager.viewerPosZ;
        double minX, maxX;
        double minZ, maxZ;
        double minY, maxY;
        double y, y1, y2;

    }
}
