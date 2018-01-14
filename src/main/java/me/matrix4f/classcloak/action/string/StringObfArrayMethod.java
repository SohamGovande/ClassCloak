package me.matrix4f.classcloak.action.string;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import me.matrix4f.classcloak.action.name.creation.ClassNameCreator;

import java.util.Random;

import static org.objectweb.asm.Opcodes.*;

public class StringObfArrayMethod extends StringObfMethod {

    private int randomNegPosInt(Random random, int bound) {
        int rand = random.nextInt(bound*2+1)-bound;
        while(rand == 0)
            rand = random.nextInt(bound*2+1)-bound;
        return rand;
    }

    public int[] obfuscate(String string, long hash) {
        Random random = new Random(hash);

        char[] chars = string.toCharArray();

        int[] array = new int[chars.length];
        final int v1 = randomNegPosInt(random, 32);
        final int v2 = randomNegPosInt(random, 1024);
        final int v3 = randomNegPosInt(random, Math.abs(v1*v2));
        final int v4 = randomNegPosInt(random, 65535);
        final int v5 = randomNegPosInt(random, 5);
        final int v6 = randomNegPosInt(random, 32768);

        for(int i = 0; i < chars.length; i++) {
            array[i] = (((int)chars[i]*v1*v2-v3+v4)*v5+v6);
        }
        return array;
    }

    public String deobfuscate(int[] data, long hash) {
        Random random = new Random(hash);
        char[] chars = new char[data.length];

        final int v1 = randomNegPosInt(random, 32);
        final int v2 = randomNegPosInt(random, 1024);
        final int v3 = randomNegPosInt(random, Math.abs(v1*v2));
        final int v4 = randomNegPosInt(random, 65535);
        final int v5 = randomNegPosInt(random, 5);
        final int v6 = randomNegPosInt(random, 32768);

        for(int i = 0; i < data.length; i++) {
            chars[i] = (char) ((((data[i]-v6)/v5-v4+v3)/v2)/v1);
        }

        return new String(chars);
    }

    public StringObfInfo generateInfo() {
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        String className = ClassNameCreator.instance.getName(null);
        cw.visit(52, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", null);

        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(15, l0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLocalVariable("this", "L" + className + ";", null, l0, l1, 0);
            mv.visitMaxs(1,1);
            mv.visitEnd();
        }
        String randomNegPosIntName = "a";
        String randomNegPosIntDesc = "(Ljava/util/Random;I)I";
        {
            mv = cw.visitMethod(ACC_PRIVATE + ACC_STATIC, randomNegPosIntName, randomNegPosIntDesc, null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(18, l0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitInsn(ICONST_2);
            mv.visitInsn(IMUL);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IADD);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Random", "nextInt", "(I)I", false);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitInsn(ISUB);
            mv.visitVarInsn(ISTORE, 2);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLineNumber(19, l1);
            mv.visitFrame(F_APPEND, 1, new Object[]{INTEGER}, 0, null);
            mv.visitVarInsn(ILOAD, 2);
            Label l2 = new Label();
            mv.visitJumpInsn(IFNE, l2);
            Label l3 = new Label();
            mv.visitLabel(l3);
            mv.visitLineNumber(20, l3);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitInsn(ICONST_2);
            mv.visitInsn(IMUL);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IADD);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Random", "nextInt", "(I)I", false);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitInsn(ISUB);
            mv.visitVarInsn(ISTORE, 2);
            mv.visitJumpInsn(GOTO, l1);
            mv.visitLabel(l2);
            mv.visitLineNumber(21, l2);
            mv.visitFrame(F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitInsn(IRETURN);
            Label l4 = new Label();
            mv.visitLabel(l4);
            mv.visitLocalVariable("", "Ljava/util/Random;", null, l0, l4, 0);
            mv.visitLocalVariable("", "I", null, l0, l4, 1);
            mv.visitLocalVariable("", "I", null, l1, l4, 2);
            mv.visitMaxs(3, 3);
            mv.visitEnd();
        }
        String deobfuscateMethodName = "b";
        String deobfuscateMethodDesc = "([IJ)Ljava/lang/String;";
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, deobfuscateMethodName, deobfuscateMethodDesc, null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(44, l0);
            mv.visitTypeInsn(NEW, "java/util/Random");
            mv.visitInsn(DUP);
            mv.visitVarInsn(LLOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/Random", "<init>", "(J)V", false);
            mv.visitVarInsn(ASTORE, 3);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLineNumber(45, l1);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ARRAYLENGTH);
            mv.visitIntInsn(NEWARRAY, T_CHAR);
            mv.visitVarInsn(ASTORE, 4);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitLineNumber(47, l2);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitIntInsn(BIPUSH, 32);
            mv.visitMethodInsn(INVOKESTATIC, className, randomNegPosIntName, randomNegPosIntDesc, false);
            mv.visitVarInsn(ISTORE, 5);
            Label l3 = new Label();
            mv.visitLabel(l3);
            mv.visitLineNumber(48, l3);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitIntInsn(SIPUSH, 1024);
            mv.visitMethodInsn(INVOKESTATIC, className, randomNegPosIntName, randomNegPosIntDesc, false);
            mv.visitVarInsn(ISTORE, 6);
            Label l4 = new Label();
            mv.visitLabel(l4);
            mv.visitLineNumber(49, l4);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitVarInsn(ILOAD, 5);
            mv.visitVarInsn(ILOAD, 6);
            mv.visitInsn(IMUL);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "abs", "(I)I", false);
            mv.visitMethodInsn(INVOKESTATIC, className, randomNegPosIntName, randomNegPosIntDesc, false);
            mv.visitVarInsn(ISTORE, 7);
            Label l5 = new Label();
            mv.visitLabel(l5);
            mv.visitLineNumber(50, l5);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitLdcInsn(new Integer(65535));
            mv.visitMethodInsn(INVOKESTATIC, className, randomNegPosIntName, randomNegPosIntDesc, false);
            mv.visitVarInsn(ISTORE, 8);
            Label l6 = new Label();
            mv.visitLabel(l6);
            mv.visitLineNumber(51, l6);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitInsn(ICONST_5);
            mv.visitMethodInsn(INVOKESTATIC, className, randomNegPosIntName, randomNegPosIntDesc, false);
            mv.visitVarInsn(ISTORE, 9);
            Label l7 = new Label();
            mv.visitLabel(l7);
            mv.visitLineNumber(52, l7);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitLdcInsn(new Integer(32768));
            mv.visitMethodInsn(INVOKESTATIC, className, randomNegPosIntName, randomNegPosIntDesc, false);
            mv.visitVarInsn(ISTORE, 10);
            Label l8 = new Label();
            mv.visitLabel(l8);
            mv.visitLineNumber(54, l8);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, 11);
            Label l9 = new Label();
            mv.visitLabel(l9);
            mv.visitFrame(F_FULL, 11, new Object[]{"[I", LONG, "java/util/Random", "[C", INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER}, 0, new Object[]{});
            mv.visitVarInsn(ILOAD, 11);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ARRAYLENGTH);
            Label l10 = new Label();
            mv.visitJumpInsn(IF_ICMPGE, l10);
            Label l11 = new Label();
            mv.visitLabel(l11);
            mv.visitLineNumber(55, l11);
            mv.visitVarInsn(ALOAD, 4);
            mv.visitVarInsn(ILOAD, 11);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 11);
            mv.visitInsn(IALOAD);
            mv.visitVarInsn(ILOAD, 10);
            mv.visitInsn(ISUB);
            mv.visitVarInsn(ILOAD, 9);
            mv.visitInsn(IDIV);
            mv.visitVarInsn(ILOAD, 8);
            mv.visitInsn(ISUB);
            mv.visitVarInsn(ILOAD, 7);
            mv.visitInsn(IADD);
            mv.visitVarInsn(ILOAD, 6);
            mv.visitInsn(IDIV);
            mv.visitVarInsn(ILOAD, 5);
            mv.visitInsn(IDIV);
            mv.visitInsn(I2C);
            mv.visitInsn(CASTORE);
            Label l12 = new Label();
            mv.visitLabel(l12);
            mv.visitLineNumber(54, l12);
            mv.visitIincInsn(11, 1);
            mv.visitJumpInsn(GOTO, l9);
            mv.visitLabel(l10);
            mv.visitLineNumber(58, l10);
            mv.visitFrame(F_CHOP, 1, null, 0, null);
            mv.visitTypeInsn(NEW, "java/lang/String");
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 4);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V", false);
            mv.visitInsn(ARETURN);
            Label l13 = new Label();
            mv.visitLabel(l13);
            mv.visitLocalVariable("", "I", null, l9, l10, 11);
            mv.visitLocalVariable("", "[I", null, l0, l13, 0);
            mv.visitLocalVariable("", "J", null, l0, l13, 1);
            mv.visitLocalVariable("", "Ljava/util/Random;", null, l1, l13, 3);
            mv.visitLocalVariable("", "[C", null, l2, l13, 4);
            mv.visitLocalVariable("", "I", null, l3, l13, 5);
            mv.visitLocalVariable("", "I", null, l4, l13, 6);
            mv.visitLocalVariable("", "I", null, l5, l13, 7);
            mv.visitLocalVariable("", "I", null, l6, l13, 8);
            mv.visitLocalVariable("", "I", null, l7, l13, 9);
            mv.visitLocalVariable("", "I", null, l8, l13, 10);
            mv.visitMaxs(4, 12);
            mv.visitEnd();
        }
        cw.visitEnd();

        ClassReader reader = new ClassReader(cw.toByteArray());
        ClassNode node = new ClassNode();
        reader.accept(node, 0);

        return new StringObfInfo(node, deobfuscateMethodName, deobfuscateMethodDesc);
    }
}
