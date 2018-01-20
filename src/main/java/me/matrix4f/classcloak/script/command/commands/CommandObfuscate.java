package me.matrix4f.classcloak.script.command.commands;

import me.matrix4f.classcloak.ClassCloak;
import me.matrix4f.classcloak.action.DebugObfuscateAction;
import me.matrix4f.classcloak.action.ObfSettings.*;
import me.matrix4f.classcloak.action.name.NameObfuscateAction;
import me.matrix4f.classcloak.action.reflection.ReflectionEntry;
import me.matrix4f.classcloak.action.reflection.ReflectionMethodMap;
import me.matrix4f.classcloak.action.reflection.ReflectionVerifyAction;
import me.matrix4f.classcloak.action.string.StringObfuscateAction;
import me.matrix4f.classcloak.script.command.api.Command;
import me.matrix4f.classcloak.script.parsing.CommandException;
import me.matrix4f.classcloak.target.*;
import me.matrix4f.classcloak.util.XMLUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static me.matrix4f.classcloak.ClassCloak.*;
import static me.matrix4f.classcloak.action.ObfGlobal.*;
import static me.matrix4f.classcloak.action.reflection.ReflectionMethodMap.*;

public class CommandObfuscate extends Command {

    public CommandObfuscate() {
        super("obfuscate");
    }

    @Override
    protected void doExecution(Element cmdElem, NodeList args) throws CommandException {
        for (int j = 0; j < args.getLength(); j++) {
            if (args.item(j).getNodeType() != Node.ELEMENT_NODE)
                continue;

            Element currentChild = (Element) args.item(j);
            NodeList children = currentChild.getChildNodes();
            switch (currentChild.getTagName()) {
                case "debugInfo": {
                    Optional<Element> localVarElement = XMLUtils.firstElement(children, "localVars",false,this);
                    if(localVarElement.isPresent()) {
                        Element localVar = localVarElement.get();
                        String action = XMLUtils.getAttribute(localVar, "action", this);
                        debugSettings.localVarAction = CmdHelper.parseEnum(this, DebugSettings.LOCALVARS, "action", action);
                    }

                    Optional<Element> lineNumberElement = XMLUtils.firstElement(children, "lineNumbers",false,this);
                    if(lineNumberElement.isPresent()) {
                        Element lineNumbers = lineNumberElement.get();
                        String action = XMLUtils.getAttribute(lineNumbers, "action", this);
                        debugSettings.lineNumberPwd = XMLUtils.getAttribute(lineNumbers, "password", this);
                        debugSettings.lineNumberAction = CmdHelper.parseEnum(this, DebugSettings.LINENUMBERS, "action", action);
                    }

                    Optional<Element> sourceFileElement = XMLUtils.firstElement(children, "sourceFiles",false,this);
                    if(sourceFileElement.isPresent()) {
                        Element sourceFiles = sourceFileElement.get();
                        String action = XMLUtils.getAttribute(sourceFiles, "action", this);
                        debugSettings.sourceAction = CmdHelper.parseEnum(this, DebugSettings.SOURCE, "action", action);
                    }
                    actions.add(new DebugObfuscateAction());
                    break;
                }
                case "name": {
                    nameSettings.exclusions = CmdHelper.parseExclusionsFrom(this, children);
                    Optional<Element> overloadingOptional = XMLUtils.firstElement(children, "overloading", false, this);
                    if (overloadingOptional.isPresent()) {
                        Element overloadingSettings = overloadingOptional.get();
                        Optional<Element> methodOptional = XMLUtils.elementsWithTagName(overloadingSettings.getChildNodes(), "methods")
                                .findFirst();
                        if (methodOptional.isPresent()) {
                            Element methodOverloading = methodOptional.get();
                            String option = XMLUtils.getAttribute(methodOverloading, "option", this);
                            nameSettings.overloadMethods = CmdHelper.parseEnum(this, NameSettings.METHOD_OVERLOADING, "option", option);
                        }
                        XMLUtils.elementsWithTagName(overloadingSettings.getChildNodes(), "fields")
                                .findFirst()
                                .ifPresent(field -> nameSettings.overloadFields = true);
                    }
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
                case "reflectionHandling": {
                    reflectionSettings.inclusions = CmdHelper.parseTargetsFrom(this, children, "include");
//                    reflectionSettings.hashFunction = XMLUtils.findString(this, children, "algorithm").get(0);
                    List<Element> xmlEntries = XMLUtils.stream(children)
                            .filter(node -> node.getNodeType() == Node.ELEMENT_NODE)
                            .map(node -> (Element) node)
                            .filter(node -> node.getTagName().equals("entry"))
                            .collect(Collectors.toList());
                    for (Element entry : xmlEntries) {
                        NodeList childs = entry.getChildNodes();
                        List<MethodNodeTarget> targets = CmdHelper.parseTargetsFrom(this, childs, "from")
                                .stream()
                                .filter(node -> node instanceof MethodNodeTarget)
                                .map(node -> (MethodNodeTarget) node)
                                .collect(Collectors.toList());
                        List<String> methods = Arrays.asList(CLASS_GETDECLAREDFIELD, CLASS_GETDECLAREDMETHOD, CLASS_GETFIELD, CLASS_GETMETHOD, CLASS_FORNAME, FIELD_GETNAME, CLASS_GETNAME, METHOD_GETNAME);
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
                    if (reflectionSettings.inclusions.size() == 0) {
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
                    throw new CommandException(this, "Invalid subcommand: " + currentChild.getTagName());
            }
        }
    }
}
