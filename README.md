##ClassCloak: a free, open-source obfuscator for Java programs

ClassCloak aims to be the best free, robust, yet powerful obfuscator for your Java programs.

**The Why**

Why put your code through a Java obfuscator? Can't you distribute your software as-is?

The short answer: **NO!**

Java code, in its raw, compiled form, is inherently insecure and easy to reverse engineer.
People can run decompilers to recover source code from your distributed JAR file, but, worse yet, serious security exploits could be leaked if hackers could view this sensitive data in the JAR, such as passwords and authentication server IPs.
Believe it or not, this information is frighteningly simple to retrieve from a simple decompiler. If you don't use an obfuscator, hackers have an easy key to unlock the secrets of your application.

**But it doesn't have to be that way.**

Advanced Java Obfuscators such as ClassCloak perform a multtifaceted approach in order to secure your JAR, including things such as renaming classes and fields to useless names, destroying local variables tables to misdirect hackers, all the while handling advanced reflection API calls. 

What's more, ClassCloak is completely free to use, for people like you!

**The How**

First, ClassCloak scans a script file to determine which actions you wish to perform on your input program.
Next, our engine decompiles your Java JAR program class by class, retrieving bytecode information from the classes.
Once this has been done, ClassCloak utilizes a powerful bytecode library called ASM along with a StackInstructionHelper. All bytecode transformations are now applied to your class bytecode.
Lastly, your JAR is saved into an exported JAR archive, in which you can find your obfuscated program!

**Features**
Current features:
- A heavy-duty scripting language that allows you to concisely express basic tasks, yet unfolds to allow for much more!
- Support for name obfuscation (changes your class, field, and method names to meaningless ones)
- Support for heavy-duty string obfuscation, so that attackers to your application can't use Strings to reverse-engineer your code
- Support for handling complex reflection API calls! Yes, you heard that right! Reflection *with* name obfuscation? Yup!
	- ClassCloak generates mappings for every field, method, or classname that it obfuscates with its Name Obfuscate technology. Using these mappings, it generates a fake class which contains them and remaps them to their obfuscated names at runtime. 2,000 lines of code went into this great feature! 

Planned features:
- Flow obfuscation, so that decompilers fail to decompile code
- Saving and loading mappings from a file
- Multi-level string encryption
- Invokedynamic call site method & field hiding
- Reflection method hiding