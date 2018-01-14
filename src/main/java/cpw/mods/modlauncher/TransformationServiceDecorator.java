package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import static cpw.mods.modlauncher.Logging.*;

/**
 * Decorates {@link cpw.mods.modlauncher.api.ITransformationService} to track state and other runtime metadata.
 */
public class TransformationServiceDecorator {
    private final ITransformationService service;
    private boolean isValid;

    TransformationServiceDecorator(ITransformationService service) {
        this.service = service;
    }

    void onLoad(IEnvironment env, Set<String> otherServices) {
        try {
            launcherLog.debug("Loading service {}", this.service::name);
            this.service.onLoad(env, otherServices);
            this.isValid = true;
            launcherLog.debug("Loaded service {}", this.service::name);
        } catch (IncompatibleEnvironmentException e) {
            launcherLog.error("Service failed to load {}", this.service.name(), e);
            this.isValid = false;
        }
    }

    boolean isValid() {
        return isValid;
    }

    void onInitialize(IEnvironment environment) {
        launcherLog.debug("Initializing transformation service {}", this.service::name);
        this.service.initialize(environment);
        launcherLog.debug("Initialized transformation service {}", this.service::name);
    }

    public void gatherTransformers(TransformStore transformStore) {
        launcherLog.debug("Initializing transformers for transformation service {}", this.service::name);
        final List<ITransformer> transformers = this.service.transformers();
        Objects.requireNonNull(transformers, "The transformers list should not be null");
        final Map<Type, List<ITransformer>> transformersByType = transformers.stream().collect(Collectors.groupingBy(
                t ->
                {
                    final Type[] genericInterfaces = t.getClass().getGenericInterfaces();
                    for (Type typ : genericInterfaces) {
                        ParameterizedType pt = (ParameterizedType) typ;
                        if (pt.getRawType().equals(ITransformer.class)) {
                            return pt.getActualTypeArguments()[0];
                        }
                    }
                    throw new RuntimeException("How did a non-transformer get here????");
                }
        ));
        for (Type type : transformersByType.keySet()) {
            final TransformTargetLabel.LabelType labelType = TransformTargetLabel.LabelType.getTypeFor(type).orElseThrow(() -> new IllegalArgumentException("Invalid transformer type found"));
            for (ITransformer<?> xform : transformersByType.get(type)) {
                final Set<ITransformer.Target> targets = xform.targets();
                if (targets.isEmpty()) continue;
                final Map<TransformTargetLabel.LabelType, List<TransformTargetLabel>> labelTypeListMap = targets.stream().map(TransformTargetLabel::new).collect(Collectors.groupingBy(TransformTargetLabel::getLabelType));
                if (labelTypeListMap.keySet().size() > 1 || !labelTypeListMap.keySet().contains(labelType)) {
                    launcherLog.error("Invalid target {} for transformer {}", labelType, xform);
                    throw new IllegalArgumentException("The transformer contains invalid targets");
                }
                labelTypeListMap.values().stream().flatMap(Collection::stream).forEach(target -> transformStore.addTransformer(target, xform));
            }
        }
        launcherLog.debug("Initialized transformers for transformation service {}", this.service::name);
    }

    ITransformationService getService() {
        return service;
    }
}
