package me.matrix4f.classcloak.target;

public class ModifierFilter {

    private boolean negate;
    private int flag;

    public ModifierFilter(boolean negate, int flag) {
        this.negate = negate;
        this.flag = flag;
    }

    public boolean accepts(int accessFlags) {
        if(negate)
            return (accessFlags & flag) != flag;
        else
            return (accessFlags & flag) == flag;
    }

    public boolean isNegated() {
        return negate;
    }

    public int getFlag() {
        return flag;
    }
}
