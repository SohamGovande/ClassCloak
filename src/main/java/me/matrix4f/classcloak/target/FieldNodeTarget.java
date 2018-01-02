package me.matrix4f.classcloak.target;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import me.matrix4f.classcloak.action.ObfGlobal;
import me.matrix4f.classcloak.util.BytecodeUtils;

import java.util.ArrayList;
import java.util.List;

import static me.matrix4f.classcloak.target.ClassNodeTarget.*;
import static me.matrix4f.classcloak.util.parsing.ParsingUtils.conformsToWildcards;

public class FieldNodeTarget extends NodeTarget {

    private ClassNodeTarget parentTarget;
    private String fieldStr;
    
    private String targetFieldName = null;
    private String targetInstanceof = null;
    private List<ModifierFilter> modifierFilters = new ArrayList<>();
    private List<String> targetInterfaces = new ArrayList<>();
    
    public FieldNodeTarget(String asString) throws InvalidTargetException {
        super(asString);
        //FieldName#field
        int indexOfHash = asString.indexOf('#');
        if(indexOfHash == -1) {
            //todo throw error
            //impossible
        }
        String clazzName = asString.substring(0, indexOfHash).trim();
        parentTarget = new ClassNodeTarget(clazzName);
        fieldStr = asString.substring(indexOfHash+1).trim();

        String[] words = fieldStr.split("[ ]");
        int readerState = MODIFIER_OR_NAME;

        for(int i = 0; i < words.length; i++) {
            if(readerState == MODIFIER_OR_NAME) {
                String wd = words[i];
                boolean negate = false;
                if(wd.length() > 0 && wd.charAt(0) == '!') {
                    negate = true;
                    wd = wd.substring(1);
                }

                int targetFlag = ModifierMap.getFlag(wd);
                if(targetFlag == -1 && targetFieldName == null) { //not a modifier
                    targetFieldName = words[i];
                    readerState = SUPER_OR_INTER;
                } else if(targetFlag == -1) { //field has already been chosen
                    throw new InvalidTargetException("Field Target | Cannot have more than one name. Previous name: " + targetFieldName + ". Tried to set to: " + words[i]);
                } else { //valid modifier
                    modifierFilters.add(new ModifierFilter(negate, targetFlag));
                }
            } else if(readerState == SUPER_OR_INTER) {
                //only accept keywords implements and extends
                if(words[i].equalsIgnoreCase("instanceof"))
                    readerState = SUPER;
                else if(words[i].equalsIgnoreCase("implements"))
                    readerState = INTER;
                else
                    throw new InvalidTargetException("Field Target | Was expecting to read \"instanceof\" or \"implements\", not \"" + words[i] + "\"");
            } else if(readerState == SUPER) {
                if(targetInstanceof == null)
                    targetInstanceof = words[i];
                else
                    throw new InvalidTargetException("Field Target | Cannot have more than one superclass defined. Previous superclass: " + targetInstanceof + ". Tried to set to: " + words[i]);
                readerState = SUPER_OR_INTER;
            } else if(readerState == INTER) {
                if(words[i].equalsIgnoreCase("instanceof")) {
                    readerState = SUPER;
                    continue;
                }
                targetInterfaces.add(words[i]);
            }
        }
        if(readerState == SUPER && targetInstanceof == null) {
            throw new InvalidTargetException("Field Target | No superfield specified.");
        }
    }

    @Override
    public boolean doesExcludeNode(Object... node) {
        if(node.length != 2)
            return false;

        if (!(node[0] instanceof FieldNode) || !(node[1] instanceof ClassNode))
            return false;

        FieldNode target = (FieldNode) node[0];
        ClassNode parent = (ClassNode) node[1];

        for(ModifierFilter mf : modifierFilters) {
            if(!mf.accepts(target.access)) {
                return false;
            }
        }

        if(!conformsToWildcards(targetFieldName, target.name)) {
            return false;
        }
        
        if(!parentTarget.doesExcludeNode(parent)) {
            return false;
        }
        
        if(targetInstanceof != null) {
            if(!targetInstanceof.equals("*")) {
                String targetNoarray = targetInstanceof.replace("[]", "").replace('.', '/');
                String fieldDescNoarray = target.desc.replace("[", "");
                int targetArrayCount = BytecodeUtils.getArrayDimensionsInJavaName(targetInstanceof);
                int fieldArrayCount = BytecodeUtils.getArrayDimensionsInDescriptor(target.desc);
                if (fieldDescNoarray.length() == 1) {
                    String fieldTypeJava = BytecodeUtils.convertDescriptorToJavaName(fieldDescNoarray);
                    if (!conformsToWildcards(targetNoarray, fieldTypeJava) || targetArrayCount != fieldArrayCount) {
                        return false;
                    }
                } else {
                    List<ClassNode> all = ObfGlobal.allClasses();
                    ClassNode fieldType = ClassHierarchy.findOrLoad(all, fieldDescNoarray.substring(1, fieldDescNoarray.length() - 1));
                    boolean extend = ClassHierarchy.doesClassExtend(all, targetNoarray, fieldType);

                    if (!extend) {
                        return false;
                    }
                    if (targetArrayCount != fieldArrayCount) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
}
