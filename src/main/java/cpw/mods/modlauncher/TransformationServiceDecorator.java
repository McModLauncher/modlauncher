/*
 * Modlauncher - utility to launch Minecraft-like game environments with runtime transformation
 * Copyright ©2016-2017 cpw and others
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.accesstransformer.AccessTransformation;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cpw.mods.modlauncher.Logging.launcherLog;

/**
 * Decorates {@link cpw.mods.modlauncher.api.ITransformationService} to track state and other runtime metadata.
 */
public class TransformationServiceDecorator
{
    private final ITransformationService service;
    private boolean isValid;

    TransformationServiceDecorator(ITransformationService service)
    {
        this.service = service;
    }

    void onLoad(IEnvironment env, Set<String> otherServices)
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

    void onInitialize(IEnvironment environment)
    {
        launcherLog.debug("Initializing transformation service {}", () -> this.service);
        this.service.initialize(environment);
        launcherLog.debug("Initialized transformation service {}", () -> this.service);
    }

    public void gatherTransformers(TransformStore transformStore)
    {
        launcherLog.debug("Initializing transformers for transformation service {}", () -> this.service);
        final List<ITransformer> transformers = this.service.transformers();
        Objects.requireNonNull(transformers, "The transformers list should not be null");
        final Map<Type, List<ITransformer>> transformersByType = transformers.stream().collect(Collectors.groupingBy(
                t ->
                {
                    final Type[] genericInterfaces = t.getClass().getGenericInterfaces();
                    for (Type typ : genericInterfaces)
                    {
                        ParameterizedType pt = (ParameterizedType)typ;
                        if (pt.getRawType().equals(ITransformer.class))
                        {
                            return pt.getActualTypeArguments()[0];
                        }
                    }
                    throw new RuntimeException("How did a non-transformer get here????");
                }
        ));
        for (Type type : transformersByType.keySet())
        {
            final TransformTargetLabel.LabelType labelType = TransformTargetLabel.LabelType.getTypeFor(type).orElseThrow(() -> new IllegalArgumentException("Invalid transformer type found"));
            for (ITransformer<?> xform : transformersByType.get(type))
            {
                final Set<ITransformer.Target> targets = xform.targets();
                final Map<TransformTargetLabel.LabelType, List<TransformTargetLabel>> labelTypeListMap = targets.stream().map(TransformTargetLabel::new).collect(Collectors.groupingBy(TransformTargetLabel::getLabelType));
                if (labelTypeListMap.keySet().size() > 1 || !labelTypeListMap.keySet().contains(labelType))
                {
                    throw new IllegalArgumentException("The transformer contains invalid targets");
                }
                labelTypeListMap.values().stream().flatMap(Collection::stream).forEach(target -> transformStore.addTransformer(target, xform));
            }
        }
        launcherLog.debug("Collection access transformers for transformation service {}", () -> this.service);
        this.service.accessTransformers().forEach(transformStore::addAccessTransformer);
        launcherLog.debug("Initialized transformers for transformation service {}", () -> this.service);
    }

    ITransformationService getService()
    {
        return service;
    }
}
