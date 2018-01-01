package me.matrix4f.classcloak.classreading.cp;

public abstract class ConstantRef extends Constant {

    private short classIndex, natIndex;

    public ConstantRef(byte type, short classIndex, short natIndex) {
        super(type);
        this.classIndex = classIndex;
        this.natIndex = natIndex;
    }

    public short getClassIndex() {
        return classIndex;
    }

    public short getNatIndex() {
        return natIndex;
    }
}
