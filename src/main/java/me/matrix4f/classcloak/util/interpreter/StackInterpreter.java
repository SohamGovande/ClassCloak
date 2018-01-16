package me.matrix4f.classcloak.util.interpreter;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import me.matrix4f.classcloak.util.BytecodeUtils;
import me.matrix4f.classcloak.util.Util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.*;

public class StackInterpreter {

    private List<StackBranchInterpreter> branches;

    public StackInterpreter(String ownerClass, MethodNode node) {
        branches = getBranches(node.instructions.getFirst())
                .stream()
                .map(nodes -> new StackBranchInterpreter(nodes, ownerClass, node))
                .collect(Collectors.toList());
    }

    public Stream<StackBranchInterpreter> allBranchesContaining(AbstractInsnNode needle) {
        return branches.stream()
                .filter(node -> node.getInsns().contains(needle));
    }

    public void interpret() {
        try {
            for (StackBranchInterpreter branchInterpreter : branches)
                branchInterpreter.interpret();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    private List<List<AbstractInsnNode>> getBranches(AbstractInsnNode begin) {
        List<List<AbstractInsnNode>> insnLists = new ArrayList<>();
        if (begin == null) {
            return new ArrayList<>();
        }

        List<AbstractInsnNode> buffer = new ArrayList<>();
        AbstractInsnNode counter = begin;
        List<Integer> jsrReturnAddresses = new ArrayList<>();
        List<AbstractInsnNode> jsrNodes = new ArrayList<>();
        boolean nextStoreReturnAddress = false;

        List<LabelNode> labels = BytecodeUtils.getLabelsInList(begin);
        LabelNode lastLabel = labels.stream().findFirst().orElse(null);

        //this variable can be set to true so that for the current iteration of the loop,
        //the counter isn't moved to the next node
        boolean dontIncrement = false;
        while (true) {
            if (counter instanceof LabelNode)
                lastLabel = (LabelNode) counter;

            buffer.add(counter);

            if (counter instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode node = (TableSwitchInsnNode) counter;
                List<List<List<AbstractInsnNode>>> tableEntryBranches = new ArrayList<>();
                for (LabelNode label : node.labels)
                    tableEntryBranches.add(getBranches(label));

                tableEntryBranches.add(getBranches(node.dflt));

                List<List<AbstractInsnNode>> fullLists = new ArrayList<>(tableEntryBranches.size());
                for (List<List<AbstractInsnNode>> branch : tableEntryBranches) {
                    for (List<AbstractInsnNode> incompleteList : branch) {
                        List<AbstractInsnNode> completeList = new ArrayList<>(buffer);
                        completeList.addAll(incompleteList);
                        fullLists.add(completeList);
                    }
                }

                insnLists.addAll(fullLists);
            } else if (counter instanceof LookupSwitchInsnNode) {
                LookupSwitchInsnNode node = (LookupSwitchInsnNode) counter;
                List<List<List<AbstractInsnNode>>> lookupEntryBranches = new ArrayList<>();
                for (LabelNode label : node.labels)
                    lookupEntryBranches.add(getBranches(label));

                lookupEntryBranches.add(getBranches(node.dflt));

                List<List<AbstractInsnNode>> fullLists = new ArrayList<>(lookupEntryBranches.size());
                for (List<List<AbstractInsnNode>> branch : lookupEntryBranches) {
                    for (List<AbstractInsnNode> incompleteList : branch) {

                        List<AbstractInsnNode> completeList = new ArrayList<>(buffer);
                        completeList.addAll(incompleteList);
                        fullLists.add(completeList);
                    }
                }

                insnLists.addAll(fullLists);
            } else if (counter instanceof JumpInsnNode) {
                JumpInsnNode node = (JumpInsnNode) counter;
                int indexOfTarget = labels.indexOf(node.label);
                int indexOfThis = labels.indexOf(lastLabel);
                if (counter.getOpcode() == GOTO) {
                    if (indexOfTarget > indexOfThis) { //dont jump backward, will cause stack overflow error
                        counter = node.label;
                        dontIncrement = true; //the label will be read
                    }
                } else if (Util.contains(BytecodeUtils.opcodesIf, counter.getOpcode())) {
                    //continue existing branch as if the if-statement succeeded
                    if (indexOfTarget > indexOfThis) { //dont jump backward, will cause stack overflow error
                        //continue reading instructions as if it did succeed
                        List<List<AbstractInsnNode>> didntSucceed = getBranches(node.label);
                        List<List<AbstractInsnNode>> fullLists = new ArrayList<>(didntSucceed.size());
                            for (List<AbstractInsnNode> incompleteList : didntSucceed) {
                            List<AbstractInsnNode> completeList = new ArrayList<>(buffer);
                            completeList.addAll(incompleteList);
                            fullLists.add(completeList);
                        }

                        insnLists.addAll(fullLists);
                    }
                } else if (counter.getOpcode() == JSR) {
                    counter = node.label;
                    dontIncrement = true; //the label will be read

                    jsrNodes.add(counter);
                    nextStoreReturnAddress = true;
                }
            }
            if (nextStoreReturnAddress && counter.getOpcode() == ASTORE)
                jsrReturnAddresses.add(((VarInsnNode) counter).var);

            if (counter.getOpcode() == RET) {
                int var = ((VarInsnNode) counter).var;
                counter = jsrNodes.get(jsrReturnAddresses.indexOf(var));
            }

            //if returns from method, then break
            if (Util.contains(BytecodeUtils.opcodesReturn, counter.getOpcode()))
                break;

            if (!dontIncrement)
                counter = counter.getNext();
            else
                dontIncrement = false; //next time do increment
            if (counter == null)
                break;
        }
        insnLists.add(buffer);
        return insnLists;
    }

    public List<StackBranchInterpreter> getBranches() {
        return branches;
    }
}
