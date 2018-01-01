package me.matrix4f.classcloak.util.interpreter;

import org.objectweb.asm.tree.AbstractInsnNode;

public class StackAction {

    private Type stackType;
    private ObjectType objectType;
    private AbstractInsnNode source;

    public StackAction(AbstractInsnNode source, Type stackType, ObjectType objectType) {
        this.source = source;
        this.stackType = stackType;
        this.objectType = objectType;
    }

    public AbstractInsnNode getSource() {
        return source;
    }

    public Type getStackType() {
        return stackType;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public enum Type {
        PUSH, POP
    }
}
