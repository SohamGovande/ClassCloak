package me.matrix4f.classcloak.classreading.cp;

public class ConstantFloat extends Constant {

    private float value;

    public ConstantFloat(int bits) {
        super(CONSTANT_Float);
        value = Float.intBitsToFloat(bits);
    }

    public float getValue() {
        return value;
    }
}
