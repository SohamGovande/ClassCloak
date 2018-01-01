package me.matrix4f.classcloak;

import org.objectweb.asm.tree.ClassNode;
import me.matrix4f.classcloak.classreading.CstPoolReader;

public class JarFileEntry {

    private ClassNode target; //null if not found
    private CstPoolReader cstReader; //null if not found

    private String entryName;
    private byte[] originalData;

    public JarFileEntry(ClassNode target, String entryName, byte[] originalData) {
        this.target = target;
        this.entryName = entryName;
        this.originalData = originalData;
        if(target != null)
            cstReader = new CstPoolReader(originalData);
    }

    public boolean isTargettingClass() {
        return target != null;
    }

    public ClassNode getTargetNode() {
        return target;
    }

    public String getEntryName() {
        return entryName;
    }

    public byte[] getOriginalData() {
        return originalData;
    }

    public CstPoolReader getCstReader() {
        return cstReader;
    }

    public void setCstReader(CstPoolReader cstReader) {
        this.cstReader = cstReader;
    }

    public void setTargetNode(ClassNode target) {
        this.target = target;
    }
}
