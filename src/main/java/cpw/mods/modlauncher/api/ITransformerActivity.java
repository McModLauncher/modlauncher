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

package cpw.mods.modlauncher.api;

import java.lang.reflect.Field;

import org.objectweb.asm.Opcodes;

public interface ITransformerActivity {
    /**
     * reason will be set to this value when TransformerClassWriter is trying to compute frames by loading the class
     * hierarchy for a class. No real classloading will be done when this reason is submitted.
     */
    String COMPUTING_FRAMES_REASON = "computing_frames";

    /**
     * reason will be set to this value when we're attempting to actually classload the class
     */
    String CLASSLOADING_REASON = "classloading";

    /**
     * the maximum ASM API version
     */
    int ASMAPI_VERSION = detectMaxASMAPIVersion();

    String[] getContext();

    Type getType();

    String getActivityString();

    private static int detectMaxASMAPIVersion() {
        int version = Opcodes.ASM4;
        for (Field field : Opcodes.class.getDeclaredFields()) {
            if (field.getType() == int.class && field.getName().startsWith("ASM")) {
                if (field.getName().endsWith("_EXPERIMENTAL")) {
                    break;
                }
                try {
                    version = Math.max(version, field.getInt(null));
                } catch (Exception e) {
                    throw new RuntimeException(e); // should never happen
                }
            }
        }
        return version;
    }

    enum Type {
        PLUGIN("pl"), TRANSFORMER("xf"), REASON("re");

        private final String label;

        Type(final String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
