package me.matrix4f.classcloak.util;

import java.text.DecimalFormat;

public class TimeUtils {

    private static long startTime;

    public static void start() {
        startTime = System.currentTimeMillis();
    }

    public static double secondsPassed() {
        return msPassed() / 1000.0;
    }

    public static String secondsPassedAsString() {
        return new DecimalFormat("#.#").format(secondsPassed());
    }

    public static long msPassed() {
        long ms = System.currentTimeMillis() - startTime;
        startTime = 0;
        return ms;
    }
}
