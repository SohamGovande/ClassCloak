package me.matrix4f.classcloak.action;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import me.matrix4f.classcloak.JarFileEntry;
import me.matrix4f.classcloak.util.NodeNameRemapper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;

public class ObfGlobal {

    public static final ObfSettings.NameSettings nameSettings = new ObfSettings.NameSettings();
    public static final ObfSettings.StringObfSettings stringSettings = new ObfSettings.StringObfSettings();
    public static final ObfSettings.ReflectionHandlingSettings reflectionSettings = new ObfSettings.ReflectionHandlingSettings();
    public static final ObfSettings.DebugSettings debugSettings = new ObfSettings.DebugSettings();

    public static final NodeNameRemapper remapper = new NodeNameRemapper();

    public static File inputFile = null, outputFile = null;
    public static Manifest manifest = null;

    public static List<ClassNode> sourceClasses = null, previousClasses = null;
    public static List<JarFileEntry> inputJarEntries = null;

    public static List<ClassNode> classpath = new ArrayList<>();
    public static List<JarFileEntry> classpathJarEntries = new ArrayList<>();

    public static File mappingsSaveFile = null;

    public static List<ClassNode> allClasses() {
        ArrayList<ClassNode> nodes = new ArrayList<>(sourceClasses);
        nodes.addAll(classpath);
        return nodes;
    }

    public static ClassNode loadClassFromCP(JarFileEntry clazz) {
        ClassReader cr = new ClassReader(clazz.getOriginalData());
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        classpath.add(cn);
        return cn;
    }
}
