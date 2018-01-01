package me.matrix4f.classcloak.util.interpreter;

import org.objectweb.asm.tree.LabelNode;

public class ObjectType {

    private Type type;
    private Object data;

    public ObjectType(Type type, Object data) {
        this.type = type;
        this.data = data;
    }

    public Type getType() {
        return type;
    }

    public LabelNode getReturnAddress() {
        return (LabelNode) data;
    }

    public String getDescriptor() {
        return (String) data;
    }

    public enum Type {
        RETURN_ADDRESS, OBJECT
    }
}
