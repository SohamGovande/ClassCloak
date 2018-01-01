package me.matrix4f.classcloak.action;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;
import me.matrix4f.classcloak.Action;
import me.matrix4f.classcloak.ClassCloak;
import me.matrix4f.classcloak.util.BytecodeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static me.matrix4f.classcloak.Globals.LOGGER;
import static me.matrix4f.classcloak.action.ObfGlobal.lineSettings;
import static me.matrix4f.classcloak.action.ObfSettings.LineObfSettings.*;

public class LineNumberObfuscateAction extends Action {
    @Override
    public void execute() {
        LOGGER.info("Applying line number transformations with obf method");

        List<ClassNode> classNodes = ObfGlobal.classes;
        classNodes.forEach(classNode -> {
            if(lineSettings.shouldExclude(classNode))
                return;
            classNode.methods.forEach(method -> {
                if(lineSettings.shouldExclude(method, classNode))
                    return;

                List<LineNumberNode> lineNumbers = BytecodeUtils.streamInstructions(LineNumberNode.class, method).collect(Collectors.toList());
                switch (lineSettings.obfMethod) {
                    case DELETE:
                        lineNumbers.forEach(method.instructions::remove);
                        break;
                    case RANDOM: {
                        lineNumbers.forEach(node -> node.line = ClassCloak.rand.nextInt());
                        break;
                    }
                    case SCRAMBLE: {
                        List<LineNumberNode> duplicate = new ArrayList<>(lineNumbers);
                        lineNumbers.forEach(lnn -> {
                            LineNumberNode copy = duplicate.get(ClassCloak.rand.nextInt(duplicate.size()));
                            lnn.line = copy.line;
                            duplicate.remove(copy);
                        });
                        break;
                    }
                    case SINGLE: {
                        lineNumbers.forEach(node -> node.line = 0);
                    }
                }
            });
        });
    }
}
