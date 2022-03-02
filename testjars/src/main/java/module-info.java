module cpw.mods.modlauncher.testjars {
    exports cpw.mods.modlauncher.testjar;
    
    provides cpw.mods.modlauncher.testjar.ITestServiceLoader with cpw.mods.modlauncher.testjar.TestServiceLoader;
}
