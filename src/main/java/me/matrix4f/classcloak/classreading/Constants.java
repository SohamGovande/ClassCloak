package me.matrix4f.classcloak.classreading;

public interface Constants {

    byte CONSTANT_Utf8 = 1,
        CONSTANT_Integer = 3,
        CONSTANT_Float = 4,
        CONSTANT_Long = 5,
        CONSTANT_Double = 6,
        CONSTANT_Class = 7,
        CONSTANT_String = 8,
        CONSTANT_Fieldref = 9,
        CONSTANT_Methodref = 10,
        CONSTANT_InterfaceMethodref = 11,
        CONSTANT_NameAndType = 12,
        CONSTANT_MethodHandle = 15,
        CONSTANT_MethodType = 16,
        CONSTANT_InvokeDynamic = 18,
        //java 9
        CONSTANT_Module = 19,
        CONSTANT_Package = 20;
}
