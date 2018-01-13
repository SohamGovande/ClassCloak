package me.matrix4f.classcloak.util;

import org.objectweb.asm.tree.*;

import java.util.LinkedList;
import java.util.List;

public class InsnCloneFactory {

    public static LinkedList<AbstractInsnNode> cloneList(List<AbstractInsnNode> nodes) {
        LinkedList<AbstractInsnNode> list = new LinkedList<>();
        nodes.stream()
                .map(InsnCloneFactory::cloneGeneric)
                .forEach(list::add);
        return list;
    }

    @SuppressWarnings("unchecked")
    public static <T extends AbstractInsnNode> T cloneGeneric(T node) {
        if(node instanceof InsnNode)
            return (T) cloneInsn((InsnNode) node);
        if(node instanceof IntInsnNode)
            return (T) cloneIntInsn((IntInsnNode) node);
        if(node instanceof VarInsnNode)
            return (T) cloneVarInsn((VarInsnNode) node);
        if(node instanceof MethodInsnNode)
            return (T) cloneMethodInsn((MethodInsnNode) node);
        if(node instanceof FieldInsnNode)
            return (T) cloneFieldInsn((FieldInsnNode) node);
        if(node instanceof InvokeDynamicInsnNode)
            return (T) cloneInvokeDynamic((InvokeDynamicInsnNode) node);
        if(node instanceof FrameNode)
            return (T) cloneFrame((FrameNode) node);
        if(node instanceof LineNumberNode)
            return (T) cloneLine((LineNumberNode) node);
        if(node instanceof IincInsnNode)
            return (T) cloneIincInsn((IincInsnNode) node);
        if(node instanceof LdcInsnNode)
            return (T) cloneLdcInsn((LdcInsnNode) node);
        if(node instanceof MultiANewArrayInsnNode)
            return (T) cloneMANAI((MultiANewArrayInsnNode) node);
        if(node instanceof LookupSwitchInsnNode)
            return (T) cloneLSI((LookupSwitchInsnNode) node);
        if(node instanceof TableSwitchInsnNode)
            return (T) cloneTSI((TableSwitchInsnNode) node);
        if(node instanceof JumpInsnNode)
            return (T) cloneJump((JumpInsnNode) node);
        if(node instanceof TypeInsnNode)
            return (T) cloneTypeInsn((TypeInsnNode) node);
        if(node instanceof LabelNode)
            return node;
        System.err.println("BADNODE " + node);
        return node;
    }

    public static InsnNode cloneInsn(InsnNode node) {
        return new InsnNode(node.getOpcode());
    }

    public static IntInsnNode cloneIntInsn(IntInsnNode node) {
        return new IntInsnNode(node.getOpcode(), node.operand);
    }

    public static VarInsnNode cloneVarInsn(VarInsnNode node) {
        return new VarInsnNode(node.getOpcode(), node.var);
    }

    public static InvokeDynamicInsnNode cloneInvokeDynamic(InvokeDynamicInsnNode node) {
        return new InvokeDynamicInsnNode(node.name, node.desc, node.bsm, node.bsmArgs);
    }

    public static IincInsnNode cloneIincInsn(IincInsnNode node) {
        return new IincInsnNode(node.var, node.incr);
    }

    public static MethodInsnNode cloneMethodInsn(MethodInsnNode node) {
        return new MethodInsnNode(node.getOpcode(), node.owner, node.name, node.desc, node.itf);
    }

    public static FieldInsnNode cloneFieldInsn(FieldInsnNode node) {
        return new FieldInsnNode(node.getOpcode(), node.owner, node.name, node.desc);
    }

    public static FrameNode cloneFrame(FrameNode node) {
        boolean nullLocals = node.local == null;
        boolean nullStack = node.stack == null;
        return new FrameNode(node.type, nullLocals ? 0 : node.local.size(), nullLocals ? new Object[0] : node.local.toArray(), nullStack ? 0 : node.stack.size(), nullStack ? new Object[0] : node.stack.toArray());
    }

    public static LineNumberNode cloneLine(LineNumberNode node) {
        return new LineNumberNode(node.line, node.start);
    }

    public static TypeInsnNode cloneTypeInsn(TypeInsnNode node) {
        return new TypeInsnNode(node.getOpcode(), node.desc);
    }

    public static LdcInsnNode cloneLdcInsn(LdcInsnNode node) {
        return new LdcInsnNode(node.cst);
    }

    public static TableSwitchInsnNode cloneTSI(TableSwitchInsnNode node) {
        return new TableSwitchInsnNode(node.min, node.max, node.dflt, node.labels.toArray(new LabelNode[node.labels.size()]));
    }

    public static LookupSwitchInsnNode cloneLSI(LookupSwitchInsnNode node) {
        return new LookupSwitchInsnNode(node.dflt, node.keys.stream().mapToInt(Integer::intValue).toArray(), node.labels.toArray(new LabelNode[node.labels.size()]));
    }

    public static JumpInsnNode cloneJump(JumpInsnNode node) {
        return new JumpInsnNode(node.getOpcode(), node.label);
    }

    public static MultiANewArrayInsnNode cloneMANAI(MultiANewArrayInsnNode node) {
        return new MultiANewArrayInsnNode(node.desc, node.dims);
    }
}
