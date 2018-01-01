package me.matrix4f.classcloak.script.parsing;

import me.matrix4f.classcloak.script.command.api.Command;

public class CommandException extends Exception {

    public CommandException(String msg) {
        super(msg);
    }

    public CommandException(Command command, String msg) {
        super("Command" + command.getName() + ": " + msg);
    }
}
