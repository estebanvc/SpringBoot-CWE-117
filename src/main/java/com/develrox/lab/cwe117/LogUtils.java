package com.develrox.lab.cwe117;

public class LogUtils {
    public static String escapeLogInput(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("\n", "\\\\n")
                .replaceAll("\r", "\\\\r")
                .replaceAll("\t", "\\\\t");
    }
}
