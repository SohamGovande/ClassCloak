package me.matrix4f.classcloak.script.command.commands;

import me.matrix4f.classcloak.JarFileEntry;
import me.matrix4f.classcloak.action.ObfGlobal;
import me.matrix4f.classcloak.script.command.api.Command;
import me.matrix4f.classcloak.script.parsing.CommandException;
import me.matrix4f.classcloak.util.FileIOUtils;
import me.matrix4f.classcloak.util.TimeUtils;
import me.matrix4f.classcloak.util.XMLUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static me.matrix4f.classcloak.Globals.LOGGER;

public class CommandClasspath extends Command {

    public CommandClasspath() {
        super("classpath");
    }

    @SuppressWarnings("ConstantConditions")
    private void listFiles(File dir, boolean recursive, List<File> fileList) {
        if (!recursive) {
            //noinspection ConstantConditions - already checked if file exists
            Stream.of(dir.listFiles((dir1, name) -> name.endsWith(".jar")))
                    .forEach(fileList::add);
        } else {
            File[] children = dir.listFiles(File::isFile);
            File[] directories = dir.listFiles(File::isDirectory);

            fileList.addAll(Arrays.asList(children));
            for(File directory : directories)
                listFiles(directory, true, fileList);
        }
    }

    @Override
    protected void doExecution(Element cmdElem, NodeList args) throws CommandException {
        LOGGER.info("Loading classpath...");
        List<String> places = XMLUtils.findString(this, args, "entry");
        List<File> classPathFiles = new ArrayList<>();
        for(String s : places) {
            boolean recursiveSearch = false;
            if (s.charAt(0) == '+') {
                recursiveSearch = true;
                s = s.substring(1);
            }
            s = s.trim();

            File file = new File(s);
            if (!file.exists())
                throw new CommandException(this, (file.isDirectory() ? "Directory " : "File ") + file.getAbsolutePath() + " does not exist.");

            if(file.isDirectory()) {
                listFiles(file, recursiveSearch, classPathFiles);
            } else {
                if (!file.getName().endsWith(".jar"))
                    throw new CommandException(this, "Only jar files accepted. (" + file.getAbsolutePath() + ")");
                classPathFiles.add(file);
            }
        }
        for(File jarFile : classPathFiles) {
            TimeUtils.start();
            List<JarFileEntry> entries = FileIOUtils.readJARfile(jarFile, false);
            int size = 0;
            for(JarFileEntry entry : entries) {
                if(entry.getEntryName().endsWith(".class")) {
                    ObfGlobal.classpathJarEntries.add(entry);
                    size++;
                }
            }
            LOGGER.info(": " + jarFile.getName() + " (" + size + " classes in " + TimeUtils.msPassed() + "ms)");
            System.gc();
        }
    }
}
