# Modlauncher plumbing

## Major parts

* ```ILaunchPluginService```
    
    ServiceLoader element. Intended for systemwide transformation capabilities: Mixin, Access Transformers.
    
    Loaded from the classpath at system instantiation, as an immutable list.
    
    Plugin architecture so the individual elements can be upgraded without the core system needing a change.
    
    Separated API package for minimal required surface area for consumers of this API.
    
    It is not expected to receive widespread adoption as custom launch profiles will be needed.
    
    **Forge note** It's possible we could implement a ForgePatcher plugin here.

* ```ILaunchHandlerService```
    
    ServiceLoader element. Intended to control the launch target (the main class to be launched after setup is complete). 
    Has a default to launch vanilla minecraft.
    
    Loaded from the classpath at system instantiation, as an immutable list. Target is selected by name from list of 
    targets through command line argument ```--launchTarget```.
    
    Plugin architecture so mods can provide alternatives.
    
    It is not expected to receive widespread adoption.
    
    **Note** Metadata about the launch target should be provided (NYI)
    
    **Forge note** Forge will provide a semi-patched and deobfuscated minecraft module here.
    
* ```ITransformationService```

    ServiceLoader element. Intended to allow mod systems to inject class transforming code into the classloader and
    additional command line arguments to the system for configuration purposes.
    
    Loaded from classpath at system instantiation, as a semi-immutable list. All transformers will be loaded and given
    lifecycle events where they can create ```ITransformer``` instances which ultimately act on classloaded code.
    
    There will be the possibility to add additional services to this list during early setup phases (example: FML discovers 
    LiteLoader in the FML mods directory).
    
    Plugin architecture because this is what mod systems will be expected to deliver.
    
    It is expected that high level mod systems tweakers, such as FML, LiteLoader and others will implement this.
    
    There may be a vanilla-legacy instance of this as well.
    
    **Note** Metadata for communication between systems is NYI
    
## How TransformationService should work

* During first init phase, it should identify any resources which it believes should be a loading candidate, and offer
them to the system. This will allow modlauncher to identify additional transformation services.
* Subsequent phases should create and register transformers, prior to launch.

## How LaunchPlugins should work
* They will receive an initialization step, where they can query the launch service that has been targetted. This will
contain the metadata they need to discover structure. Artifacts offered to modlauncher (Jars for loading) will be offered
to launch plugins as well, so they can integrate additional changes.

##How forge might work
* ForgeFML provides a TransformationService.
* ForgePatchedMC is a LaunchHandlerService - providing a pre-patched and deobfuscated Minecraft for launch into.
* ForgePatcherPlugin is a LaunchPluginService providing additional hot-patching if necessary.
* AccessTransformer is a LaunchPluginService providing AT capabilities.
* Mixin is a LaunchPluginService providing enhanced runtime patch capabilities.
