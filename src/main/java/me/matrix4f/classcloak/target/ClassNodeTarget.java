package me.matrix4f.classcloak.target;

import org.objectweb.asm.tree.ClassNode;
import me.matrix4f.classcloak.util.parsing.ParsingUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClassNodeTarget extends NodeTarget {

    protected static final int
            MODIFIER_OR_NAME = 0,
            SUPER = 1,
            INTER = 2,
            SUPER_OR_INTER = 3,
            METHOD_ARGS = 4;

    private String targetCName = null;
    private String targetSuperclass = null;
    private List<ModifierFilter> modifierFilters = new ArrayList<>();
    private List<String> targetInterfaces = new ArrayList<>();

    public ClassNodeTarget(String asString) throws InvalidTargetException {
        super(asString);

        String[] words = asString.split("[ ]");
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
                if(targetFlag == -1 && targetCName == null) { //not a modifier
                    targetCName = words[i];
                    readerState = SUPER_OR_INTER;
                } else if(targetFlag == -1) { //class has already been chosen
                    throw new InvalidTargetException("Class Target | Cannot have more than one name. Previous name: " + targetCName + ". Tried to set to: " + words[i]);
                } else { //valid modifier
                    modifierFilters.add(new ModifierFilter(negate, targetFlag));
                }
            } else if(readerState == SUPER_OR_INTER) {
                //only accept keywords implements and extends
                if(words[i].equalsIgnoreCase("extends"))
                    readerState = SUPER;
                else if(words[i].equalsIgnoreCase("implements"))
                    readerState = INTER;
                else
                    throw new InvalidTargetException("Class Target | Was expecting to read \"extends\" or \"implements\", not \"" + words[i] + "\"");
            } else if(readerState == SUPER) {
                if(targetSuperclass == null)
                    targetSuperclass = words[i];
                else
                    throw new InvalidTargetException("Class Target | Cannot have more than one superclass. Previous superclass: " + targetSuperclass + ". Tried to set to: " + words[i]);
                readerState = SUPER_OR_INTER;
            } else if(readerState == INTER) {
                if(words[i].equalsIgnoreCase("extends")) {
                    readerState = SUPER;
                    continue;
                }
                targetInterfaces.add(words[i]);
            }
        }
        if(readerState == SUPER && targetSuperclass == null) {
            throw new InvalidTargetException("Class Target | No superclass specified.");
        }
    }

    @Override
    public boolean doesExcludeNode(Object... node) {
        if(node.length != 1)
            return false;
        if (!(node[0] instanceof ClassNode))
            return false;

        ClassNode target = (ClassNode) node[0];
        int flags = target.access;

        if(!ParsingUtils.conformsToWildcards(targetCName, target.name.replace('/','.')))
            return false;

        for(ModifierFilter mf : modifierFilters) {
            if(!mf.accepts(flags)) {
                return false;
            }
        }

        if(targetSuperclass != null && !ParsingUtils.conformsToWildcards(targetSuperclass, target.superName.replace('/','.')))
            return false;

        for(String targetTest : targetInterfaces) {
            boolean contains = false;
            for (String inter : target.interfaces) {
                if(ParsingUtils.conformsToWildcards(targetTest, inter.replace('/','.'))) {
                    contains = true;
                    break;
                }
            }
            if(!contains)
                return false;
        }

        return true;
    }
}
