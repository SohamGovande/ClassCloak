package me.matrix4f.classcloak.script.parsing;

public class InvalidArgTypeException extends CommandException {

    public InvalidArgTypeException(int lineNumber, Class expected, Class actual) {
        super("Unexpected token: Line " + lineNumber + " | Expected: " + expected.getSimpleName() + " | Actual: " + actual);
    }
}
