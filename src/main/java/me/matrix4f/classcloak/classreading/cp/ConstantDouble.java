package me.matrix4f.classcloak.classreading.cp;

public class ConstantDouble extends Constant {

    private double value;

    public ConstantDouble(int high, int low) {
        super(CONSTANT_Double);
        value = Double.longBitsToDouble(((long) high << 32) + low);
    }

    public double getValue() {
        return value;
    }
}
