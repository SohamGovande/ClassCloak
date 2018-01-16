package me.matrix4f.classcloak.action.reflection;

import me.matrix4f.classcloak.util.interpreter.StackBranchInterpreter;
import me.matrix4f.classcloak.util.interpreter.StackInterpreter;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import me.matrix4f.classcloak.Action;
import me.matrix4f.classcloak.action.ObfGlobal;
import me.matrix4f.classcloak.action.name.creation.ClassNameCreator;
import me.matrix4f.classcloak.action.name.map.ClassReference;
import me.matrix4f.classcloak.action.name.map.FieldReference;
import me.matrix4f.classcloak.action.name.map.MethodReference;
import me.matrix4f.classcloak.action.name.map.NameObfMap;
import me.matrix4f.classcloak.action.opaquepredicates.NodeOpaquePred;
import me.matrix4f.classcloak.util.BytecodeUtils;
import me.matrix4f.classcloak.util.MethodBuilder;

import java.util.*;
import java.util.stream.Collectors;

import static me.matrix4f.classcloak.util.BytecodeUtils.toList;
import static me.matrix4f.classcloak.util.InsnCloneFactory.cloneList;
import static org.objectweb.asm.Opcodes.*;
import static me.matrix4f.classcloak.Globals.LOGGER;
import static me.matrix4f.classcloak.action.ObfGlobal.sourceClasses;
import static me.matrix4f.classcloak.action.ObfGlobal.reflectionSettings;

/**
 * Maps certain things into a class and retrieves those values at runtime
 */
//todo
public class ReflectionVerifyAction extends Action {

//    private static final boolean USE_HASH = false;

    private static String hashMethodName, hashMethodDesc,
            unmapMethodNameName, unmapMethodNameDesc,
            unmapMethodDescName, unmapMethodDescDesc,
            unmapFieldName, unmapFieldDesc,
            unmapClassName, unmapClassDesc,
            descBuilderName, descBuilderDesc,
            unmapFieldNameBackwardName, unmapFieldNameBackwardDesc,
            unmapMethodNameBackwardName, unmapMethodNameBackwardDesc,
            unmapClassNameBackwardName, unmapClassNameBackwardDesc;

    @Override
    public void execute() {
        LOGGER.info("Running smart reflection...");
        ClassNode reflectionClass = generateClass();

        for (ReflectionEntry entry : reflectionSettings.entries)
            for (ClassNode clazz : sourceClasses)
                for (MethodNode method : clazz.methods)
                    if (entry.getFrom().stream().anyMatch(node -> node.doesExcludeNode(method, clazz)))
                        try {
                            performEdits(reflectionClass, clazz, entry.getMethodMap(), method);
                        } catch (Exception e) {
                            e.printStackTrace(System.out);
                        }

        ObfGlobal.sourceClasses.add(reflectionClass);
    }

    private void performClassGetNameChanges(ClassNode reflectionClass, ClassNode parent, ReflectionMethodMap map, MethodNode context) {
        List<MethodInsnNode> invokers = BytecodeUtils.getInvokers(context.instructions, "java/lang/reflect/Class.getName()Ljava/lang/String;");
        for (MethodInsnNode invoker : invokers) {
            context.instructions.insert(invoker, new MethodInsnNode(INVOKESTATIC, reflectionClass.name, unmapClassNameBackwardName, unmapClassNameBackwardDesc, false));
            context.instructions.remove(invoker);
        }
    }

    private void performMethodGetNameChanges(ClassNode reflectionClass, ClassNode parent, ReflectionMethodMap map, MethodNode context) {
        List<MethodInsnNode> invokers = BytecodeUtils.getInvokers(context.instructions, "java/lang/reflect/Method.getName()Ljava/lang/String;");
        for (MethodInsnNode invoker : invokers) {
            context.instructions.insert(invoker, new MethodInsnNode(INVOKESTATIC, reflectionClass.name, unmapMethodNameBackwardName, unmapMethodNameBackwardDesc, false));
            context.instructions.remove(invoker);
        }
    }

    private void performFieldGetNameChanges(ClassNode reflectionClass, ClassNode parent, ReflectionMethodMap map, MethodNode context) {
        List<MethodInsnNode> invokers = BytecodeUtils.getInvokers(context.instructions, "java/lang/reflect/Field.getName()Ljava/lang/String;");
        for (MethodInsnNode invoker : invokers) {
            context.instructions.insert(invoker, new MethodInsnNode(INVOKESTATIC, reflectionClass.name, unmapFieldNameBackwardName, unmapFieldNameBackwardDesc, false));
            context.instructions.remove(invoker);
        }
    }

    private void performClassForNameChanges(ClassNode reflectionClass, ClassNode parent, ReflectionMethodMap map, MethodNode context) {
        BytecodeUtils.getInvokers(context.instructions, "java/lang/Class.forName(Ljava/lang/String;)Ljava/lang/Class;")
                .forEach(node ->
                        context.instructions.insertBefore(node, new MethodInsnNode(INVOKESTATIC, reflectionClass.name, unmapClassName, unmapClassDesc, false))
                );
    }

    private void performClassGetMethodOrDeclaredMethodChanges(ClassNode reflectionClass, ClassNode parent, ReflectionMethodMap map, MethodNode context) {
        List<MethodInsnNode> declaredMethodCalls = new LinkedList<>();
        if (map.get(ReflectionMethodMap.CLASS_GETDECLAREDMETHOD))
            declaredMethodCalls.addAll(BytecodeUtils.getInvokers(context.instructions, "java/lang/Class.getDeclaredMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;"));
        if (map.get(ReflectionMethodMap.CLASS_GETMETHOD))
            declaredMethodCalls.addAll(BytecodeUtils.getInvokers(context.instructions, "java/lang/Class.getMethod(Ljava/l   ang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;"));

        if (declaredMethodCalls.size() == 0)
            return;

        StackInterpreter methodInterpreter = new StackInterpreter(parent.name, context);
        methodInterpreter.interpret();

        for (MethodInsnNode invoker : declaredMethodCalls) {
            List<StackBranchInterpreter> interpreters = methodInterpreter.allBranchesContaining(invoker).collect(Collectors.toList());
            for (StackBranchInterpreter interpreter : interpreters) {
                int stackSizePrior = interpreter.getStackSizeAt(invoker);

                LinkedList<AbstractInsnNode> statementInsns = interpreter.observeStackBackward(invoker, stackSizePrior);

                LinkedList<AbstractInsnNode> createClassInsns = interpreter.observeStackForward(statementInsns.getFirst(), statementInsns.getLast(), stackSizePrior);
                LinkedList<AbstractInsnNode> createStringInsns = interpreter.observeStackForward(statementInsns.getFirst(), statementInsns.getLast(), stackSizePrior + 1);
                LinkedList<AbstractInsnNode> createArrayInsns = interpreter.observeStackForward(statementInsns.getFirst(), statementInsns.getLast(), stackSizePrior + 2);

                createArrayInsns.removeAll(createStringInsns);
                createStringInsns.removeAll(createClassInsns);

                InsnList insns = context.instructions;

                InsnList inject = new InsnList();
                inject.add(toList(cloneList(createClassInsns)));
                inject.add(toList(cloneList(createStringInsns)));
//                    if(USE_HASH)
//                        inject.add(new MethodInsnNode(INVOKESTATIC, reflectionClass.name, hashMethodName, hashMethodDesc, false));
                if(!Optimization.isBasicArray(createArrayInsns)) {
                    inject.add(toList(cloneList(createArrayInsns)));
                    inject.add(new MethodInsnNode(INVOKESTATIC, reflectionClass.name, descBuilderName, descBuilderDesc, false));
                } else {
                    inject.add(new LdcInsnNode(Optimization.classArrayToDescriptor(createArrayInsns)));
                }
                inject.add(new MethodInsnNode(INVOKESTATIC, reflectionClass.name, unmapMethodDescName, unmapMethodDescDesc, false));
                insns.insert(createStringInsns.getFirst(), inject);
                createStringInsns.forEach(insns::remove);
            }
        }
    }

    private void performClassGetFieldChanges(ClassNode reflectionClass, ClassNode parent, ReflectionMethodMap map, MethodNode context) {
        List<MethodInsnNode> invokers = new LinkedList<>();
        if (map.get(ReflectionMethodMap.CLASS_GETFIELD))
            invokers.addAll(BytecodeUtils.getInvokers(context.instructions, "java/lang/Class.getField(Ljava/lang/String;)Ljava/lang/reflect/Field;"));
        if (map.get(ReflectionMethodMap.CLASS_GETDECLAREDFIELD))
            invokers.addAll(BytecodeUtils.getInvokers(context.instructions, "java/lang/Class.getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;"));

        if (invokers.size() == 0)
            return;

        StackInterpreter methodInterpreter = new StackInterpreter(parent.name, context);
        methodInterpreter.interpret();

        for (MethodInsnNode invoker : invokers) {
            List<StackBranchInterpreter> validBranches = methodInterpreter.allBranchesContaining(invoker).collect(Collectors.toList());
            for (StackBranchInterpreter interpreter : validBranches) {
                int stackSizePrior = interpreter.getStackSizeAt(invoker);
                LinkedList<AbstractInsnNode> statementInsns = interpreter.observeStackBackward(invoker, stackSizePrior);

                LinkedList<AbstractInsnNode> createClassInsns = interpreter.observeStackForward(statementInsns.getFirst(), statementInsns.getLast(), stackSizePrior);
                LinkedList<AbstractInsnNode> createStringInsns = interpreter.observeStackForward(statementInsns.getFirst(), statementInsns.getLast(), stackSizePrior + 1);
                createClassInsns.forEach(createStringInsns::remove);

                InsnList insns = context.instructions;

                InsnList inject = new InsnList();
                inject.add(toList(cloneList(createClassInsns)));
                inject.add(toList(cloneList(createStringInsns)));
                inject.add(new MethodInsnNode(INVOKESTATIC, reflectionClass.name, unmapFieldName, unmapFieldDesc, false));

                insns.insertBefore(createStringInsns.getFirst(), inject);
                createStringInsns.forEach(insns::remove);
            }
        }
    }

    private void performEdits(ClassNode reflectionClass, ClassNode parent, ReflectionMethodMap map, MethodNode context) {
        if (map.get(ReflectionMethodMap.CLASS_FORNAME))
            performClassForNameChanges(reflectionClass, parent, map, context);

        if (map.get(ReflectionMethodMap.CLASS_GETDECLAREDMETHOD) || map.get(ReflectionMethodMap.CLASS_GETMETHOD))
            performClassGetMethodOrDeclaredMethodChanges(reflectionClass, parent, map, context);

        if (map.get(ReflectionMethodMap.CLASS_GETFIELD) || map.get(ReflectionMethodMap.CLASS_GETDECLAREDFIELD))
            performClassGetFieldChanges(reflectionClass, parent, map, context);

        if (map.get(ReflectionMethodMap.FIELD_GETNAME))
            performFieldGetNameChanges(reflectionClass, parent, map, context);

        if (map.get(ReflectionMethodMap.METHOD_GETNAME))
            performMethodGetNameChanges(reflectionClass, parent, map, context);

        if (map.get(ReflectionMethodMap.CLASS_GETNAME))
            performClassGetNameChanges(reflectionClass, parent, map, context);
    }

    private void generateMethodAccessor(ClassVisitor cw, String methodName, String methodDesc, String className, String mapName, String mapDesc, String opqPredName, String opqPredDesc) {
        MethodBuilder mw = MethodBuilder.newBuilder();

        LabelNode noKey = new LabelNode();
        LabelNode forLoopCheck = new LabelNode();
        LabelNode continueBranch = new LabelNode();
        LabelNode firstLbl = new LabelNode(), lastLbl = new LabelNode();

        mw.label(firstLbl)
                .getstatic(className, mapName, mapDesc)
                .aload(0)
                .aconst_null()
                .invokeinterface("java/util/Map", "getOrDefault", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                .astore(3)

                .aload(3)
                .ifnull(noKey)

                .aload(3)
                .checkcast("java/util/Map")
                .invokeinterface("java/util/Map", "entrySet", "()Ljava/util/Set;")
                .invokeinterface("java/util/Set", "toArray", "()[Ljava/lang/Object;")
                .astore(4)

                .iconst_0()
                .istore(5)

                .label(forLoopCheck)
                .iload(5)
                .aload(4)
                .arraylength()
                .icmpGEQUAL(noKey)

                .aload(4)
                .iload(5)
                .aaload()
                .checkcast("java/util/Map$Entry")
                .invokeinterface("java/util/Map$Entry", "getKey", "()Ljava/lang/Object;")
                .checkcast("[Ljava/lang/String;")
                .astore(6)

                .aload(6)
                .iconst_0()
                .aaload()
                .aload(1)
                .invokevirtual("java/lang/String", "equals", "(Ljava/lang/Object;)Z")
                .ifeq(continueBranch)

                .aload(6)
                .iconst_1()
                .aaload()
                .aload(2)
                .invokevirtual("java/lang/String", "equals", "(Ljava/lang/Object;)Z")
                .ifeq(continueBranch)

                .aload(4)
                .iload(5)
                .aaload()
                .checkcast("java/util/Map$Entry")
                .invokeinterface("java/util/Map$Entry", "getValue", "()Ljava/lang/Object;")
                .checkcast("[Ljava/lang/String;")
                .iconst_0()
                .aaload()
                .areturn()

                .label(continueBranch)
                .iinc(5, 1)
                .goto_(forLoopCheck)

                .label(noKey)
                .aload(1)
                .label(lastLbl)
                .areturn()

                .localVar("", "L;", null, firstLbl, lastLbl, 0)
                .localVar("", "L;", null, firstLbl, lastLbl, 1)
                .localVar("", "L;", null, firstLbl, lastLbl, 2)
                .localVar("", "L;", null, firstLbl, lastLbl, 3)
                .localVar("", "L;", null, firstLbl, lastLbl, 4)
                .localVar("", "L;", null, firstLbl, lastLbl, 5)

                .writeMethod(cw, ACC_PUBLIC + ACC_STATIC, methodName, methodDesc, null, null);
    }

    private String encrypt(String in) {
        return /*USE_HASH ? StringUtils.sha256(in) :*/ in;
    }

    private void generateClinit(ClassVisitor cw,
                                String className,
                                String fieldMapName, String fieldMapDesc,
                                String methodDescMapName, String methodDescMapDesc,
                                String methodMapName, String methodMapDesc,
                                String classMapName, String classMapDesc,
                                String opqPredName, String opqPredDesc) {
        MethodBuilder mw = MethodBuilder.newBuilder().label(0)
                //instantiate field map
                .new_("java/util/HashMap")
                .dup()
                .invokespecial("java/util/HashMap")
                .putstatic(className, fieldMapName, fieldMapDesc)
                //instantiate class map
                .new_("java/util/HashMap")
                .dup()
                .invokespecial("java/util/HashMap")
                .putstatic(className, classMapName, classMapDesc)
                //instantiate method map
                .new_("java/util/HashMap")
                .dup()
                .invokespecial("java/util/HashMap")
                .putstatic(className, methodMapName, methodMapDesc)
                //instantiate method desc map
                .new_("java/util/HashMap")
                .dup()
                .invokespecial("java/util/HashMap")
                .putstatic(className, methodDescMapName, methodDescMapDesc);

        for (Map.Entry<ClassNode, ClassReference> entry : NameObfMap.Classes.entrySet()) {
            //store in class map
            if (reflectionSettings.shouldInclude(entry.getKey()) &&
                    !entry.getValue().oldName.equals(entry.getValue().newName)) {
                mw.getstatic(className, classMapName, classMapDesc)
                        .ldc(encrypt(entry.getValue().oldName.replace('/', '.')))
                        .ldc(entry.getValue().newName.replace('/', '.'))
                        .invokevirtual("java/util/HashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                        .pop();
            }

            ClassNode cn = entry.getKey();
            ClassReference cr = entry.getValue();

            List<Map.Entry<FieldNode, FieldReference>> fields = NameObfMap.Fields.entrySet()
                    .stream()
                    .filter(e -> e.getValue().parent == cr)
                    .filter(e -> !e.getValue().oldName.equals(e.getValue().newName))
                    .filter(e -> reflectionSettings.shouldInclude(e.getKey(), entry.getKey()))
                    .collect(Collectors.toList());

            List<Map.Entry<MethodNode, MethodReference>> methods = NameObfMap.Methods.entrySet()
                    .stream()
                    .filter(e -> e.getValue().parent == cr)
                    .filter(e -> !e.getValue().oldName.equals(e.getValue().newName))
                    .filter(e -> reflectionSettings.shouldInclude(e.getKey(), entry.getKey()))
                    .collect(Collectors.toList());

            if (fields.size() > 0) {
                mw.new_("java/util/HashMap")
                        .dup()
                        .invokespecial("java/util/HashMap")
                        .astore(0);
            }

            if (methods.size() > 0) {
                mw.new_("java/util/HashMap")
                        .dup()
                        .invokespecial("java/util/HashMap")
                        .astore(1)

                        .new_("java/util/HashMap")
                        .dup()
                        .invokespecial("java/util/HashMap")
                        .astore(2);
            }

            for (Map.Entry<FieldNode, FieldReference> fieldEntry : fields) {
                //STORE FIELD INFO
                mw.aload(0)
                        .ldc(encrypt(fieldEntry.getValue().oldName))
                        .ldc(fieldEntry.getValue().newName)
                        .invokeinterface("java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                        .pop();
            }
            if (fields.size() > 0) {
                mw.getstatic(className, fieldMapName, fieldMapDesc)
                        .ldc(Type.getType("L" + entry.getValue().newName + ";"))
                        .aload(0)
                        .invokeinterface("java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                        .pop();
            }


            for (Map.Entry<MethodNode, MethodReference> methodEntry : methods) {
                mw.aload(2)
                        .ldc(encrypt(methodEntry.getValue().oldName))
                        .ldc(methodEntry.getValue().newName)
                        .invokeinterface("java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                        .pop();

                //STORE METHOD INFO
                mw.aload(1)
                        //create first new array
                        .iconst_2()
                        .anewarray("java/lang/String")
                        //store method OLD info
                        .dup()
                        .iconst_0()
                        .ldc(encrypt(methodEntry.getValue().oldName))
                        .aastore()
                        .dup()
                        .iconst_1()
                        .ldc(encrypt(methodEntry.getValue().oldDescriptor.substring(0, methodEntry.getValue().oldDescriptor.indexOf(')') + 1)))
                        .aastore()
                        //create first new array of obfuscated stuff
                        .iconst_2()
                        .anewarray("java/lang/String")
                        //store method NEW info
                        .dup()
                        .iconst_0()
                        .ldc(methodEntry.getValue().newName)
                        .aastore()
                        .dup()
                        .iconst_1()
                        .ldc(methodEntry.getValue().newDescriptor.substring(0, methodEntry.getValue().newDescriptor.indexOf(')') + 1))
                        .aastore()
                        .invokeinterface("java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                        .pop();
            }

            if (methods.size() > 0) {
                mw.getstatic(className, methodDescMapName, methodDescMapDesc)
                        .ldc(Type.getType("L" + entry.getValue().newName + ";"))
                        .aload(1)
                        .invokeinterface("java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                        .pop()

                        .getstatic(className, methodMapName, methodMapDesc)
                        .ldc(Type.getType("L" + entry.getValue().newName + ";"))
                        .aload(2)
                        .invokeinterface("java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                        .pop();
            }
        }

        LabelNode l1 = new LabelNode(), l2 = new LabelNode();
        NodeOpaquePred opaquePred = new NodeOpaquePred(false);
        mw.getInstructions().add(opaquePred.generate(3));
        mw.acmpNOTEQUAL(l1)
                .iconst_1()
                .goto_(l2)

                .label(l1)
                .iconst_0();


        mw.label(l2)
                .putstatic(className, opqPredName, opqPredDesc)
                .return_()
                .localVar("", "L;", null, 0, 2, 0)
                .localVar("", "L;", null, 0, 2, 1)
                .localVar("", "L;", null, 0, 2, 2)
                .localVar("", "L;", null, 0, 2, 3)
                .localVar("", "L;", null, 0, 2, 4)

                .writeMethod(cw, ACC_STATIC, "<clinit>", "()V", null, null);
    }

    private void generateHashFunction(ClassWriter cw, String methodName, String methodDesc) {
        LabelNode l0 = new LabelNode();
        LabelNode l1 = new LabelNode();
        LabelNode l2 = new LabelNode();

        MethodBuilder.newBuilder()
                .trycatchblock(l0, l1, l2, "java/security/NoSuchAlgorithmException")
                .trycatchblock(l0, l1, l2, "java/io/UnsupportedEncodingException")

                .label(l0)

                .ldc("SHA-256")
                .invokestatic("java/security/MessageDigest", "getInstance", "(Ljava/lang/String;)Ljava/security/MessageDigest;")

                .aload(0)
                .ldc("UTF-8")
                .invokevirtual("java/lang/String", "getBytes", "(Ljava/lang/String;)[B")
                .invokevirtual("java/security/MessageDigest", "digest", "([B)[B")
                .invokestatic("javax/xml/bind/DatatypeConverter", "printHexBinary", "([B)Ljava/lang/String;")

                .label(l1)
                .areturn()

                .label(l2)
                .invokevirtual("java/lang/Exception", "printStackTrace", "()V")

                .aconst_null()
                .areturn()

                .localVar("", "L;", null, l0, l1, 0)

                .writeMethod(cw, ACC_PUBLIC + ACC_STATIC, methodName, methodDesc, null, null);
    }

    private void generateFieldAccessor(ClassVisitor cw, String methodName, String methodDesc, String className, String mapName, String mapDesc) {
        MethodBuilder mw = MethodBuilder.newBuilder();
        LabelNode beginMethod = new LabelNode();
        LabelNode forLoopEnd = new LabelNode();
        LabelNode returnStatement = new LabelNode();

        mw.label(beginMethod)
                .getstatic(className, mapName, mapDesc)
                .aload(0)
                .aconst_null()
                .invokevirtual("java/util/HashMap", "getOrDefault", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                .astore(2)

                .aload(2)
                .ifnull(forLoopEnd)

                .aload(2)
                .checkcast("java/util/Map")
                .aload(1)
                .invokeinterface("java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;")
                .goto_(returnStatement)

                .label(forLoopEnd)
                .aload(1)

                .label(returnStatement)
                .checkcast("java/lang/String")
                .areturn()

                .localVar("", "L;", null, beginMethod, returnStatement, 0)
                .localVar("", "L;", null, beginMethod, returnStatement, 1)
                .localVar("", "L;", null, beginMethod, returnStatement, 2)

                .writeMethod(cw, ACC_PUBLIC + ACC_STATIC, methodName, methodDesc, null, null);
    }

    private void generateClassAccessor(ClassVisitor cw, String methodName, String methodDesc, String className, String mapName, String mapDesc) {
        LabelNode l0 = new LabelNode();
        LabelNode l1 = new LabelNode();
        LabelNode l2 = new LabelNode();

        MethodBuilder.newBuilder()
                .label(l0)
                .getstatic(className, mapName, mapDesc)
                .aload(0)
                .invokevirtual("java/util/HashMap", "containsKey", "(Ljava/lang/Object;)Z")
                .ifeq(l1)

                .getstatic(className, mapName, mapDesc)
                .aload(0)
                .invokevirtual("java/util/HashMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;")
                .checkcast("java/lang/String")
                .areturn()

                .label(l1)
                .aload(0)
                .areturn()
                .label(l2)

                .localVar("", "L;", null, l0, l2, 0)

                .writeMethod(cw, ACC_PUBLIC + ACC_STATIC, methodName, methodDesc, null, null);
    }

    private void generateDescBuilder(ClassVisitor cw, String methodName, String methodDesc, String opqPredName, String opqPredDesc, String className) {
        LabelNode afterForLoop = new LabelNode();
        LabelNode forLoopCheck = new LabelNode();
        LabelNode whileLoopCheck = new LabelNode();
        LabelNode afterWhileLoop = new LabelNode();
        LabelNode afterIntClass = new LabelNode(),
                afterShortClass = new LabelNode(),
                afterLongClass = new LabelNode(),
                afterBoolClass = new LabelNode(),
                afterByteClass = new LabelNode(),
                afterFloatClass = new LabelNode(),
                afterDoubleClass = new LabelNode(),
                afterCharClass = new LabelNode(),

                afterIsPrimitveCheck = new LabelNode(),
                afterIsArrayCheck = new LabelNode(),
                firstLabel = new LabelNode(),
                lastLabel = new LabelNode();
        MethodBuilder.newBuilder()
                .label(firstLabel)

                .new_("java/lang/StringBuilder")
                .dup()
                .ldc("(")
                .invokespecial("java/lang/StringBuilder", "(Ljava/lang/String;)V")
                .astore(1)

                //i = 0;
                .iconst_0()
                .istore(2)

                //if(i < arraylength)
                .label(forLoopCheck)
                .iload(2)
                .aload(0)
                .arraylength()
                .icmpGEQUAL(afterForLoop)

                //Class class = classes[i]
                .aload(0)
                .iload(2)
                .aaload()
                .astore(3)

                //StringBuilder buf = new StringBuilder();
                .new_("java/lang/StringBuilder")
                .dup()
                .invokespecial("java/lang/StringBuilder")
                .astore(4)

                //while(true)
                .label(whileLoopCheck)
                .iconst_0()
                .ifne(afterWhileLoop)

                .aload(3)
                .invokevirtual("java/lang/Class", "isPrimitive", "()Z")
                .ifeq(afterIsPrimitveCheck)

                //class is primitive

                .aload(3)
                .getstatic("java/lang/Integer", "TYPE", "Ljava/lang/Class;")
                .acmpNOTEQUAL(afterIntClass)
                .aload(4)
                .bipush((byte) 'I')
                .invokevirtual("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;")
                .pop()
                .goto_(afterWhileLoop)
                .label(afterIntClass)

                .aload(3)
                .getstatic("java/lang/Character", "TYPE", "Ljava/lang/Class;")
                .acmpNOTEQUAL(afterCharClass)
                .aload(4)
                .bipush((byte) 'C')
                .invokevirtual("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;")
                .pop()
                .goto_(afterWhileLoop)
                .label(afterCharClass)

                .aload(3)
                .getstatic("java/lang/Double", "TYPE", "Ljava/lang/Class;")
                .acmpNOTEQUAL(afterDoubleClass)
                .aload(4)
                .bipush((byte) 'D')
                .invokevirtual("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;")
                .pop()
                .goto_(afterWhileLoop)
                .label(afterDoubleClass)

                .aload(3)
                .getstatic("java/lang/Float", "TYPE", "Ljava/lang/Class;")
                .acmpNOTEQUAL(afterFloatClass)
                .aload(4)
                .bipush((byte) 'F')
                .invokevirtual("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;")
                .pop()
                .goto_(afterWhileLoop)
                .label(afterFloatClass)

                .aload(3)
                .getstatic("java/lang/Long", "TYPE", "Ljava/lang/Class;")
                .acmpNOTEQUAL(afterLongClass)
                .aload(4)
                .bipush((byte) 'J')
                .invokevirtual("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;")
                .pop()
                .goto_(afterWhileLoop)
                .label(afterLongClass)

                .aload(3)
                .getstatic("java/lang/Boolean", "TYPE", "Ljava/lang/Class;")
                .acmpNOTEQUAL(afterBoolClass)
                .aload(4)
                .bipush((byte) 'Z')
                .invokevirtual("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;")
                .pop()

//                .getstatic(className, opqPredName, opqPredDesc)
//                .ifne(lastLabel)

                .goto_(afterWhileLoop)
                .label(afterBoolClass)

                .aload(3)
                .getstatic("java/lang/Short", "TYPE", "Ljava/lang/Class;")
                .acmpNOTEQUAL(afterShortClass)
                .aload(4)
                .bipush((byte) 'S')
                .invokevirtual("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;")
                .pop()
                .goto_(afterWhileLoop)
                .label(afterShortClass)

//                .aload(3)
//                .getstatic("java/lang/Byte","TYPE","Ljava/lang/Class;")
//                .acmpNOTEQUAL(afterByteClass)
                .aload(4)
                .bipush((byte) 'B')
                .invokevirtual("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;")
                .pop()
                .goto_(afterWhileLoop)
//                .label(afterByteClass)

//                .goto_(afterWhileLoop)

                .label(afterIsPrimitveCheck)
                .aload(3)
                .invokevirtual("java/lang/Class", "isArray", "()Z")
                .ifne(afterIsArrayCheck)

                //class is a regular class
                //buf.append("L").append(clas.getName().replace('.','/')).append(";");
                .aload(4)
                .bipush((byte) 76) //L
                .invokevirtual("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;")

                .aload(3)
                .invokevirtual("java/lang/Class", "getName", "()Ljava/lang/String;")
                .bipush((byte) 46) // '.'
                .bipush((byte) 47) // '/'
                .invokevirtual("java/lang/String", "replace", "(CC)Ljava/lang/String;")
                .invokevirtual("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;")

                .bipush((byte) 59)
                .invokevirtual("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;")
                .pop()


                .goto_(afterWhileLoop)

                .label(afterIsArrayCheck)
                //class is an array

                //clas = clas.getComponentType();
                .aload(3)
                .invokevirtual("java/lang/Class", "getComponentType", "()Ljava/lang/Class;")
                .astore(3)

                //buf.append("[");
                .aload(4)
                .bipush((byte) 91) // [
                .invokevirtual("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;")
                .pop()

                .goto_(whileLoopCheck)

                .label(afterWhileLoop)

                .getstatic(className, opqPredName, opqPredDesc)
                .ifne(firstLabel)

                .aload(1)
                .aload(4)
                .invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;")
                .invokevirtual("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;")
                .pop()

                .iinc(2, 1)

                .getstatic(className, opqPredName, opqPredDesc)
                .ifne(afterIsArrayCheck)

                .goto_(forLoopCheck)

                .label(afterForLoop)

                .aload(1)
                .ldc(")")
                .invokevirtual("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;")
                .invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;")
                .label(lastLabel)
                .areturn()

                .localVar("", "L;", null, firstLabel, lastLabel, 0)
                .localVar("", "L;", null, firstLabel, lastLabel, 1)
                .localVar("", "L;", null, firstLabel, lastLabel, 2)
                .localVar("", "L;", null, firstLabel, lastLabel, 3)
                .localVar("", "L;", null, firstLabel, lastLabel, 4)
                .localVar("", "L;", null, firstLabel, lastLabel, 5)

                .writeMethod(cw, ACC_PUBLIC + ACC_STATIC, methodName, methodDesc, null, null);
        Type.getDescriptor(int[].class);
    }

    private void generateFieldNameBackwardAccessor(ClassVisitor cw, String methodName, String methodDesc, String className, String mapName, String mapDesc) {
        MethodBuilder mw = MethodBuilder.newBuilder();
        LabelNode beginMethod = new LabelNode();
        LabelNode doesContainKey = new LabelNode();
        LabelNode returnStatement = new LabelNode();
        LabelNode forLoopCheck = new LabelNode(), afterForLoop = new LabelNode(), continueBranch = new LabelNode();

        mw.label(beginMethod)
                .aload(0)
                .invokevirtual("java/lang/reflect/Field", "getDeclaringClass", "()Ljava/lang/Class;")
                .astore(1)

                .getstatic(className, mapName, mapDesc)
                .aload(1)
                .aconst_null()

                .invokeinterface("java/util/Map", "getOrDefault", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                .astore(2)
                .aload(2)
                .ifnonnnull(doesContainKey)

                .aload(0)
                .invokevirtual("java/lang/reflect/Field", "getName", "()Ljava/lang/String;")
                .areturn()

                .label(doesContainKey)
                .aload(2)
                .checkcast("java/util/Map")
                .invokeinterface("java/util/Map", "entrySet", "()Ljava/util/Set;")
                .invokeinterface("java/util/Set", "toArray", "()[Ljava/lang/Object;")
                .astore(3)

                .iconst_0()
                .istore(4)

                .label(forLoopCheck)
                .iload(4)
                .aload(3)
                .arraylength()
                .icmpGEQUAL(afterForLoop)

                .aload(3)
                .iload(4)
                .aaload()
                .checkcast("java/util/Map$Entry")
                .astore(5)

                .aload(5)
                .invokeinterface("java/util/Map$Entry", "getValue", "()Ljava/lang/Object;")
                .checkcast("java/lang/String")
                .aload(0)
                .invokevirtual("java/lang/reflect/Field", "getName", "()Ljava/lang/String;")
                .invokevirtual("java/lang/String", "equals", "(Ljava/lang/Object;)Z")
                .ifeq(continueBranch)

                .aload(5)
                .invokeinterface("java/util/Map$Entry", "getKey", "()Ljava/lang/Object;")
                .checkcast("java/lang/String")
                .areturn()

                .label(continueBranch)
                .iinc(4, 1)
                .goto_(forLoopCheck)
                .label(afterForLoop)

                .aload(0)
                .invokevirtual("java/lang/reflect/Field", "getName", "()Ljava/lang/String;")

                .label(returnStatement)
                .areturn()

                .localVar("", "L;", null, beginMethod, returnStatement, 0)
                .localVar("", "L;", null, beginMethod, returnStatement, 1)
                .localVar("", "L;", null, beginMethod, returnStatement, 2)
                .localVar("", "L;", null, beginMethod, returnStatement, 3)
                .localVar("", "L;", null, beginMethod, returnStatement, 4)
                .localVar("", "L;", null, beginMethod, returnStatement, 5)

                .writeMethod(cw, ACC_PUBLIC + ACC_STATIC, methodName, methodDesc, null, null);
    }

    private void generateMethodNameBackwardAccessor(ClassVisitor cw, String methodName, String methodDesc, String className, String mapName, String mapDesc) {
        MethodBuilder mw = MethodBuilder.newBuilder();
        LabelNode beginMethod = new LabelNode();
        LabelNode doesContainKey = new LabelNode();
        LabelNode returnStatement = new LabelNode();
        LabelNode forLoopCheck = new LabelNode(), afterForLoop = new LabelNode(), continueBranch = new LabelNode();

        mw.label(beginMethod)
                .aload(0)
                .invokevirtual("java/lang/reflect/Method", "getDeclaringClass", "()Ljava/lang/Class;")
                .astore(1)

                .getstatic(className, mapName, mapDesc)
                .aload(1)
                .aconst_null()

                .invokeinterface("java/util/Map", "getOrDefault", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                .astore(2)
                .aload(2)
                .ifnonnnull(doesContainKey)

                .aload(0)
                .invokevirtual("java/lang/reflect/Method", "getName", "()Ljava/lang/String;")
                .areturn()

                .label(doesContainKey)

                .aload(2)
                .checkcast("java/util/Map")
                .invokeinterface("java/util/Map", "entrySet", "()Ljava/util/Set;")
                .invokeinterface("java/util/Set", "toArray", "()[Ljava/lang/Object;")
                .astore(3)

                .aload(0)
                .invokevirtual("java/lang/reflect/Method", "getParameterTypes", "()[Ljava/lang/Class;")
                .invokestatic(className, descBuilderName, descBuilderDesc)
                .astore(6)

                .iconst_0()
                .istore(4)

                .label(forLoopCheck)
                .iload(4)
                .aload(3)
                .arraylength()
                .icmpGEQUAL(afterForLoop)

                .aload(3)
                .iload(4)
                .aaload()
                .checkcast("java/util/Map$Entry")
                .astore(5)

                .aload(5)
                .invokeinterface("java/util/Map$Entry", "getValue", "()Ljava/lang/Object;")
                .checkcast("[Ljava/lang/String;")
                .iconst_0()
                .aaload()
                .aload(0)
                .invokevirtual("java/lang/reflect/Method", "getName", "()Ljava/lang/String;")
                .invokevirtual("java/lang/String", "equals", "(Ljava/lang/Object;)Z")
                .ifeq(continueBranch)

                .aload(5)
                .invokeinterface("java/util/Map$Entry", "getValue", "()Ljava/lang/Object;")
                .checkcast("[Ljava/lang/String;")
                .iconst_1()
                .aaload()
                .aload(6)
                .invokevirtual("java/lang/String", "equals", "(Ljava/lang/Object;)Z")
                .ifeq(continueBranch)

                .aload(5)
                .invokeinterface("java/util/Map$Entry", "getKey", "()Ljava/lang/Object;")
                .checkcast("[Ljava/lang/String;")
                .iconst_0()
                .aaload()
                .areturn()

                .label(continueBranch)
                .iinc(4, 1)
                .goto_(forLoopCheck)
                .label(afterForLoop)

                .aload(0)
                .invokevirtual("java/lang/reflect/Method", "getName", "()Ljava/lang/String;")

                .label(returnStatement)
                .areturn()

//                .localVar("","L;",null,beginMethod, returnStatement, 0)
//                .localVar("","L;",null,beginMethod, returnStatement, 1)
//                .localVar("","L;",null,beginMethod, returnStatement, 2)
//                .localVar("","L;",null,beginMethod, returnStatement, 3)
//                .localVar("","L;",null,beginMethod, returnStatement, 4)
//                .localVar("","L;",null,beginMethod, returnStatement, 5)

                .writeMethod(cw, ACC_PUBLIC + ACC_STATIC, methodName, methodDesc, null, null);
    }

    private void generateClassNameBackwardAccessor(ClassVisitor cw, String methodName, String methodDesc, String className, String mapName, String mapDesc) {
        MethodBuilder.newBuilder()
                .getstatic(className, mapName, mapDesc)

                .aload(0)
                .invokevirtual("java/lang/Class", "getName", "()Ljava/lang/String;")
                .aload(0)
                .invokevirtual("java/lang/Class", "getName", "()Ljava/lang/String;")
                .invokeinterface("java/util/Map", "getOrDefault", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                .checkcast("java/lang/String")

                .areturn()
                .writeMethod(cw, ACC_PUBLIC + ACC_STATIC, methodName, methodDesc, null, null);
    }

    private ClassNode generateClass() {
        String className = ClassNameCreator.instance.getName(null);
        ClassNode cw = new ClassNode();

        cw.visit(52, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", null);
        cw.visitSource("maps.java", null);

        String fieldMapName = "a",
                fieldMapDesc = "Ljava/util/HashMap;";
        String classMapName = "b",
                classMapDesc = "Ljava/util/HashMap;";
        String methodDescMapName = "c",
                methodDescMapDesc = "Ljava/util/HashMap;";
        String methodMapName = "d",
                methodMapDesc = "Ljava/util/HashMap;";
        String opqPredName = "e",
                opqPredDesc = "Z";

        //field desc map
//        cw.visitField(ACC_PRIVATE + ACC_STATIC, fieldDescMapName, fieldDescMapDesc, genericFieldDescMapDesc, null).visitEnd();
        //field map
        cw.visitField(ACC_PRIVATE + ACC_STATIC, fieldMapName, fieldMapDesc, null, null).visitEnd();
        //class map
        cw.visitField(ACC_PRIVATE + ACC_STATIC, classMapName, classMapDesc, null, null).visitEnd();
        //method desc map
        cw.visitField(ACC_PRIVATE + ACC_STATIC, methodDescMapName, methodDescMapDesc, null, null).visitEnd();
        //method map
        cw.visitField(ACC_PRIVATE + ACC_STATIC, methodMapName, methodMapDesc, null, null).visitEnd();
        //opaque predicate - always false
        cw.visitField(ACC_PRIVATE + ACC_STATIC, opqPredName, opqPredDesc, null, null).visitEnd();

        MethodBuilder.newEmptyConstructorExtendingObject(className, cw);

        generateClinit(cw, className, fieldMapName, fieldMapDesc, methodDescMapName, methodDescMapDesc, methodMapName, methodMapDesc, classMapName, classMapDesc, opqPredName, opqPredDesc);

        generateFieldAccessor(cw, unmapFieldName = "discordapp", unmapFieldDesc = "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/String;", className, fieldMapName, fieldMapDesc);
        generateMethodAccessor(cw, unmapMethodDescName = "dubmo", unmapMethodDescDesc = "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", className, methodDescMapName, methodDescMapDesc, opqPredName, opqPredDesc);
        generateClassAccessor(cw, unmapClassName = "princess", unmapClassDesc = "(Ljava/lang/String;)Ljava/lang/String;", className, classMapName, classMapDesc);

        generateDescBuilder(cw, descBuilderName = "frog", descBuilderDesc = "([Ljava/lang/Class;)Ljava/lang/String;", opqPredName, opqPredDesc, className);

        generateFieldNameBackwardAccessor(cw, unmapFieldNameBackwardName = "unmapFieldnameBackward", unmapFieldNameBackwardDesc = "(Ljava/lang/reflect/Field;)Ljava/lang/String;", className, fieldMapName, fieldMapDesc);
        generateMethodNameBackwardAccessor(cw, unmapMethodNameBackwardName = "unmapMethodnameBackward", unmapMethodNameBackwardDesc = "(Ljava/lang/reflect/Method;)Ljava/lang/String;", className, methodDescMapName, methodDescMapDesc);
        generateClassNameBackwardAccessor(cw, unmapClassNameBackwardName = "unmapClassnameBakward", unmapClassNameBackwardDesc = "(Ljava/lang/Class;)Ljava/lang/String;", className, classMapName, classMapDesc);

//        generateFieldAccessor(cw, unmapMethodNameName = "b", unmapMethodNameDesc = "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/String;", className, methodMapName, methodMapDesc, opqPredName, opqPredDesc);
//        generateHashFunction(cw, hashMethodName = "a", hashMethodDesc = "(Ljava/lang/String;)Ljava/lang/String;");

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.accept(writer);

        ClassNode result = new ClassNode();
        new ClassReader(writer.toByteArray()).accept(result, 0);
        return result;
    }

}
