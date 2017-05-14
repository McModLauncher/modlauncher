package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.Environment;
import cpw.mods.modlauncher.api.TypesafeMap;

import java.util.Optional;

/**
 * Environment implementation class
 */
@SuppressWarnings("WeakerAccess")
public final class EnvironmentImpl implements Environment
{
    private TypesafeMap environment;

    EnvironmentImpl()
    {
        environment = new TypesafeMap(Environment.class);
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
