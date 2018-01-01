package me.matrix4f.classcloak.util;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import sun.management.snmp.jvmmib.JvmMemMgrPoolRelTableMeta;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.*;

public class BytecodeUtils {

    public static HashMap<Character, String> primitivesNameMap = new HashMap<>();
    public static HashMap<Integer, String> newarrayTypeMap = new HashMap<>();
    public static final int[] opcodesIf = {
            IFEQ, IFNE,
            IFLT, IFGE, IFGT, IFLE,
            IF_ICMPEQ, IF_ICMPNE,
            IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE,
            IF_ACMPEQ, IF_ACMPNE,
            IFNULL, IFNONNULL
    };
    public static final int[] opcodesReturn = {
            RETURN, ARETURN, IRETURN, LRETURN, DRETURN, FRETURN
    };


    static {
        primitivesNameMap.put('I',"int");
        primitivesNameMap.put('F',"float");
        primitivesNameMap.put('B',"byte");
        primitivesNameMap.put('C',"char");
        primitivesNameMap.put('D',"double");
        primitivesNameMap.put('Z',"boolean");
        primitivesNameMap.put('J',"long");
        primitivesNameMap.put('S',"short");

        newarrayTypeMap.put(T_INT,      "I");
        newarrayTypeMap.put(T_CHAR,     "C");
        newarrayTypeMap.put(T_BOOLEAN,  "Z");
        newarrayTypeMap.put(T_BYTE,     "B");
        newarrayTypeMap.put(T_LONG,     "J");
        newarrayTypeMap.put(T_DOUBLE,   "D");
        newarrayTypeMap.put(T_SHORT,    "S");
        newarrayTypeMap.put(T_FLOAT,    "F");
    }
    public static HashMap<String, Character> getNamePrimitivesMap() {
        HashMap<String,Character> map = new HashMap<>();
        primitivesNameMap.forEach((character, s) -> map.put(s,character));
        return map;
    }

    public static List<LabelNode> getLabelsInList(AbstractInsnNode node) {
        AbstractInsnNode counter = node;
        while(counter.getPrevious() != null)
            counter = counter.getPrevious();

        List<LabelNode> list = new ArrayList<>();

        if(counter instanceof LabelNode)
            list.add((LabelNode) counter);

        while(counter.getNext() != null) {
            counter = counter.getNext();
            if(counter instanceof LabelNode)
                list.add((LabelNode) counter);
        }

        return list;
    }

    public static List<AbstractInsnNode> constructList(AbstractInsnNode node) {
        AbstractInsnNode counter = node;
        while(counter.getPrevious() != null)
            counter = counter.getPrevious();

        List<AbstractInsnNode> list = new ArrayList<>();

        list.add(counter);

        while(counter.getNext() != null) {
            counter = counter.getNext();
            list.add(counter);
        }

        return list;
    }

    public static <T extends AbstractInsnNode> Stream<T> streamInstructions(Class<T> clazz, MethodNode method) {
        return Arrays.stream(method.instructions.toArray())
                .filter(node -> node.getClass() == clazz)
                .map(node-> (T) node);
    }

    public static void writeMethodInsns(InsnList insnList, MethodVisitor mv) {
        Arrays.stream(insnList.toArray())
                .forEach(insn -> insn.accept(mv));
    }

    public static String nameToDescNoL(String name) {
        StringBuilder builder = new StringBuilder(name);
        int arrays = 0;
        int arrayIndex;
        while((arrayIndex = builder.indexOf("[]")) != -1) {
            builder.delete(arrayIndex, arrayIndex+2);
            arrays++;
        }
        String noArrays = builder.toString();
        Optional<Character> primitive = primitivesNameMap.entrySet()
                .stream()
                .filter(e -> e.getValue().equalsIgnoreCase(noArrays))
                .map(Map.Entry::getKey)
                .findFirst();
        StringBuilder desc = new StringBuilder();
        for(int i = 0; i < arrays; i++)
            desc.append("[");

        if(primitive.isPresent()) {
            desc.append(primitive.get());
        } else {
            desc.append(noArrays);
        }
        return desc.toString();
    }

    public static String getInternalName(String desc) {
        desc = desc.replace("[","");
        if(desc.length() == 1)
            return null;
        return desc.substring(1,desc.length()-1);
    }

    public static List<String> getInternalNames(String methodDesc) {
        Type[] types = Type.getArgumentTypes(methodDesc);
        Type returnType = Type.getReturnType(methodDesc);
        List<String> internalNames = Stream.of(types)
                .map(Type::getDescriptor)
                .map(BytecodeUtils::getInternalName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        String returnTypeName = getInternalName(returnType.getDescriptor());
        if(returnTypeName != null)
            internalNames.add(returnTypeName);
        return internalNames;
    }

    public static String nameToDesc(String name) {
        StringBuilder builder = new StringBuilder(name);
        int arrays = 0;
        int arrayIndex;
        while((arrayIndex = builder.indexOf("[]")) != -1) {
            builder.delete(arrayIndex, arrayIndex+2);
            arrays++;
        }
        String noArrays = builder.toString().replace('.','/');
        Optional<Character> primitive = primitivesNameMap.entrySet()
                .stream()
                .filter(e -> e.getValue().equalsIgnoreCase(noArrays))
                .map(Map.Entry::getKey)
                .findFirst();
        StringBuilder desc = new StringBuilder();
        for(int i = 0; i < arrays; i++)
            desc.append("[");

        if(primitive.isPresent()) {
            desc.append(primitive.get());
        } else {
            desc.append("L").append(noArrays).append(";");
        }
        return desc.toString();
    }

    public static int dimensionArrayCountInExternal(String externalname) {
        StringBuilder builder = new StringBuilder(externalname);
        int arrays = 0;
        int arrayIndex;
        while((arrayIndex = builder.indexOf("[]")) != -1) {
            builder.delete(arrayIndex, arrayIndex+2);
            arrays++;
        }
        return arrays;
    }

    public static int dimensionArrayCountInInternal(String desc) {
        int arrays = 0;
        while(desc.charAt(0) == '[') {
            desc = desc.substring(1);
            arrays++;
        }
        return arrays;
    }

    public static String realName(ClassNode cn) {
        return cn.name.replace('/','.');
    }

    public static String methodDescToName(String desc) {
        StringBuilder sb = new StringBuilder("(");
        Stream.of(Type.getArgumentTypes(desc))
                .map(Type::getDescriptor)
                .map(BytecodeUtils::descToName)
                .forEach(s -> sb.append(s).append(", "));
        if(sb.toString().endsWith(", ")) {
            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append(')');
        sb.append(descToName(Type.getReturnType(desc).getDescriptor()));
        return sb.toString();
    }

    public static String descToName(String desc) {
        int brackets = 0;
        while(desc.charAt(0) == '[') {
            brackets++;
            desc = desc.substring(1);
        }

        String internalName;
        if(desc.charAt(desc.length()-1) == ';') internalName = desc.substring(1,desc.length()-1);
        else                                    internalName = desc;

        if (internalName.length() == 1) //primitive
            internalName = primitivesNameMap.get(internalName.charAt(0));
        else
            internalName = internalName.replace('/','.');

        for(int i = 0; i < brackets; i++)
            internalName += "[]";

        return internalName;
    }
}
