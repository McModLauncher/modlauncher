package cpw.mods.modlauncher.serviceapi;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.nio.file.Path;
import java.util.EnumSet;

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

    /**
     * If this plugin wants to receive the {@link ClassNode} into {@link #processClass}
     * @param classType the class to consider
     * @param isEmpty if the class is empty at present (indicates no backing file found)
     * @return the set of Phases the plugin wishes to be called back with
     */
    EnumSet<Phase> handlesClass(Type classType, final boolean isEmpty);

    /**
     * Each class loaded is offered to the plugin for processing.
     * Ordering between plugins is not known.
     *
     * @param phase The phase of the supplied class node
     * @param classNode the classnode to process
     * @param classType the name of the class
     * @return the processed classnode
     */
    boolean processClass(final Phase phase, ClassNode classNode, final Type classType);

    /**
     * Adds a resource to this plugin for processing by it. Minecraft will always be the first resource offered.
     *
     * Transformers may pass additional resources.
     *
     * @param resource The resource to be considered by this plugin.
     * @param name A name for this resource.
     */
    default void addResource(Path resource, String name) {}

    /**
     * Get a plugin specific extension object from the plugin. This can be used to expose proprietary interfaces
     * to Launchers without ModLauncher needing to understand them.
     *
     * @param <T> The type of the extension
     * @return An extension object
     */
    default <T> T getExtension() {return null;}
}
