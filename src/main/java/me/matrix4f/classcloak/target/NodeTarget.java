package me.matrix4f.classcloak.target;

public abstract class NodeTarget {

    protected String asString;

    public NodeTarget(String asString) throws InvalidTargetException {
        this.asString = asString;
    }

    public abstract boolean doesExcludeNode(Object... nodeData);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "asString='" + asString + '\'' +
                '}';
    }
}
