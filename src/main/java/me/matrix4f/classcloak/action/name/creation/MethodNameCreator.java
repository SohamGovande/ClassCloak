package me.matrix4f.classcloak.action.name.creation;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MethodNameCreator {

    private List<String> dictionary;
    private int index;
    private HashMap<MethodNode, String[]> methodNameMap = new HashMap<>();
    private ClassNode classNode;

    public static HashMap<ClassNode, MethodNameCreator> classesToCreators = new HashMap<>();

    private MethodNameCreator(ClassNode cn) {
        this.classNode = cn;
        dictionary = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".chars()
                .mapToObj(i -> (char) i)
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    public static MethodNameCreator openClass(ClassNode cn) {
        MethodNameCreator mn = new MethodNameCreator(cn);
        classesToCreators.put(cn, mn);
        return mn;
    }

    public String getName(MethodNode node, boolean silent) {
        String[] name = {dictionary.get(0)};
        int tries = 1;
        String append = "";
        while(methodNameMap.values()
                .stream()
                .anyMatch(s -> s[1].equals(name[0])))
        {
            if(tries >= dictionary.size()) {
                append = Integer.toString((tries / dictionary.size()) - 1); //sub 1 so that 0 is included in the match
            }
            int dictIndex = tries % dictionary.size();
            name[0] = dictionary.get(dictIndex) + append;
            tries++;
        }
        if(!silent)
            methodNameMap.put(node, new String[] { node.name, name[0] });
        return name[0];
    }

    private String retrieveNextFromDictionary() {
        int repitions = index/dictionary.size();
        String s = dictionary.get(index);
        index++;
        return s + (repitions==0 ?"":repitions);
    }

    public HashMap<MethodNode, String[]> getMethodNameMap() {
        return methodNameMap;
    }
}
