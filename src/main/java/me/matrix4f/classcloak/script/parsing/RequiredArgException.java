package me.matrix4f.classcloak.script.parsing;

public class RequiredArgException extends CommandException {

    public RequiredArgException(String cmdName, String argName, String type) {
        super("Command " + cmdName + " requires argument " + argName + " with type " + type + ".");
    }
}
