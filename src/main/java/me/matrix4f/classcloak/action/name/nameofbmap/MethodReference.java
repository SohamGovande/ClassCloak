package me.matrix4f.classcloak.action.name.nameofbmap;

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
}
