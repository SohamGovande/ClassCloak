package me.matrix4f.classcloak.classreading.cp;

public class ConstantPackage extends Constant {

    private short nameIndex;

    public ConstantPackage(short nameIndex) {
        super(CONSTANT_Package);
        this.nameIndex = nameIndex;
    }

    public short getNameIndex() {
        return nameIndex;
    }
}
