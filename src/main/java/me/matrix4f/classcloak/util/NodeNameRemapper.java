package me.matrix4f.classcloak.util;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import me.matrix4f.classcloak.action.ObfGlobal;
import me.matrix4f.classcloak.action.name.namecreation.MethodNameCreator;
import me.matrix4f.classcloak.mapping.Mappings;
import me.matrix4f.classcloak.mapping.Mappings.*;
import me.matrix4f.classcloak.target.NodeTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static me.matrix4f.classcloak.action.ObfGlobal.classes;

public class NodeNameRemapper {

    private Mappings mappings = new Mappings();

    public void apply() {
        mappings.apply();
    }

    private String changeDescriptor(String desc, String target, String newTarget) {
        String value;
        String noArrayDesc = desc.replace("[","");
        if(noArrayDesc.length() == 1) { //primitive type
            value = desc;
        } else {
            String internalName = noArrayDesc.substring(1,noArrayDesc.length()-1);
            if(internalName.equals(target)) {
                String brackets = desc.replaceAll("[^\\[]", ""); //leave only brackets
                value = brackets + "L" + newTarget + ";";
            } else {
                value = desc;
            }
        }
        return value;
    }

    private String changeDescIfTargets(String desc, String target, String newTarget) {
        String newDesc = changeDescriptor(desc, target, newTarget);
        if(newDesc.equals(desc))
            return null;
        else
            return newDesc;
    }

    private String changeMethodDescriptor(String descriptor, String target, String newTarget) {
        Type[] argTypes = Type.getArgumentTypes(descriptor);
        Type returnType = Type.getReturnType(descriptor);
        StringBuilder descriptorBuilder = new StringBuilder("(");

        for(Type type : argTypes)
            descriptorBuilder.append(changeDescriptor(type.getDescriptor(), target, newTarget));

        return descriptorBuilder.append(")")
                .append(changeDescriptor(returnType.getDescriptor(), target, newTarget))
                .toString();
    }

    private String changeMethodDescIfDiff(String descriptor, String target, String newTarget) {
        String newDesc = changeMethodDescriptor(descriptor, target, newTarget);
        if(newDesc.equals(descriptor))
            return null;
        else
            return newDesc;
    }

    private void transformMethodWhileRenamingClass(MethodNode methodNode, String targetClassName, String newName) {
        String newDesc = changeMethodDescIfDiff(methodNode.desc, targetClassName, newName);
        if(newDesc != null)
            mappings.add(new MethodDescMapping(methodNode, newDesc));

        //method invokes from target class
        BytecodeUtils.streamInstructions(MethodInsnNode.class, methodNode)
                .filter(node -> node.owner.equals(targetClassName))
                .forEach(node -> mappings.add(new MethodInsnNodeOwnerMapping(node, newName)));

        //method invokes containing target class in descriptor
        BytecodeUtils.streamInstructions(MethodInsnNode.class, methodNode)
                .forEach(node -> {
                    String newMDesc = changeMethodDescIfDiff(node.desc, targetClassName, newName);
                    if(newMDesc != null)
                        mappings.add(new MethodInsnNodeDescMapping(node, newMDesc));
                });

        //method new instances
        BytecodeUtils.streamInstructions(TypeInsnNode.class, methodNode)
                .filter(node -> node.desc.equals(targetClassName))
                .forEach(node -> node.desc = newName);

        //TargetClassName.field -> ab.field
        BytecodeUtils.streamInstructions(FieldInsnNode.class, methodNode)
                .filter(node -> node.owner.equals(targetClassName))
                .forEach(node -> mappings.add(new FieldInsnNodeOwnerMapping(node, newName)));

        BytecodeUtils.streamInstructions(FieldInsnNode.class, methodNode)
                .forEach(node -> {
                    String newFDesc = changeDescIfTargets(node.desc, targetClassName, newName);
                    if(newFDesc != null)
                        mappings.add(new FieldInsnNodeDescMapping(node, newFDesc));
                });

        //Bob.class -> a.class
        BytecodeUtils.streamInstructions(LdcInsnNode.class, methodNode)
                .filter(ldc -> ldc.cst instanceof Type && ((Type) ldc.cst).getDescriptor().equals("L" + targetClassName + ";"))
                .forEach(ldc -> ldc.cst = Type.getType("L" + newName + ";"));

        //TargetClassName myField -> ab myField
        if(methodNode.localVariables != null) { //interfaces have null local vars
            methodNode.localVariables
                    .forEach(node -> {
                        String newMDesc = changeDescIfTargets(node.desc, targetClassName, newName);
                        if(newMDesc != null)
                            node.desc = newMDesc;
                    });
        }
    }

    public void changeClassName(String targetClassName, String newName) {
        ClassNode targetClass = classes.stream().filter(classNode -> classNode.name.equals(targetClassName)).findFirst().get();

        classes.forEach(classNode -> {
            //replace interface implements declarations
            for(int i = 0; i < classNode.interfaces.size(); i++)
                if(classNode.interfaces.get(i).equals(targetClassName))
                    mappings.add(new ClassInterfaceMapping(classNode, new Object[] {
                            i,
                            newName
                    }));

            //replace superclass extends declarations
            if(classNode.superName.equals(targetClassName))
                mappings.add(new ClassSuperclassMapping(classNode, newName));

            classNode.fields.forEach(fieldNode -> {
                String newDesc = changeDescIfTargets(fieldNode.desc, targetClassName, newName);
                if(newDesc != null)
                    mappings.add(new FieldDescMapping(fieldNode, newDesc));
            });

            classNode.methods.forEach(methodNode -> {
                transformMethodWhileRenamingClass(methodNode, targetClassName, newName);
            });
        });
        mappings.add(new ClassNameMapping(targetClass, newName));
    }

    private boolean doesClassInherit(ClassNode clazz, String maybeInherits) {
        return !clazz.name.equals("java/lang/Object") && (clazz.interfaces.contains(maybeInherits) || clazz.superName.equals(maybeInherits));
    }

    /**
     *
     * @param exclusions
     * @param owner The owner of the target method
     * @param target The target method to be renamed
     * @param newName The new name of the method
     * @return The name the method has been changed to. May be potentially different than the supplied name due to superclasses having a method with the same name.
     */
    public void changeMethodName(List<NodeTarget> exclusions, ClassNode owner, MethodNode target, String newName) {
        //see if this method extends another method
        List<ClassNode> searchIn = ObfGlobal.allClasses();

        Optional<List<MethodNode>> beingOverridden = searchIn.stream()
                //check for inheritance (extend/implement) first
                .filter(cn -> doesClassInherit(owner, cn.name))
                .map(cn -> cn.methods)
                //for classes that are being extended, check all their methods to see if target method is overriding it
                .filter(mnList -> mnList.stream()
                        .anyMatch(mn -> mn.desc.equals(target.desc) && mn.name.equals(target.name)))
                .findFirst();
        //don't rename it if it overrides something
        if(beingOverridden.isPresent())
            return;

        //classes that contain methods that override the target method
        List<ClassNode> owners = new ArrayList<>();
        owners.add(owner);

        //methods that override the target method
        List<MethodNode> overriding = new ArrayList<>();
        overriding.add(target);
        //findAndLoad methods that override this one
        searchIn.stream()
                .filter(cn -> doesClassInherit(cn, owner.name))
                .forEach(cn -> {
                    Optional<MethodNode> ov = cn.methods.stream()
                            .filter(mn -> mn.name.equals(target.name))
                            .filter(mn -> mn.desc.equals(target.desc))
                            .findFirst();
                    if(ov.isPresent()) {
                        overriding.add(ov.get());
                        owners.add(cn);
                    }
                });

        for(int i = 1; i < overriding.size(); i++) { //overriding.length = owners.length
            int finalI = i;
            if(exclusions.stream().anyMatch(ex -> ex.doesExcludeNode(overriding.get(finalI), owners.get(finalI))))
                return;
        }

        List<String> ownerNames = owners.stream().map(cn -> cn.name).collect(Collectors.toList());

        //findOrLoad a better method to findOrLoad a name of the method
        if(owners.size() > 1) { //other classes override it woo
            List<String> possibleNames = new ArrayList<>();
            MethodNameCreator.classesToCreators.entrySet()
                    .stream()
                    .filter(entry -> owners.contains(entry.getKey()))
                    .filter(entry -> entry.getKey() != owners.get(0))
                    .forEach(entry -> {
                        MethodNode override = entry.getKey().methods.stream()
                                .filter(method -> method.name.equals(target.name))
                                .filter(method -> method.desc.equals(target.desc))
                                .findFirst()
                                .get();
                        String n = entry.getValue().getName(override, true);
                        possibleNames.add(n);
                    });

            newName = possibleNames.stream()
                    .filter(s -> owners.stream()
                            .map(cn -> cn.methods)
                            .anyMatch(
                                    mnList -> mnList.stream().noneMatch(mn -> mn.name.equals(s) && mn.desc.equals(s))
                            ))
                    .findFirst()
                    .get();
        }

        String finalNewName = newName;
        classes.forEach(classNode -> {
            classNode.methods.forEach(methodNode -> {
                BytecodeUtils.streamInstructions(MethodInsnNode.class, methodNode)
                        .filter(node -> node.desc.equals(target.desc))
                        .filter(node -> ownerNames.contains(node.owner))
                        .filter(node -> node.name.equals(target.name))

                        .forEach(node -> mappings.add(new MethodInsnNodeNameMapping(node, finalNewName)));
            });
        });
        overriding.forEach(node -> mappings.add(new MethodNameMapping(node, finalNewName)));
    }

    public void changeFieldName(ClassNode owner, FieldNode target, String newName) {
        classes.forEach(classNode -> {
            classNode.methods.forEach(methodNode -> {
                BytecodeUtils.streamInstructions(FieldInsnNode.class, methodNode)
                        .filter(node -> node.desc.equals(target.desc))
                        .filter(node -> node.owner.equals(owner.name))
                        .filter(node -> node.name.equals(target.name))

                        .forEach(node -> mappings.add(new FieldInsnNodeNameMapping(node, newName)));
            });
        });
        mappings.add(new FieldNameMapping(target, newName));
    }

    public Mappings getMappings() {
        return mappings;
    }
}
