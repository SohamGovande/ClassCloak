package me.matrix4f.classcloak.mapping;

import me.matrix4f.classcloak.Globals;
import me.matrix4f.classcloak.action.name.map.ClassReference;
import me.matrix4f.classcloak.action.name.map.FieldReference;
import me.matrix4f.classcloak.action.name.map.MethodReference;
import me.matrix4f.classcloak.action.name.map.NameObfMap;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static me.matrix4f.classcloak.util.BytecodeUtils.*;

public class MappingWriter {
    private String format(String oldName, String newName) {
        if(oldName.equals(newName))
            return oldName + " -> ~Same~";
        return oldName + " -> " + newName;
    }

    public void write(PrintStream writer) {
        writer.println("//" + Globals.NAME + " " + Globals.VERSION + " Mappings File");
        writer.println();

        for(ClassReference classRef : NameObfMap.Classes.values()) {
            writer.println(format(classRef.oldName.replace('/','.'), classRef.newName.replace('/','.')));

            List<FieldReference> fieldRefs = NameObfMap.Fields.values()
                    .stream()
                    .filter(fr -> fr.parent == classRef).collect(Collectors.toList());

            if(fieldRefs.size() > 0)
                writer.println("  Fields:");

            fieldRefs.forEach(fieldRef ->
                    writer.println("    " + format(fieldRef.oldToString(), fieldRef.newToString())));

            List<MethodReference> methodRefs = NameObfMap.Methods.values()
                    .stream()
                    .filter(mr -> mr.parent == classRef)
                    .sorted(Comparator.comparing(o -> o.oldName))
                    .collect(Collectors.toList());
            if(methodRefs.size() > 0)
                writer.println("  Methods:");

            methodRefs.forEach(methodRef ->
                    writer.println("    " + format(methodRef.oldToString(), methodRef.newToString())));

            writer.println();
        }
    }
}
