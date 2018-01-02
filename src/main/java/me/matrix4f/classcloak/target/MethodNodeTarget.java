package me.matrix4f.classcloak.target;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import me.matrix4f.classcloak.util.BytecodeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static me.matrix4f.classcloak.target.ClassNodeTarget.*;
import static me.matrix4f.classcloak.util.parsing.ParsingUtils.conformsToWildcards;

public class MethodNodeTarget extends NodeTarget {

    private ClassNodeTarget classNodeTarget;
    private String methodStr;

    private List<ModifierFilter> modifierFilters = new ArrayList<>();
    private List<String> methodParams = null;
    private String targetMethodName = null;

    public MethodNodeTarget(String asString) throws InvalidTargetException {
        super(asString);

        //ClassName:field
        int indexOfHash = asString.indexOf(':');
        if(indexOfHash == -1) {
            //todo throw error
            //impossible
        }

        String clazzName = asString.substring(0, indexOfHash).trim();
        classNodeTarget = new ClassNodeTarget(clazzName);
        methodStr = asString.substring(indexOfHash+1).trim();

        String[] words = methodStr.split("[ ]");
        int readerState = MODIFIER_OR_NAME;
        String argsFull = "";

        for(int i = 0; i < words.length; i++) {
            if(readerState == MODIFIER_OR_NAME) {
                String wd = words[i];
                boolean negate = false;
                if(wd.length() > 0 && wd.charAt(0) == '!') {
                    negate = true;
                    wd = wd.substring(1);
                }

                int targetFlag = ModifierMap.getFlag(wd);
                if(targetFlag == -1 && targetMethodName == null) { //not a modifier
                    targetMethodName = words[i];
                    readerState = METHOD_ARGS;
                } else if(targetFlag == -1) { //field has already been chosen
                    throw new InvalidTargetException("Method Target | Cannot have more than one name. Previous name: " + targetMethodName + ". Tried to set to: " + words[i]);
                } else { //valid modifier
                    modifierFilters.add(new ModifierFilter(negate, targetFlag));
                }
            } else if(readerState == METHOD_ARGS) {
                argsFull += words[i] + " ";
            }
        }
        argsFull = argsFull.trim();

        if(argsFull.startsWith("("))
            argsFull = argsFull.substring(1);
        if(argsFull.endsWith(")"))
            argsFull = argsFull.substring(0,argsFull.length()-1);
        String[] argsSpecified = argsFull.split("[ ,]");
        if(argsFull.length() > 0) {
            methodParams = new ArrayList<>();
            Stream.of(argsSpecified)
                    .filter(str -> !str.isEmpty())
                    .forEach(methodParams::add);
        }
    }

    @Override
    public boolean doesExcludeNode(Object... node) {
        if(node.length != 2)
            return false;
        if (!(node[0] instanceof MethodNode) || !(node[1] instanceof ClassNode))
            return false;

        MethodNode target = (MethodNode) node[0];
        ClassNode parent = (ClassNode) node[1];

        for(ModifierFilter mf : modifierFilters) {
            if(!mf.accepts(target.access)) {
                return false;
            }
        }

        if(!conformsToWildcards(targetMethodName, target.name)) {
            return false;
        }
        if(!classNodeTarget.doesExcludeNode(parent)) {
            return false;
        }

        if(methodParams != null) {
            Type[] types = Type.getArgumentTypes(target.desc);
            if(methodParams.size() != types.length) {
                return false;
            }

            for(int i = 0; i < types.length; i++) {
                if(methodParams.get(i).equals("*"))
                    continue;
                Type type = types[i];
                if(type.getDescriptor().replace("[","").length() != 1) { //Object type; non primitive
                    String methodParamDesc = BytecodeUtils.convertTypeNameToDescriptor(methodParams.get(i));
                    if(!conformsToWildcards(methodParamDesc, type.getDescriptor()))
                        return false;
                } else {
                    String methodParamDesc = BytecodeUtils.convertTypeNameToDescriptorWithoutPrecedingL(methodParams.get(i));
                    if(!conformsToWildcards(methodParamDesc, type.getDescriptor())) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
