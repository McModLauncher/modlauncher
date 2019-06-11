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

import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;

import java.util.*;
import java.util.stream.Collectors;

public class TransformerAuditTrail implements cpw.mods.modlauncher.api.ITransformerAuditTrail {
    private Map<String, List<TransformerActivity>> audit = new HashMap<>();

    private static class TransformerActivity {
        private final Type type;
        private final String[] context;

        private TransformerActivity(Type type, String... context) {
            this.type = type;
            this.context = context;
        }

        String getActivityString() {
            return this.type.getLabel() + ":"+ String.join(":",this.context);
        }
    }

    enum Type {
        PLUGIN("pl"), TRANSFORMER("xf");

        private final String label;

        Type(final String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public void addPluginAuditTrail(String clazz, ILaunchPluginService plugin, ILaunchPluginService.Phase phase) {
        getTransformerActivities(clazz).add(new TransformerActivity(Type.PLUGIN, plugin.name(), phase.name().substring(0,1)));
    }

    public void addTransformerAuditTrail(String clazz, ITransformationService transformService, ITransformer<?> transformer) {
        getTransformerActivities(clazz).add(new TransformerActivity(Type.TRANSFORMER, concat(transformService.name(), transformer.labels())));
    }

    private String[] concat(String first, String[] rest) {
        final String[] res = new String[rest.length + 1];
        res[0] = first;
        System.arraycopy(rest, 0, res, 1, rest.length);
        return res;
    }
    private List<TransformerActivity> getTransformerActivities(final String clazz) {
        return audit.computeIfAbsent(clazz, v-> new ArrayList<>());
    }

    @Override
    public String getAuditString(final String clazz) {
        return audit.getOrDefault(clazz, Collections.emptyList()).stream().map(TransformerActivity::getActivityString).collect(Collectors.joining(","));
    }
}
