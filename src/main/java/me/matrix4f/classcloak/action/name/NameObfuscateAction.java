package me.matrix4f.classcloak.action.name;

import org.objectweb.asm.tree.*;
import me.matrix4f.classcloak.Action;
import me.matrix4f.classcloak.Globals;
import me.matrix4f.classcloak.action.ObfGlobal;
import me.matrix4f.classcloak.action.ObfSettings;
import me.matrix4f.classcloak.action.name.creation.ClassNameCreator;
import me.matrix4f.classcloak.action.name.creation.FieldNameCreator;
import me.matrix4f.classcloak.action.name.creation.MethodNameCreator;
import me.matrix4f.classcloak.action.name.map.ClassReference;
import me.matrix4f.classcloak.action.name.map.NameObfMap;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;
import static me.matrix4f.classcloak.action.ObfGlobal.manifest;
import static me.matrix4f.classcloak.action.ObfGlobal.remapper;

public class NameObfuscateAction extends Action implements Globals {

    private List<ClassNode> classNodes;

    @Override
    public void execute() {
        this.classNodes = ObfGlobal.sourceClasses;
        LOGGER.info("Performing name obfuscation.");

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

            classNode.methods.forEach(method -> {
                boolean isMainMethod = isMainClass && method.access == ACC_PUBLIC+ACC_STATIC && method.name.equals("main") && method.desc.equals("([Ljava/lang/String;)V");
                boolean isExplicitlyExcluded = obfSettings.shouldExclude(method, classNode);
                boolean isUnrenamable = method.name.charAt(0) == '<';
                boolean canRename = !isMainMethod && !isUnrenamable && !isExplicitlyExcluded;
                //is main method
                if(canRename)
                    remapper.changeMethodName(ObfGlobal.nameSettings.exclusions, classNode, method, mnc.getName(method, false));
            });

            classNode.fields.forEach(field -> {
                if(obfSettings.shouldExclude(field, classNode))
                    return;
                remapper.changeFieldName(classNode, field, fnc.getName(field, false));
            });

            if(!isMainClass && !obfSettings.shouldExclude(classNode)) {
                classref.newName = ClassNameCreator.instance.getName(classNode);
                remapper.changeClassName(classNode.name, classref.newName);
            }
        });
        NameObfMap.takePostSnapshot();
    }

}
