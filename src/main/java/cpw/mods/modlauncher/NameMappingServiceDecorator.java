package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;

/**
 * Decorator for Naming Services
 */
class NameMappingServiceDecorator {
    private final INameMappingService service;

    public NameMappingServiceDecorator(INameMappingService service) {
        this.service = service;
    }
}
