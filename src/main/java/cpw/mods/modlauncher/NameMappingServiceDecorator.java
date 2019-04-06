package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Decorator for Naming Services
 */
class NameMappingServiceDecorator {
    private final INameMappingService service;

    public NameMappingServiceDecorator(INameMappingService service) {
        this.service = service;
    }

    public boolean validTarget(final String origin) {
        return Objects.equals(this.service.understanding().getValue(), origin);
    }
    public String understands() {
        return this.service.understanding().getKey();
    }

    public BiFunction<INameMappingService.Domain, String, String> function() {
        return this.service.namingFunction();
    }

    public String toString() {
        return this.service.mappingName() + ":" + this.service.mappingVersion();
    }
}
