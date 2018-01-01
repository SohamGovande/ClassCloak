package me.matrix4f.classcloak.target;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class ModifierMap {

    private static final HashMap<String, Integer> map = new HashMap<>();

    static {
        map.put("public",1);
        map.put("private",2);
        map.put("protected",4);
        map.put("static",8);
        map.put("final",16);
        map.put("super",32);
        map.put("synchronized",32);
        map.put("volatile",64);
//        map.put("bridge",64);
//        map.put("varargs",128);
        map.put("transient",128);
        map.put("native",256);
        map.put("interface",512);
        map.put("abstract",1024);
        map.put("strictfp",2048);
        map.put("synthetic",4096);
        map.put("annotation",8192);
        map.put("enum",16384);
//        map.put("mandated",32768);
        map.put("deprecated",131072);
    }

    public static String getName(int flag) {
        return map.entrySet()
                .stream()
                .filter(entry -> entry.getValue() == flag)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public static int getFlag(String name) {
        return map.entrySet()
                .stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(-1);
    }
}
