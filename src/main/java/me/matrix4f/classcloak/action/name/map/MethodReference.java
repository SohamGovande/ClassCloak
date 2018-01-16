package me.matrix4f.classcloak.action.name.map;

import org.objectweb.asm.Type;
import me.matrix4f.classcloak.util.BytecodeUtils;

public class MethodReference {

    public ClassReference parent;
    public String oldName, newName, oldDescriptor, newDescriptor;

    public MethodReference(ClassReference parent, String oldName, String newName, String oldDescriptor, String newDescriptor) {
        this.parent = parent;
        this.oldName = oldName;
        this.newName = newName;
        this.oldDescriptor = oldDescriptor;
        this.newDescriptor = newDescriptor;
    }

    public String oldToString() {
        Type[] types = Type.getArgumentTypes(oldDescriptor);
        String buf = BytecodeUtils.convertDescriptorToJavaName(Type.getReturnType(oldDescriptor).getDescriptor()) + " " + oldName + "(";
        for(Type type : types)
            buf += BytecodeUtils.convertDescriptorToJavaName(type.getDescriptor()) + ", ";
        if(buf.endsWith(", "))
            buf = buf.substring(0, buf.length() - 2);
        buf += ")";
        return buf;
    }
    
    public String newToString() {
        Type[] types = Type.getArgumentTypes(newDescriptor);
        String buf = BytecodeUtils.convertDescriptorToJavaName(Type.getReturnType(newDescriptor).getDescriptor()) + " " + newName + "(";
        for(Type type : types)
            buf += BytecodeUtils.convertDescriptorToJavaName(type.getDescriptor()) + ", ";
        if(buf.endsWith(", "))
            buf = buf.substring(0, buf.length() - 2);
        buf += ")";
        return buf;
    }
}
