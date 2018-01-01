package me.matrix4f.classcloak.util;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import me.matrix4f.classcloak.JarFileEntry;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class FileIOUtils {

    public static List<String> readLines(String file) throws IOException {
        return Arrays.stream(new String(IOUtils.toByteArray(new FileInputStream(file)))
                .replace("\r","\n")
                .split("\n"))
                .filter(str -> !str.trim().isEmpty())
                .collect(Collectors.toList());
    }

    public static List<JarFileEntry> readJARfile(File jar, boolean parseClasses) {
        List<JarFileEntry> entries = new ArrayList<>();
        try {
            JarFile jarFile = new JarFile(jar);
            List<String> names = jarFile.stream()
                    .map(ZipEntry::getName)
                    .collect(Collectors.toList());

            //separately handle the manifest because for some reason it outputs the wrong thing
            JarEntry manifest = jarFile.getJarEntry("META-INF/MANIFEST.MF");
            List<InputStream> streams = new ArrayList<>();
            if(manifest != null) {
                streams.add(jarFile.getInputStream(manifest));
                entries.add(new JarFileEntry(null,"META-INF/MANIFEST.MF", IOUtils.toByteArray(streams.get(streams.size()-1))));
            }

            List<String> processedNames = new ArrayList<>();
            for(String name : names.stream().filter(s->s.endsWith(".class")).collect(Collectors.toList())) { //process classes first
                streams.add(jarFile.getInputStream(jarFile.getJarEntry(name)));
                byte[] bytes = IOUtils.toByteArray(streams.get(streams.size()-1));

                ClassNode node = null;
                if(parseClasses) {
                    node = new ClassNode();
                    ClassReader classReader = new ClassReader(bytes);
                    classReader.accept(node, 0);
                }

                entries.add(new JarFileEntry(node, name, bytes));
                processedNames.add(name);
            }

            for(String name : names.stream()
                    .filter(s->!processedNames.contains(s))
                    .filter(s->!s.equals("META-INF/MANIFEST.MF")) //we already handled the manifest
                    .collect(Collectors.toList())) {
                streams.add(jarFile.getInputStream(jarFile.getJarEntry(name)));
                byte[] bytes = IOUtils.toByteArray(streams.get(streams.size()-1));
                entries.add(new JarFileEntry(null, name, bytes));
            }

            for(InputStream stream : streams)
                stream.close();
            jarFile.close();

            System.gc();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return entries;
    }

    public static void writeToJAR(List<JarFileEntry> jarEntries, File jar) {
        try {
            JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar));
            for(JarFileEntry entry : jarEntries) {
                byte[] output;
                String writePath;

                if(entry.isTargettingClass()) {
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                    entry.getTargetNode().accept(writer);
                    output = writer.toByteArray();
                    writePath = entry.getTargetNode().name + ".class";
                } else {
                    output = entry.getOriginalData();
                    writePath = entry.getEntryName();
                }
                JarEntry nextEntry = new JarEntry(writePath);
                jos.putNextEntry(nextEntry);
                jos.write(output);
                jos.closeEntry();
            }
            jos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
