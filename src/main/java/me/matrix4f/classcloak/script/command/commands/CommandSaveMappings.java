package me.matrix4f.classcloak.script.command.commands;

import me.matrix4f.classcloak.ClassCloak;
import me.matrix4f.classcloak.action.ObfGlobal;
import me.matrix4f.classcloak.action.SaveMappingsAction;
import me.matrix4f.classcloak.script.command.api.Command;
import me.matrix4f.classcloak.script.parsing.CommandException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;

public class CommandSaveMappings extends Command {
    public CommandSaveMappings() {
        super("saveMappings");
    }

    @Override
    protected void doExecution(Element cmdElem, NodeList args) throws CommandException {
        if(cmdElem.hasAttribute("path")) {
            ObfGlobal.mappingsSaveFile = new File(cmdElem.getAttribute("path"));
            ClassCloak.actions.add(new SaveMappingsAction());
        } else {
            throw new CommandException(this, "No path specified.");
        }
    }
}
