package org.ses.ginject.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class NamingUtils {

    public static String getFileFromClass(String className, String directoryRoot) {
        return directoryRoot + File.separator + className.replace(".", File.separator) + ".class";
    }

    public static FileInputStream getFileOutputStreamFromClass(String className, String directoryRoot) throws FileNotFoundException {
        return new FileInputStream(getFileFromClass(className, directoryRoot));
    }
}
