package me.matrix4f.classcloak.classreading.cp;

public class ConstantFieldRef extends ConstantRef {
    public ConstantFieldRef(short classIndex, short natIndex) {
        super(CONSTANT_Fieldref, classIndex, natIndex);
    }
}
