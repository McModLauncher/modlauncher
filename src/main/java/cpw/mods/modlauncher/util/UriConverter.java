package cpw.mods.modlauncher.util;

import joptsimple.ValueConverter;

import java.net.URI;

public class UriConverter implements ValueConverter<URI>  {
    @Override
    public URI convert(String value) {
        return URI.create(value);
    }

    @Override
    public Class<? extends URI> valueType() {
        return URI.class;
    }

    @Override
    public String valuePattern() {
        return "[scheme:]scheme-specific-part[#fragment]";
    }
}
