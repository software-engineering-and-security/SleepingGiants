package org.ses.ginject.util;

import java.util.HashSet;
import java.util.Set;

public class ModificationLog {

    public Set<String> implementedSerializable;
    public Set<String> constantModifications;
    public int addedClassConstants = 0;
    public int addedStringConstants = 0;

    public ModificationLog() {
        this.implementedSerializable = new HashSet<>();
        this.constantModifications = new HashSet<>();
    }

    private static String quoteString(String str) {
        return String.format("\"%s\"", str);
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append("{\n");

        output.append(quoteString("inserted_serializable")).append(" : [ ");
        for (String className : implementedSerializable)
            output.append(quoteString(className)).append(",");

        output.replace(output.length() - 1, output.length(), "");

        output.append(" ],\n");
        output.append(quoteString("inserted_constants")).append(" : [ ");
        for (String className : constantModifications)
            output.append(quoteString(className)).append(",");

        output.replace(output.length() - 1, output.length(), "");


        output.append(" ],\n");

        output.append(quoteString("class_constant_cnt")).append(" : ").append(addedClassConstants).append(",");
        output.append(quoteString("string_constant_cnt")).append(" : ").append(addedStringConstants);

        output.append("\n}");
        return output.toString();
    }
}
