package me.matrix4f.classcloak.action.name.map;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import me.matrix4f.classcloak.action.ObfGlobal;
import me.matrix4f.classcloak.mapping.Mapping;
import me.matrix4f.classcloak.mapping.Mappings.*;
import me.matrix4f.classcloak.util.parsing.ParsingUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NameObfMap {

    public static Map<ClassNode, ClassReference> Classes = new HashMap<>();
    public static Map<FieldNode, FieldReference> Fields = new HashMap<>();
    public static Map<MethodNode, MethodReference> Methods = new HashMap<>();

    public static void takePreSnapshot() {
        Classes.clear();
        Fields.clear();
        Methods.clear();

        ObfGlobal.sourceClasses.forEach(classNode -> {
            ClassReference cr = new ClassReference(classNode.name, classNode.name);
            Classes.put(classNode, cr);

            classNode.fields.forEach(fieldNode -> {
                Fields.put(fieldNode, new FieldReference(cr, fieldNode.name, fieldNode.name, fieldNode.desc, fieldNode.desc));
            });

            classNode.methods.forEach(methodNode -> {
                Methods.put(methodNode, new MethodReference(cr, methodNode.name, methodNode.name, methodNode.desc, methodNode.desc));
            });
        });
    }

    public static void takePostSnapshot() {
        List<Mapping> mappings = ObfGlobal.remapper.getMappings().asList();
        List<ClassNameMapping> classNameChanges = mappings.stream()
                .filter(mapping -> mapping instanceof ClassNameMapping)
                .map(mapping -> (ClassNameMapping) mapping)
                .collect(Collectors.toList());
        List<MethodNameMapping> methodNameChanges = mappings.stream()
                .filter(mapping -> mapping instanceof MethodNameMapping)
                .map(mapping -> (MethodNameMapping) mapping)
                .collect(Collectors.toList());
        List<FieldNameMapping> fieldNameChanges = mappings.stream()
                .filter(mapping -> mapping instanceof FieldNameMapping)
                .map(mapping -> (FieldNameMapping) mapping)
                .collect(Collectors.toList());
        List<FieldDescMapping> fieldDescChanges = mappings.stream()
                .filter(mapping -> mapping instanceof FieldDescMapping)
                .map(mapping -> (FieldDescMapping) mapping)
                .collect(Collectors.toList());
        List<MethodDescMapping> methodDescChanges = mappings.stream()
                .filter(mapping -> mapping instanceof MethodDescMapping)
                .map(mapping -> (MethodDescMapping) mapping)
                .collect(Collectors.toList());

        classNameChanges.forEach(mapping -> {
            if(Classes.containsKey(mapping.getNode()))
                Classes.get(mapping.getNode()).newName = mapping.getArg();
        });

        methodNameChanges.forEach(mapping -> {
            if(Methods.containsKey(mapping.getNode()))
                Methods.get(mapping.getNode()).newName = mapping.getArg();
        });

        fieldNameChanges.forEach(mapping -> {
            if(Fields.containsKey(mapping.getNode()))
                Fields.get(mapping.getNode()).newName = mapping.getArg();
        });

        methodDescChanges.forEach(mapping -> {
            if(Methods.containsKey(mapping.getNode()))
                Methods.get(mapping.getNode()).newDescriptor = mapping.getArg();
        });

        fieldDescChanges.forEach(mapping -> {
            if(Fields.containsKey(mapping.getNode()))
                Fields.get(mapping.getNode()).newDescriptor = mapping.getArg();
        });
    }

    public static ClassReference findClassByOld(String oldName) {
        return Classes.values().stream()
                .filter(clazz -> ParsingUtils.conformsToWildcards(oldName, clazz.oldName))
                .findFirst()
                .orElse(null);
    }

    public static ClassReference findClassByNew(String newName) {
        return Classes.values().stream()
                .filter(clazz -> ParsingUtils.conformsToWildcards(newName, clazz.newName))
                .findFirst()
                .orElse(null);
    }

    public static FieldReference findFieldByOld(ClassReference parent, String oldName, String oldDescriptor) {
        return Fields.values().stream()
                .filter(field -> field.parent == parent 
                        && ParsingUtils.conformsToWildcards(oldName, field.oldName)
                        && ParsingUtils.conformsToWildcards(oldDescriptor, field.oldDescriptor))
                .findFirst()
                .orElse(null);
    }
    
    public static FieldReference findFieldByNew(ClassReference parent, String newName, String newDescriptor) {
        return Fields.values().stream()
                .filter(field -> field.parent == parent
                        && ParsingUtils.conformsToWildcards(newName, field.newName)
                        && ParsingUtils.conformsToWildcards(newDescriptor, field.newDescriptor))
                .findFirst()
                .orElse(null);
    }
    
    public static MethodReference findMethodByOld(ClassReference parent, String oldName, String oldDescriptor) {
        return Methods.values().stream()
                .filter(field -> field.parent == parent
                        && ParsingUtils.conformsToWildcards(oldName, field.oldName)
                        && ParsingUtils.conformsToWildcards(oldDescriptor, field.oldDescriptor))
                .findFirst()
                .orElse(null);
    }

    public static MethodReference findMethodByNew(ClassReference parent, String newName, String newDescriptor) {
        return Methods.values().stream()
                .filter(field -> field.parent == parent
                        && ParsingUtils.conformsToWildcards(newName, field.newName)
                        && ParsingUtils.conformsToWildcards(newDescriptor, field.newDescriptor))
                .findFirst()
                .orElse(null);
    }
}
