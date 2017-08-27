## FML Access Transformer file specification

The access transformer file format allows for targetting classes and their members for transformation 
of the access_flags element of the member. This allows for transforming private methods into public methods,
for example.

[Java Language Specification for class files](https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html)

### Access transformations
* ````public```` transforms the member to be public
* ````protected```` transforms the member to be protected
* ````private```` transforms the member to be private
* ````default```` transforms the member to be package private ('default')

Any access transform can have ````+f```` or ````-f```` appended to add or remove the ACC_FINAL flag.

### Targets
Targets can be classes, methods or fields. The ````*```` wildcard can replace a method or field name.

Methods are identified by parentheses after the member name.

### Comments
Comments are delimited by a ````#```` sign. All content until end of line is ignored.

### Important note
An access transformation pre-processor is *required* to allow compilation against access-transformed compiled code.

ForgeGradle has such a tool. Other tools are available to do the same thing.

### Directive lines
####Class transformer
````
<access modifier> <classname>
````
Example. Transforms the class ````net.minecraft.world.gen.structure.StructureVillagePieces$Village```` to public.
````
public net.minecraft.world.gen.structure.StructureVillagePieces$Village
````

####Method transformer
````
<access modifier> <classname> <methodname><methodsignature>
````
Example. Transforms the method ````boolean net.minecraft.world.World.isValid(BlockPos p)```` to public.
````
public net.minecraft.world.World isValid(Lnet/minecraft/util/math/BlockPos;)Z
````

Example. Transforms all methods in class ````net.minecraft.world.biome.Biome```` to public.
````
public net.minecraft.world.biome.Biome *()
````
####Field transformer
````
<access modifier> <classname> <fieldname>
````
Example. Transforms the field ````net.minecraft.world.WorldType.worldTypes```` to public with the final flag removed.
````
public-f net.minecraft.world.WorldType worldTypes
````
Example. Transforms all fields in class ````net.minecraft.client.gui.GuiIngame```` to protected.
````
protected net.minecraft.client.gui.GuiIngame *
````