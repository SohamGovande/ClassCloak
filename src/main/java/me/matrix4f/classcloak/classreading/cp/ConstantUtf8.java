package me.matrix4f.classcloak.classreading.cp;

import java.io.UnsupportedEncodingException;

public class ConstantUtf8 extends Constant {

    private String value;

    public ConstantUtf8(byte[] bytes) {
        super(CONSTANT_Utf8);
        try {
            value = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public String getValue() {
        return value;
    }
}
