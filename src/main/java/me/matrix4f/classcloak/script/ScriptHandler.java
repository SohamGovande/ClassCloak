package me.matrix4f.classcloak.script;

import me.matrix4f.classcloak.script.command.api.Command;
import me.matrix4f.classcloak.script.command.commands.CommandClasspath;
import me.matrix4f.classcloak.script.command.commands.CommandInput;
import me.matrix4f.classcloak.script.command.commands.CommandObfuscate;
import me.matrix4f.classcloak.script.command.commands.CommandSave;
import me.matrix4f.classcloak.script.parsing.CommandException;
import me.matrix4f.classcloak.util.XMLUtils;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static me.matrix4f.classcloak.Globals.LOGGER;

public class ScriptHandler {

    private File scriptFile;
    private List<Command> commands;

    public ScriptHandler(File script) {
        scriptFile = script;
        commands = Arrays.asList(
                new CommandObfuscate(),
                new CommandInput(),
                new CommandSave(),
                new CommandClasspath()
        );
    }

    private void process(String fileData) throws CommandException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringElementContentWhitespace(true);
            dbf.setIgnoringComments(true);

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(fileData.getBytes()));

            Element root = (Element) doc.getChildNodes().item(0);
            for(Node cmdNode : XMLUtils.stream(root.getChildNodes()).collect(Collectors.toList())) {
                if(cmdNode.getNodeType() != Node.ELEMENT_NODE)
                    continue;
                Element cmdElem = (Element) cmdNode;
                int index;
                if ((index = commands.stream()
                        .map(Command::getName)
                        .collect(Collectors.toList())
                        .indexOf(cmdElem.getTagName())) != -1) {
                    commands.get(index).execute(cmdElem, cmdElem.getChildNodes());
                } else {
                    LOGGER.fatal("No command found by name " + cmdElem.getTagName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new CommandException("Error parsing script : " + e.getClass() + ": " + e.getMessage());
        }
    }

    public void loadScript() {
        try {
            String fileData = new String(IOUtils.toByteArray(new FileInputStream(scriptFile)));
            process(fileData);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CommandException e) {
            System.err.println(e.getMessage());
        }
    }
}
