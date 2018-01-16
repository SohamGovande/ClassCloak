package me.matrix4f.classcloak.action.reflection;

import me.matrix4f.classcloak.util.BytecodeUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.*;

import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.DUP;

public class Optimization {

    private static Map<String, Class> primitiveClasses = new HashMap<>();
    static {
        primitiveClasses.put("java/lang/Integer",int.class);
        primitiveClasses.put("java/lang/Byte",byte.class);
        primitiveClasses.put("java/lang/Void",void.class);
        primitiveClasses.put("java/lang/Character",char.class);
        primitiveClasses.put("java/lang/Short",short.class);
        primitiveClasses.put("java/lang/Long",long.class);
        primitiveClasses.put("java/lang/Float",float.class);
        primitiveClasses.put("java/lang/Double",double.class);
        primitiveClasses.put("java/lang/Boolean",boolean.class);
    }

    public static String classArrayToDescriptor(List<AbstractInsnNode> classArrayInsns) {
        int size = BytecodeUtils.getIntValue(classArrayInsns.get(0));
        String[] classes = new String[size];
        int index = 0;
        for (int i = 4; i < classArrayInsns.size(); i += 4) {
            AbstractInsnNode node = classArrayInsns.get(i);
            if(node instanceof FieldInsnNode)
                classes[index] = Type.getDescriptor(primitiveClasses.get(((FieldInsnNode) node).owner));
            else
                classes[index] = ((Type) ((LdcInsnNode) node).cst).getDescriptor();
            index++;
        }
        StringBuilder buf = new StringBuilder("(");
        for(String str : classes)
            buf.append(str);
        buf.append(")");
        return buf.toString();
    }

    public static boolean isBasicArray(List<AbstractInsnNode> arrayInsns) {
        int counter = 0;

        for (int i = 0; i < arrayInsns.size(); i++) {
            AbstractInsnNode node = arrayInsns.get(i);
            if (i == 0) {
                if (BytecodeUtils.getIntValue(node) == null)
                    return false;
            } else if (i == 1) {
                if (node.getOpcode() != ANEWARRAY || !((TypeInsnNode) node).desc.equals("java/lang/Class"))
                    return false;
            } else {
                int c = counter;
                counter++;
                switch (c) {
                    case 0:
                        if (node.getOpcode() != DUP)
                            return false;
                        break;
                    case 1:
                        if (BytecodeUtils.getIntValue(node) == null)
                            return false;
                        break;
                    case 2:
                        if (node instanceof FieldInsnNode) {
                            FieldInsnNode field = (FieldInsnNode) node;
                            if (!field.desc.equals("Ljava/lang/Class;") || !field.name.equals("TYPE") || !primitiveClasses.containsKey(field.owner))
                                return false;
                        } else if (node instanceof LdcInsnNode) {
                            if (!(((LdcInsnNode) node).cst instanceof Type))
                                return false;
                        } else {
                            return false;
                        }
                        break;
                    case 3:
                        if (node.getOpcode() != AASTORE)
                            return false;
                        counter = 0;
                }
            }
        }
        return counter == 0;
    }
}
