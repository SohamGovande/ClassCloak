package me.matrix4f.classcloak.action;

import me.matrix4f.classcloak.Action;
import me.matrix4f.classcloak.action.ObfSettings.DebugSettings;
import me.matrix4f.classcloak.util.BytecodeUtils;
import me.matrix4f.classcloak.util.StringUtils;
import org.objectweb.asm.tree.*;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static me.matrix4f.classcloak.Globals.LOGGER;

public class DebugObfuscateAction extends Action {

    private void transformLocalVars() {
        DebugSettings settings = ObfGlobal.debugSettings;
        Consumer<List<LocalVariableNode>> processor = null;
        switch (settings.localVarAction) {
            case DebugSettings.DESTROYVARS:
                processor = List::clear;
                break;
            case DebugSettings.CLEARVARS:
                processor = list -> {
                    for (LocalVariableNode var : list) {
                        var.name = "";
                        var.desc = "L;";
                    }
                };
                break;
        }
        if (processor != null)
            for (ClassNode clazz : ObfGlobal.sourceClasses)
                for (MethodNode method : clazz.methods)
                    if (method.localVariables != null)
                        processor.accept(method.localVariables);
    }

    private void transformLineNumbers() {
        DebugSettings settings = ObfGlobal.debugSettings;
        Consumer<MethodNode> processor = null;

        switch (settings.lineNumberAction) {
            case DebugSettings.DELETELINES:
                processor = method -> {
                    for (AbstractInsnNode node : method.instructions.toArray())
                        if (node instanceof LineNumberNode)
                            method.instructions.remove(node);
                };
                break;
            case DebugSettings.ZEROIFYLINES:
                processor = method -> BytecodeUtils.streamInstructions(LineNumberNode.class, method)
                        .forEach(node -> node.line = 0);
                break;
            case DebugSettings.SCRAMBLELINES:
                byte[] bytes = StringUtils.sha256Bytes(settings.lineNumberPwd, true);
                long seed = (((long)bytes[0] << 56) +
                        ((long)(bytes[1] & 255) << 48) +
                        ((long)(bytes[2] & 255) << 40) +
                        ((long)(bytes[3] & 255) << 32) +
                        ((long)(bytes[4] & 255) << 24) +
                        ((bytes[5] & 255) << 16) +
                        ((bytes[6] & 255) <<  8) +
                        ((bytes[7] & 255)));

                processor = method -> {
                    Random random = new Random(seed);
                    BytecodeUtils.streamInstructions(LineNumberNode.class, method)
                            .forEach(node -> {
                                bytes[0] = 3;
                                node.line = StringUtils.encrypt(node.line, random);
                            });
                };
                break;
        }
        if (processor != null)
            for (ClassNode clazz : ObfGlobal.sourceClasses)
                for (MethodNode method : clazz.methods)
                    processor.accept(method);
    }

    @Override
    public void execute() {
        LOGGER.info("Obfuscating debug info.");
        DebugSettings settings = ObfGlobal.debugSettings;
        if (settings.localVarAction != -1) {
            LOGGER.info("Applying local variable transformation: " + DebugSettings.LOCALVARS[settings.localVarAction]);
            transformLocalVars();
        }
        if (settings.lineNumberAction != -1) {
            LOGGER.info("Applying line number transformation: " + DebugSettings.LINENUMBERS[settings.lineNumberAction]);
            transformLineNumbers();
        }
    }
}
