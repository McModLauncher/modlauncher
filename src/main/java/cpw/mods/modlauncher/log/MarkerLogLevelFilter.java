package cpw.mods.modlauncher.log;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.PerformanceSensitive;

@Plugin(name = "MarkerLogLevelFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
@PerformanceSensitive("allocation")
public class MarkerLogLevelFilter extends AbstractFilter {
    public static final String ATTR_MARKER = "marker";
    public static final String ATTR_MINIMUM_LEVEL = "minimumLevel";

    private final String marker;
    private final String minimumLevel;

    protected MarkerLogLevelFilter(String marker, String minimumLevel, Result onMatch, Result onMismatch) {
        super(onMatch, onMismatch);
        this.marker = marker;
        this.minimumLevel = minimumLevel;
    }

    private Result filter(Marker marker, Level level) {
        if (marker == null || level == null || !marker.isInstanceOf(this.marker))
            return Result.NEUTRAL;

        final var comparedLevel = Level.getLevel(this.minimumLevel);
        if (comparedLevel.isLessSpecificThan(level))
            return onMatch;

        return onMismatch;
    }

    @Override
    public Result filter(LogEvent event) {
        return filter(event.getMarker(), event.getLevel());
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return filter(marker, level);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        return filter(marker, level);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        return filter(marker, level);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0) {
        return filter(marker, level);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1) {
        return filter(marker, level);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2) {
        return filter(marker, level);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3) {
        return filter(marker, level);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4) {
        return filter(marker, level);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
        return filter(marker, level);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
        return filter(marker, level);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
        return filter(marker, level);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {
        return filter(marker, level);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
        return filter(marker, level);
    }

    @Override
    public String toString() {
        return "MarkerLogLevelFilter{" + "marker='" + marker + '\'' + ", minimumLevel=" + minimumLevel + '}';
    }

    @PluginFactory
    public static MarkerLogLevelFilter createFilter(
        @PluginAttribute(ATTR_MARKER) final String marker,
        @PluginAttribute(ATTR_MINIMUM_LEVEL) final String minLevel,
        @PluginAttribute(AbstractFilterBuilder.ATTR_ON_MATCH) final Result match,
        @PluginAttribute(AbstractFilterBuilder.ATTR_ON_MISMATCH) final Result mismatch) {

        if (marker == null) {
            LOGGER.error("A marker must be provided for MarkerLogLevelFilter");
            return null;
        }

        if (minLevel == null) {
            LOGGER.error("A minimum level must be provided for MarkerLogLevelFilter");
            return null;
        }

        return new MarkerLogLevelFilter(marker, minLevel, match, mismatch);
    }
}
