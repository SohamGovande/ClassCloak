package me.matrix4f.classcloak.script.command.commands;

import me.matrix4f.classcloak.ClassCloak;
import me.matrix4f.classcloak.action.ObfGlobal;
import me.matrix4f.classcloak.action.SaveAction;
import me.matrix4f.classcloak.script.command.api.Command;
import me.matrix4f.classcloak.script.parsing.CommandException;
import me.matrix4f.classcloak.util.XMLUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;

public class CommandSave extends Command {
    public CommandSave() {
        super("save");
    }

    @Override
    protected void doExecution(Element cmdElem, NodeList args) throws CommandException {
        if(cmdElem.hasAttribute("path")) {
            ObfGlobal.outputFile = new File(cmdElem.getAttribute("path"));
            ClassCloak.actions.add(new SaveAction());
        } else {
            throw new CommandException(this, "No path specified.");
        }
    }
}
