package me.matrix4f.classcloak.action.name;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import me.matrix4f.classcloak.Action;
import me.matrix4f.classcloak.Globals;
import me.matrix4f.classcloak.action.ObfGlobal;
import me.matrix4f.classcloak.action.ObfSettings;
import me.matrix4f.classcloak.action.name.namecreation.ClassNameCreator;
import me.matrix4f.classcloak.action.name.namecreation.FieldNameCreator;
import me.matrix4f.classcloak.action.name.namecreation.MethodNameCreator;
import me.matrix4f.classcloak.action.name.nameofbmap.ClassReference;
import me.matrix4f.classcloak.action.name.nameofbmap.NameObfMap;
import me.matrix4f.classcloak.action.opaquepredicates.NodeOpaquePred;
import me.matrix4f.classcloak.target.ClassHierarchy;
import me.matrix4f.classcloak.util.BytecodeUtils;
import me.matrix4f.classcloak.util.NodeNameRemapper;
import me.matrix4f.classcloak.util.interpreter.StackBranchInterpreter;
import me.matrix4f.classcloak.util.interpreter.StackInterpreter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.*;
import static me.matrix4f.classcloak.action.ObfGlobal.manifest;
import static me.matrix4f.classcloak.action.ObfGlobal.remapper;

public class NameObfuscateAction extends Action implements Globals {

    private List<ClassNode> classNodes;

    @Override
    public void execute() {
        this.classNodes = ObfGlobal.classes;
        LOGGER.info("Performing name obfuscation.");

        ClassHierarchy.findOrLoad(classNodes, "me/matrix4f/test/Main")
                .methods
                .stream()
                .filter(node -> node.name.equals("sayHi"))
                .findFirst()
                .ifPresent(methodNode -> {
                    System.out.println("FOUND METHOD sayHi");
                    StackInterpreter interpreter = new StackInterpreter(methodNode);
                    interpreter.interpret();
                    System.out.println(interpreter.getBranches().size());
                    List<Field> fields = new ArrayList<>(Arrays.asList(Opcodes.class.getDeclaredFields()));
                    Collections.reverse(fields);

                    interpreter.getBranches()
                            .stream()
                            .map(StackBranchInterpreter::getInsns)
                            .forEach(nodes -> {
                                System.out.println("BRANCH");
                                nodes.forEach(node -> {
                                    if(node.getOpcode() > 0) {
                                        fields.stream()
                                                .filter(field -> {
                                                    try {
                                                        return field.get(null).equals(node.getOpcode());
                                                    } catch (IllegalAccessException e) {
                                                        return false;
                                                    }
                                                })
                                                .findFirst()
                                                .ifPresent(field -> {
                                                    System.out.println(field.getName().toLowerCase());
                                                });
                                    }
                                });
                            });

                });

        if(1 == 1)
            return;

        NameObfMap.takePreSnapshot();

        String mainClass = manifest == null ? "" : //will result to it being ignored
                manifest.getMainAttributes()
                        .entrySet()
                        .stream()
                        .filter(e-> e.getKey().toString().startsWith("Main-Class"))
                        .map(e-> e.getValue().toString())
                        .findFirst()
                        .orElse("")
                        .replace('.','/');

        ObfSettings obfSettings = ObfGlobal.nameSettings;
        classNodes.forEach(classNode -> {
            boolean isMainClass = classNode.name.equals(mainClass);
            ClassReference classref = new ClassReference(classNode.name, classNode.name);
            MethodNameCreator mnc = MethodNameCreator.openClass(classNode);
            FieldNameCreator fnc = FieldNameCreator.openClass(classNode);

            classNode.methods
                    .stream()
                    .filter(method-> method.name.charAt(0) != '<')
                    .forEach(method -> {
                        if(obfSettings.shouldExclude(method, classNode))
                            return;

                        //is main method
                        if(!(isMainClass && method.access == ACC_PUBLIC+ACC_STATIC && method.name.equals("main") && method.desc.equals("([Ljava/lang/String;)V"))) {
                            String newname = mnc.getName(method, false);
                            remapper.changeMethodName(ObfGlobal.nameSettings.exclusions, classNode, method, newname);
                        }

                        if(method.localVariables != null) { //interfaces have null local vars
                            method.localVariables.forEach(var -> var.name = "");
                        }
                    });

            classNode.fields.forEach(field -> {
                if(obfSettings.shouldExclude(field, classNode))
                    return;
                String newname = fnc.getName(field, false);
                remapper.changeFieldName(classNode, field, newname);
            });

            if(!isMainClass && !obfSettings.shouldExclude(classNode)) {
                classref.newName = ClassNameCreator.instance.getName(classNode);
                remapper.changeClassName(classNode.name, classref.newName);
            }
        });

        for(int i = 0; i < classNodes.size(); i++) {
            ClassNode classNode = classNodes.get(i);
            boolean isMainClass = classNode.name.equals(mainClass);
            for(int j = 0; j < classNodes.get(i).methods.size(); j++) {
                MethodNode method = classNodes.get(i).methods.get(j);

                //is main method
                if(isMainClass && method.access == ACC_PUBLIC+ACC_STATIC && method.name.equals("main") && method.desc.equals("([Ljava/lang/String;)V")) {
                }
            }
        }

        NameObfMap.takePostSnapshot();
    }

}
