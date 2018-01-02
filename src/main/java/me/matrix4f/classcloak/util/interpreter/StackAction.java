package me.matrix4f.classcloak.util.interpreter;

import org.objectweb.asm.tree.AbstractInsnNode;

public class StackAction {

    private Type operationType;
    private ObjectType objectType;
    private AbstractInsnNode source;

    public StackAction(AbstractInsnNode source, Type operationType, ObjectType objectType) {
        this.source = source;
        this.operationType = operationType;
        this.objectType = objectType;
    }

    public AbstractInsnNode getSource() {
        return source;
    }

    public Type getOperationType() {
        return operationType;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public enum Type {
        PUSH, POP
    }
}
