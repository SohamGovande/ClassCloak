package me.matrix4f.classcloak.classreading.cp;


import me.matrix4f.classcloak.classreading.Constants;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Constant implements Constants {

    private byte type;

    public Constant(byte type) {
        this.type = type;
    }

    public byte getType() {
        return type;
    }

    @Override
    public String toString() {
        final String[] s = {getClass().getSimpleName().substring("Constant".length())+" "};
        List<Field> fields = Arrays.stream(getClass().getDeclaredFields()).collect(Collectors.toList());
        if(getClass().getSuperclass() != Constant.class)
            fields.addAll(Arrays.stream(getClass().getSuperclass().getDeclaredFields()).collect(Collectors.toList()));

        fields.forEach(field -> {
            try {
                field.setAccessible(true);
                s[0] += field.getName() + ":" + field.get(Constant.this)+" ";
            } catch (Exception e) {}
        });
        return s[0];
    }
}
