package me.matrix4f.classcloak.action.string;

import org.objectweb.asm.tree.*;
import me.matrix4f.classcloak.Action;
import me.matrix4f.classcloak.ClassCloak;
import me.matrix4f.classcloak.action.ObfGlobal;
import me.matrix4f.classcloak.util.BytecodeUtils;

import java.util.List;
import java.util.Objects;

import static org.objectweb.asm.Opcodes.*;
import static me.matrix4f.classcloak.Globals.LOGGER;
import static me.matrix4f.classcloak.action.ObfGlobal.stringSettings;
import static me.matrix4f.classcloak.action.ObfSettings.StringObfSettings.FAST_STRINGS;
import static me.matrix4f.classcloak.action.ObfSettings.StringObfSettings.INT_ARRAYS;

public class StringObfuscateAction extends Action {

    @Override
    public void execute() {
        LOGGER.info("Performing string encryption.");

        StringObfArrayMethod arrayObfuscator = new StringObfArrayMethod();
        StringObfFastStringsMethod fastStringsObfuscator = new StringObfFastStringsMethod();;
        StringObfMethod obfMethod = null;

        switch(stringSettings.obfMethod) {
            case INT_ARRAYS:
                obfMethod = arrayObfuscator;
                break;
            case FAST_STRINGS:
                obfMethod = fastStringsObfuscator;
                break;
        }
        Objects.requireNonNull(obfMethod, "String action method is null - this should never happen");

        List<ClassNode> classNodes = ObfGlobal.classes;

        StringObfInfo info = obfMethod.generateInfo();
        classNodes.add(info.getDeobfuscatorClass());

        classNodes.forEach(classNode -> {
            if(stringSettings.shouldExclude(classNode))
                return;

            classNode.methods.forEach(method -> {
                if(stringSettings.shouldExclude(method, classNode))
                    return;

                switch (stringSettings.obfMethod) {
                    case INT_ARRAYS:
                        BytecodeUtils.streamInstructions(LdcInsnNode.class, method)
                                .filter(ldc -> ldc.cst instanceof String)
                                .forEach(ldc -> {
                                    long hash = ClassCloak.rand.nextLong();
                                    String str = (String) ldc.cst;
                                    int[] data = arrayObfuscator.obfuscate(str, hash);
                                    InsnList instructions = method.instructions;

                                    InsnList inject = new InsnList();
                                    inject.add(new LdcInsnNode(data.length));
                                    inject.add(new IntInsnNode(NEWARRAY, T_INT));
                                    for (int i = 0; i < data.length; i++) {
                                        inject.add(new InsnNode(DUP));
                                        inject.add(new LdcInsnNode(i));
                                        inject.add(new LdcInsnNode(data[i]));
                                        inject.add(new InsnNode(IASTORE));
                                    }
                                    inject.add(new LdcInsnNode(hash));
                                    inject.add(new MethodInsnNode(INVOKESTATIC, info.getDeobfuscatorClass().name, info.getDeobfuscateMethodName(), info.getDeobfuscateMethodDesc(), false));

                                    instructions.insertBefore(ldc, inject);
                                    instructions.remove(ldc);
                                });
                        break;
                    case FAST_STRINGS:
                        BytecodeUtils.streamInstructions(LdcInsnNode.class, method)
                                .filter(ldc -> ldc.cst instanceof String)
                                .forEach(ldc -> {
                                    InsnList a = new InsnList();
                                    a.add(new LdcInsnNode(StringObfFastStringsMethod.obfuscate((String) ldc.cst)));;
                                    a.add(new MethodInsnNode(INVOKESTATIC, info.getDeobfuscatorClass().name, info.getDeobfuscateMethodName(), info.getDeobfuscateMethodDesc(), false));

                                    method.instructions.insertBefore(ldc, a);
                                    method.instructions.remove(ldc);
                                });
                        break;
                }
            });
        });
    }

}
