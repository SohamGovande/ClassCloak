package me.matrix4f.classcloak.mapping;

public abstract class Mapping<T, E> {

    protected T node;
    protected E arg;

    public Mapping(T node, E arg) {
        this.node = node;
        this.arg = arg;
    }

    public abstract void apply();

    public E getArg() {
        return arg;
    }

    public T getNode() {
        return node;
    }
}
