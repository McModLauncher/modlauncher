/*
 * ModLauncher - for launching Java programs with in-flight transformation ability.
 *
 *     Copyright (C) 2017-2019 cpw
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cpw.mods.modlauncher;

import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.slf4j.event.Level.*;

class LogHelper {
    static final Marker MODLAUNCHER = MarkerFactory.getMarker("MODLAUNCHER");
    static final Marker CLASSLOADING = MarkerFactory.getMarker("CLASSLOADING");
    static final Marker LAUNCHPLUGIN = MarkerFactory.getMarker("LAUNCHPLUGIN");
    static {
        MODLAUNCHER.add(CLASSLOADING);
        MODLAUNCHER.add(LAUNCHPLUGIN);
    }

    public static void fatal(Marker marker, String message) {
        LoggerFactory.getLogger("MODLAUNCHER").error(marker, message);
    }

    public static void fatal(Marker marker, String message, Supplier<?>... args) {
        log(ERROR, marker, message, args);
    }

    public static void error(Marker marker, String message) {
        LoggerFactory.getLogger("MODLAUNCHER").error(marker, message);
    }

    public static void error(Marker marker, String message, Supplier<?>... args) {
        log(ERROR, marker, message, args);
    }

    public static void info(Marker marker, String message) {
        LoggerFactory.getLogger("MODLAUNCHER").info(marker, message);
    }

    public static void info(Marker marker, String message, Supplier<?>... args) {
        log(INFO, marker, message, args);
    }

    public static void debug(Marker marker, String message) {
        LoggerFactory.getLogger("MODLAUNCHER").debug(marker, message);
    }

    public static void debug(Marker marker, String message, Supplier<?>... args) {
        log(DEBUG, marker, message, args);
    }

    public static void log(Level level, Marker marker, String message, Supplier<?>... args) {
        final var logger = LoggerFactory.getLogger("MODLAUNCHER");
        final boolean shouldLog = switch(level) {
            case TRACE -> logger.isTraceEnabled(marker);
            case DEBUG -> logger.isDebugEnabled(marker);
            case INFO -> logger.isInfoEnabled(marker);
            case WARN -> logger.isWarnEnabled(marker);
            case ERROR -> logger.isErrorEnabled(marker);
        };

        if (!shouldLog) return;
        Stream.of(args).reduce(logger.makeLoggingEventBuilder(level).addMarker(marker), LoggingEventBuilder::addArgument, (l1, l2) -> l2).log(message);
    }
}
