package me.matrix4f.classcloak.script.command.commands;

import me.matrix4f.classcloak.action.ObfGlobal;
import me.matrix4f.classcloak.script.command.api.Command;
import me.matrix4f.classcloak.script.parsing.CommandException;
import me.matrix4f.classcloak.util.XMLUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;

public class CommandInput extends Command {

    public CommandInput() {
        super("input");
    }

    @Override
    protected void doExecution(Element cmdElem, NodeList args) throws CommandException {
        if(cmdElem.hasAttribute("path")) {
            String value = cmdElem.getAttribute("path");

            int indexOfDot = value.lastIndexOf('.');
            String extension = value.substring(indexOfDot + 1);

            if (!extension.equalsIgnoreCase("jar"))
                throw new CommandException(this, "Only input JAR files, not " + extension.toUpperCase() + " files!");

            File file = new File(value);
            if (!file.exists())
                throw new CommandException(this, file.getAbsolutePath() + " does not exist.");

            ObfGlobal.inputFile = file;
        } else {
            throw new CommandException(this, "No path specified.");
        }
    }
}
