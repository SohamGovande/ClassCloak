package me.matrix4f.classcloak.target;

import org.objectweb.asm.tree.ClassNode;
import me.matrix4f.classcloak.JarFileEntry;
import me.matrix4f.classcloak.action.ObfGlobal;
import me.matrix4f.classcloak.util.parsing.ParsingUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static me.matrix4f.classcloak.Globals.LOGGER;

public class ClassHierarchy {

    public static ClassNode findOrLoad(List<ClassNode> search, String name) {
        Optional<ClassNode> optional = search.stream()
                .filter(cn -> cn.name.equals(name))
                .findFirst();
        if(optional.isPresent())
            return optional.get();

        List<String> jarEntryNames = ObfGlobal.classpathJarEntries
                .stream()
                .map(JarFileEntry::getEntryName)
                .map(str -> str.substring(0,".class".length()))
                .collect(Collectors.toList());
        int index;
        if ((index = jarEntryNames.indexOf(name)) != -1) {
            return ObfGlobal.loadClassFromCP(ObfGlobal.classpathJarEntries.get(index));
        } else {
            LOGGER.fatal("No class found by name " + name + ".");
        }
        //TODO good error messages
        return null;
    }

    public static boolean doesClassExtend(List<ClassNode> all, String expectedSuperclass, ClassNode clazz) {
        if(expectedSuperclass.equals("java/lang/Object"))
            return true;

        do {
            if(ParsingUtils.conformsToWildcards(expectedSuperclass, clazz.name))
                return true;
            clazz = findOrLoad(all, clazz.superName);
        } while(!clazz.name.equals("java/lang/Object"));

        return false;
    }
}