package me.matrix4f.classcloak.action.name.creation;

import org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ClassNameCreator {

    private List<String> dictionary;
    private int index;
    private HashMap<ClassNode, String[]> classNameMap;
    public static ClassNameCreator instance;

    static {
        instance = create();
    }

    private ClassNameCreator() {
        dictionary = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".chars()
                .mapToObj(i -> (char) i)
                .map(Object::toString)
                .collect(Collectors.toList());
        classNameMap = new HashMap<>();
    }

    public static ClassNameCreator create() {
        return new ClassNameCreator();
    }

    public String getName(ClassNode cn) {
        int repitions = index/dictionary.size();
        String s = dictionary.get(index);
        index++;
        String newName = s + (repitions==0 ?"":repitions);
        if(cn != null) {
            classNameMap.put(cn, new String[]{
                    cn.name,
                    newName
            });
        }
        return newName;
    }

    public HashMap<ClassNode, String[]> getClassNameMap() {
        return classNameMap;
    }
}
