package me.matrix4f.classcloak.action;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import me.matrix4f.classcloak.Action;

import java.util.ArrayList;
import java.util.List;

import static me.matrix4f.classcloak.Globals.LOGGER;
import static me.matrix4f.classcloak.action.ObfGlobal.*;
import static me.matrix4f.classcloak.util.BytecodeUtils.convertDescriptorToJavaName;
import static me.matrix4f.classcloak.util.BytecodeUtils.convertMethodDescriptorToJava;
import static me.matrix4f.classcloak.util.BytecodeUtils.getJavaName;

public class ClassPathVerifierAction extends Action {

    private void verify() throws CE {
        checkForDuplicateClasses();
        for(ClassNode cn : sourceClasses) {
            checkClassMethodDeclarations(cn);
            checkClassFields(cn);
            checkClassInfo(cn);
        }
    }

    private void checkClassMethodDeclarations(ClassNode cn) throws CE {
        for(MethodNode mn : cn.methods) {
            for(Type type : Type.getArgumentTypes(mn.desc)) {
                if(!classPathContains(type.getDescriptor()))
                    throw new CE(cn.name + "." + mn.name + convertMethodDescriptorToJava(mn.desc) + "'s parameter of " + convertDescriptorToJavaName(type.getDescriptor()) + " wasn't found in the classpath.");
            }
            Type type = Type.getReturnType(mn.desc);
            if(!classPathContains(type.getDescriptor()))
                throw new CE(cn.name + "." + mn.name + convertMethodDescriptorToJava(mn.desc) + "'s return type, " + convertDescriptorToJavaName(type.getDescriptor()) + ", wasn't found in the classpath.");
        }
    }

    private void checkClassFields(ClassNode cn) throws CE {
        for(FieldNode fn : cn.fields) {
            if(!classPathContains(fn.desc))
                throw new CE(getJavaName(cn) + "'s field, " + fn.name + " (" + convertDescriptorToJavaName(fn.desc) + ")'s type wasn't found in the classpath.");
        }
    }

    private void checkForDuplicateClasses() throws CE {
        List<ClassNode> nodes = ObfGlobal.allClasses();

        List<String> processed = new ArrayList<>();
        for(ClassNode cn : nodes) {
            boolean contains = false;
            for(String s : processed) { //check for duplicates
                if(s.equals(cn.name)) {
                    contains = true;
                    break;
                }
            }
            if(contains)
                throw new CE("Duplicate class " + getJavaName(cn) + " found.");
            else
                processed.add(cn.name);
        }
    }

    private void checkClassInfo(ClassNode cn) throws CE{
        if(!classPathContains(cn.superName))
            throw new CE(getJavaName(cn) + "'s superclass name, " + cn.superName.replace('/','.') + ", wasn't found in the classpath");
        for(String inter : cn.interfaces)
            if(!classPathContains(inter))
                throw new CE(getJavaName(cn) + "'s interface name, " + inter.replace('/','.') + ", wasn't found in the classpath");
    }

    private boolean classPathContains(String s) {
        s = s.replace("[","");
        if(s.startsWith("L") && s.endsWith(";"))
            return classPathContains(s.substring(1,s.length()-1));

        if(s.length() == 1) //primitive type
            return true;
        String finalS = s;
        return (classpath.stream().anyMatch(node -> node.name.equals(finalS))) ||
                (sourceClasses.stream().anyMatch(node -> node.name.equals(finalS)));
    }

    @Override
    public void execute() {
        LOGGER.info("Verifying all classes and cross-checking with classpath...");
        try {
            verify();
        } catch (CE e) {
            LOGGER.fatal(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.fatal(e.getMessage());
        }
    }

    private static class CE extends Exception{ //ClassPathException

        public CE(String message) {
            super(message);
        }
    }
}
