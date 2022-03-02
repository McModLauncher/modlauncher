open module cpw.mods.modlauncher.test {
    requires cpw.mods.modlauncher;
    requires cpw.mods.modlauncher.testjars;
    requires cpw.mods.securejarhandler;
    
    requires org.junit.jupiter.api;
    requires powermock.core;
    requires powermock.reflect;
    requires org.objectweb.asm;
    requires org.objectweb.asm.tree;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires jopt.simple;
    requires static org.jetbrains.annotations;

    exports cpw.mods.modlauncher.test;

    uses cpw.mods.modlauncher.testjar.ITestServiceLoader;

    provides cpw.mods.modlauncher.api.ILaunchHandlerService with cpw.mods.modlauncher.test.MockLauncherHandlerService;
    provides cpw.mods.modlauncher.api.ITransformationService with cpw.mods.modlauncher.test.MockTransformerService;
}
