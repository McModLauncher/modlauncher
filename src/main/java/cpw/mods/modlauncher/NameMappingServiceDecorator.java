package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.INameMappingService;

/**
 * Decorator for Naming Services
 */
public class NameMappingServiceDecorator
{
    private final INameMappingService service;

    public NameMappingServiceDecorator(INameMappingService service)
    {
        this.service = service;
    }
}
