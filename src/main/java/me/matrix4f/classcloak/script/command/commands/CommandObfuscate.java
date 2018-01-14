package me.matrix4f.classcloak.script.command.commands;

import me.matrix4f.classcloak.action.LineNumberObfuscateAction;
import me.matrix4f.classcloak.action.name.NameObfuscateAction;
import me.matrix4f.classcloak.action.reflection.ReflectionEntry;
import me.matrix4f.classcloak.action.reflection.ReflectionMethodMap;
import me.matrix4f.classcloak.action.reflection.ReflectionVerifyAction;
import me.matrix4f.classcloak.action.string.StringObfuscateAction;
import me.matrix4f.classcloak.script.command.api.Command;
import me.matrix4f.classcloak.script.parsing.CommandException;
import me.matrix4f.classcloak.target.ClassNodeTarget;
import me.matrix4f.classcloak.target.FieldNodeTarget;
import me.matrix4f.classcloak.target.InvalidTargetException;
import me.matrix4f.classcloak.target.MethodNodeTarget;
import me.matrix4f.classcloak.util.XMLUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static me.matrix4f.classcloak.ClassCloak.*;
import static me.matrix4f.classcloak.action.ObfGlobal.*;
import static me.matrix4f.classcloak.action.ObfSettings.LineObfSettings;
import static me.matrix4f.classcloak.action.ObfSettings.StringObfSettings;
import static me.matrix4f.classcloak.action.reflection.ReflectionMethodMap.*;

public class CommandObfuscate extends Command {

    public CommandObfuscate() {
        super("obfuscate");
    }

    @Override
    protected void doExecution(Element cmdElem, NodeList args) throws CommandException {
        for(int j = 0; j < args.getLength(); j++) {
            if(args.item(j).getNodeType() != Node.ELEMENT_NODE)
                continue;

            Element element = (Element) args.item(j);
            NodeList children = element.getChildNodes();
            switch (element.getTagName()) {
                case "name": {
                    nameSettings.exclusions = CmdHelper.parseExclusionsFrom(this, children);
                    actions.add(new NameObfuscateAction());
                    break;
                }
                case "string": {
                    stringSettings.exclusions = CmdHelper.parseExclusionsFrom(this, children);

                    String method = XMLUtils.findString(this, children, "method").get(0);
                    stringSettings.obfMethod = CmdHelper.parseEnum(this, StringObfSettings.METHOD_LIST, "method", method);
                    actions.add(new StringObfuscateAction());
                    break;
                }
                case "line_numbers": {
                    lineSettings.exclusions = CmdHelper.parseExclusionsFrom(this, children);

                    String modify = XMLUtils.findString(this, children, "modify").get(0);
                    lineSettings.obfMethod = CmdHelper.parseEnum(this, LineObfSettings.METHOD_LIST, "modify", modify);
                    actions.add(new LineNumberObfuscateAction());
                    break;
                }
                case "reflection_handling": {
                    reflectionSettings.inclusions = CmdHelper.parseTargetsFrom(this, children, "include");
//                    reflectionSettings.hashFunction = XMLUtils.findString(this, children, "algorithm").get(0);
                    List<Element> xmlEntries = XMLUtils.stream(children)
                            .filter(node -> node.getNodeType() == Node.ELEMENT_NODE)
                            .map(node -> (Element) node)
                            .filter(node -> node.getTagName().equals("entry"))
                            .collect(Collectors.toList());
                    for(Element entry : xmlEntries) {
                        NodeList childs = entry.getChildNodes();
                        List<MethodNodeTarget> targets = CmdHelper.parseTargetsFrom(this, childs, "from")
                                .stream()
                                .filter(node -> node instanceof MethodNodeTarget)
                                .map(node -> (MethodNodeTarget) node)
                                .collect(Collectors.toList());
                        List<String> methods = Arrays.asList(CLASS_GETDECLAREDFIELD,CLASS_GETDECLAREDMETHOD,CLASS_GETFIELD,CLASS_GETMETHOD,CLASS_FORNAME);
                        ReflectionMethodMap map = new ReflectionMethodMap();

                        XMLUtils.stream(childs)
                                .filter(node -> node.getNodeType() == Node.ELEMENT_NODE)
                                .map(node -> (Element) node)
                                .filter(elem -> methods.contains(elem.getTagName()))
//                                .filter(elem -> elem.hasAttribute("remap"))
//                                .forEach(element1 -> map.put(element1.getTagName(), Boolean.parseBoolean(element1.getAttribute("remap"))))
                                .forEach(element1 -> map.put(element1.getTagName(), true));
                        reflectionSettings.entries.add(new ReflectionEntry(targets, map));
                    }
                    if(reflectionSettings.inclusions.size() == 0) {
                        try {
                            reflectionSettings.inclusions.add(new ClassNodeTarget("*"));
                            reflectionSettings.inclusions.add(new FieldNodeTarget("*#*"));
                            reflectionSettings.inclusions.add(new MethodNodeTarget("*:*"));
                        } catch (InvalidTargetException e) {
                            //never happens
                            e.printStackTrace();
                        }
                    }
                    actions.add(new ReflectionVerifyAction());
                    break;
                }
                default:
                    throw new CommandException(this, "Invalid subcommand: " + element.getTagName());
            }
        }
    }
}
