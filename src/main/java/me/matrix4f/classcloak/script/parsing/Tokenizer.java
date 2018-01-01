package me.matrix4f.classcloak.script.parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Character.isWhitespace;

public class Tokenizer {

    public static List<String> getRawTokens(String full) {
        full = full.trim();
        while(full.contains("  "))
            full = full.replace("  ", " ");

        StringBuilder tokenBuffer = new StringBuilder();
        List<String> tokens = new ArrayList<>();
        int[] splits = {
                ':',
                '='
        };
        boolean singleQuotes = false, doubleQuotes = false;
        List<Character> splitCharList = IntStream.of(splits)
                .boxed()
                .map(integer -> (char) (int) integer)
                .collect(Collectors.toList());

        for(int i = 0; i < full.length(); i++) {
            char c = full.charAt(i);
            if(c == '\'')
                singleQuotes = !singleQuotes;
            if(c == '\"')
                doubleQuotes = !doubleQuotes;

            if((!isWhitespace(c) && !splitCharList.contains(c)) || singleQuotes || doubleQuotes) {
                tokenBuffer.append(c);
            } else {
                if(tokenBuffer.length() > 0)
                    tokens.add(tokenBuffer.toString());
                tokenBuffer = new StringBuilder();
            }
        }
        if(tokenBuffer.length() > 0)
            tokens.add(tokenBuffer.toString());
        return tokens;
    }
}
