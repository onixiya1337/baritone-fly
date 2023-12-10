package baritone.api;

import baritone.api.utils.NotificationHelper;
import baritone.api.utils.SettingsUtil;
import baritone.api.utils.TypeUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.util.IChatComponent;

import java.awt.*;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Settings {

    public final Setting<Double> maxYawOffsetForForward = new Setting<>(15d);

    public final Setting<Boolean> checkOvershootParkour = new Setting<>(true);

    public final Setting<Integer> pathHistoryCutoffAmount = new Setting<>(50);

    public final Setting<Integer> maxPathHistoryLength = new Setting<>(300);

    public final Setting<Boolean> allowOvershootDiagonalDescend = new Setting<>(false);

    public final Setting<Boolean> sprintAscends = new Setting<>(true);

    public final Setting<Double> maxCostIncrease = new Setting<>(10D);

    public final Setting<Double> pathCutoffFactor = new Setting<>(0.9);

    public final Setting<Integer> pathCutoffMinimumLength = new Setting<>(30);


    public final Setting<Boolean> cutoffAtLoadBoundary = new Setting<>(false);

    public final Setting<Long> primaryTimeoutMS = new Setting<>(500L);

    public final Setting<Long> failureTimeoutMS = new Setting<>(2000L);

    public final Setting<Long> planAheadPrimaryTimeoutMS = new Setting<>(4000L);

    public final Setting<Long> planAheadFailureTimeoutMS = new Setting<>(5000L);

    public final Setting<Boolean> simplifyUnloadedYCoord = new Setting<>(true);

    public final Setting<Boolean> slowPath = new Setting<>(false);

    public final Setting<Long> slowPathTimeDelayMS = new Setting<>(100L);

    public final Setting<Boolean> minimumImprovementRepropagation = new Setting<>(true);

    public final Setting<Long> slowPathTimeoutMS = new Setting<>(40000L);

    public final Setting<Integer> pathingMapDefaultSize = new Setting<>(1024);

    public final Setting<Float> pathingMapLoadFactor = new Setting<>(0.75f);

    public final Setting<Boolean> allowParkour = new Setting<>(false);

    public final Setting<Boolean> allowJumpAt256 = new Setting<>(true);

    public final Setting<Boolean> allowParkourAscend = new Setting<>(false);

    public final Setting<Float> yawSmoothingFactor = new Setting<>(0.2f);

    public final Setting<Float> pitchSmoothingFactor = new Setting<>(0.22f);

    public final Setting<Boolean> safeMode = new Setting<>(true);

    public final Setting<Boolean> allowDiagonalDescend = new Setting<>(false);

    public final Setting<Boolean> allowDiagonalAscend = new Setting<>(false);

    public final Setting<Integer> rightClickSpeed = new Setting<>(4);

    public final Setting<Double> walkOnWaterOnePenalty = new Setting<>(3D);

    public final Setting<Double> jumpPenalty = new Setting<>(2D);

    public final Setting<Double> backtrackCostFavoringCoefficient = new Setting<>(0.5);

    public final Setting<Integer> maxFallHeightNoWater = new Setting<>(10);

    public final Setting<Boolean> allowDownward = new Setting<>(true);

    public final Setting<Boolean> allowSprint = new Setting<>(true);

    public final Setting<Boolean> renderPathIgnoreDepth = new Setting<>(true);

    public final Setting<Float> pathRenderLineWidthPixels = new Setting<>(5F);

    public final Setting<Boolean> fadePath = new Setting<>(false);

    public final Setting<Color> colorCurrentPath = new Setting<>(Color.RED);

    public final Setting<Boolean> renderPath = new Setting<>(true);

    public final Setting<Integer> movementTimeoutTicks = new Setting<>(600);

    public final Setting<Integer> costVerificationLookahead = new Setting<>(5);

    public final Setting<Boolean> allowWalkOnBottomSlab = new Setting<>(true);

    public final Setting<Boolean> assumeWalkOnLava = new Setting<>(false);

    public final Setting<Boolean> allowVines = new Setting<>(false);

    public final Setting<Boolean> assumeWalkOnWater = new Setting<>(false);

    public final Setting<List<Block>> blocksToAvoid = new Setting<>(new ArrayList<>(
            // Leave Empty by Default
    ));

    public final Setting<Boolean> sprintInWater = new Setting<>(false);

    public final Setting<Boolean> overshootTraverse = new Setting<>(true);

    public final Setting<Boolean> assumeStep = new Setting<>(false);

    public final Setting<Integer> pathingMaxChunkBorderFetch = new Setting<>(50);

    public final Setting<Integer> planningTickLookahead = new Setting<>(150);

    public final Setting<Boolean> disconnectOnArrival = new Setting<>(false);

    public final Setting<Double> costHeuristic = new Setting<>(3.563);

    public final Setting<Integer> axisHeight = new Setting<>(120);

    public final Setting<Boolean> pathThroughCachedOnly = new Setting<>(false);

    public final Setting<Double> randomLooking = new Setting<>(0d);

    public final Setting<Float> blockReachDistance = new Setting<>(4.5f);

    public final Setting<Double> randomLooking113 = new Setting<>(0d);

    public final Setting<Boolean> freeLook = new Setting<>(false);

    public final Setting<Boolean> blockFreeLook = new Setting<>(false);

    public final Setting<Boolean> antiCheatCompatibility = new Setting<>(true);

    public final Setting<Boolean> shortBaritonePrefix = new Setting<>(false);

    public final Setting<Boolean> chatDebug = new Setting<>(false);

    public final Setting<Boolean> desktopNotifications = new Setting<>(false);

    public final Setting<Boolean> censorCoordinates = new Setting<>(false);

    @JavaOnly
    public final Setting<Consumer<IChatComponent>> logger = new Setting<>(msg -> Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(msg));

    @JavaOnly
    public final Setting<BiConsumer<String, Boolean>> notifier = new Setting<>(NotificationHelper::notify);

    public final Map<String, Setting<?>> byLowerName;

    public final List<Setting<?>> allSettings;

    public final Map<Setting<?>, Type> settingTypes;

    public final class Setting<T> {

        public T value;

        public final T defaultValue;

        private String name;

        private boolean javaOnly;

        private Setting(T value) {
            if (value == null) {
                throw new IllegalArgumentException("Cannot determine value type class from null");
            }
            this.value = value;
            this.defaultValue = value;
            this.javaOnly = false;
        }

        public final String getName() {
            return name;
        }

        public Class<T> getValueClass() {
            // noinspection unchecked
            return (Class<T>) TypeUtils.resolveBaseClass(getType());
        }

        @Override
        public String toString() {
            return SettingsUtil.settingToString(this);
        }

        public void reset() {
            value = defaultValue;
        }

        public final Type getType() {
            return settingTypes.get(this);
        }

        public boolean isJavaOnly() {
            return javaOnly;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    private @interface JavaOnly {}

    Settings() {
        Field[] temp = getClass().getFields();

        Map<String, Setting<?>> tmpByName = new HashMap<>();
        List<Setting<?>> tmpAll = new ArrayList<>();
        Map<Setting<?>, Type> tmpSettingTypes = new HashMap<>();

        try {
            for (Field field : temp) {
                if (field.getType().equals(Setting.class)) {
                    Setting<?> setting = (Setting<?>) field.get(this);
                    String name = field.getName();
                    setting.name = name;
                    setting.javaOnly = field.isAnnotationPresent(JavaOnly.class);
                    name = name.toLowerCase();
                    if (tmpByName.containsKey(name)) {
                        throw new IllegalStateException("Duplicate setting name");
                    }
                    tmpByName.put(name, setting);
                    tmpAll.add(setting);
                    tmpSettingTypes.put(setting, ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]);
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        byLowerName = Collections.unmodifiableMap(tmpByName);
        allSettings = Collections.unmodifiableList(tmpAll);
        settingTypes = Collections.unmodifiableMap(tmpSettingTypes);
    }

    @SuppressWarnings("unchecked")
    public <T> List<Setting<T>> getAllValuesByType(Class<T> cla$$) {
        List<Setting<T>> result = new ArrayList<>();
        for (Setting<?> setting : allSettings) {
            if (setting.getValueClass().equals(cla$$)) {
                result.add((Setting<T>) setting);
            }
        }
        return result;
    }
}
