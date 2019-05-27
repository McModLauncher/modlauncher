package cpw.mods.modlauncher.log;

import joptsimple.internal.Strings;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.pattern.*;

import java.util.Collections;

/**
 * Started as a copy of {@link org.apache.logging.log4j.core.pattern.ExtendedThrowablePatternConverter} because
 * there is no mechanism to hook additional data into that class, which is very rubbish.
 */
@Plugin(name = "TransformingThrowablePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "tEx" })
public class TransformingThrowablePatternConverter extends ThrowablePatternConverter {
    static final String SUFFIXFLAG="☃☃☃☃☃SUFFIXFLAG☃☃☃☃☃";
    /**
     * @param name    Name of converter.
     * @param style   CSS style for output.
     * @param options options, may be null.
     * @param config config.
     */
    protected TransformingThrowablePatternConverter(final Configuration config, final String[] options) {
        super("TransformingThrowable", "throwable", options, config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final ThrowableProxy proxy = event.getThrownProxy();
        final Throwable throwable = event.getThrown();
        if ((throwable != null || proxy != null) && options.anyLines()) {
            if (proxy == null) {
                super.format(event, toAppendTo);
                return;
            }
            final int len = toAppendTo.length();
            if (len > 0 && !Character.isWhitespace(toAppendTo.charAt(len - 1))) {
                toAppendTo.append(' ');
            }
            final TextRenderer textRenderer = new ExtraDataTextRenderer(options.getTextRenderer());
            proxy.formatExtendedStackTraceTo(toAppendTo, options.getIgnorePackages(),
                    textRenderer, SUFFIXFLAG, options.getSeparator());
        }
    }

    /**
     * Gets an instance of the class.
     *
     * @param config The current Configuration.
     * @param options pattern options, may be null.  If first element is "short",
     *                only the first line of the throwable will be formatted.
     * @return instance of class.
     */
    public static TransformingThrowablePatternConverter newInstance(final Configuration config, final String[] options) {
        return new TransformingThrowablePatternConverter(config, options);
    }


    public static String generateEnhancedStackTrace(final Throwable throwable) {
        final ThrowableProxy proxy = new ThrowableProxy(throwable);
        final StringBuilder buffer = new StringBuilder();
        final TextRenderer textRenderer = new ExtraDataTextRenderer(PlainTextRenderer.getInstance());
        proxy.formatExtendedStackTraceTo(buffer, Collections.emptyList(),
                textRenderer, SUFFIXFLAG, Strings.LINE_SEPARATOR);
        return buffer.toString();
    }
}
