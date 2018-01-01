package me.matrix4f.classcloak.util.interpreter;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import me.matrix4f.classcloak.util.BytecodeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.objectweb.asm.Opcodes.*;

public class StackBranchInterpreter {

    private List<AbstractInsnNode> list;

    private List<LocalVarAction> localVarActions = new ArrayList<>();
    private List<StackAction> stackActions = new ArrayList<>();

    public StackBranchInterpreter(List<AbstractInsnNode> list, String descriptor) {
        this.list = list;
        Type[] types = Type.getArgumentTypes(descriptor);
        for(int i = 0; i < types.length; i++) {
            localVarActions.add(new LocalVarAction(
                    null, LocalVarAction.Type.CREATE, i,
                    new ObjectType(
                            ObjectType.Type.OBJECT, types[i].getDescriptor())
                    )
            );
        }
    }

    public void interpret() {
        for(AbstractInsnNode node : list) {
            switch (node.getType()) {
                case AbstractInsnNode.INSN:
                    interpretInsn((InsnNode) node);
                    break;
                case AbstractInsnNode.INT_INSN:
                    interpretIntInsn((IntInsnNode) node);
                    break;
                case AbstractInsnNode.VAR_INSN:
                    interpretVarInsn((VarInsnNode) node);
                    break;
                case AbstractInsnNode.TYPE_INSN:
                    interpretTypeInsn((TypeInsnNode) node);
                    break;
                case AbstractInsnNode.FIELD_INSN:
                    interpretFieldInsn((FieldInsnNode) node);
                    break;
                case AbstractInsnNode.METHOD_INSN:
                    interpretMethodInsn((MethodInsnNode) node);
                    break;
                case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
                    interpretInvokeDynamicInsn((InvokeDynamicInsnNode) node);
                    break;
                case AbstractInsnNode.JUMP_INSN:
                    interpretJumpInsn((JumpInsnNode) node);
                    break;
//                case AbstractInsnNode.LABEL:
//                    break;
                case AbstractInsnNode.LDC_INSN:
                    interpretLdcInsn((LdcInsnNode) node);
                    break;
                case AbstractInsnNode.IINC_INSN:
                    interpretIincInsn((IincInsnNode) node);
                    break;
                case AbstractInsnNode.TABLESWITCH_INSN:
                    interpretTableswitchInsn((TableSwitchInsnNode) node);
                    break;
                case AbstractInsnNode.LOOKUPSWITCH_INSN:
                    interpretLookupswitchInsn((LookupSwitchInsnNode) node);
                    break;
                case AbstractInsnNode.MULTIANEWARRAY_INSN:
                    interpretMultiANewArrayInsn((MultiANewArrayInsnNode) node);
                    break;
//                case AbstractInsnNode.FRAME:
//                    break;
//                case AbstractInsnNode.LINE:
//                    break;
            }
        }
    }

//    private void interpretLabel(LabelNode node) {}
//    private void interpretLine(LineNumberNode node) {}
//    private void interpretFrame(FrameNode node) {}

    //UTILITY METHODS
    private void pushStackObj(AbstractInsnNode node, String desc) {
        stackActions.add(new StackAction(node, StackAction.Type.PUSH, new ObjectType(ObjectType.Type.OBJECT, desc)));
    }

    private void popStack(AbstractInsnNode node) {
        stackActions.add(new StackAction(node, StackAction.Type.POP, null));
    }

    private void setLocalVar(VarInsnNode node, String desc) {
        LocalVarAction.Type type;
        if(localVarActions.stream()
                .mapToInt(LocalVarAction::getIndex)
                .anyMatch(var -> var == node.var)) {
            type = LocalVarAction.Type.CHANGE_TYPE;
        } else {
            type = LocalVarAction.Type.CREATE;
        }
        localVarActions.add(new LocalVarAction(node, type, node.var, new ObjectType(ObjectType.Type.OBJECT, desc)));
    }

    private String getLocalVarDesc(int index) {
        return localVarActions.stream()
                .sorted(Collections.reverseOrder())
                .filter(var -> var.getIndex() == index)
                .map(LocalVarAction::getObjectType)
                .map(ObjectType::getDescriptor)
                .findFirst()
                .get();
    }


    //INTERPRETING METHODS
    private void interpretInsn(InsnNode node) {
        switch (node.getOpcode()) {
            case ACONST_NULL:
                pushStackObj(node, "Ljava/lang/Object;");
                break;
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5:
            case ICONST_M1:
                pushStackObj(node, "I");
                break;
            case LCONST_0:
            case LCONST_1:
                pushStackObj(node, "J");
                break;
            case DCONST_0:
            case DCONST_1:
                pushStackObj(node, "D");
                break;
            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
                pushStackObj(node, "F");
                break;
            case IALOAD:
                popStack(node);
                popStack(node);
                pushStackObj(node, "I");
                break;
            case BALOAD:
                popStack(node);
                popStack(node);
                pushStackObj(node, "B");
                break;
            case FALOAD:
                popStack(node);
                popStack(node);
                pushStackObj(node, "F");
                break;
            case AALOAD:
                //todo better decsriptor, track arrays perhaps?
                //todo track the stack and find of which type this array is
                popStack(node); //pop index
                //the array reference is on the stack
                popStack(node);
                pushStackObj(node, "Ljava/lang/Object;");
                break;
        }
    }

    private void interpretIntInsn(IntInsnNode node) {
        switch (node.getOpcode()) {
            case BIPUSH:
                pushStackObj(node, "B");
                break;
            case SIPUSH:
                pushStackObj(node, "S");
                break;
            case NEWARRAY:
                popStack(node);
                pushStackObj(node, BytecodeUtils.newarrayTypeMap.get(node.operand));
                break;
        }
    }

    private void interpretVarInsn(VarInsnNode node) {
        switch (node.getOpcode()) {
            //TODO ret
            case ILOAD:
                pushStackObj(node, "I");
                break;
            case LLOAD:
                pushStackObj(node, "J");
                break;
            case FLOAD:
                pushStackObj(node, "F");
                break;
            case DLOAD:
                pushStackObj(node, "D");
                break;
            case ALOAD:
                pushStackObj(node, getLocalVarDesc(node.var));
                break;
            case ISTORE:
                popStack(node);
                setLocalVar(node, "I");
                break;
            case LSTORE:
                popStack(node);
                setLocalVar(node,"J");
                break;
            case DSTORE:
                popStack(node);
                setLocalVar(node,"D");
                break;
            case FSTORE:
                popStack(node);
                setLocalVar(node,"F");
                break;
            case ASTORE:
                popStack(node);
                setLocalVar(node, getLocalVarDesc(node.var));
                break;
        }
    }

    private void interpretTypeInsn(TypeInsnNode node) {
        switch (node.getOpcode()) {
            case NEW:
                pushStackObj(node, node.desc);
                break;
            case ANEWARRAY:
                popStack(node);
                pushStackObj(node, node.desc);
                break;
            case CHECKCAST:
                popStack(node);
                pushStackObj(node, node.desc);
                break;
            case INSTANCEOF:
                popStack(node);
                pushStackObj(node, "Z");
                break;
        }
    }

    private void interpretFieldInsn(FieldInsnNode node) {
        switch (node.getOpcode()) {
            case PUTFIELD:
                popStack(node);
                popStack(node);
                break;
            case GETFIELD:
                popStack(node);
                pushStackObj(node, node.desc);
                break;

            case PUTSTATIC:
                popStack(node);
                break;
            case GETSTATIC:
                pushStackObj(node, node.desc);
                break;
        }
    }

    private void interpretMethodInsn(MethodInsnNode node) {
        //pop the object off the stack (if it isn't static)
        if(node.getOpcode() != INVOKESTATIC)
            popStack(node);
        //pop the arguments off the stack
        IntStream.range(0, Type.getArgumentTypes(node.desc).length)
                .forEach(value -> popStack(node));

        //push the return type
        Type returnType;
        if((returnType = Type.getReturnType(node.desc)) != Type.VOID_TYPE)
            pushStackObj(node, returnType.getDescriptor());
    }

    private void interpretInvokeDynamicInsn(InvokeDynamicInsnNode node) {
        int tag = node.bsm.getTag();

        boolean nonStaticMethod = tag == H_INVOKEVIRTUAL || tag == H_INVOKEINTERFACE || tag == H_INVOKESPECIAL || tag == H_NEWINVOKESPECIAL;
        boolean isMethod = nonStaticMethod ||  tag == H_INVOKESTATIC;
        boolean isField = tag == H_PUTFIELD || tag == H_GETFIELD || tag == H_PUTSTATIC || tag == H_GETSTATIC;

        if(isMethod) {
            //pop the object off the stack (if it isn't static)
            if(nonStaticMethod)
                popStack(node);

            //pop the arguments off the stack
            IntStream.range(0, Type.getArgumentTypes(node.desc).length)
                    .forEach(value -> popStack(node));

            //push the return type
            Type returnType;
            if ((returnType = Type.getReturnType(node.desc)) != Type.VOID_TYPE)
                pushStackObj(node, returnType.getDescriptor());
        } else if(isField) {
            switch (tag) {
                case H_PUTFIELD:
                    popStack(node);
                    popStack(node);
                    break;
                case H_GETFIELD:
                    popStack(node);
                    pushStackObj(node, node.desc);
                    break;
                    
                case H_PUTSTATIC:
                    popStack(node);
                    break;
                case H_GETSTATIC:
                    pushStackObj(node, node.desc);
                    break;
            }
        }
    }

    private void interpretJumpInsn(JumpInsnNode node) {
        switch (node.getOpcode()) {
            case IFEQ:
            case IFNE:

            case IFLT:
            case IFLE:

            case IFGE:
            case IFGT:

            case IFNULL:
            case IFNONNULL:

                popStack(node);
                break;
            case IF_ICMPEQ:
            case IF_ICMPNE:

            case IF_ICMPGE:
            case IF_ICMPGT:

            case IF_ICMPLE:
            case IF_ICMPLT:

            case IF_ACMPEQ:
            case IF_ACMPNE:
                popStack(node);
                popStack(node);
                break;
            case GOTO:
                break;
            case JSR:
                stackActions.add(new StackAction(node, StackAction.Type.PUSH, new ObjectType(ObjectType.Type.RETURN_ADDRESS, node.label)));
                break;
        }
    }

    private void interpretLdcInsn(LdcInsnNode node) {
        if(node.cst instanceof Integer)
            pushStackObj(node, "I");
        else if(node.cst instanceof Long)
            pushStackObj(node, "J");
        else if(node.cst instanceof Float)
            pushStackObj(node, "F");
        else if(node.cst instanceof Double)
            pushStackObj(node, "D");
        else if(node.cst instanceof String)
            pushStackObj(node, "Ljava/lang/String;");
        else if(node.cst instanceof Type)
            pushStackObj(node, ((Type) node.cst).getDescriptor());
    }

    private void interpretIincInsn(IincInsnNode node) {}

    private void interpretTableswitchInsn(TableSwitchInsnNode node) {
        popStack(node);
    }

    private void interpretLookupswitchInsn(LookupSwitchInsnNode node) {
        popStack(node);
    }

    private void interpretMultiANewArrayInsn(MultiANewArrayInsnNode node) {
        IntStream.range(0, node.dims)
                .forEach(value -> popStack(node));
        pushStackObj(node, node.desc);
    }

    public List<AbstractInsnNode> getInsns() {
        return list;
    }
}
