package me.matrix4f.classcloak.action;

import me.matrix4f.classcloak.Action;
import me.matrix4f.classcloak.mapping.MappingWriter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class SaveMappingsAction extends Action {
    @Override
    public void execute() {
        try {
            PrintStream printStream = new PrintStream(new FileOutputStream(ObfGlobal.mappingsSaveFile));

            MappingWriter writer = new MappingWriter();
            writer.write(printStream);

            printStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
