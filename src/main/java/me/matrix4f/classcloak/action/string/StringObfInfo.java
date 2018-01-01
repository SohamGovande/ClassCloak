package me.matrix4f.classcloak.action.string;

import org.objectweb.asm.tree.ClassNode;

public class StringObfInfo {

    private ClassNode deobfuscatorClass;
    private String deobfuscateMethodName;
    private String deobfuscateMethodDesc;

    public StringObfInfo(ClassNode deobfuscatorClass, String deobfuscateMethodName, String deobfuscateMethodDesc) {
        this.deobfuscatorClass = deobfuscatorClass;
        this.deobfuscateMethodName = deobfuscateMethodName;
        this.deobfuscateMethodDesc = deobfuscateMethodDesc;
    }

    public ClassNode getDeobfuscatorClass() {
        return deobfuscatorClass;
    }

    public String getDeobfuscateMethodName() {
        return deobfuscateMethodName;
    }

    public String getDeobfuscateMethodDesc() {
        return deobfuscateMethodDesc;
    }
}
