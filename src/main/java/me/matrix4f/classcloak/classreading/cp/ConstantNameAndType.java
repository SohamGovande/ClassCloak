package me.matrix4f.classcloak.classreading.cp;

public class ConstantNameAndType extends Constant {

    private short nameIndex, descIndex;

    public ConstantNameAndType(short nameIndex, short descIndex) {
        super(CONSTANT_NameAndType);
        this.nameIndex = nameIndex;
        this.descIndex = descIndex;
    }

    public short getDescIndex() {
        return descIndex;
    }

    public short getNameIndex() {
        return nameIndex;
    }
}
