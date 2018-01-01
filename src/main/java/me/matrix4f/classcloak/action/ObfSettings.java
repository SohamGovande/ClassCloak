package me.matrix4f.classcloak.action;

import com.sun.deploy.util.StringUtils;
import me.matrix4f.classcloak.action.reflection.ReflectionEntry;
import me.matrix4f.classcloak.script.command.api.Command;
import me.matrix4f.classcloak.script.command.commands.CommandObfuscate;
import me.matrix4f.classcloak.script.parsing.CommandException;
import me.matrix4f.classcloak.target.NodeTarget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ObfSettings {

    public List<NodeTarget> exclusions = new ArrayList<>();

    public boolean shouldExclude(Object... data) {
        return exclusions.stream().anyMatch(exclusion -> exclusion.doesExcludeNode(data));
    }

    public static class StringObfSettings extends ObfSettings {
        public static final int INT_ARRAYS = 0, FAST_STRINGS = 1;
        public static final String[] METHOD_LIST = {"int_arrays","fast_strings"};

        public int obfMethod = 0;
    }

    public static class LineObfSettings extends ObfSettings {
        public static final int DELETE = 0, SCRAMBLE = 1, RANDOM = 2, SINGLE = 3;
        public static final String[] METHOD_LIST = {"delete","scramble","random","single"};

        public int obfMethod = 0;
    }

    public static class ReflectionHandlingSettings {
        public List<NodeTarget> inclusions = new ArrayList<>();
//        public String hashFunction = "SHA-256";
        public List<ReflectionEntry> entries = new ArrayList<>();

        public boolean shouldInclude(Object... data) {
            return inclusions.stream()
                    .filter(exclusion -> exclusion.doesExcludeNode(data))
                    .count() > 0;
        }
    }
}
