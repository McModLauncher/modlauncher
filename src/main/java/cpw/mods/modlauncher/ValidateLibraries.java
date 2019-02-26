package cpw.mods.modlauncher;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class ValidateLibraries {
    static void validate() {
        List<Map.Entry<String,String>> toCheck = Arrays.asList(
                pair("log4j", "org.apache.logging.log4j.LogManager"),
                pair("asm", "org.objectweb.asm.ClassVisitor"),
                pair("joptsimple", "joptsimple.OptionParser")
        );
        final List<Map.Entry<String, String>> brokenLibs = toCheck.stream().filter(ValidateLibraries::tryLoad).collect(Collectors.toList());
        brokenLibs.forEach(e->System.err.println("Failed to find class associated with library "+e.getKey()));
        if (!brokenLibs.isEmpty()) throw new InvalidLauncherSetupException("Missing classes, cannot continue");
    }

    private static boolean tryLoad(final Map.Entry<String, String> nameClazz) {
        try {
            Class.forName(nameClazz.getValue(), false, ClassLoader.getSystemClassLoader());
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }

    private static Map.Entry<String,String> pair(String name, String clazzName) {
        return new AbstractMap.SimpleEntry<>(name, clazzName);
    }
}
