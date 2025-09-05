package org.ses.ginject.pattern;

import sootup.core.views.View;
import sootup.java.bytecode.inputlocation.JrtFileSystemAnalysisInputLocation;
import sootup.java.core.views.JavaView;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

public class Example implements Serializable {

    public String str;
    public Class clazz;

    public Example() {}

    public Example(String str, Class clazz) {
        this.str = str;
        this.clazz = clazz;
    }

    public static void main(String args[]) throws IOException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        View view = new JavaView(new JrtFileSystemAnalysisInputLocation());

        view.getTypeHierarchy().implementersOf(view.getIdentifierFactory().getClassType("javax.sql.DataSource")).forEach(sootClass -> {
            System.out.println(sootClass);
        });

    }

    public void method() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method m = Example.class.getMethod("method2");
        m.invoke(new Example());
    }

    public void method2() {}

}
