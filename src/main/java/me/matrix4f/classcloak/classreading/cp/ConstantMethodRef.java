package me.matrix4f.classcloak.classreading.cp;

public class ConstantMethodRef extends ConstantRef {
    public ConstantMethodRef(short classIndex, short natIndex) {
        super(CONSTANT_Methodref, classIndex, natIndex);
    }
}
