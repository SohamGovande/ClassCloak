package me.matrix4f.logging;

import java.util.GregorianCalendar;

import static java.util.Calendar.*;

public class Logger {

    private String name;

    private Logger() {}

    public static Logger create(String name) {
        Logger l = new Logger();
        l.name = name;
        return l;
    }

    private String getPrefix() {
        GregorianCalendar c = new GregorianCalendar();
        String sec = Integer.toString(c.get(SECOND));
        while(sec.length() < 2)
            sec = "0" + sec;

        String hor = Integer.toString(c.get(HOUR_OF_DAY));
        while(hor.length() < 2)
            hor = "0" + hor;

        String min = Integer.toString(c.get(MINUTE));
        while(min.length() < 2)
            min = "0" + min;

        return String.format("[%d/%d/%d] [%s:%s:%s] [%s] ", c.get(MONTH)+1, c.get(DAY_OF_MONTH), c.get(YEAR), hor, min, sec, name);
    }

    private void log(String channel, String message) {
        String s = getPrefix() + "[" + channel.toUpperCase() + "]: " + message;
        System.out.println(s);
    }

    public void info(Object message) {
        log("info", message.toString());
    }

    public void warn(Object message) {
        log("warning", message.toString());
    }

    public void fatal(Object message) {
        log("fatal_error", message.toString());
        System.exit(-1);
    }

    public void error(Object message) {
        log("error", message.toString());
    }
}
