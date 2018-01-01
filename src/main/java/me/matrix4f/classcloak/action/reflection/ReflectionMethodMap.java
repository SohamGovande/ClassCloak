package me.matrix4f.classcloak.action.reflection;

import java.util.HashMap;
import java.util.Map;

public class ReflectionMethodMap {

    public static final String
            CLASS_GETMETHOD = "class_getMethod",
            CLASS_GETDECLAREDMETHOD = "class_getDeclaredMethod",
            CLASS_GETFIELD = "class_getField",
            CLASS_GETDECLAREDFIELD = "class_getDeclaredField",
            CLASS_FORNAME = "class_forName";

    private Map<String, Boolean> map;

    public ReflectionMethodMap() {
        map = new HashMap<>();
    }

    public Boolean get(String key) {
        return map.get(key);
    }

    public Boolean put(String key, Boolean value) {
        return map.put(key, value);
    }

    @Override
    public String toString() {
        return "ReflectionMethodMap{" +
                "map=" + map +
                '}';
    }
}
