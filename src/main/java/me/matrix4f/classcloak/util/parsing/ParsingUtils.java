package me.matrix4f.classcloak.util.parsing;

import java.util.regex.Pattern;

public class ParsingUtils {

    public static String concat(String separator, Object... args) {
        StringBuilder builder = new StringBuilder();
        for(Object arg : args)
            builder.append(arg).append(separator);
        return builder.length() > 0 ? builder.substring(0,builder.length() - separator.length()) : builder.toString();
    }

    public static boolean isDouble(String str) {
        for(int i = 0; i < str.length(); i++)
            if(!Character.isDigit(str.charAt(i)) && i != '.')
                return false;
        return true;
    }

    public static boolean isInt(String str) {
        for(int i = 0; i < str.length(); i++)
            if(!Character.isDigit(str.charAt(i)))
                return false;
        return true;
    }

    public static boolean conformsToWildcards(String src, String compareTo) {
        if(src.equals(compareTo))
            return true;
        final String sqbPlaceholder = "asdf987as9difzjslkdfj23598jsdkfzkjcgkjwhzelkjfhsdkjhlq2kj3h6lkjahsfkjsdjah235iuy19478zsdufjlk29385q984712987";
        final String sqbClosePlaceholder = "ofusoidzufyikjdfiaj239587uwoi;jasidfj982ua5oijdsf;kJDSFj08oaisdjf;asd98uAKSJD;FLKAJSD;LFKAJS8AUa;skldjf82q35u9a8uisdjf";
        return Pattern.matches(
                src.replace("[",sqbPlaceholder)
                        .replace("]",sqbClosePlaceholder)

                        .replace(".","[.]")
                        .replace("(","[(]")
                        .replace(")","[)]")

                        .replace(sqbPlaceholder,"[[]")
                        .replace(sqbClosePlaceholder,"[\\]]")

                        .replace("?",".")
                        .replace("*",".+"),
                compareTo
        );
    }
}
