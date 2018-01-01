package me.matrix4f.classcloak.action.opaquepredicates;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

public abstract class OpaquePred {

    protected boolean truthValue;

    public OpaquePred(boolean truthValue) {
        this.truthValue = truthValue;
    }

    public abstract InsnList generate(int l);

    public InsnList generate(MethodNode mn) {
        return generate(mn.maxLocals);
    }
}
