package me.matrix4f.classcloak.util.interpreter;

import org.objectweb.asm.tree.*;
import me.matrix4f.classcloak.util.BytecodeUtils;
import me.matrix4f.classcloak.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;

public class StackInterpreter {

    private List<StackBranchInterpreter> branches;

    public StackInterpreter(MethodNode node) {
        branches = getBranches(node.instructions.getFirst())
                .stream()
                .map(nodes -> new StackBranchInterpreter(nodes, node.desc))
                .collect(Collectors.toList());
    }

    public void interpret() {
        branches.forEach(StackBranchInterpreter::interpret);
    }

    private List<List<AbstractInsnNode>> getBranches(AbstractInsnNode begin) {
        List<List<AbstractInsnNode>> insnLists = new ArrayList<>();
        if(begin == null)
            return new ArrayList<>();

        List<AbstractInsnNode> buffer = new ArrayList<>();
        AbstractInsnNode counter = begin;
        List<Integer> jsrReturnAddresses = new ArrayList<>();
        List<AbstractInsnNode> jsrNodes = new ArrayList<>();
        boolean nextStoreReturnAddress = false;
        List<LabelNode> labels = BytecodeUtils.getLabelsInList(begin);

        LabelNode lastLabel = null;
        while(true) {
            if(counter instanceof LabelNode)
                lastLabel = (LabelNode) counter;

            buffer.add(counter);

            if(counter instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode node = (TableSwitchInsnNode) counter;
                List<List<List<AbstractInsnNode>>> tableEntryBranches = new ArrayList<>();
                for(LabelNode label : node.labels)
                    tableEntryBranches.add(getBranches(label));

                tableEntryBranches.add(getBranches(node.dflt));

                List<List<AbstractInsnNode>> fullLists = new ArrayList<>(tableEntryBranches.size());
                for (List<List<AbstractInsnNode>> branch : tableEntryBranches) {
                    for(List<AbstractInsnNode> incompleteList : branch) {

                        List<AbstractInsnNode> completeList = new ArrayList<>(buffer);
                        completeList.addAll(incompleteList);
                        fullLists.add(completeList);
                    }
                }

                insnLists.addAll(fullLists);
            } else if(counter instanceof LookupSwitchInsnNode) {
                LookupSwitchInsnNode node = (LookupSwitchInsnNode) counter;
                List<List<List<AbstractInsnNode>>> lookupEntryBranches = new ArrayList<>();
                for(LabelNode label : node.labels)
                    lookupEntryBranches.add(getBranches(label));

                lookupEntryBranches.add(getBranches(node.dflt));

                List<List<AbstractInsnNode>> fullLists = new ArrayList<>(lookupEntryBranches.size());
                for (List<List<AbstractInsnNode>> branch : lookupEntryBranches) {
                    for(List<AbstractInsnNode> incompleteList : branch) {

                        List<AbstractInsnNode> completeList = new ArrayList<>(buffer);
                        completeList.addAll(incompleteList);
                        fullLists.add(completeList);
                    }
                }

                insnLists.addAll(fullLists);
            } else if(counter instanceof JumpInsnNode) {
                JumpInsnNode node = (JumpInsnNode) counter;
                if(counter.getOpcode() == GOTO) {
                    if(labels.indexOf(node.label) >= labels.indexOf(lastLabel))
                        counter = node.label;
                } else if(Util.contains(BytecodeUtils.opcodesIf, counter.getOpcode())) {
                    //continue existing branch as if the if-statement succeeded
                    counter = node.label;
                    //continue reading instructions as if it didn't succeed
                    List<List<AbstractInsnNode>> didntSucceed = getBranches(node.getNext());
                    List<List<AbstractInsnNode>> fullLists = new ArrayList<>(didntSucceed.size());
                    for (List<AbstractInsnNode> incompleteList : didntSucceed) {
                        List<AbstractInsnNode> completeList = new ArrayList<>(buffer);
                        completeList.addAll(incompleteList);
                        fullLists.add(completeList);
                    }

                    insnLists.addAll(fullLists);
                } else if(counter.getOpcode() == JSR) {
                    counter = node.label;
                    jsrNodes.add(counter);
                    nextStoreReturnAddress = true;
                }
            }
            if(nextStoreReturnAddress && counter.getOpcode() == ASTORE)
                jsrReturnAddresses.add(((VarInsnNode) counter).var);

            if(counter.getOpcode() == RET) {
                int var = ((VarInsnNode) counter).var;
                counter = jsrNodes.get(jsrReturnAddresses.indexOf(var));
            }

            //if returns from method, then break
            if(Util.contains(BytecodeUtils.opcodesReturn, counter.getOpcode()))
                break;

            counter = counter.getNext();
            if(counter == null)
                break;
        }
        insnLists.add(buffer);
        return insnLists;
    }

    public List<StackBranchInterpreter> getBranches() {
        return branches;
    }
}
