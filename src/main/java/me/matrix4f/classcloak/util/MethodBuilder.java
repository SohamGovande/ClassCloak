package me.matrix4f.classcloak.util;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class MethodBuilder {

    private InsnList insnList;
    private List<LabelNode> labels;
    private List<LocalVariableNode> localVars;
    private List<TryCatchBlockNode> tryCatchBlockNodes;

    private MethodBuilder() {
        insnList = new InsnList();
        labels = new ArrayList<>();
        localVars = new ArrayList<>();
        tryCatchBlockNodes = new ArrayList<>();
    }

    public static MethodBuilder newBuilder() {
        return new MethodBuilder();
    }

    public static void newEmptyConstructorExtendingObject(String className, ClassWriter cv) {
        newBuilder()
                .label(0)
                .aload(0) //this
                .invokespecial("java/lang/Object")
                .return_()
                .label(1)

                .localVar("","L" + className + ";", null, 0, 1, 0)
                .writeMethod(cv, ACC_PUBLIC, "<init>", "()V", null, null);
    }
            
    public InsnList getInstructions() {
        return insnList;
    }

    public void writeMethod(ClassVisitor cv, int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        mv.visitCode();
        tryCatchBlockNodes.forEach(node -> mv.visitTryCatchBlock(
                node.start.getLabel(), node.end.getLabel(), node.handler.getLabel(), node.type
        ));

        BytecodeUtils.writeMethodInsns(insnList, mv);

        localVars.forEach(v -> mv.visitLocalVariable(v.name, v.desc, v.signature, v.start.getLabel(), v.end.getLabel(), v.index));

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    public void replace(InsnList target, AbstractInsnNode src) {
        target.insert(src, insnList);
        target.remove(src);
    }
    
    public void insertAfter(InsnList target, AbstractInsnNode src) {
        target.insert(src, insnList);
    }
    
    public void insertBefore(InsnList target, AbstractInsnNode src) {
        target.insertBefore(src, insnList);
    }

    public MethodBuilder label(int unused) {
        LabelNode l = new LabelNode();
        insnList.add(l);
        labels.add(l);
        return this;
    }
    
    public LabelNode getLabelAt(int index) {
        return labels.get(index);
    }

    public MethodBuilder localVar(String name, String desc, String signature, LabelNode start, LabelNode end, int index) {
        localVars.add(new LocalVariableNode(name, desc, signature, start, end, index));
        return this;
    }

    public MethodBuilder localVar(String name, String desc, String signature, int start, int end, int index) {
        localVars.add(new LocalVariableNode(name, desc, signature, labels.get(start), labels.get(end), index));
        return this;
    }

    public MethodBuilder abstractNode(AbstractInsnNode node) {
        if(node != null)
            insnList.add(node);
        return this;
    }

    public MethodBuilder trycatchblock(LabelNode start, LabelNode end, LabelNode catchBlock, String exception) {
        tryCatchBlockNodes.add(new TryCatchBlockNode(start,end,catchBlock,exception));
        return this;
    }

    public MethodBuilder goto_(LabelNode lbl) {
        insnList.add(new JumpInsnNode(GOTO, lbl));
        return this;
    }

    public MethodBuilder ifeq(LabelNode lbl) {
        insnList.add(new JumpInsnNode(IFEQ, lbl));
        return this;
    }

    public MethodBuilder ifne(LabelNode lbl) {
        insnList.add(new JumpInsnNode(IFNE, lbl));
        return this;
    }

    public MethodBuilder ifg(LabelNode lbl) {
        insnList.add(new JumpInsnNode(IFGT, lbl));
        return this;
    }

    public MethodBuilder ifl(LabelNode lbl) {
        insnList.add(new JumpInsnNode(IFLT, lbl));
        return this;
    }

    public MethodBuilder ifle(LabelNode lbl) {
        insnList.add(new JumpInsnNode(IFLE, lbl));
        return this;
    }

    public MethodBuilder ifge(LabelNode lbl) {
        insnList.add(new JumpInsnNode(IFGE, lbl));
        return this;
    }

    public MethodBuilder goto_(int lbl) {
        insnList.add(new JumpInsnNode(GOTO, labels.get(lbl)));
        return this;
    }

    public MethodBuilder ifeq(int lbl) {
        insnList.add(new JumpInsnNode(IFEQ, labels.get(lbl)));
        return this;
    }

    public MethodBuilder ifne(int lbl) {
        insnList.add(new JumpInsnNode(IFNE, labels.get(lbl)));
        return this;
    }

    public MethodBuilder ifg(int lbl) {
        insnList.add(new JumpInsnNode(IFGT, labels.get(lbl)));
        return this;
    }

    public MethodBuilder ifl(int lbl) {
        insnList.add(new JumpInsnNode(IFLT, labels.get(lbl)));
        return this;
    }

    public MethodBuilder ifle(int lbl) {
        insnList.add(new JumpInsnNode(IFLE, labels.get(lbl)));
        return this;
    }

    public MethodBuilder ifge(int lbl) {
        insnList.add(new JumpInsnNode(IFGE, labels.get(lbl)));
        return this;
    }

    public MethodBuilder new_(String desc) {
        insnList.add(new TypeInsnNode(NEW, desc));
        return this;
    }

    public MethodBuilder getstatic(String owner, String fname, String fdesc) {
        insnList.add(new FieldInsnNode(GETSTATIC, owner, fname, fdesc));
        return this;
    }

    public MethodBuilder putstatic(String owner, String fname, String fdesc) {
        insnList.add(new FieldInsnNode(PUTSTATIC, owner, fname, fdesc));
        return this;
    }

    public MethodBuilder getfield(String owner, String fname, String fdesc) {
        insnList.add(new FieldInsnNode(GETFIELD, owner, fname, fdesc));
        return this;
    }

    public MethodBuilder iinc(int var, int amt) {
        insnList.add(new IincInsnNode(var, amt));
        return this;
    }

    public MethodBuilder putfield(String owner, String fname, String fdesc) {
        insnList.add(new FieldInsnNode(PUTFIELD, owner, fname, fdesc));
        return this;
    }

    public MethodBuilder invokespecial(String owner) {
        insnList.add(new MethodInsnNode(INVOKESPECIAL, owner, "<init>", "()V", false));
        return this;
    }

    public MethodBuilder invokespecial(String owner, String desc) {
        insnList.add(new MethodInsnNode(INVOKESPECIAL, owner, "<init>", desc, false));
        return this;
    }

    public MethodBuilder invokevirtual(String owner, String name, String desc) {
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL, owner, name, desc, false));
        return this;
    }

    public MethodBuilder invokestatic(String owner, String name, String desc) {
        insnList.add(new MethodInsnNode(INVOKESTATIC, owner, name, desc, false));
        return this;
    }

    public MethodBuilder invokeinterface(String owner, String name, String desc) {
        insnList.add(new MethodInsnNode(INVOKEINTERFACE, owner, name, desc, true));
        return this;
    }

    public MethodBuilder i2l() {
        insnList.add(new InsnNode(I2L));
        return this;
    }

    public MethodBuilder i2s() {
        insnList.add(new InsnNode(I2S));
        return this;
    }

    public MethodBuilder i2d() {
        insnList.add(new InsnNode(I2D));
        return this;
    }

    public MethodBuilder i2f() {
        insnList.add(new InsnNode(I2F));
        return this;
    }

    public MethodBuilder i2b() {
        insnList.add(new InsnNode(I2B));
        return this;
    }

    public MethodBuilder i2c() {
        insnList.add(new InsnNode(I2C));
        return this;
    }

    public MethodBuilder l2i() {
        insnList.add(new InsnNode(L2I));
        return this;
    }

    public MethodBuilder l2f() {
        insnList.add(new InsnNode(L2F));
        return this;
    }

    public MethodBuilder l2d() {
        insnList.add(new InsnNode(L2D));
        return this;
    }

    public MethodBuilder f2i() {
        insnList.add(new InsnNode(F2I));
        return this;
    }

    public MethodBuilder f2d() {
        insnList.add(new InsnNode(F2D));
        return this;
    }

    public MethodBuilder f2l() {
        insnList.add(new InsnNode(F2L));
        return this;
    }

    public MethodBuilder d2i() {
        insnList.add(new InsnNode(D2I));
        return this;
    }

    public MethodBuilder d2l() {
        insnList.add(new InsnNode(D2L));
        return this;
    }

    public MethodBuilder d2f() {
        insnList.add(new InsnNode(D2F));
        return this;
    }

    public MethodBuilder ineg() {
        insnList.add(new InsnNode(INEG));
        return this;
    }

    public MethodBuilder nop() {
        insnList.add(new InsnNode(NOP));
        return this;
    }

    public MethodBuilder aconst_null() {
        insnList.add(new InsnNode(ACONST_NULL));
        return this;
    }

    public MethodBuilder acmpNOTEQUAL(LabelNode lbl) {
        insnList.add(new JumpInsnNode(IF_ACMPNE, lbl));
        return this;
    }

    public MethodBuilder acmpNOTEQUAL(int lbl) {
        insnList.add(new JumpInsnNode(IF_ACMPNE, labels.get(lbl)));
        return this;
    }

    public MethodBuilder acmpEQUAL(LabelNode lbl) {
        insnList.add(new JumpInsnNode(IF_ACMPEQ, lbl));
        return this;
    }

    public MethodBuilder acmpEQUAL(int lbl) {
        insnList.add(new JumpInsnNode(IF_ACMPEQ, labels.get(lbl)));
        return this;
    }

    public MethodBuilder ifnonnnull(int lbl) {
        insnList.add(new JumpInsnNode(IFNONNULL, labels.get(lbl)));
        return this;
    }

    public MethodBuilder ifnonnnull(LabelNode lbl) {
        insnList.add(new JumpInsnNode(IFNONNULL, lbl));
        return this;
    }

    public MethodBuilder icmpEQUAL(int lbl) {
        insnList.add(new JumpInsnNode(IF_ICMPEQ, labels.get(lbl)));
        return this;
    }

    public MethodBuilder icmpNOTEQUAL(int lbl) {
        insnList.add(new JumpInsnNode(IF_ICMPNE, labels.get(lbl)));
        return this;
    }

    public MethodBuilder icmpLEQUAL(int lbl) {
        insnList.add(new JumpInsnNode(IF_ICMPLE, labels.get(lbl)));
        return this;
    }

    public MethodBuilder icmpGEQUAL(int lbl) {
        insnList.add(new JumpInsnNode(IF_ICMPGE, labels.get(lbl)));
        return this;
    }

    public MethodBuilder icmpG(int lbl) {
        insnList.add(new JumpInsnNode(IF_ICMPGT, labels.get(lbl)));
        return this;
    }

    public MethodBuilder icmpL(int lbl) {
        insnList.add(new JumpInsnNode(IF_ICMPLT, labels.get(lbl)));
        return this;
    }
    
    public MethodBuilder icmpEQUAL(LabelNode lbl) {
        insnList.add(new JumpInsnNode(IF_ICMPEQ, lbl));
        return this;
    }
    
    public MethodBuilder icmpNOTEQUAL(LabelNode lbl) {
        insnList.add(new JumpInsnNode(IF_ICMPNE, lbl));
        return this;
    }

    public MethodBuilder icmpLEQUAL(LabelNode lbl) {
        insnList.add(new JumpInsnNode(IF_ICMPLE, lbl));
        return this;
    }

    public MethodBuilder icmpGEQUAL(LabelNode lbl) {
        insnList.add(new JumpInsnNode(IF_ICMPGE, lbl));
        return this;
    }

    public MethodBuilder icmpG(LabelNode lbl) {
        insnList.add(new JumpInsnNode(IF_ICMPGT, lbl));
        return this;
    }

    public MethodBuilder icmpL(LabelNode lbl) {
        insnList.add(new JumpInsnNode(IF_ICMPLT, lbl));
        return this;
    }

    public MethodBuilder lneg() {
        insnList.add(new InsnNode(LNEG));
        return this;
    }

    public MethodBuilder dneg() {
        insnList.add(new InsnNode(DNEG));
        return this;
    }
    
    public MethodBuilder fneg() {
        insnList.add(new InsnNode(FNEG));
        return this;
    }

    public MethodBuilder daload() {
        insnList.add(new InsnNode(DALOAD));
        return this;
    }

    public MethodBuilder faload() {
        insnList.add(new InsnNode(FALOAD));
        return this;
    }

    public MethodBuilder iaload() {
        insnList.add(new InsnNode(IALOAD));
        return this;
    }

    public MethodBuilder baload() {
        insnList.add(new InsnNode(BALOAD));
        return this;
    }

    public MethodBuilder saload() {
        insnList.add(new InsnNode(SALOAD));
        return this;
    }

    public MethodBuilder laload() {
        insnList.add(new InsnNode(LALOAD));
        return this;
    }
    
    public MethodBuilder aastore() {
        insnList.add(new InsnNode(AASTORE));
        return this;
    }

    public MethodBuilder dastore() {
        insnList.add(new InsnNode(DASTORE));
        return this;
    }

    public MethodBuilder fastore() {
        insnList.add(new InsnNode(FASTORE));
        return this;
    }

    public MethodBuilder iastore() {
        insnList.add(new InsnNode(IASTORE));
        return this;
    }

    public MethodBuilder bastore() {
        insnList.add(new InsnNode(BASTORE));
        return this;
    }

    public MethodBuilder sastore() {
        insnList.add(new InsnNode(SASTORE));
        return this;
    }

    public MethodBuilder lastore() {
        insnList.add(new InsnNode(LASTORE));
        return this;
    }

    public MethodBuilder pop() {
        insnList.add(new InsnNode(POP));
        return this;
    }

    public MethodBuilder dup() {
        insnList.add(new InsnNode(DUP));
        return this;
    }

    public MethodBuilder swap() {
        insnList.add(new InsnNode(SWAP));
        return this;
    }

    public MethodBuilder return_() {
        insnList.add(new InsnNode(RETURN));
        return this;
    }

    public MethodBuilder checkcast(String type) {
        insnList.add(new TypeInsnNode(CHECKCAST, type));
        return this;
    }

    public MethodBuilder aaload() {
        insnList.add(new InsnNode(AALOAD));
        return this;
    }

    public MethodBuilder fmul() {
        insnList.add(new InsnNode(FMUL));
        return this;
    }

    public MethodBuilder label(LabelNode lbl) {
        insnList.add(lbl);
        labels.add(lbl);
        return this;
    }

    public MethodBuilder dsub() {
        insnList.add(new InsnNode(DSUB));
        return this;
    }

    public MethodBuilder dadd() {
        insnList.add(new InsnNode(DADD));
        return this;
    }

    public MethodBuilder ddiv() {
        insnList.add(new InsnNode(DDIV));
        return this;
    }

    public MethodBuilder drem() {
        insnList.add(new InsnNode(DREM));
        return this;
    }

    public MethodBuilder lsub() {
        insnList.add(new InsnNode(LSUB));
        return this;
    }

    public MethodBuilder ladd() {
        insnList.add(new InsnNode(LADD));
        return this;
    }

    public MethodBuilder ldiv() {
        insnList.add(new InsnNode(LDIV));
        return this;
    }

    public MethodBuilder lrem() {
        insnList.add(new InsnNode(LREM));
        return this;
    }
    
    public MethodBuilder fsub() {
        insnList.add(new InsnNode(FSUB));
        return this;
    }

    public MethodBuilder fadd() {
        insnList.add(new InsnNode(FADD));
        return this;
    }

    public MethodBuilder fdiv() {
        insnList.add(new InsnNode(FDIV));
        return this;
    }

    public MethodBuilder frem() {
        insnList.add(new InsnNode(FREM));
        return this;
    }
    
    public MethodBuilder imul() {
        insnList.add(new InsnNode(IMUL));
        return this;
    }

    public MethodBuilder isub() {
        insnList.add(new InsnNode(ISUB));
        return this;
    }
    
    public MethodBuilder iadd() {
        insnList.add(new InsnNode(IADD));
        return this;
    }

    public MethodBuilder idiv() {
        insnList.add(new InsnNode(IDIV));
        return this;
    }
    
    public MethodBuilder irem() {
        insnList.add(new InsnNode(IREM));
        return this;
    }

    public MethodBuilder newarray(int type) {
        insnList.add(new IntInsnNode(NEWARRAY, type));
        return this;
    }

    public MethodBuilder anewarray(String type) {
        insnList.add(new TypeInsnNode(ANEWARRAY, type));
        return this;
    }

    public MethodBuilder ireturn() {
        insnList.add(new InsnNode(IRETURN));
        return this;
    }

    public MethodBuilder freturn() {
        insnList.add(new InsnNode(FRETURN));
        return this;
    }

    public MethodBuilder areturn() {
        insnList.add(new InsnNode(ARETURN));
        return this;
    }

    public MethodBuilder lreturn() {
        insnList.add(new InsnNode(LRETURN));
        return this;
    }

    public MethodBuilder dreturn() {
        insnList.add(new InsnNode(DRETURN));
        return this;
    }

    public MethodBuilder fconst_0() {
        insnList.add(new InsnNode(FCONST_0));
        return this;
    }

    public MethodBuilder fconst_1() {
        insnList.add(new InsnNode(FCONST_1));
        return this;
    }

    public MethodBuilder fconst_2() {
        insnList.add(new InsnNode(FCONST_2));
        return this;
    }

    public MethodBuilder iconst_0() {
        insnList.add(new InsnNode(ICONST_0));
        return this;
    }
    
    public MethodBuilder iconst_1() {
        insnList.add(new InsnNode(ICONST_1));
        return this;
    }

    public MethodBuilder iconst_2() {
        insnList.add(new InsnNode(ICONST_2));
        return this;
    }

    public MethodBuilder iconst_3() {
        insnList.add(new InsnNode(ICONST_3));
        return this;
    }

    public MethodBuilder iconst_4() {
        insnList.add(new InsnNode(ICONST_4));
        return this;
    }

    public MethodBuilder iconst_5() {
        insnList.add(new InsnNode(ICONST_5));
        return this;
    }

    public MethodBuilder iconst_m1() {
        insnList.add(new InsnNode(ICONST_M1));
        return this;
    }

    public MethodBuilder dconst_0() {
        insnList.add(new InsnNode(DCONST_0));
        return this;
    }

    public MethodBuilder dconst_1() {
        insnList.add(new InsnNode(DCONST_1));
        return this;
    }

    public MethodBuilder lconst_0() {
        insnList.add(new InsnNode(LCONST_0));
        return this;
    }

    public MethodBuilder lconst_1() {
        insnList.add(new InsnNode(LCONST_1));
        return this;
    }

    public MethodBuilder sipush(int i) {
        insnList.add(new VarInsnNode(SIPUSH, i));
        return this;
    }

    public MethodBuilder bipush(byte i) {
        insnList.add(new VarInsnNode(BIPUSH, i));
        return this;
    }

    public MethodBuilder aload(int i) {
        insnList.add(new VarInsnNode(ALOAD, i));
        return this;
    }

    public MethodBuilder astore(int i) {
        insnList.add(new VarInsnNode(ASTORE, i));
        return this;
    }

    public MethodBuilder iload(int i) {
        insnList.add(new VarInsnNode(ILOAD, i));
        return this;
    }

    public MethodBuilder istore(int i) {
        insnList.add(new VarInsnNode(ISTORE, i));
        return this;
    }

    public MethodBuilder ldc(Object cst) {
        insnList.add(new LdcInsnNode(cst));
        return this;
    }

    public MethodBuilder fload(int i) {
        insnList.add(new VarInsnNode(FLOAD, i));
        return this;
    }

    public MethodBuilder fstore(int i) {
        insnList.add(new VarInsnNode(FSTORE, i));
        return this;
    }

    public MethodBuilder dload(int i) {
        insnList.add(new VarInsnNode(DLOAD, i));
        return this;
    }

    public MethodBuilder dstore(int i) {
        insnList.add(new VarInsnNode(DSTORE, i));
        return this;
    }

    public MethodBuilder lload(int i) {
        insnList.add(new VarInsnNode(LLOAD, i));
        return this;
    }

    public MethodBuilder lstore(int i) {
        insnList.add(new VarInsnNode(LSTORE, i));
        return this;
    }


}
