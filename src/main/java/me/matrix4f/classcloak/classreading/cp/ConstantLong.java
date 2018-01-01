package me.matrix4f.classcloak.classreading.cp;

public class ConstantLong extends Constant {

    private long value;

    public ConstantLong(int high, int low) {
        super(CONSTANT_Long);
        value = ((long) high << 32) + low;
    }

    public long getValue() {
        return value;
    }
}
