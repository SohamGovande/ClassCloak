package me.matrix4f.classcloak.action.reflection;

import java.util.HashMap;
import java.util.Map;

public class ReflectionMethodMap {

    public static final String
            CLASS_GETMETHOD = "class_getMethod",
            CLASS_GETDECLAREDMETHOD = "class_getDeclaredMethod",
            CLASS_GETFIELD = "class_getField",
            CLASS_GETDECLAREDFIELD = "class_getDeclaredField",
            CLASS_FORNAME = "class_forName",
            FIELD_GETNAME = "field_getName",
            CLASS_GETNAME = "class_getName",
            METHOD_GETNAME = "method_getName";

    private Map<String, Boolean> map;

    public ReflectionMethodMap() {
        map = new HashMap<>();
    }

    public boolean get(String key) {
        return map.getOrDefault(key, false);
    }

    public void put(String key, Boolean value) {
        map.put(key, value);
    }

    @Override
    public String toString() {
        return "ReflectionMethodMap{" +
                "map=" + map +
                '}';
    }
}
