package me.matrix4f.classcloak.util.interpreter;

import jdk.internal.org.objectweb.asm.Opcodes;
import me.matrix4f.classcloak.util.BytecodeUtils;
import me.matrix4f.classcloak.util.interpreter.ObjectType.C;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

import static me.matrix4f.classcloak.Globals.LOGGER;
import static me.matrix4f.classcloak.util.interpreter.ObjectType.C.ONE;
import static me.matrix4f.classcloak.util.interpreter.ObjectType.C.TWO;
import static org.objectweb.asm.Opcodes.*;

public class StackBranchInterpreter {

    private List<AbstractInsnNode> list;

    private List<LocalVarAction> localVarActions = new ArrayList<>();
    private List<StackAction> stackActions = new ArrayList<>();

    private List<Field> fields = Arrays.asList(Opcodes.class.getDeclaredFields());

    public StackBranchInterpreter(List<AbstractInsnNode> list, String ownerClass, MethodNode method) {
        this.list = list;
        method.tryCatchBlocks.forEach(tcb -> {
            int index = list.indexOf(tcb.handler);
            if(index != -1)
                list.add(index, new TypeInsnNode(NEW, tcb.type));
        });

        if((method.access & ACC_STATIC) != ACC_STATIC) {
            //non static method has a local variable entry at index 0 with value this
            localVarActions.add(new LocalVarAction(
                    null, LocalVarAction.Type.CREATE, 0,
                    new ObjectType(
                            ObjectType.Type.OBJECT,
                            "L" + ownerClass + ";"
                    )
            ));
        }
        int prevSize = localVarActions.size();
        Type[] types = Type.getArgumentTypes(method.desc);
        for(int i = 0; i < types.length; i++) {
            localVarActions.add(new LocalVarAction(
                    null, LocalVarAction.Type.CREATE, i+prevSize, //add previous size in case the THIS was added
                    new ObjectType(
                            ObjectType.Type.OBJECT, types[i].getDescriptor())
                    )
            );
        }

        Collections.reverse(fields);
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

    //UTILITY METHODS FOR ANALYSIS
    private void pushStack(AbstractInsnNode node, String desc) {
        pushStack(node, new ObjectType(ObjectType.Type.OBJECT, desc));
    }

    private void pushStack(AbstractInsnNode node, ObjectType type) {
        stackActions.add(new StackAction(node, StackAction.Type.PUSH, type));
        if(getStackSizeAt(node) < 0)
            System.out.println("STACK SIZE IS BELOW 0 ERROR");
    }

    private void popStack(AbstractInsnNode node) {
        stackActions.add(new StackAction(node, StackAction.Type.POP, null));
        if(getStackSizeAt(node) < 0)
            System.out.println("STACK SIZE IS BELOW 0 ERROR");
    }

    private void setLocalVar(VarInsnNode node, ObjectType objectType) {
        LocalVarAction.Type type;
        if(localVarActions.stream()
                .mapToInt(LocalVarAction::getIndex)
                .anyMatch(var -> var == node.var)) {
            type = LocalVarAction.Type.CHANGE_TYPE;
        } else {
            type = LocalVarAction.Type.CREATE;
        }
        localVarActions.add(new LocalVarAction(node, type, node.var, objectType));
    }

    private void setLocalVar(VarInsnNode node, String desc) {
        setLocalVar(node, new ObjectType(ObjectType.Type.OBJECT, desc));
    }

    /**
     * Gets the type of the local variable at the specified index BEFORE the specified node
     * @param location The node at which to get the type of local variable
     * @param index The index of the local variable
     * @return The type of the local variable - null if not found
     */
    private ObjectType getLocalVarType(AbstractInsnNode location, int index) {
        ObjectType targetType = null;
        for(LocalVarAction action : localVarActions) {
            if(action.getSource() == location)
                break;
            if(action.getIndex() == index)
                targetType = action.getObjectType();
        }
        return targetType;
    }

    /**
     * Finds the type of the object on the stack before the specified instruction
     * @param location The instruction at which to find to find the stack size
     * @param down 0 for topmost, -1 for 2nd topmost etc
     * @param lenient Whether to return null if no value exists at the stack size
     * @return The type of the object on the stack at that position
     */
    public ObjectType getStackTypeAt(AbstractInsnNode location, int down, boolean lenient) {
        LinkedList<ObjectType> stack = new LinkedList<>();

        for (StackAction action : stackActions) {
            //stop when reaching the node
            if (action.getSource() == location) {
                break;
            }
            switch (action.getOperationType()) {
                case PUSH:
                    stack.add(action.getObjectType());
                    break;
                case POP:
                    stack.removeLast();
                    break;
            }
        }
        if(!lenient) {
            return stack.get(stack.size() - down - 1);
        } else {
            try {
                return stack.get(stack.size() - down - 1);
            } catch (IndexOutOfBoundsException e) {
                return null;
            }
        }
    }

    /**
     *
     * @param node The node at which to collect stack elements
     * @param size The number of elements to collect from the stack
     * @return An array containing the stack elements with the first element being the topmost element,
     * 2nd element is right below the topmost element, etc.
     */
    public ObjectType[] collectStack(AbstractInsnNode node, int size) {
        ObjectType[] stack = new ObjectType[size];
        for(int i = 0; i < size; i++)
            stack[i] = getStackTypeAt(node, i, true);
        return stack;
    }

    /**
     * Finds the stack size before the specified instruction
     * @param location The instruction at which to find the stack size
     * @return The size of the stack at that position
     */
    public int getStackSizeBefore(AbstractInsnNode location) {
        int size = 0;

        for (StackAction action : stackActions) {
            //stop when reaching the node
            if (action.getSource() == location) {
                break;
            }
            switch (action.getOperationType()) {
                case PUSH:
                    size++;
                    break;
                case POP:
                    size--;
                    break;
            }
        }
        return size;
    }

    public int getStackSizeAt(AbstractInsnNode location) {
        int size = 0;

        for (int i = 0; i < stackActions.size(); i++) {
            StackAction action = stackActions.get(i);
            switch (action.getOperationType()) {
                case PUSH:
                    size++;
                    break;
                case POP:
                    size--;
                    break;
            }
            //stop when reaching the node
            if (action.getSource() == location && (i == stackActions.size()-1 || stackActions.get(i+1).getSource() != location)) {
                break;
            }
        }
        return size;
    }

    public boolean stackCategoriesAre(AbstractInsnNode location, C... states) {
        if(getStackSizeBefore(location) < states.length)
            return false;
        ObjectType[] stack = collectStack(location, states.length);
        for(int i = 0; i < states.length; i++)
            if(stack[i].getCategory() != states[i])
                return false;

        return true;
    }

    //INTERPRETING METHODS
    private void interpretInsn(InsnNode node) {
        switch (node.getOpcode()) {
            case ACONST_NULL:
                pushStack(node, "Ljava/lang/Object;");
                break;
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5:
            case ICONST_M1:
                pushStack(node, "I");
                break;
            case LCONST_0:
            case LCONST_1:
                pushStack(node, "J");
                break;
            case DCONST_0:
            case DCONST_1:
                pushStack(node, "D");
                break;
            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
                pushStack(node, "F");
                break;
            case IALOAD:
                popStack(node);
                popStack(node);
                pushStack(node, "I");
                break;
            case BALOAD:
                popStack(node);
                popStack(node);
                pushStack(node, "B");
                break;
            case FALOAD:
                popStack(node);
                popStack(node);
                pushStack(node, "F");
                break;
            case AALOAD:
                popStack(node); //pop index
                popStack(node); //pop array reference

                String arrayObjectDesc = getStackTypeAt(node, 1, false).getDescriptor().substring(1); //remove the [
                
                pushStack(node, arrayObjectDesc);
                break;
            case SALOAD:
                popStack(node);
                popStack(node);
                pushStack(node, "S");
                break;
            case CALOAD:
                popStack(node);
                popStack(node);
                pushStack(node, "C");
                break;
            case LALOAD:
                popStack(node);
                popStack(node);
                pushStack(node, "L");
                break;
            case DALOAD:
                popStack(node);
                popStack(node);
                pushStack(node, "D");
                break;
            case IASTORE:
                popStack(node);
                popStack(node);
                popStack(node);
                break;
            case LASTORE:
                popStack(node);
                popStack(node);
                popStack(node);
                break;
            case FASTORE:
                popStack(node);
                popStack(node);
                popStack(node);
                break;
            case DASTORE:
                popStack(node);
                popStack(node);
                popStack(node);
                break;
            case AASTORE:
                popStack(node);
                popStack(node);
                popStack(node);
                break;
            case BASTORE:
                popStack(node);
                popStack(node);
                popStack(node);
                break;
            case CASTORE:
                popStack(node);
                popStack(node);
                popStack(node);
                break;
            case SASTORE:
                popStack(node);
                popStack(node);
                popStack(node);
                break;
            case POP:
            case ATHROW:
            case IRETURN:
            case FRETURN:
            case DRETURN:
            case LRETURN:
            case ARETURN:
                popStack(node);
                break;
            case POP2:
                popStack(node);
                popStack(node);
                break;
            case DUP:
                pushStack(node, getStackTypeAt(node, 0, false).getDescriptor());
                break;
            case DUP_X1: {
                ObjectType topValue = getStackTypeAt(node, 0, false);
                ObjectType belowTop = getStackTypeAt(node, 1, false);
                ObjectType topValueClone = topValue.clone();

                //v2, v1 -> v1, v2, v1
                popStack(node); // pop v1
                popStack(node); // pop v2
                pushStack(node, topValueClone); //push clone v1
                pushStack(node, belowTop); //push v2
                pushStack(node, topValue); //push v1
                break;
            }
            case DUP_X2: {
                if(stackCategoriesAre(node, ONE, ONE, ONE)) {
                    ObjectType[] stack = collectStack(node, 3);
                    //v3, v2, v1 -> v1, v3, v2, v1
                    popStack(node); //pop v1
                    popStack(node); //pop v2
                    popStack(node); //pop v3
                    pushStack(node, stack[0].clone()); //push v1's clone
                    pushStack(node, stack[2]); //push v3
                    pushStack(node, stack[1]); //push v2
                    pushStack(node, stack[0]); //push v1
                } else if(stackCategoriesAre(node, ONE, TWO)) {
                    ObjectType[] stack = collectStack(node, 2);
                    //v2, v1 -> v1, v2, v1
                    popStack(node); // pop v1
                    popStack(node); // pop v2
                    pushStack(node, stack[0].clone()); //push clone v1
                    pushStack(node, stack[1]); //push v2
                    pushStack(node, stack[0]); //push v1
                } else LOGGER.fatal("DUP X2 ANALYZER STACK ERROR");
                break;
            }
            case DUP2: {
                ObjectType topStack = getStackTypeAt(node, 0, false);
                if(topStack.isC1()) {
                    //v2, v1 -> v2, v1, v2, v1
                    ObjectType belowTop = getStackTypeAt(node, 1, false);
                    pushStack(node, belowTop.clone()); //push v2
                    pushStack(node, topStack.clone()); //push v1
                } else {
                    //v1 -> v1, v1
                    pushStack(node, topStack.clone());
                }
                break;
            }
            case DUP2_X1: {
                if(stackCategoriesAre(node, ONE, ONE, ONE)) {
                    //v3, v2, v1 -> v2, v1, v3, v2, v1
                    ObjectType[] stack = collectStack(node, 3);

                    popStack(node); //pop v1
                    popStack(node); //pop v2
                    popStack(node); //pop v3
                    pushStack(node, stack[1].clone()); //push v2's clone
                    pushStack(node, stack[0].clone()); //push v1's clone
                    pushStack(node, stack[2]); //push v3
                    pushStack(node, stack[1]); //push v2
                    pushStack(node, stack[0]); //push v1
                } else if(stackCategoriesAre(node, TWO, ONE)) {
                    //v2, v1 -> v1, v2, v1
                    ObjectType[] stack = collectStack(node, 2);
                    popStack(node); // pop v1
                    popStack(node); // pop v2
                    pushStack(node, stack[0].clone()); //push clone v1
                    pushStack(node, stack[1]); //push v2
                    pushStack(node, stack[0]); //push v1
                } else LOGGER.fatal("DUP2 X1 ANALYZER STACK ERROR");
                break;
            }
            case DUP2_X2: {
                if(stackCategoriesAre(node, ONE, ONE, ONE, ONE)) {
                    //v4, v3, v2, v1 -> v2, v1, v4, v3, v2, v1
                    ObjectType[] stack = collectStack(node, 4);
                    //pop v1, v2, v3, v4
                    popStack(node);
                    popStack(node);

                    popStack(node);
                    popStack(node);
                    //push v2, v1
                    pushStack(node, stack[1].clone());
                    pushStack(node, stack[0].clone());
                    //push v4, v3, v2, v1
                    pushStack(node, stack[3]);
                    pushStack(node, stack[2]);
                    pushStack(node, stack[1]);
                    pushStack(node, stack[0]);
                } else if(stackCategoriesAre(node, TWO, ONE, ONE) || stackCategoriesAre(node, ONE, ONE, TWO)) {
                    //v3, v2, v1 -> v1, v3, v2, v1
                    ObjectType[] stack = collectStack(node, 3);
                    //pop v3, v2, v1
                    popStack(node);
                    popStack(node);
                    popStack(node);
                    //push v1
                    pushStack(node, stack[0].clone());
                    //push v3, v2, v1
                    pushStack(node, stack[2]);
                    pushStack(node, stack[1]);
                    pushStack(node, stack[0]);
                } else if(stackCategoriesAre(node, TWO, TWO)) {
                    //v2, v1, -> v1, v2, v1
                    ObjectType[] stack = collectStack(node, 3);
                    //pop v2, v1
                    popStack(node);
                    popStack(node);
                    //push v1
                    pushStack(node, stack[0].clone());
                    //push v2, v1
                    pushStack(node, stack[1]);
                    pushStack(node, stack[0]);
                } else LOGGER.fatal("DUP2 X2 ANALYZER STACK ERROR");
                break;
            }
            case SWAP: {
                popStack(node);
                popStack(node);
                ObjectType[] stack = collectStack(node, 2);
                pushStack(node, stack[1]);
                pushStack(node, stack[0]);
                break;
            }
            case IADD:
            case ISUB:
            case IMUL:
            case IDIV:
            case IREM:

            case LADD:
            case LSUB:
            case LMUL:
            case LDIV:
            case LREM:

            case FADD:
            case FSUB:
            case FMUL:
            case FDIV:
            case FREM:

            case DADD:
            case DSUB:
            case DMUL:
            case DDIV:
            case DREM:

            case ISHR:
            case ISHL:
            case IUSHR:

            case LSHR:
            case LSHL:
            case LUSHR:

            case IAND:
            case IOR:
            case IXOR:

            case LAND:
            case LOR:
            case LXOR:
                popStack(node);
                break;

            case I2L:
            case F2L:
            case D2L:
                popStack(node);
                pushStack(node, "L");
                break;
            case I2F:
            case D2F:
            case L2F:
                popStack(node);
                pushStack(node, "F");
                break;
            case L2I:
            case D2I:
            case F2I:
                popStack(node);
                pushStack(node, "I");
                break;
            case L2D:
            case F2D:
            case I2D:
                popStack(node);
                pushStack(node, "D");
                break;
            case I2B:
                popStack(node);
                pushStack(node, "B");
                break;
            case I2C:
                popStack(node);
                pushStack(node, "C");
                break;
            case I2S:
                popStack(node);
                pushStack(node, "S");
                break;
            case LCMP:
            case FCMPL:
            case FCMPG:
            case DCMPL:
            case DCMPG:
                popStack(node);
                popStack(node);
                pushStack(node, "I");
                break;
            case ARRAYLENGTH:
                popStack(node);
                pushStack(node, "I");
                break;
            case MONITORENTER:
            case MONITOREXIT:
                popStack(node);
                break;
        }
    }

    private void interpretIntInsn(IntInsnNode node) {
        switch (node.getOpcode()) {
            case BIPUSH:
                pushStack(node, "B");
                break;
            case SIPUSH:
                pushStack(node, "S");
                break;
            case NEWARRAY:
                popStack(node);
                pushStack(node, BytecodeUtils.newarrayTypeMap.get(node.operand));
                break;
        }
    }

    private void interpretVarInsn(VarInsnNode node) {
        switch (node.getOpcode()) {
            case ILOAD:
                pushStack(node, "I");
                break;
            case LLOAD:
                pushStack(node, "J");
                break;
            case FLOAD:
                pushStack(node, "F");
                break;
            case DLOAD:
                pushStack(node, "D");
                break;
            case ALOAD:
                pushStack(node, getLocalVarType(node, node.var));
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
                ObjectType type = getStackTypeAt(node, 0, false);
                popStack(node);
                setLocalVar(node, type);
                break;
        }
    }

    private void interpretTypeInsn(TypeInsnNode node) {
        switch (node.getOpcode()) {
            case NEW:
                pushStack(node, "L" + node.desc + ";");
                break;
            case ANEWARRAY:
                popStack(node);
                pushStack(node, "[L" + node.desc + ";");
                break;
            case CHECKCAST:
                popStack(node);
                pushStack(node, node.desc.charAt(node.desc.length()-1) == ';' ? node.desc : ("L" + node.desc + ";"));
                break;
            case INSTANCEOF:
                popStack(node);
                pushStack(node, "Z");
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
                pushStack(node, node.desc);
                break;

            case PUTSTATIC:
                popStack(node);
                break;
            case GETSTATIC:
                pushStack(node, node.desc);
                break;
        }
    }

    private void interpretMethodInsn(MethodInsnNode node) {
        //pop the object off the stack (if it isn't static)
        if(node.getOpcode() != INVOKESTATIC)
            popStack(node);

        //pop the arguments off the stack
        for(int i = 0; i < Type.getArgumentTypes(node.desc).length; i++)
            popStack(node);

        //push the return type
        Type returnType;
        if((returnType = Type.getReturnType(node.desc)) != Type.VOID_TYPE) {
            pushStack(node, returnType.getDescriptor());
        }
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
            for(int i = 0; i <=  Type.getArgumentTypes(node.desc).length; i++)
                popStack(node);

            //push the return type
            Type returnType;
            if ((returnType = Type.getReturnType(node.desc)) != Type.VOID_TYPE)
                pushStack(node, returnType.getDescriptor());
        } else if(isField) {
            switch (tag) {
                case H_PUTFIELD:
                    popStack(node);
                    popStack(node);
                    break;
                case H_GETFIELD:
                    popStack(node);
                    pushStack(node, node.desc);
                    break;
                    
                case H_PUTSTATIC:
                    popStack(node);
                    break;
                case H_GETSTATIC:
                    pushStack(node, node.desc);
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
            pushStack(node, "I");
        else if(node.cst instanceof Long)
            pushStack(node, "J");
        else if(node.cst instanceof Float)
            pushStack(node, "F");
        else if(node.cst instanceof Double)
            pushStack(node, "D");
        else if(node.cst instanceof String)
            pushStack(node, "Ljava/lang/String;");
        else if(node.cst instanceof Type)
            pushStack(node, ((Type) node.cst).getDescriptor());
    }

    private void interpretIincInsn(IincInsnNode node) {}

    private void interpretTableswitchInsn(TableSwitchInsnNode node) {
        popStack(node);
    }

    private void interpretLookupswitchInsn(LookupSwitchInsnNode node) {
        popStack(node);
    }

    private void interpretMultiANewArrayInsn(MultiANewArrayInsnNode node) {
        for(int i = 0; i < node.dims; i++)
            popStack(node);
        pushStack(node, node.desc);
    }

    public List<AbstractInsnNode> getInsns() {
        return list;
    }
}
