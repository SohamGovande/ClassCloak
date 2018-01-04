package me.matrix4f.classcloak.action;

import me.matrix4f.classcloak.Action;
import me.matrix4f.classcloak.JarFileEntry;
import me.matrix4f.classcloak.util.FileIOUtils;

import static me.matrix4f.classcloak.Globals.LOGGER;

public class SaveAction extends Action {
    @Override
    public void execute() {
        if(ObfGlobal.outputFile.exists())
            ObfGlobal.outputFile.delete();

        LOGGER.info("Saving all classes...");

        //add any new classes that were created
        ObfGlobal.sourceClasses.stream()
                .filter(node -> !ObfGlobal.previousClasses.contains(node))
                .map(node -> new JarFileEntry(node, node.name, null))
                .forEach(ObfGlobal.inputJarEntries::add);

        FileIOUtils.writeToJAR(ObfGlobal.inputJarEntries, ObfGlobal.outputFile);

        LOGGER.info("Save finished.");
    }
}
