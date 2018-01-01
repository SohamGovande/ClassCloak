package me.matrix4f.classcloak.script.command.api;

import me.matrix4f.classcloak.script.parsing.CommandException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public abstract class Command {

    private String name;
    private NodeList args;

    public Command(String name) {
        this.name = name;
    }

    protected abstract void doExecution(Element cmdElem, NodeList args) throws CommandException;

    public void execute(Element cmdElem, NodeList arguments) throws CommandException {
        this.args = arguments;
        doExecution(cmdElem, args);
    }

    public String getName() {
        return name;
    }
}
