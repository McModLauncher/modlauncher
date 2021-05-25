module cpw.mods.modlauncher.testing {
    requires cpw.mods.modlauncher.api;
    requires org.junit.jupiter.api;
    requires org.apache.logging.log4j;
    requires cpw.mods.modlauncher.testjars;
    requires powermock.reflect;
    requires cpw.mods.modlauncher;
    uses cpw.mods.modlauncher.api.ILaunchHandlerService;
    uses cpw.mods.modlauncher.api.INameMappingService;
    uses cpw.mods.modlauncher.api.ITransformationService;
    uses cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
    uses cpw.mods.modlauncher.serviceapi.ITransformerDiscoveryService;
}