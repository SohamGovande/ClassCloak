package me.matrix4f.classcloak.action.name.creation;

import me.matrix4f.classcloak.action.ObfGlobal;
import me.matrix4f.classcloak.action.ObfSettings;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class FieldNameCreator {

    private List<String> dictionary;
    private int index;
    private HashMap<FieldNode, String[]> fieldNameMap = new HashMap<>();
    private ClassNode classNode;

    public static HashMap<ClassNode, FieldNameCreator> classesToCreators = new HashMap<>();

    private FieldNameCreator(ClassNode cn) {
        this.classNode = cn;
        dictionary = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".chars()
                .mapToObj(i -> (char) i)
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    public static FieldNameCreator openClass(ClassNode cn) {
        FieldNameCreator mn = new FieldNameCreator(cn);
        classesToCreators.put(cn, mn);
        return mn;
    }

    public String getName(FieldNode node, boolean silent) {
        String[] name = {dictionary.get(0)};
        int tries = 1;
        String append = "";
        ObfSettings.NameSettings nameSettings = ObfGlobal.nameSettings;
        while (fieldNameMap.entrySet()
                .stream()
                .anyMatch(entry -> {
                    FieldNode fieldNode = entry.getKey();
                    String[] s = entry.getValue();
                    return s[1].equals(name[0]) && (!nameSettings.overloadFields || fieldNode.desc.equals(node.desc));
                })) {

            if (tries >= dictionary.size()) {
                append = Integer.toString((tries / dictionary.size()) - 1); //sub 1 so that 0 is included in the match
            }
            int dictIndex = tries % dictionary.size();
            name[0] = dictionary.get(dictIndex) + append;
            tries++;
        }
        if (!silent)
            fieldNameMap.put(node, new String[]{node.name, name[0]});
        return name[0];
    }

    private String retrieveNextFromDictionary() {
        int repitions = index / dictionary.size();
        String s = dictionary.get(index);
        index++;
        return s + (repitions == 0 ? "" : repitions);
    }

    public HashMap<FieldNode, String[]> getFieldNameMap() {
        return fieldNameMap;
    }
}
