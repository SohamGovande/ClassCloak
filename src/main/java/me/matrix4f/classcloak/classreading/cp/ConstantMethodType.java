package me.matrix4f.classcloak.classreading.cp;

public class ConstantMethodType extends Constant {

    private short descIndex;

    public ConstantMethodType(short descIndex) {
        super(CONSTANT_MethodType);
        this.descIndex = descIndex;
    }

    public short getDescIndex() {
        return descIndex;
    }
}
