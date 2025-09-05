package org.ses.ginject;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.common.stmt.AbstractDefinitionStmt;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.model.SourceType;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.Type;
import sootup.core.views.View;
import sootup.java.bytecode.inputlocation.ArchiveBasedAnalysisInputLocation;
import sootup.java.bytecode.inputlocation.JrtFileSystemAnalysisInputLocation;
import sootup.java.core.views.JavaView;

import java.io.*;
import java.util.*;

public class FindInterfacesMain {

    private static final Logger logger = LogManager.getLogger(FindInterfacesMain.class.getName());

    public static void main(String[] args) throws FileNotFoundException {

        if (args.length < 1) {
            logger.error("No input JAR file defined");
            System.exit(1);
        }

        PrintStream outputWriter = System.out;

        if (args.length == 2) {
            outputWriter = new PrintStream(new FileOutputStream(args[1]));
        }

        File inputFile = new File(args[0]);

        Map<MethodSignature, Integer> implementedInterfaces = new HashMap<>();
        Set<SootClass> serializableClasses = new HashSet<>();
        View jclView = new JavaView(new JrtFileSystemAnalysisInputLocation());
        View view = new JavaView(new ArchiveBasedAnalysisInputLocation(inputFile.toPath(), SourceType.Library));

        view.getClasses().forEach(sootClass -> {
            view.getTypeHierarchy().implementedInterfacesOf(sootClass.getType()).forEach(classType -> {
                if (classType.equals(view.getIdentifierFactory().getClassType("java.io.Serializable"))) {
                    serializableClasses.add(sootClass);
                }
            });
        });

        for (SootClass serializableClass : serializableClasses) {
            view.getTypeHierarchy().implementedInterfacesOf(serializableClass.getType()).forEach(classType -> {
                String packageBase = classType.getPackageName().getName().split("\\.")[0];
                if (packageBase.equals("java") || packageBase.equals("javax")) {

                    SootClass jclIface = jclView.getClass(classType).orElse(null);
                    if (jclIface != null) {
                        for (SootMethod interfaceMethod : jclIface.getMethods()) {
                            if (interfaceMethod.isAbstract())
                                implementedInterfaces.put(interfaceMethod.getSignature(),
                                        implementedInterfaces.getOrDefault(interfaceMethod.getSignature(), 0) + 1);
                        }
                    }
                }
            });
        }

        printImplementationLog(implementedInterfaces);
        printJavaSource(implementedInterfaces.keySet(), outputWriter);
    }

    public static void printImplementationLog(Map<MethodSignature, Integer> implementedInterfaceMethods) {

        Map<ClassType, Integer> implementedInterfaces = new HashMap<>();
        int interfaceMethodCnt = 0;

        for (MethodSignature signature : implementedInterfaceMethods.keySet()) {
            ClassType classType = signature.getDeclClassType();
            if (!implementedInterfaces.containsKey(classType))
                implementedInterfaces.put(classType, implementedInterfaceMethods.get(signature));

            interfaceMethodCnt++;
        }

        System.out.println("{");
        for (ClassType classType : implementedInterfaces.keySet()) {
            System.out.printf("\"%s\" : %d,\n", classType.getFullyQualifiedName(), implementedInterfaces.get(classType));
        }
        System.out.printf("\"%s\" : %d\n", "method_cnt", interfaceMethodCnt);
        System.out.println("}");

    }

    public static void printJavaSource(Set<MethodSignature> interfaceMethods, PrintStream writer) {

        Map<String, String> properties = new HashMap<>();

        for (MethodSignature signature : interfaceMethods) {
            String classType = signature.getDeclClassType().getFullyQualifiedName();

            if (!properties.containsKey(classType)) {
                properties.put(classType.replace("$", "."), "_" + signature.getDeclClassType().getClassName().toLowerCase());
            }

            HashMap<String, Integer> duplicateContainedParams = new HashMap<>();

            for (Type parameterType : signature.getParameterTypes()) {
                String key = parameterType.toString().replace("$", ".");
                if (!properties.containsKey(key)) {
                    // special case because naming conflict with java.lang.Boolean
                    if (parameterType instanceof PrimitiveType.BooleanType) {
                        properties.put(key, "_bool");
                    } else {
                        String propName = "_" + key.split("\\.")[key.split("\\.").length - 1].toLowerCase().replace("[]", "_arrary");
                        properties.put(key, propName);
                    }
                }
                duplicateContainedParams.put(key, duplicateContainedParams.getOrDefault(key, 0) + 1);
            }



            // update duplicateProp index in properties map to highest number:
            for (String duplicateProp : duplicateContainedParams.keySet()) {
                if (duplicateContainedParams.get(duplicateProp) > 1) {
                    String propName = properties.get(duplicateProp);
                    if (propName.contains("##")) {
                        int currentPropMax = Integer.parseInt(propName.split("##")[1]);
                        int newPropMax = duplicateContainedParams.get(duplicateProp);
                        if (newPropMax > currentPropMax) {
                            properties.put(duplicateProp, propName.split("##")[0] + "##" + duplicateContainedParams.get(duplicateProp));
                        }

                    } else {
                        properties.put(duplicateProp, propName + "##" + duplicateContainedParams.get(duplicateProp));
                    }
                }
            }
        }

        writer.println("package ses.ginject.pattern;");
        writer.println("import java.io.Serializable;");
        writer.println("import java.io.IOException;");
        writer.println("import java.io.ObjectInputStream;\n");
        writer.println("public class Caller implements Serializable {");

        for (String property : properties.keySet()) {
            if (properties.get(property).contains("##")) {
                String[] pSplit = properties.get(property).split("##");
                for (int i = 0; i < Integer.parseInt(pSplit[1]); i++) {
                    if (i > 0) writer.println("\tpublic " + property + " " + pSplit[0] + i + ";");
                    else writer.println("\tpublic " + property + " " + pSplit[0] + ";");
                }

            } else {
                writer.println("\tpublic " + property + " " + properties.get(property) + ";");
            }
        }
        writer.println();

        StringBuilder constructor = new StringBuilder();
        constructor.append("\tpublic Caller ( ");
        for (String property : properties.keySet()) {
            if (properties.get(property).contains("##")) {
                String[] pSplit = properties.get(property).split("##");
                for (int i = 0; i < Integer.parseInt(pSplit[1]); i++) {
                    String propertyName = pSplit[0];
                    if (i > 0) propertyName += i;
                    constructor.append(String.format(" %s %s,", property, propertyName));
                }
            } else {
                constructor.append(String.format(" %s %s,", property, properties.get(property)));
            }
        }
        constructor.replace(constructor.length() - 1, constructor.length(), "");
        constructor.append(") {");
        writer.println(constructor);
        for (String property : properties.keySet()) {
            if (properties.get(property).contains("##")) {
                String[] pSplit = properties.get(property).split("##");
                for (int i = 0; i < Integer.parseInt(pSplit[1]); i++) {
                    String propertyName = pSplit[0];
                    if (i > 0) propertyName += i;
                    writer.println("\t\tthis. " + propertyName + " = " + propertyName + ";");
                }

            } else {
                writer.println("\t\tthis." + properties.get(property) + " = " + properties.get(property) + ";");
            }
        }


        writer.println("\t}");

        writer.println();
        writer.println("\tprivate void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {");
        writer.println("\t\ts.defaultReadObject();");
        writer.println("\t\tthis.hashCode();");
        writer.println("\t}");

        writer.println("\t@Override");
        writer.println("\tpublic int hashCode() {");

        for (MethodSignature signature : interfaceMethods) {

            HashMap<String, Integer> dupParams = new HashMap<>();

            String propertyName = properties.get(signature.getDeclClassType().getFullyQualifiedName().replace("$", "."));
            dupParams.put(signature.getDeclClassType().getFullyQualifiedName(), 1);

            StringBuilder call = new StringBuilder();
            call.append("try { ");
            call.append(String.format("%s.%s(", propertyName, signature.getSubSignature().getName()));

            for (Type param : signature.getParameterTypes()) {

                String sanitizedKey = param.toString().replace("$", ".");

                String paramString = properties.get(sanitizedKey).split("##")[0];
                if (dupParams.containsKey(paramString))
                    paramString += dupParams.get(paramString);

                call.append(paramString).append(",");
                dupParams.put(properties.get(sanitizedKey).split("##")[0],
                        dupParams.getOrDefault(properties.get(sanitizedKey).split("##")[0], 0) + 1);

            }
            if (!signature.getParameterTypes().isEmpty())
                call.replace(call.length() - 1, call.length(), "");

            call.append("); } catch (Throwable ignored) {}");
            writer.println("\t\t" + call);

        }

        writer.println("\t\treturn 0;");
        writer.println("\t}");

        writer.println("}");




    }


}
