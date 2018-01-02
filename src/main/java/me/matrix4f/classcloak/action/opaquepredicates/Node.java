package me.matrix4f.classcloak.action.opaquepredicates;

public class Node {

    private Node parent, child;

    public Node add() {
        if(child == null)
            child = new Node();
        child.parent = this;
        return child;
    }

    public Node childAdd() {
        if(child == null) {
            child = new Node();
            child.parent = this;
        }
        return child.add();
    }

    public Node childChildAdd() {
        if(child == null) {
            child = new Node();
            child.parent = this;
        }
        return child.childAdd();
    }
}
