module cpw.mods.modlauncher {
    requires java.base;
    requires org.apache.logging.log4j;
    requires org.objectweb.asm.tree;
    requires org.apache.logging.log4j.core;
    requires jopt.simple;
    requires cpw.mods.grossjava9hacks;
    requires static annotations;
    exports cpw.mods.modlauncher.log to org.apache.logging.log4j.core;
    exports cpw.mods.modlauncher.serviceapi;
    exports cpw.mods.modlauncher.api;
    exports cpw.mods.modlauncher;
    uses cpw.mods.modlauncher.api.ILaunchHandlerService;
    uses cpw.mods.modlauncher.api.INameMappingService;
    uses cpw.mods.modlauncher.api.ITransformationService;
    uses cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
    uses cpw.mods.modlauncher.serviceapi.ITransformerDiscoveryService;
}