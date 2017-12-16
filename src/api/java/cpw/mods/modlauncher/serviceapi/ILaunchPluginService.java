package cpw.mods.modlauncher.serviceapi;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.nio.file.*;

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
     * Adds a resource to this plugin for processing by it. Minecraft will always be the first resource offered.
     *
     * Transformers may pass additional resources.
     *
     * @param resource The resource to be considered by this plugin.
     */
    void addResource(Path resource);

    /**
     * Each class loaded is offered to the plugin for processing. All plugins will run before any transformers,
     * but ordering between plugins is not known.
     *
     * @param classNode the classnode to process
     * @param classType the name of the class
     * @return the processed classnode
     */
    ClassNode processClass(ClassNode classNode, final Type classType);

    /**
     * Get a plugin specific extension object from the plugin. This can be used to expose proprietary interfaces
     * to Launchers without ModLauncher needing to understand them.
     *
     * @param <T> The type of the extension
     * @return An extension object
     */
    <T> T getExtension();

    /**
     * If this plugin wants to receive the {@link ClassNode} into {@link #processClass}
     * @param classType the class to consider
     * @return if this plugin wants to receive a call on processClass with the classNode
     */
    boolean handlesClass(Type classType);
}
