package me.matrix4f.classcloak.util;

import me.matrix4f.classcloak.script.command.api.Command;
import me.matrix4f.classcloak.script.parsing.CommandException;
import me.matrix4f.classcloak.util.parsing.ParsingUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class XMLUtils {
    
    private XMLUtils() {}

    public static Stream<Node> stream(NodeList list) {
        Node[] nodes = new Node[list.getLength()];
        for(int i = 0; i < nodes.length; i++)
            nodes[i] = list.item(i);
        return Stream.of(nodes);
    }

    public static int findInt(Command command, NodeList haystack, String name) throws CommandException {
        for(int i = 0; i < haystack.getLength(); i++) {
            Node item = haystack.item(i);
            Element element = (Element) item;

            if(element.getTagName().equals(name)) {
                ensureNonNullText(command, element);
                if(!ParsingUtils.isInt(element.getTextContent()))
                    throw new CommandException("Command " + command.getName() + ": Property " + name + " is not of type integer.");
                return Integer.parseInt(element.getTextContent());
            }
        }
        throw new CommandException("Command " + name + ": Unable to findOrLoad integer property \"" + name + "\"");
    }

    public static int findIntOrElse(Command command, NodeList haystack, String name, int orElse) throws CommandException{
        for(int i = 0; i < haystack.getLength(); i++) {;
            Element element = (Element) haystack.item(i);

            if(element.getTagName().equals(name)) {
                ensureNonNullText(command, element);
                if(!ParsingUtils.isInt(element.getTextContent()))
                    throw new CommandException("Command " + command.getName() + ": Property " + name + " is not of type integer.");
                return Integer.parseInt(element.getTextContent());
            }
        }
        return orElse;
    }

    public static void ensureNonNullText(Command command, Element elem) throws CommandException {
        if(elem.getTextContent() == null)
            throw new CommandException("Command " + command.getName() + ": Property " + elem.getTagName() + " has a null text content");
    }

    public static String findParam(Command command, Element elem, String name) throws CommandException {
        if(elem.hasAttribute(name)) {
            return elem.getAttribute(name);
        } else {
            throw new CommandException(command, "No attribute found by name " + name + ".");
        }
    }


    public static List<String> findString(Command command, NodeList haystack, String name) throws CommandException {
        List<String> list = new ArrayList<>();
        for(int i = 0; i < haystack.getLength(); i++) {
            if(haystack.item(i).getNodeType() != Node.ELEMENT_NODE)
                continue;
            Element element = (Element) haystack.item(i);
            if(element.getTagName().equals(name)) {
                ensureNonNullText(command, element);
                list.add(element.getTextContent());
            }
        }
        if(list.size() == 0)
            throw new CommandException("Command " + command.getName() + ": No string found by tag name " + name );
        return list;
    }

    public static Stream<Element> elements(NodeList nodeList) {
        return stream(nodeList)
                .filter(node -> node instanceof Element)
                .map(node -> (Element) node);
    }

    public static Stream<Element> elementsWithTagName(NodeList list, String tagName) {
        return elements(list)
                .filter(element -> element.getTagName().equals(tagName));
    }

    public static String getAttribute(Element element, String name, Command command) throws CommandException {
        if(element.hasAttribute(name))
            return element.getAttribute(name);
        throw new CommandException(command, "No attribute " + name + " specified for " + element.getTagName());
    }

    public static Optional<Element> firstElement(NodeList list, String tagName, boolean require, Command command) throws CommandException {
        Optional<Element> first = elements(list)
                .filter(node -> node.getTagName().equals(tagName))
                .findFirst();
        if(!first.isPresent() && require)
            throw new CommandException(command, "No element " + tagName + " found.");
        return first;
    }
}
