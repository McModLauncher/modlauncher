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

import cpw.mods.modlauncher.api.accesstransformer.AccessTransformation;

import java.util.HashMap;
import java.util.Map;

/**
 * More or less the same as {@link TransformList} but for ATs
 */
public class AccessTransformerList<T>
{
    private final Map<TransformTargetLabel, AccessTransformation> transformers = new HashMap<>();
    private final Class<T> nodeType;

    AccessTransformerList(Class<T> nodeType)
    {
        this.nodeType = nodeType;
    }

    void addTransformer(AccessTransformation newAT)
    {
        AccessTransformation at = transformers.get(newAT.label);
        if (at == null)
        {
            transformers.put(newAT.label, newAT);
        }
        else
        {
            //merge the ATs
            Logging.launcherLog.debug(Logging.ACCESS_TRANSFORMING, "Found duplicate AT for {}, merging...", newAT.label);
            if (at.finalModifier.shouldBeReplacedBy(newAT.finalModifier))
            {
                Logging.launcherLog.debug(Logging.ACCESS_TRANSFORMING, "Changing rule write modifier from {} to {}", at.finalModifier, newAT.finalModifier);
                at.finalModifier = newAT.finalModifier;
            }
            if (at.visibilityModifier.shouldBeReplacedBy(newAT.visibilityModifier))
            {
                Logging.launcherLog.debug(Logging.ACCESS_TRANSFORMING, "Changing rule visibility modifier from {} to {}", at.visibilityModifier, newAT.visibilityModifier);
                at.visibilityModifier = newAT.visibilityModifier;
            }
        }
    }

    public Map<TransformTargetLabel, AccessTransformation> getTransformers()
    {
        return transformers;
    }
}
