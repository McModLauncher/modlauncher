package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.api.LauncherService;
import cpw.mods.modlauncher.api.Target;
import cpw.mods.modlauncher.api.Transformer;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionSpecBuilder;
import org.objectweb.asm.tree.ClassNode;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Created by cpw on 22/12/16.
 */
public class TestLauncherService implements LauncherService
{

    private ArgumentAcceptingOptionSpec<String> modsList;
    private ArgumentAcceptingOptionSpec<Integer> modlists;

    @Override
    public String name()
    {
        return "test";
    }

    @Override
    public void arguments(BiFunction<String, String, OptionSpecBuilder> argumentBuilder)
    {
        modsList = argumentBuilder.apply("mods", "CSV list of mods to load").withRequiredArg().withValuesSeparatedBy(",").ofType(String.class);
    }

    @Override
    public void argumentValues(OptionResult result)
    {
        result.values(modsList);
    }

    @Override
    public List<Transformer<?>> transformers()
    {
        Target.ClassTarget ct = new Target.ClassTarget("a", "b", "C");
        Transformer<ClassNode> tf = ct.makeTransformer(n -> n);
        return Arrays.asList(tf);
    }
}
