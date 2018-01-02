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

    public boolean isC1() {
        return !isC2();
    }

    public boolean isC2() {
        String descNoArray = getDescriptor().replace("[","");
        return descNoArray.equals("D") || descNoArray.equals("J");
    }

    public C getCategory() {
        return isC2() ? C.TWO : C.ONE;
    }

    public ObjectType clone() {
        return new ObjectType(type, data);
    }

    public enum Type {
        RETURN_ADDRESS, OBJECT
    }

    public enum C {
        ONE, TWO
    }
}
