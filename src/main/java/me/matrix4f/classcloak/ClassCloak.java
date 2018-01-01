package me.matrix4f.classcloak;

import org.objectweb.asm.tree.ClassNode;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import me.matrix4f.classcloak.action.ClassPathVerifierAction;
import me.matrix4f.classcloak.action.LoadNecessaryClassesAction;
import me.matrix4f.classcloak.action.ObfGlobal;
import me.matrix4f.classcloak.action.SaveAction;
import me.matrix4f.classcloak.action.name.NameObfuscateAction;
import me.matrix4f.classcloak.action.name.nameofbmap.NameObfMap;
import me.matrix4f.classcloak.script.ScriptHandler;
import me.matrix4f.classcloak.util.FileIOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import static me.matrix4f.classcloak.Globals.LOGGER;

public class ClassCloak {

    public static final Random rand = new Random();

    public static List<Action> actions = new ArrayList<>();

    public static void main(String[] args) {
//        if(1 == 1)
//            return;

        OptionParser parser = new OptionParser();
        OptionSpec<String> scriptPathOption = parser.accepts("script").withRequiredArg();
        OptionSet optionSet = parser.parse(args);

        if(!optionSet.has(scriptPathOption))
            LOGGER.fatal("Run program with java -jar classcloak.jar -script <scriptFilePath>.");

        File scriptFile = new File(optionSet.valueOf(scriptPathOption));
        if(!scriptFile.exists())
            LOGGER.fatal("Script file " + scriptFile + " does not exist.");

        LOGGER.info(Globals.NAME + " " + Globals.VERSION + " initialized!");
        LOGGER.info("Loading script...");

        actions.add(new LoadNecessaryClassesAction());
        actions.add(new ClassPathVerifierAction()); //force-verify the class path


        ScriptHandler scriptHandler = new ScriptHandler(scriptFile);
        scriptHandler.loadScript();

        LOGGER.info("Script loaded!");

        if(ObfGlobal.inputFile == null)
            LOGGER.fatal("No input file specified!");

        try {
            LOGGER.info("Reading input file...");
            List<JarFileEntry> jarEntries = FileIOUtils.readJARfile(ObfGlobal.inputFile, true);
            JarFileEntry manifestEntry = jarEntries.stream()
                    .filter(entry -> entry.getEntryName().equals("META-INF/MANIFEST.MF"))
                    .findFirst()
                    .orElse(null);

            //initialize manifest
            Manifest manifest = null;
            if (manifestEntry != null)
                manifest = new Manifest(new ByteArrayInputStream(manifestEntry.getOriginalData()));
            ObfGlobal.manifest = manifest;

            //filter all jar file entries into only node

            ObfGlobal.classes = jarEntries.stream()
                    .filter(JarFileEntry::isTargettingClass)
                    .map(JarFileEntry::getTargetNode)
                    .collect(Collectors.toList());
            ObfGlobal.previousClasses = new ArrayList<>(ObfGlobal.classes);
            ObfGlobal.inputJarEntries = jarEntries;

            SaveAction saveAction = actions.stream()
                    .filter(action -> action instanceof SaveAction)
                    .map(action -> (SaveAction) action)
                    .findFirst()
                    .orElse(null);

            //don't save yet
            actions.stream()
                .filter(action -> action != saveAction)
                .forEach(Action::execute);

            //Apply all remaps for name obfuscation
            ObfGlobal.remapper.apply();

            //save after remapping
            if(saveAction != null)
                saveAction.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
