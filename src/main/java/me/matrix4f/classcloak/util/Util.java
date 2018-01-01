package me.matrix4f.classcloak.util;

public class Util {

    public static boolean contains(int[] haystack, int needle) {
        for(int i = 0; i < haystack.length; i++)
            if(haystack[i] == needle)
                return true;
        return false;
    }
}
