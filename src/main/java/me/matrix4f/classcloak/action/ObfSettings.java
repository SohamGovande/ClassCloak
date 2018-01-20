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

    public static class NameSettings extends ObfSettings {
        public static final int NONE = 0, SIMPLE = 1, ADVANCED = 2;
        public static final String[] METHOD_OVERLOADING = {"none","simple","advanced"};
        public boolean overloadFields = false;
        public int overloadMethods = SIMPLE;
    }

    public static class DebugSettings {
        public static final int DESTROYVARS = 0, CLEARVARS = 1, KEEPVARS = 2;
        public static final String[] LOCALVARS = {"destroy","makeEmpty","keep"};
        public int localVarAction = -1;

        public static final int KEEPLINES = 0, DELETELINES = 1, ZEROIFYLINES = 2, SCRAMBLELINES = 3;
        public static final String[] LINENUMBERS = {"keep","delete","zeroify","scramble"};
        public int lineNumberAction = -1;
        public String lineNumberPwd = "";

        public static final int KEEPSOURCE = 0, DELETESOURCE = 1;
        public static final String[] SOURCE = {"keep","delete"};
        public int sourceAction = -1;
    }
}
