package baritone;

import baritone.api.IBaritone;
import baritone.api.IBaritoneProvider;
import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BaritoneProvider implements IBaritoneProvider {

    private final List<IBaritone> all;
    private final List<IBaritone> allView;

    public BaritoneProvider() {
        this.all = new CopyOnWriteArrayList<>();
        this.allView = Collections.unmodifiableList(this.all);

        final Baritone primary = (Baritone) this.createBaritone(Minecraft.getMinecraft());
    }

    @Override
    public IBaritone getPrimaryBaritone() {
        return this.all.get(0);
    }

    @Override
    public List<IBaritone> getAllBaritones() {
        return this.allView;
    }

    @Override
    public synchronized IBaritone createBaritone(Minecraft minecraft) {
        IBaritone baritone = this.getBaritoneForMinecraft(minecraft);
        if (baritone == null) {
            this.all.add(baritone = new Baritone(minecraft));
        }
        return baritone;
    }

    @Override
    public synchronized boolean destroyBaritone(IBaritone baritone) {
        return baritone != this.getPrimaryBaritone() && this.all.remove(baritone);
    }
}
