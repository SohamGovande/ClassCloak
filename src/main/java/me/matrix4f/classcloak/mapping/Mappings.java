package me.matrix4f.classcloak.mapping;

import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Mappings {

    private List<Mapping> mappings = new ArrayList<>();

    public void apply() {
        mappings.forEach(Mapping::apply);
    }

    public boolean add(Mapping mapping) {
        return mappings.add(mapping);
    }

    public List<Mapping> asList() {
        return mappings;
    }

    public static class FieldInsnNodeNameMapping extends Mapping<FieldInsnNode, String> {
        public FieldInsnNodeNameMapping(FieldInsnNode node, String arg) { super(node, arg); }
        public void apply() { node.name = arg; }
    }

    public static class FieldInsnNodeDescMapping extends Mapping<FieldInsnNode, String> {
        public FieldInsnNodeDescMapping(FieldInsnNode node, String arg) { super(node, arg); }
        public void apply() { node.desc = arg; }
    }

    public static class FieldInsnNodeOwnerMapping extends Mapping<FieldInsnNode, String> {
        public FieldInsnNodeOwnerMapping(FieldInsnNode node, String arg) { super(node, arg); }
        public void apply() { node.owner = arg; }
    }

    public static class MethodInsnNodeNameMapping extends Mapping<MethodInsnNode, String> {
        public MethodInsnNodeNameMapping(MethodInsnNode node, String arg) { super(node, arg); }
        public void apply() { node.name = arg; }
    }

    public static class MethodInsnNodeDescMapping extends Mapping<MethodInsnNode, String> {
        public MethodInsnNodeDescMapping(MethodInsnNode node, String arg) { super(node, arg); }
        public void apply() { node.desc = arg; }
    }

    public static class MethodInsnNodeOwnerMapping extends Mapping<MethodInsnNode, String> {
        public MethodInsnNodeOwnerMapping(MethodInsnNode node, String arg) { super(node, arg); }
        public void apply() { node.owner = arg; }
    }

    public static class ClassNameMapping extends Mapping<ClassNode, String> {
        public ClassNameMapping(ClassNode node, String arg) { super(node, arg); }
        public void apply() { node.name = arg; }
    }

    public static class ClassSuperclassMapping extends Mapping<ClassNode, String> {
        public ClassSuperclassMapping(ClassNode node, String arg) { super(node, arg); }
        public void apply() { node.superName = arg; }
    }

    public static class ClassInterfaceMapping extends Mapping<ClassNode, Object[]> {
        public ClassInterfaceMapping(ClassNode node, Object[] arg) { super(node, arg); }
        public void apply() { node.interfaces.set((Integer) arg[0], (String) arg[1]); }
    }

    public static class MethodNameMapping extends Mapping<MethodNode, String> {
        public MethodNameMapping(MethodNode node, String arg) { super(node, arg); }
        public void apply() { node.name = arg; }
    }

    public static class MethodDescMapping extends Mapping<MethodNode, String> {
        public MethodDescMapping(MethodNode node, String arg) { super(node, arg); }
        public void apply() { node.desc = arg; }
    }

    public static class FieldNameMapping extends Mapping<FieldNode, String> {
        public FieldNameMapping(FieldNode node, String arg) { super(node, arg); }
        public void apply() { node.name = arg; }
    }

    public static class FieldDescMapping extends Mapping<FieldNode, String> {
        public FieldDescMapping(FieldNode node, String arg) { super(node, arg); }
        public void apply() { node.desc = arg; }
    }
}
