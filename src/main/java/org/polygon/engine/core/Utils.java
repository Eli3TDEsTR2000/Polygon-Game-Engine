package org.polygon.engine.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Utils {

    private Utils() {

    }

    // reads file and saves it into a string object
    public static String readFile(String filePath) {
        String str;
        try {
            str = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch(IOException exception) {
            throw new RuntimeException("Couldn't read file [" + filePath + "] ", exception);
        }
        return str;
    }
}
