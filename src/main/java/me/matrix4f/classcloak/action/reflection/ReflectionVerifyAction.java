package me.matrix4f.classcloak.action.reflection;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import me.matrix4f.classcloak.Action;
import me.matrix4f.classcloak.action.ObfGlobal;
import me.matrix4f.classcloak.action.name.namecreation.ClassNameCreator;
import me.matrix4f.classcloak.action.name.nameofbmap.ClassReference;
import me.matrix4f.classcloak.action.name.nameofbmap.FieldReference;
import me.matrix4f.classcloak.action.name.nameofbmap.MethodReference;
import me.matrix4f.classcloak.action.name.nameofbmap.NameObfMap;
import me.matrix4f.classcloak.action.opaquepredicates.NodeOpaquePred;
import me.matrix4f.classcloak.util.BytecodeUtils;
import me.matrix4f.classcloak.util.MethodBuilder;
import me.matrix4f.classcloak.util.StringUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.*;
import static me.matrix4f.classcloak.Globals.LOGGER;
import static me.matrix4f.classcloak.action.ObfGlobal.classes;
import static me.matrix4f.classcloak.action.ObfGlobal.reflectionSettings;

/**
 * Maps certain things into a class and retrieves those values at runtime
 *
 */
//todo
public class ReflectionVerifyAction extends Action {

    private static String hashMethodName, hashMethodDesc,
                        unmapMethodNameName, unmapMethodNameDesc,
                        unmapMethodDescName, unmapMethodDescDesc,
                        unmapFieldName, unmapFieldDesc,
                        unmapClassName, unmapClassDesc;

    @Override
    public void execute() {
        LOGGER.info("Running smart reflection...");
        ClassNode reflectionClass = generateClass();

        for(ReflectionEntry entry : reflectionSettings.entries)
            for(ClassNode clazz : classes)
                for(MethodNode method : clazz.methods)
                    if(entry.getFrom().stream().anyMatch(node -> node.doesExcludeNode(method, clazz)))
                        performEdits(reflectionClass, entry.getMethodMap(), method);

        ObfGlobal.classes.add(reflectionClass);
    }

    private void performEdits(ClassNode reflectionClass, ReflectionMethodMap map, MethodNode context) {
        if(map.get(ReflectionMethodMap.CLASS_FORNAME)) {
            BytecodeUtils.streamInstructions(MethodInsnNode.class, context)
                    .filter(insn -> insn.name.equals("forName") && insn.desc.equals("(Ljava/lang/String;)Ljava/lang/Class;") && insn.owner.equals("java/lang/Class"))
                    .forEach(node -> {
                        context.instructions.insertBefore(node, new MethodInsnNode(
                                INVOKESTATIC, reflectionClass.name, unmapClassName, unmapClassDesc, false
                        ));
                    });
        }
        if(map.get(ReflectionMethodMap.CLASS_GETDECLAREDMETHOD)) {
            List<MethodInsnNode> declaredMethodCalls = BytecodeUtils.streamInstructions(MethodInsnNode.class, context)
                    .filter(insn -> insn.name.equals("getDeclaredMethod") && insn.desc.equals("(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;") && insn.owner.equals("java/lang/Class"))
                    .collect(Collectors.toList());

            if(declaredMethodCalls.size() == 0)
                return;
            List<List<AbstractInsnNode>> between = new ArrayList<>();

            for(MethodInsnNode invoker : declaredMethodCalls) {
                AbstractInsnNode last = invoker;
                List<AbstractInsnNode> list = new ArrayList<>();
                boolean go = true;
                while(go) {
                    last = last.getPrevious();
                    list.add(last);
                    if(last == null)
                        break;
                    if(last instanceof TypeInsnNode) {
                        TypeInsnNode node = (TypeInsnNode) last;
                        if(node.getOpcode() == ANEWARRAY && node.desc.equals("java/lang/Class")) {
                            AbstractInsnNode twoBack = node.getPrevious().getPrevious();
                            int size = 0;
                            if(twoBack instanceof MethodInsnNode) {
                                MethodInsnNode method = (MethodInsnNode) twoBack;
                                if(method.getOpcode() != INVOKESTATIC)
                                    size = Type.getArgumentTypes(method.desc).length + 1;
                                else
                                    size = Type.getArgumentTypes(method.desc).length;
                            } else if(twoBack instanceof LdcInsnNode) {
                                size = 1;
                            } else if(twoBack instanceof VarInsnNode) {
                                VarInsnNode var = (VarInsnNode) twoBack;
                                if(var.getOpcode() == ALOAD || var.getOpcode() == AALOAD)
                                    size = 1;
                            } else if(twoBack instanceof FieldInsnNode) {
                                FieldInsnNode field = (FieldInsnNode) twoBack;
                                if(field.desc.equals("Ljava/lang/String;"))
                                    size = 2;
                            }

                            if(size != 0)
                                go = false;
                        }
                    }
                    Collections.reverse(list);
                }
                between.add(list);
            }

            Map<String,Character> primitiveClasses = new HashMap<>();
            primitiveClasses.put("java/lang/Byte",'B');
            primitiveClasses.put("java/lang/Short",'S');
            primitiveClasses.put("java/lang/Character",'C');
            primitiveClasses.put("java/lang/Integer",'I');
            primitiveClasses.put("java/lang/Long",'J');
            primitiveClasses.put("java/lang/Boolean",'Z');
            primitiveClasses.put("java/lang/Float",'F');
            primitiveClasses.put("java/lang/Double",'D');
            primitiveClasses.put("java/lang/Void",'V');

            for(List<AbstractInsnNode> list : between) {
                boolean first = true;
                String name = "";
                StringBuilder descBuilder = new StringBuilder("(");
                for(AbstractInsnNode node : list) {
                    if(first) { //first node is ldc string
                        name = (String) ((LdcInsnNode) node).cst;
                        first = false;
                        continue;
                    }

                    if(node instanceof LdcInsnNode && ((LdcInsnNode) node).cst instanceof Type) {
                        descBuilder.append(((Type) ((LdcInsnNode) node).cst).getDescriptor());
                    } else if(node instanceof FieldInsnNode) {
                        FieldInsnNode fin = (FieldInsnNode) node;
                        if(fin.name.equals("TYPE") && fin.desc.equals("Ljava/lang/Class;") && primitiveClasses.containsKey(fin.owner)) {
                            descBuilder.append(primitiveClasses.get(fin.owner));
                        }
                    }
                }
                descBuilder.append(')');
                String desc = descBuilder.toString();

            }
        }
    }

    private void generateReflectionObjectAccessorWithDesc(ClassWriter cw, String methodName, String methodDesc, String className, String mapName, String mapDesc, String opqPredName, String opqPredDesc) {
        MethodBuilder mw = MethodBuilder.newBuilder();
        LabelNode l1 = new LabelNode();
        LabelNode l2 = new LabelNode();

        final int labelLast = 5;
        LabelNode lblLastPointer = new LabelNode();

        mw.label(0)
                //opaque predicate
//                .getstatic(className, opqPredName, opqPredDesc)
//                .ifne(l2)

                //if it isn't in the class name hash map then skip code
                .getstatic(className, mapName, mapDesc)
                .aload(0)
                .invokevirtual("java/util/HashMap","containsKey","(Ljava/lang/Object;)Z")
                .ifeq(lblLastPointer)

                //int i = -1;
                .label(1)
                .iconst_m1()
                .istore(3)

                .getstatic(className, mapName, mapDesc)
                .invokevirtual("java/util/HashMap","entrySet","()Ljava/util/Set;")
                .invokevirtual("java/util/Set","toArray","()[Ljava/lang/Object;")
                .astore(4)

                //i++
                .label(2)
                .iload(3)
                .iconst_1()
                .iadd()
                .istore(3)

                //if(i < map.size())

                .iload(3)
                .getstatic(className, mapName, mapDesc)
                .invokevirtual("java/util/HashMap","size","()I")
                .icmpGEQUAL(lblLastPointer)

                //Entry entry = (Entry) map.entrySet().get(i);
                .label(3)
                .aload(4) //load object[]
                .iload(3) //i
                .aaload() //set[i]
                .checkcast("java/util/Map$Entry")
                .astore(5)

                //if(entry.getKey()[0] != name), continue to next block
                .label(l2)
                .aload(1)
                .aload(5)
                .invokevirtual("java/util/Map$Entry","getKey","()Ljava/lang/Object;")
                .checkcast("[Ljava/lang/String;")
                .iconst_0()
                .aaload()
                .invokevirtual("java/lang/String","equals","(Ljava/lang/String;)Z")
                .ifeq(l1)

                //if(entry.getKey()[1] != desc), continue to next block
                .aload(2)
                .aload(5)
                .invokevirtual("java/util/Map$Entry","getKey","()Ljava/lang/Object;")
                .checkcast("[Ljava/lang/String;")
                .iconst_1()
                .aaload()
                .invokevirtual("java/lang/String","equals","(Ljava/lang/String;)Z")
                .ifeq(l1)

                //return entry.getValue()[0]
                .aload(5)
                .invokevirtual("java/util/Map$Entry","getValue","()Ljava/lang/Object;")
                .checkcast("[Ljava/lang/String;")
                .iconst_0()
                .aaload()
                .areturn()

                .label(l1) //l4
                .goto_(2)

                .label(lblLastPointer)
                //return original field name
                .aload(1)
                .areturn()

                .localVar("", "Ljava/lang/Class;", null, 0, labelLast, 0)
                .localVar("", "Ljava/lang/String;", null,0, labelLast, 1)
                .localVar("", "Ljava/lang/String;", null,0, labelLast, 2)
                .localVar("","I",null,1,labelLast,3)
                .localVar("","[Ljava/lang/Object;",null,1,labelLast, 4)
                .localVar("","Ljava/util/Map$Entry;",null,2,labelLast,5)

                .writeMethod(cw, ACC_PUBLIC+ACC_STATIC, methodName, methodDesc, null, null);
    }


    private String encrypt(String in) {
        return in;
//        return StringUtils.sha256(in);
    }

    private void generateClinit(ClassWriter cw,
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

        for(Map.Entry<ClassNode, ClassReference> entry : NameObfMap.Classes.entrySet()) {
            //store in class map
            if(reflectionSettings.shouldInclude(entry.getKey()) &&
                    !entry.getValue().oldName.equals(entry.getValue().newName)) {
                mw.getstatic(className, classMapName, classMapDesc)
                        .ldc(encrypt(entry.getValue().oldName.replace('/','.')))
                        .ldc(entry.getValue().newName.replace('/','.'))
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

            if(fields.size() > 0) {
                mw.new_("java/util/HashMap")
                        .dup()
                        .invokespecial("java/util/HashMap")
                        .astore(0);
            }

            if(methods.size() > 0) {
                mw.new_("java/util/HashMap")
                        .dup()
                        .invokespecial("java/util/HashMap")
                        .astore(1)

                        .new_("java/util/HashMap")
                        .dup()
                        .invokespecial("java/util/HashMap")
                        .astore(2);
            }

            for(Map.Entry<FieldNode, FieldReference> fieldEntry : fields) {
                //STORE FIELD INFO
                mw.aload(0)
                        .ldc(encrypt(fieldEntry.getValue().oldName))
                        .ldc(fieldEntry.getValue().newName)
                        .invokeinterface("java/util/Map","put","(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                        .pop();
            }
            if(fields.size() > 0) {
                mw.getstatic(className, fieldMapName, fieldMapDesc)
                        .ldc(Type.getType("L" + entry.getValue().newName + ";"))
                        .aload(0)
                        .invokeinterface("java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                        .pop();
            }


            for(Map.Entry<MethodNode, MethodReference> methodEntry : methods) {
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
                        .ldc(encrypt(methodEntry.getValue().oldDescriptor.substring(0,methodEntry.getValue().oldDescriptor.indexOf(')')+1)))
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
                        .ldc(methodEntry.getValue().newDescriptor.substring(0,methodEntry.getValue().newDescriptor.indexOf(')')+1))
                        .aastore()
                        .invokeinterface("java/util/Map","put","(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                        .pop();
            }

            if(methods.size() > 0) {
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
                .putstatic(className,opqPredName,opqPredDesc)
                .return_()
                .localVar("", "Ljava/util/Map;", null, 0, 2, 0)
                .localVar("", "Ljava/util/Map;", null, 0, 2, 1)
                .localVar("", "Ljava/util/Map;", null, 0, 2, 2)

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
                .invokestatic("java/security/MessageDigest","newInstance","(Ljava/lang/String;)Ljava/security/MessageDigest;")

                .aload(0)
                .ldc("UTF-8")
                .invokevirtual("java/lang/String","getBytes","(Ljava/lang/String;)[B")
                .invokevirtual("java/security/MessageDigest","digest","([B)[B")
                .invokestatic("javax/xml/bind/DatatypeConverter","printHexBinary","([B)Ljava/lang/String;")

                .label(l1)
                .areturn()

                .label(l2)
                .invokevirtual("java/lang/Exception","printStackTrace","()V")

                .aconst_null()
                .areturn()

                .localVar("","Ljava/lang/String;",null,l0,l1,0)

                .writeMethod(cw, ACC_PUBLIC+ACC_STATIC, methodName, methodDesc, null, null);
    }

    private void generateReflectionObjectNoDesc(ClassWriter cw, String methodName, String methodDesc, String className, String mapName, String mapDesc, String opqPredName, String opqPredDesc) {
        MethodBuilder mw = MethodBuilder.newBuilder();
        LabelNode l1 = new LabelNode();
        LabelNode l2 = new LabelNode();

        final int labelLast = 6;
        LabelNode lblLastPointer = new LabelNode();

        mw.label(0)
                //opaque predicate
                .getstatic(className, opqPredName, opqPredDesc)
                .ifne(l2)

                //if it isn't in the class name hash map then skip code
                .getstatic(className, mapName, mapDesc)
                .aload(0)
                .invokevirtual("java/util/HashMap","containsKey","(Ljava/lang/Object;)Z")
                .ifeq(lblLastPointer)

                //int i = -1;
                .label(1)
                .iconst_m1()
                .istore(2)

                .getstatic(className, mapName, mapDesc)
                .invokevirtual("java/util/HashMap","entrySet","()Ljava/util/Set;")
                .invokevirtual("java/util/Set","toArray","()[Ljava/lang/Object;")
                .astore(3)

                //i++
                .label(2)
                .iload(2)
                .iconst_1()
                .iadd()
                .istore(2)

                //if(i < map.size())

                .iload(2)
                .getstatic(className, mapName, mapDesc)
                .invokevirtual("java/util/HashMap","size","()I")
                .icmpGEQUAL(lblLastPointer)

                //Entry entry = (Entry) map.entrySet().get(i);
                .label(3)
                .aload(3) //load object[]
                .iload(2) //i
                .aaload() //set[i]
                .checkcast("java/util/Map$Entry")
                .astore(4)
                .label(l2)

                //if(entry.getKey()[0] != name), continue to next block
                .aload(1)
                .aload(4)
                .invokevirtual("java/util/Map$Entry","getKey","()Ljava/lang/Object;")
                .checkcast("java/lang/String")
                .invokevirtual("java/lang/String","equals","(Ljava/lang/String;)Z")
                .ifeq(l1)

                //return entry.getValue()[0]
                .aload(4)
                .invokevirtual("java/util/Map$Entry","getValue","()Ljava/lang/Object;")
                .checkcast("java/lang/String")
                .areturn()

                .label(l1) //l4
                .goto_(2)

                .label(lblLastPointer)
                //return original field name
                .aload(1)
                .areturn()

                .localVar("", "Ljava/lang/Class;", null, 0, labelLast, 0)
                .localVar("", "Ljava/lang/String;", null,0, labelLast, 1)
                .localVar("","I",null,0,labelLast,2)
                .localVar("","[Ljava/lang/Object;",null,0,labelLast, 3)
                .localVar("","Ljava/util/Map$Entry;",null,0,labelLast,4)

                .writeMethod(cw, ACC_PUBLIC+ACC_STATIC, methodName, methodDesc, null, null);
    }

    private void generateClassAccessor(ClassWriter cw, String methodName, String methodDesc, String className, String mapName, String mapDesc) {
        LabelNode l0 = new LabelNode();
        LabelNode l1 = new LabelNode();
        LabelNode l2 = new LabelNode();

        MethodBuilder.newBuilder()
                .label(l0)
                .getstatic(className, mapName, mapDesc)
                .aload(0)
                .invokevirtual("java/util/HashMap","containsKey","(Ljava/lang/Object;)Z")
                .ifeq(l1)

                .getstatic(className, mapName, mapDesc)
                .aload(0)
                .invokevirtual("java/util/HashMap","get","(Ljava/lang/Object;)Ljava/lang/Object;")
                .checkcast("java/lang/String")
                .areturn()

                .label(l1)
                .aload(0)
                .areturn()
                .label(l2)

                .localVar("","Ljava/lang/String;",null,l0,l2,0)

                .writeMethod(cw, ACC_PUBLIC+ACC_STATIC, methodName, methodDesc, null, null);
    }

    private ClassNode generateClass() {
        String className = ClassNameCreator.instance.getName(null);
        ClassWriter cw = new ClassWriter(0);

        cw.visit(52, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", null);

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
        generateReflectionObjectNoDesc(cw, unmapFieldName = "a", unmapFieldDesc = "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/String;", className, fieldMapName, fieldMapDesc, opqPredName, opqPredDesc);
        generateReflectionObjectNoDesc(cw, unmapMethodNameName = "b", unmapMethodNameDesc = "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/String;", className, methodMapName, methodMapDesc, opqPredName, opqPredDesc);
        generateReflectionObjectAccessorWithDesc(cw, unmapMethodDescName = "a", unmapMethodDescDesc = "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", className, methodDescMapName, methodDescMapDesc, opqPredName, opqPredDesc);
        generateHashFunction(cw, hashMethodName = "a", hashMethodDesc = "(Ljava/lang/String;)Ljava/lang/String;");
        generateClassAccessor(cw, unmapClassName = "b", unmapClassDesc = "(Ljava/lang/String;)Ljava/lang/String;", className, classMapName, classMapDesc);

        cw.visitEnd();

        ClassReader reader = new ClassReader(cw.toByteArray());
        ClassNode node = new ClassNode();
        reader.accept(node, 0);

        /*List<ClassNode> classNodes = ObfGlobal.classes;
        String mainClass = manifest == null ? "" : //will result to it being ignored
                manifest.getMainAttributes()
                        .entrySet()
                        .stream()
                        .filter(e-> e.getKey().toString().startsWith("Main-Class"))
                        .map(e-> e.getValue().toString())
                        .findFirst()
                        .orElse("")
                        .replace('.','/');

        for(int i = 0; i < classNodes.size(); i++) {
            ClassNode classNode = classNodes.get(i);
            boolean isMainClass = classNode.name.equals(mainClass);
            for(int j = 0; j < classNodes.get(i).methods.size(); j++) {
                MethodNode method = classNodes.get(i).methods.get(j);

                //is main method
                if(isMainClass && method.access == ACC_PUBLIC+ACC_STATIC && method.name.equals("main") && method.desc.equals("([Ljava/lang/String;)V")) {
                    InsnList il = MethodBuilder.newBuilder()
//                            .getstatic("java/lang/System","out","Ljava/io/PrintStream;")
//                            .invokestatic(className, m)
//
//                            .invokevirtual("java/io/PrintStream","println","(Ljava/lang/String;)V")
                            .getInstructions();

                    il.add(method.instructions);
                    method.instructions = il;
                }
            }
        }*/
        return node;
    }

}
