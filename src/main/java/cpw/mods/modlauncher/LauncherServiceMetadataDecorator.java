package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.Environment;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import cpw.mods.modlauncher.api.LauncherService;
import cpw.mods.modlauncher.api.Transformer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cpw.mods.modlauncher.Logging.launcherLog;

public class LauncherServiceMetadataDecorator
{
    private final LauncherService service;
    private boolean isValid;

    LauncherServiceMetadataDecorator(LauncherService service)
    {
        this.service = service;
    }

    void onLoad(Environment env, Set<String> otherServices)
    {
        try
        {
            launcherLog.debug("Loading service {}", () -> this.service);
            this.service.onLoad(env, otherServices);
            this.isValid = true;
            launcherLog.debug("Loaded service {}", () -> this.service);
        }
        catch (IncompatibleEnvironmentException e)
        {
            launcherLog.error("Service failed to load {}", e);
            this.isValid = false;
        }
    }

    boolean isValid()
    {
        return isValid;
    }

    void onInitialize(Environment environment)
    {
        launcherLog.debug("Initializing service {}", () -> this.service);
        this.service.initialize(environment);
        launcherLog.debug("Initialized service {}", () -> this.service);
    }

    public void gatherTransformers(TransformStore transformStore)
    {
        launcherLog.debug("Initializing transformers for service {}", () -> this.service);
        final List<Transformer> transformers = this.service.transformers();
        Objects.requireNonNull(transformers, "The transformers list should not be null");
        final Map<Type, List<Transformer>> transformersByType = transformers.stream().collect(Collectors.groupingBy(
                t ->
                {
                    final Type[] genericInterfaces = t.getClass().getGenericInterfaces();
                    for (Type typ : genericInterfaces)
                    {
                        ParameterizedType pt = (ParameterizedType)typ;
                        if (pt.getRawType().equals(Transformer.class))
                        {
                            return pt.getActualTypeArguments()[0];
                        }
                    }
                    throw new RuntimeException("How did a non-transformer get here????");
                }
        ));
        for (Type type : transformersByType.keySet())
        {
            final TargetLabel.LabelType labelType = TargetLabel.LabelType.getTypeFor(type).orElseThrow(() -> new IllegalArgumentException("Invalid transformer type found"));
            for (Transformer<?> xform : transformersByType.get(type))
            {
                final Set<Transformer.Target> targets = xform.targets();
                final Map<TargetLabel.LabelType, List<TargetLabel>> labelTypeListMap = targets.stream().map(TargetLabel::new).collect(Collectors.groupingBy(TargetLabel::getLabelType));
                if (labelTypeListMap.keySet().size() > 1 || !labelTypeListMap.keySet().contains(labelType))
                {
                    throw new IllegalArgumentException("The transformer contains invalid targets");
                }
                labelTypeListMap.values().stream().flatMap(Collection::stream).forEach(target -> transformStore.addTransformer(target, xform));
            }
        }
        launcherLog.debug("Initialized transformers for service {}", () -> this.service);
    }

    LauncherService getService()
    {
        return service;
    }
}
