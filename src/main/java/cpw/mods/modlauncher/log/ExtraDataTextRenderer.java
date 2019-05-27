package cpw.mods.modlauncher.log;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformerAuditTrail;
import org.apache.logging.log4j.core.pattern.TextRenderer;

import java.util.Optional;

public class ExtraDataTextRenderer implements TextRenderer {
    private final TextRenderer wrapped;
    private final Optional<ITransformerAuditTrail> auditData;
    private ThreadLocal<TransformerContext> currentClass = new ThreadLocal<>();

    ExtraDataTextRenderer(final TextRenderer wrapped) {
        this.wrapped = wrapped;
        this.auditData = Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.AUDITTRAIL.get());
    }

    @Override
    public void render(final String input, final StringBuilder output, final String styleName) {
        if ("StackTraceElement.ClassName".equals(styleName)) {
            currentClass.set(new TransformerContext());
            currentClass.get().setClassName(input);
        } else if ("StackTraceElement.MethodName".equals(styleName)) {
            final TransformerContext transformerContext = currentClass.get();
            if (transformerContext != null) {
                transformerContext.setMethodName(input);
            }
        } else if ("Suffix".equals(styleName)) {
            final TransformerContext classContext = currentClass.get();
            currentClass.remove();
            if (classContext != null) {
                final Optional<String> auditLine = auditData.map(data -> data.getAuditString(classContext.getClassName()));
                wrapped.render(" {"+ auditLine.orElse("") +"}", output, "StackTraceElement.Transformers");
            }
            return;
        }
        wrapped.render(input, output, styleName);
    }

    @Override
    public void render(final StringBuilder input, final StringBuilder output) {
        wrapped.render(input, output);
    }

    private static class TransformerContext {

        private String className;
        private String methodName;

        public void setClassName(final String className) {
            this.className = className;
        }

        public String getClassName() {
            return className;
        }

        public void setMethodName(final String methodName) {
            this.methodName = methodName;
        }

        public String getMethodName() {
            return methodName;
        }

        @Override
        public String toString() {
            return getClassName()+"."+getMethodName();
        }
    }
}
