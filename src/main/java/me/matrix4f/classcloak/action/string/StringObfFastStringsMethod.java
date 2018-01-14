package me.matrix4f.classcloak.action.string;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import me.matrix4f.classcloak.ClassCloak;
import me.matrix4f.classcloak.action.name.creation.ClassNameCreator;
import me.matrix4f.classcloak.util.MethodBuilder;

import static org.objectweb.asm.Opcodes.*;

public class StringObfFastStringsMethod extends StringObfMethod {

    public static final int const1, const2, const3, const4, const5, const6;

    static {
        const1 = ClassCloak.rand.nextInt(100) + 900;
        const2 = ClassCloak.rand.nextInt(6);
        const3 = ClassCloak.rand.nextInt(53);
        const4 = ClassCloak.rand.nextInt(79);
        const5 = ClassCloak.rand.nextInt(20);
        const6 = ClassCloak.rand.nextInt(30);
    }

    public static String obfuscate(String in) {
        char[] chars = new char[in.length()];

        for(int i = 0; i < chars.length; i++)
            chars[i] = (char) (in.charAt(i) - (const2 - const1 + const6 + const5 + const3 - const4));
        return new String(chars);
    }

    private static String deobfuscate(String in) {
        char[] chars = new char[in.length()];
        int x = 1000;
        int y = 3;
        int z = 43;
        int w = 58;
        int f = 23;
        int a = 33;

        for(int i = 0; i < chars.length; i++)
            chars[i] = (char) (in.charAt(i) + y-x+a+f+z-w);
        return new String(chars);
    }

    @Override
    public StringObfInfo generateInfo() {
        String className = ClassNameCreator.instance.getName(null);
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        cw.visit(52, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", null);

        MethodBuilder.newEmptyConstructorExtendingObject(className, cw);

        String deobfMethodName = "a";
        String deobfMethodDesc = "(Ljava/lang/String;)Ljava/lang/String;";
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, deobfMethodName, deobfMethodDesc, null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(26, l0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
            mv.visitIntInsn(NEWARRAY, T_CHAR);
            mv.visitVarInsn(ASTORE, 1);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLineNumber(27, l1);
            mv.visitIntInsn(SIPUSH, const1);
            mv.visitVarInsn(ISTORE, 2);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitLineNumber(28, l2);
            mv.visitIntInsn(SIPUSH, const2);
            mv.visitVarInsn(ISTORE, 3);
            Label l3 = new Label();
            mv.visitLabel(l3);
            mv.visitLineNumber(29, l3);
            mv.visitIntInsn(SIPUSH, const3);
            mv.visitVarInsn(ISTORE, 4);
            Label l4 = new Label();
            mv.visitLabel(l4);
            mv.visitLineNumber(30, l4);
            mv.visitIntInsn(SIPUSH, const4);
            mv.visitVarInsn(ISTORE, 5);
            Label l5 = new Label();
            mv.visitLabel(l5);
            mv.visitLineNumber(31, l5);
            mv.visitIntInsn(SIPUSH, const5);
            mv.visitVarInsn(ISTORE, 6);
            Label l6 = new Label();
            mv.visitLabel(l6);
            mv.visitLineNumber(32, l6);
            mv.visitIntInsn(SIPUSH, const6);
            mv.visitVarInsn(ISTORE, 7);
            Label l7 = new Label();
            mv.visitLabel(l7);
            mv.visitLineNumber(34, l7);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, 8);
            Label l8 = new Label();
            mv.visitLabel(l8);
            mv.visitFrame(F_FULL, 9, new Object[]{"java/lang/String", "[C", INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER}, 0, new Object[]{});
            mv.visitVarInsn(ILOAD, 8);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitInsn(ARRAYLENGTH);
            Label l9 = new Label();
            mv.visitJumpInsn(IF_ICMPGE, l9);
            Label l10 = new Label();
            mv.visitLabel(l10);
            mv.visitLineNumber(35, l10);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ILOAD, 8);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 8);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitInsn(IADD);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitInsn(ISUB);
            mv.visitVarInsn(ILOAD, 7);
            mv.visitInsn(IADD);
            mv.visitVarInsn(ILOAD, 6);
            mv.visitInsn(IADD);
            mv.visitVarInsn(ILOAD, 4);
            mv.visitInsn(IADD);
            mv.visitVarInsn(ILOAD, 5);
            mv.visitInsn(ISUB);
            mv.visitInsn(I2C);
            mv.visitInsn(CASTORE);
            Label l11 = new Label();
            mv.visitLabel(l11);
            mv.visitLineNumber(34, l11);
            mv.visitIincInsn(8, 1);
            mv.visitJumpInsn(GOTO, l8);
            mv.visitLabel(l9);
            mv.visitLineNumber(36, l9);
            mv.visitFrame(F_CHOP, 1, null, 0, null);
            mv.visitTypeInsn(NEW, "java/lang/String");
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V", false);
            mv.visitInsn(ARETURN);
            Label l12 = new Label();
            mv.visitLabel(l12);
            mv.visitLocalVariable("", "I", null, l8, l9, 8);
            mv.visitLocalVariable("", "Ljava/lang/String;", null, l0, l12, 0);
            mv.visitLocalVariable("", "[C", null, l1, l12, 1);
            mv.visitLocalVariable("", "I", null, l2, l12, 2);
            mv.visitLocalVariable("", "I", null, l3, l12, 3);
            mv.visitLocalVariable("", "I", null, l4, l12, 4);
            mv.visitLocalVariable("", "I", null, l5, l12, 5);
            mv.visitLocalVariable("", "I", null, l6, l12, 6);
            mv.visitLocalVariable("", "I", null, l7, l12, 7);
            mv.visitMaxs(4, 9);
            mv.visitEnd();
        }

        cw.visitEnd();

        ClassReader reader = new ClassReader(cw.toByteArray());
        ClassNode node = new ClassNode();
        reader.accept(node, 0);
        return new StringObfInfo(node, deobfMethodName, deobfMethodDesc);
    }
}
