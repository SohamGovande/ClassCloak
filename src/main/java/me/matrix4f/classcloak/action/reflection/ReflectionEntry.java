package me.matrix4f.classcloak.action.reflection;

import me.matrix4f.classcloak.target.MethodNodeTarget;

import java.util.List;

public class ReflectionEntry {

    private List<MethodNodeTarget> from;
    private ReflectionMethodMap methodMap;

    public ReflectionEntry(List<MethodNodeTarget> from, ReflectionMethodMap methodMap) {
        this.from = from;
        this.methodMap = methodMap;
    }

    public List<MethodNodeTarget> getFrom() {
        return from;
    }

    public ReflectionMethodMap getMethodMap() {
        return methodMap;
    }
}
