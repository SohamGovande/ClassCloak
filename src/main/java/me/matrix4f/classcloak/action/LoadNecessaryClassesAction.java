package me.matrix4f.classcloak.action;

import me.matrix4f.classcloak.Action;
import me.matrix4f.classcloak.JarFileEntry;
import me.matrix4f.classcloak.classreading.CstPoolReader;
import me.matrix4f.classcloak.classreading.cp.*;
import me.matrix4f.classcloak.util.BytecodeUtils;

import java.util.*;
import java.util.stream.Collectors;

//todo add invokedynamic support
public class LoadNecessaryClassesAction extends Action {

    private static List<Class<? extends Constant>> filter = Arrays.asList(
            ConstantClass.class,
            ConstantFieldRef.class,
            ConstantMethodRef.class,
            ConstantUtf8.class,
            ConstantInterfaceMethodRef.class,
            ConstantMethodType.class
    );

    private List<String> extractClassesFrom(CstPoolReader cpr, Constant cst) {
        if(cst instanceof ConstantClass) {
            ConstantClass cc = (ConstantClass) cst;
            String name = ((ConstantUtf8) cpr.at(cc.getNamePointer())).getValue();
            return Collections.singletonList(name);
        }
        if(cst instanceof ConstantFieldRef) {
            ConstantFieldRef cfr = (ConstantFieldRef) cst;
            //get the class' name
            List<String> list = new ArrayList<>(
                    extractClassesFrom(cpr, cpr.at(cfr.getClassIndex()))
            );
            String desc = ((ConstantUtf8) cpr.at(((ConstantNameAndType) cpr.at(cfr.getNatIndex())).getDescIndex())).getValue();
            String internalName = BytecodeUtils.getInternalNameOfDescriptor(desc);
            list.add(internalName);
            return list;
        }
        if(cst instanceof ConstantMethodRef || cst instanceof ConstantInterfaceMethodRef) {
            ConstantRef cmr = (ConstantRef) cst;
            //get the class' name

            List<String> list = new ArrayList<>();
            List<String> add = extractClassesFrom(cpr, cpr.at(cmr.getClassIndex()));
            if(add != null)
                list.addAll(add);
            String desc = ((ConstantUtf8) cpr.at(((ConstantNameAndType) cpr.at(cmr.getNatIndex())).getDescIndex())).getValue();
            list.addAll(BytecodeUtils.getInternalNamesUsedByMethodSignature(desc));
            return list;
        }
        if(cst instanceof ConstantMethodType) {
            ConstantMethodType cmt = (ConstantMethodType) cst;
            String desc = ((ConstantUtf8) cpr.at(((ConstantNameAndType) cpr.at(cmt.getDescIndex())).getDescIndex())).getValue();
            return new ArrayList<>(BytecodeUtils.getInternalNamesUsedByMethodSignature(desc));
        }
        return null;
    }

    private Set<String> getUsedClasses() {
        Set<String> classesUsed = new HashSet<>();

        int i = 0;
        for(JarFileEntry jfe : ObfGlobal.inputJarEntries) {
            if(jfe.isTargettingClass()) {
                CstPoolReader cpr = jfe.getCstReader();
                cpr.read();
                cpr.stream()
                        .filter(cst -> filter.contains(cst.getClass()))
                        .map(cst -> extractClassesFrom(cpr, cst))
                        .filter(Objects::nonNull)
                        .forEach(classesUsed::addAll);
                i++;
            }
        }
        return classesUsed;
    }

    @Override
    public void execute() {
        Set<String> classesUsed = getUsedClasses();

        //reduce the set to only contain external classes used
        ObfGlobal.sourceClasses.stream()
                .map(cn -> cn.name)
                .forEach(classesUsed::remove);

        List<String> jarEntryNames = ObfGlobal.classpathJarEntries
                .stream()
                .map(JarFileEntry::getEntryName)
                .map(str -> str.substring(0,str.length() - ".class".length()))
                .collect(Collectors.toList());
        for (String className : classesUsed) {
            int index;
            if ((index = jarEntryNames.indexOf(className)) != -1) {
                ObfGlobal.loadClassFromCP(ObfGlobal.classpathJarEntries.get(index));
            } else {
//                LOGGER.fatal("No class found by name " + className + ".");
            }
        }

    }
}
