package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.TypesafeMap;

import java.util.Optional;

@SuppressWarnings("WeakerAccess")
public final class Environment implements IEnvironment
{
    private TypesafeMap environment;

    Environment()
    {
        environment = new TypesafeMap(IEnvironment.class);
    }

    @Override
    public final <T> Optional<T> getProperty(TypesafeMap.Key<T> key)
    {
        return environment.get(key);
    }

    public TypesafeMap getMap()
    {
        return environment;
    }
}
