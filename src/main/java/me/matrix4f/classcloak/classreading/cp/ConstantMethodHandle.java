package me.matrix4f.classcloak.classreading.cp;

public class ConstantMethodHandle extends Constant {

    private byte refKind;
    private short refIndex;

    public ConstantMethodHandle(byte refKind, short refIndex) {
        super(CONSTANT_MethodHandle);
        this.refKind = refKind;
        this.refIndex = refIndex;
    }
}
