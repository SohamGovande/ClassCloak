package me.matrix4f.classcloak.classreading.cp;

public class ConstantModule extends Constant {

    private short nameIndex;

    public ConstantModule(short nameIndex) {
        super(CONSTANT_Module);
        this.nameIndex = nameIndex;
    }

    public short getNameIndex() {
        return nameIndex;
    }
}
