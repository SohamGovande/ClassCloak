package me.matrix4f.classcloak.script.command.commands;

import com.sun.deploy.util.StringUtils;
import me.matrix4f.classcloak.script.command.api.Command;
import me.matrix4f.classcloak.script.parsing.CommandException;
import me.matrix4f.classcloak.target.*;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import sun.plugin.dom.core.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CmdHelper {

    public static int parseEnum(Command cmd, String[] possibilities, String pname, String input) throws CommandException {
        int index = -1;
        for(int i = 0; i < possibilities.length; i++) {
            if(possibilities[i].replace("_","").equalsIgnoreCase(input.replace("_",""))) {
                index = i;
                break;
            }
        }
        if(index == -1)
            throw new CommandException(cmd, "Property " + pname + " must be one of {" + StringUtils.join(Arrays.asList(possibilities),",") + "}");
        return index;
    }

    public static List<NodeTarget> parseExclusionsFrom(Command command, NodeList parent) throws CommandException {
        return parseTargetsFrom(command, parent, "exclude");
    }

    public static List<NodeTarget> parseTargetsFrom(Command command, NodeList parent, String type) throws CommandException {
        List<NodeTarget> exclusionsList = new ArrayList<>();
        List<String> exclusionArray = new ArrayList<>();
        for (int i = 0; i < parent.getLength(); i++) {
            if(parent.item(i).getNodeType() != Node.ELEMENT_NODE)
                continue;
            Element element = (Element) parent.item(i);
            if(element.getTagName().equals(type))
                exclusionArray.add(element.getTextContent());
        }

        List<String> fields = exclusionArray.stream()
                .filter(s -> s.indexOf(':') == -1)
                .filter(s -> s.indexOf('#') != -1)
                .collect(Collectors.toList());

        List<String> methods = exclusionArray.stream()
                .filter(s -> s.indexOf(':') != -1)
                .filter(s -> s.indexOf('#') == -1)
                .collect(Collectors.toList());

        List<String> classes = exclusionArray.stream()
                .filter(s -> s.indexOf(':') == -1)
                .filter(s -> s.indexOf('#') == -1)
                .collect(Collectors.toList());

        try {
            for(String s : fields)
                exclusionsList.add(new FieldNodeTarget(s));
            for(String s : methods)
                exclusionsList.add(new MethodNodeTarget(s));
            for(String s : classes)
                exclusionsList.add(new ClassNodeTarget(s));
        } catch (InvalidTargetException e) {
            throw new CommandException("Command " + command.getName() + ": Error loading target. " + e.getMessage());
        }

        return exclusionsList;
    }
}
