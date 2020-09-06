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

package cpw.mods.modlauncher.serviceapi;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Loaded from the initial classpath of the system to identify plugins that wish to work across the system.
 *
 * Mixin and AccessTransformer provide instances. Other plugins can also be added here, but their services are likely
 * not to be called upon.
 *
 */
public interface ILaunchPluginService {
    /**
     * The name of this plugin. Must be unique. Multiple plugins by the same name will result in a hard exit.
     *
     * Launcher and other systems will rely on this name to find services provided by this plugin, so it should be
     * stable.
     *
     * @return the name of the plugin
     */
    String name();

    /**
     * Indicates where the {@link #processClass(Phase, ClassNode, Type)} method should be called.
     */
    enum Phase {
        /**
         * Before regular transformer processing
         */
        BEFORE,
        /**
         * After regular transformer processing
         */
        AFTER
    }

    class ComputeFlags {
        /**
         * This plugin did not change the class and therefor requires no rewrite of the class.
         * This is the fastest option
         */
        public static final int NO_REWRITE = 0;

        /**
         * The plugin did change the class and requires a rewrite, but does not require any additional computation
         * as frames and maxs in the class did not change of have been corrected by the plugin.
         * Should not be combined with {@link #COMPUTE_FRAMES} or {@link #COMPUTE_MAXS}
         */
        public static final int SIMPLE_REWRITE = 0x100; //leave some space for eventual new flags in ClassWriter

        /**
         * The plugin did change the class and requires a rewrite, and requires max re-computation,
         * but frames are unchanged or corrected by the plugin
         */
        public static final int COMPUTE_MAXS = ClassWriter.COMPUTE_MAXS;

        /**
         * The plugin did change the class and requires a rewrite, and requires frame re-computation.
         * This is the slowest, but also safest method if you don't know what level is required.
         * This implies {@link #COMPUTE_MAXS}, so maxs will also be recomputed.
         */
        public static final int COMPUTE_FRAMES = ClassWriter.COMPUTE_FRAMES;
    }

    /**
     * If this plugin wants to receive the {@link ClassNode} into {@link #processClass}
     * @param classType the class to consider
     * @param isEmpty if the class is empty at present (indicates no backing file found)
     * @return the set of Phases the plugin wishes to be called back with
     */
    EnumSet<Phase> handlesClass(Type classType, final boolean isEmpty);

    /**
     * If this plugin wants to receive the {@link ClassNode} into {@link #processClass}
     * @param classType the class to consider
     * @param isEmpty if the class is empty at present (indicates no backing file found)
     * @param reason Reason for transformation request.
     *               "classloading" - cpw.mods.modlauncher.api.ITransformerActivity#CLASSLOADING_REASON
     *               "computing_frames" - cpw.mods.modlauncher.api.ITransformerActivity#COMPUTING_FRAMES_REASON
     *               or the name of an {@link ILaunchPluginService}
     * @return the set of Phases the plugin wishes to be called back with
     */
    default EnumSet<Phase> handlesClass(Type classType, final boolean isEmpty, final String reason) {
        return handlesClass(classType, isEmpty);
    }

    /**
     * Each class loaded is offered to the plugin for processing.
     * Ordering between plugins is not known.
     *
     * One of {@link #processClass(Phase, ClassNode, Type)}, {@link #processClass(Phase, ClassNode, Type, String)}
     * or {@link #processClassWithFlags(Phase, ClassNode, Type, String)} <em>must</em> be implemented.
     *
     * @param phase The phase of the supplied class node
     * @param classNode the classnode to process
     * @param classType the name of the class
     * @return true if the classNode needs rewriting using COMPUTE_FRAMES or false if it needs no NO_REWRITE
     */
    default boolean processClass(final Phase phase, ClassNode classNode, final Type classType) {
        throw new IllegalStateException("YOU NEED TO OVERRIDE ONE OF THE processClass methods");
    }

    /**
     * Each class loaded is offered to the plugin for processing.
     * Ordering between plugins is not known.
     *
     * @param phase The phase of the supplied class node
     * @param classNode the classnode to process
     * @param classType the name of the class
     * @param reason Reason for transformation. "classloading" or the name of an {@link ILaunchPluginService}
     * @return true if the classNode needs rewriting using COMPUTE_FRAMES or false if it needs no NO_REWRITE
     */
    default boolean processClass(final Phase phase, ClassNode classNode, final Type classType, String reason) {
        return processClass(phase, classNode, classType);
    }

    /**
     * Each class loaded is offered to the plugin for processing.
     * Ordering between plugins is not known.
     *
     * @param phase The phase of the supplied class node
     * @param classNode the classnode to process
     * @param classType the name of the class
     * @param reason Reason for transformation. "classloading" or the name of an {@link ILaunchPluginService}
     * @return The {@link ComputeFlags} for this class
     */
    default int processClassWithFlags(final Phase phase, ClassNode classNode, final Type classType, String reason) {
        return processClass(phase, classNode, classType, reason) ? ComputeFlags.COMPUTE_FRAMES : ComputeFlags.NO_REWRITE;
    }

    /**
     * Adds a resource to this plugin for processing by it. Used by forge to hand resources to access transformers
     * for example.
     *
     * @param resource The resource to be considered by this plugin.
     * @param name A name for this resource.
     */
    default void offerResource(Path resource, String name) {}

    /**
     * Offer scan results from TransformationServices to this plugin.
     *
     * @param resources A collection of all the results
     */
    default void addResources(List<Map.Entry<String, Path>> resources) {}

    default void initializeLaunch(ITransformerLoader transformerLoader,  Path[] specialPaths) {}
    /**
     * Get a plugin specific extension object from the plugin. This can be used to expose proprietary interfaces
     * to Launchers without ModLauncher needing to understand them.
     *
     * @param <T> The type of the extension
     * @return An extension object
     */
    default <T> T getExtension() {return null;}

    /**
     * Receives a call immediately after handlesClass for any transformer that declares an interest.
     *
     * the consumer can be called repeatedly to generate new AuditTrail entries in the audit log.
     *
     * @param className className that is being transformed
     * @param auditDataAcceptor accepts an array of strings to add a new audit trail record with the data
     */
    default void customAuditConsumer(String className, Consumer<String[]> auditDataAcceptor) {
    }

    interface ITransformerLoader {
        byte[] buildTransformedClassNodeFor(final String className) throws ClassNotFoundException;
    }
}
