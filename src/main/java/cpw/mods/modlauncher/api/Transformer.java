package cpw.mods.modlauncher.api;

/**
 * Created by cpw on 26/12/16.
 */
public interface Transformer<T>
{
    Target<T> target();
    T transform(T input);
}
