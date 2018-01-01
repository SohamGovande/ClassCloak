package me.matrix4f.classcloak.action.opaquepredicates;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LabelNode;
import me.matrix4f.classcloak.action.ObfGlobal;
import me.matrix4f.classcloak.action.name.namecreation.ClassNameCreator;
import me.matrix4f.classcloak.util.MethodBuilder;

import static org.objectweb.asm.Opcodes.*;

public class NodeOpaquePredClassBuilder {

    public static String
            className,
            fieldChildName,
            fieldParentName,
            desc,
            addName,
            addDesc,
            childAddName,
            childAddDesc,
            childChildAddName,
            childChildAddDesc;

    private static boolean generated = false;

    private static ClassNode gen() {
        className = ClassNameCreator.instance.getName(null);
        fieldChildName = "a";
        fieldParentName = "b";
        desc = "L" + className + ";";

        addName = "a";
        childAddName = "b";
        childChildAddName = "c";
        addDesc = childAddDesc = childChildAddDesc = "()" + desc;

        ClassWriter cw = new ClassWriter(0);
        cw.visit(52, ACC_PUBLIC+ACC_SUPER, className, null, "java/lang/Object", null);

        cw.visitField(ACC_PUBLIC, fieldChildName, desc, null, null).visitEnd();
        cw.visitField(ACC_PUBLIC, fieldParentName, desc, null, null).visitEnd();

        MethodBuilder.newEmptyConstructorExtendingObject(className, cw);

        LabelNode lbl = new LabelNode();

        //add()V method
        MethodBuilder.newBuilder()
                .label(0)
                .aload(0)
                .getfield(className, fieldChildName, desc)
                .ifnonnnull(lbl)

                .aload(0)
                .new_(className)
                .dup()
                .invokespecial(className)
                .putfield(className, fieldChildName, desc)

                .aload(0)
                .getfield(className, fieldChildName, desc)
                .aload(0)
                .putfield(className, fieldParentName, desc)

                .label(lbl)

                .aload(0)
                .getfield(className, fieldChildName, desc)
                .label(2)
                .areturn()

                .localVar("",desc,null,0,2,0)

                .writeMethod(cw, ACC_PUBLIC, addName, addDesc, null, null);

        lbl = new LabelNode();

        MethodBuilder.newBuilder()
                .label(0)

                .aload(0)
                .getfield(className, fieldChildName, desc)
                .ifnonnnull(lbl)

                .aload(0)
                .new_(className)
                .dup()
                .invokespecial(className)
                .putfield(className, fieldChildName, desc)

                .aload(0)
                .getfield(className, fieldChildName, desc)
                .aload(0)
                .putfield(className, fieldParentName, desc)

                .label(lbl)

                .aload(0)
                .getfield(className, fieldChildName, desc)
                .invokevirtual(className, addName, addDesc)
                .areturn()

                .label(2)

                .localVar("",desc,null,0,2,0)
                .writeMethod(cw, ACC_PUBLIC, childAddName, childAddDesc, null, null);

        lbl = new LabelNode();

        MethodBuilder.newBuilder()
                .label(0)

                .aload(0)
                .getfield(className, fieldChildName, desc)
                .ifnonnnull(lbl)

                .aload(0)
                .new_(className)
                .dup()
                .invokespecial(className)
                .putfield(className, fieldChildName, desc)

                .aload(0)
                .getfield(className, fieldChildName, desc)
                .aload(0)
                .putfield(className, fieldParentName, desc)

                .label(lbl)

                .aload(0)
                .getfield(className, fieldChildName, desc)
                .invokevirtual(className, childAddName, childAddDesc)
                .areturn()

                .label(2)

                .localVar("",desc,null,0,2,0)
                .writeMethod(cw, ACC_PUBLIC, childChildAddName, childChildAddDesc, null, null);


        ClassReader reader = new ClassReader(cw.toByteArray());
        ClassNode node = new ClassNode();
        reader.accept(node, 0);
        generated = true;
        return node;
    }

    public static void generateIfNeeded() {
        if(!generated)
            ObfGlobal.classes.add(gen());
    }
}
