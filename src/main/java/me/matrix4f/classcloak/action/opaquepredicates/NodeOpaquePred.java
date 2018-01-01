package me.matrix4f.classcloak.action.opaquepredicates;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import me.matrix4f.classcloak.util.BytecodeUtils;
import me.matrix4f.classcloak.util.MethodBuilder;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static me.matrix4f.classcloak.ClassCloak.rand;
import static me.matrix4f.classcloak.action.opaquepredicates.NodeOpaquePredClassBuilder.*;

public class NodeOpaquePred extends OpaquePred{

    public NodeOpaquePred(boolean truthValue) {
        super(truthValue);
    }

    private void generateRandomNodeCalls(MethodBuilder mb, int start, int dest, int depth) {
        int currentDep = 0;

        int depthSize = 0;

        mb.aload(start);
        while(currentDep < depth) {
            int depthLeft = depth - currentDep - 1; // 0 <= depthLeft < depth
            float rfloat = rand.nextFloat();
            float rfloat2 = rand.nextFloat();
            float rfloat3 = rand.nextFloat();
            int path;

            if(rfloat2 >= .5f && depthSize > 0) {
                mb.getfield(className,fieldParentName,desc);
                depthSize--;
                currentDep--;
                continue;
            }

            if(depthLeft >= 2 && rfloat3 >= .6f) {
                path = 2;
            } else if(depthLeft >= 1 && rfloat3 >= .4f) {
                path = 1;
            } else {
                path = 0;
            }

            if(path == 2) { //gets the child of the child of the child
                if(rfloat >= .5f) {
                    mb.invokevirtual(className, childChildAddName, childChildAddDesc);
                } else {
                    if(rfloat >= .25f) {
                        mb.invokevirtual(className, childAddName, childAddDesc);
                        mb.invokevirtual(className, addName, addDesc);
                    } else {
                        mb.invokevirtual(className, addName, addDesc);
                        mb.invokevirtual(className, childAddName, childAddDesc);
                    }
                }
                depthSize += 3;
                currentDep += 3;
            } else if(path == 1) { // gets the child of the child
                if(rfloat >= .5f) {
                    mb.invokevirtual(className, childAddName, childAddDesc);
                } else {
                    mb.invokevirtual(className, addName, addDesc);
                    mb.invokevirtual(className, addName, addDesc);
                }
                depthSize += 2;
                currentDep += 2;
            } else { //gets the top level child
                mb.invokevirtual(className,addName,addDesc);
                depthSize++;
                currentDep++;
            }
            mb.astore(dest);
            mb.aload(dest);
        }
        mb.pop();
    }

    @Override
    public InsnList generate(int l) {
        NodeOpaquePredClassBuilder.generateIfNeeded();

        MethodBuilder mb = MethodBuilder.newBuilder();
        int depth = rand.nextInt(6)+4;
        mb.new_(className)
                .dup()
                .invokespecial(className)
                .astore(l);

        generateRandomNodeCalls(mb, l, l+1, depth);

        if(!truthValue)
            depth = depth+2;

        generateRandomNodeCalls(mb, l, l+2, depth);
        mb.aload(l+1).aload(l+2);

        return mb.getInstructions();
    }
}
