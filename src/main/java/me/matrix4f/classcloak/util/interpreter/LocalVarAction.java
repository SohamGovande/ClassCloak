package me.matrix4f.classcloak.util.interpreter;

import org.objectweb.asm.tree.AbstractInsnNode;

public class LocalVarAction {

    private Type actionType;
    private int index;
    private ObjectType objectType;
    private AbstractInsnNode source;

    public LocalVarAction(AbstractInsnNode source, Type actionType, int index, ObjectType objectType) {
        this.source = source;
        this.actionType = actionType;
        this.index = index;
        this.objectType = objectType;
    }

    public AbstractInsnNode getSource() {
        return source;
    }

    public Type getActionType() {
        return actionType;
    }

    public int getIndex() {
        return index;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public enum Type {
        CHANGE_TYPE, CREATE
    }
}
