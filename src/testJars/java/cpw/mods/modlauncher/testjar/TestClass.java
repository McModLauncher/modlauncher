package cpw.mods.modlauncher.testjar;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test class loaded by tests and manipulated by transformers
 */
public class TestClass
{
    private String cheese = "FISH";

    private String testMethod(String cheese)
    {
        String wheee = "HELLO";
        return Stream.of(cheese, wheee).collect(Collectors.joining(" "));
    }
}
