package me.matrix4f.classcloak.action.name.nameofbmap;

public class FieldReference {

    public ClassReference parent;
    public String oldName;
    public String newName;
    public String oldDescriptor;
    public String newDescriptor;

    public FieldReference(ClassReference parent, String oldName, String newName, String oldDescriptor, String newDescriptor) {
        this.parent = parent;
        this.oldName = oldName;
        this.newName = newName;
        this.oldDescriptor = oldDescriptor;
        this.newDescriptor = newDescriptor;
    }
}
