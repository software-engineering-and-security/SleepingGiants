package org.ses.ginject;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.ses.ginject.analysis.MethodConstantMapping;
import org.ses.ginject.pattern.Caller;
import org.ses.ginject.transform.AddSerializableClassVisitor;
import org.ses.ginject.transform.ConstantToFieldClassVisitor;
import org.ses.ginject.util.JarUtils;
import org.ses.ginject.util.ModificationLog;
import org.ses.ginject.util.NamingUtils;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.common.constant.ClassConstant;
import sootup.core.jimple.common.constant.StringConstant;
import sootup.core.jimple.common.expr.AbstractBinopExpr;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.stmt.AbstractDefinitionStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.SootMethod;
import sootup.core.model.SourceType;
import sootup.core.views.View;
import sootup.java.bytecode.inputlocation.ArchiveBasedAnalysisInputLocation;
import sootup.java.core.views.JavaView;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class.getName());

    public static File InterfaceCallerClassFile;
    public static final File InterfaceCallerCopyDestination = new File("tmp" + File.separator
            + Caller.class.getName().replace(".", File.separator) + ".class");

    public static final String ALL_MODIFICATIONS = "all";
    public static final String SERIALIZABLE_MOD = "ser";
    public static final String PRIVATE_FINAL_MOD = "final";
    public static final String TRAMPOLINE_MOD = "trampoline";

    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            logger.error("No input JAR file defined");
            System.exit(1);
        }

        String modification = ALL_MODIFICATIONS;
        if (args.length >= 2) modification = args[1];



        if (modification.equals(TRAMPOLINE_MOD)) {
            if (args.length < 3) {
                logger.error("Trampoline mod without defining trampoline file");
                System.exit(1);
            }
            Main.InterfaceCallerClassFile = new File(args[2]);
        }


        File inputFile = new File(args[0]);
        System.out.println(inputFile);
        List<MethodConstantMapping> constantMappings = new ArrayList<>();
        List<String> classesToModify = new ArrayList<>();

        // SootUp analysis here:
        View view = new JavaView(new ArchiveBasedAnalysisInputLocation(inputFile.toPath(), SourceType.Library));
        view.getClasses().forEach(sootClass -> {

            if (sootClass.isInterface() || sootClass.isAbstract()) {
                classesToModify.add(sootClass.getType().getFullyQualifiedName());
            }
            try {
                for (SootMethod method : sootClass.getMethods()) {
                    if (!method.isStatic() && method.isConcrete()) {

                        MethodConstantMapping mapping = new MethodConstantMapping(method.getSignature());
                        for (Stmt stmt : method.getBody().getStmts()) {
                            addMappings(stmt ,mapping);
                        }

                        if (!mapping.stringConstValues.isEmpty() || !mapping.classConstValues.isEmpty())
                            constantMappings.add(mapping);

                    }
                }
            } catch (Exception e) {
                logger.warn("Soot Exception in " + sootClass);
            }

        });
        //---------

        ModificationLog modificationLog = new ModificationLog();

        try {
            JarUtils.extractJar(args[0], "tmp");
            // --------- ASM modifiers

            if (modification.equals(ALL_MODIFICATIONS) || modification.equals(SERIALIZABLE_MOD)) {
                for (String className : classesToModify) {

                    try {
                        ClassReader reader = new ClassReader(NamingUtils.getFileOutputStreamFromClass(className, "tmp"));
                        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                        AddSerializableClassVisitor visitor = new AddSerializableClassVisitor(Opcodes.ASM9, writer);
                        reader.accept(visitor, 0);

                        FileOutputStream out = new FileOutputStream(NamingUtils.getFileFromClass(className, "tmp"));
                        out.write(writer.toByteArray());
                        out.flush();

                        if (visitor.addedSerializable)
                            modificationLog.implementedSerializable.add(className);
                    } catch (Exception e) {logger.warn("Failed to add Serializable interface to " + className);}
                }
            }

            if (modification.equals(ALL_MODIFICATIONS) || modification.equals(PRIVATE_FINAL_MOD)) {
                for (MethodConstantMapping mapping : constantMappings) {
                    try {
                        String className = mapping.methodSignature.getDeclClassType().getFullyQualifiedName();

                        ClassReader reader = new ClassReader(NamingUtils.getFileOutputStreamFromClass(className, "tmp"));
                        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

                        ConstantToFieldClassVisitor constantVisitor = new ConstantToFieldClassVisitor(
                                Opcodes.ASM9, writer, mapping.methodSignature.getName(), className, modificationLog);

                        if (args.length >= 3 && args[2].equals("crystallizer")) constantVisitor.addConstructor = true;

                        for (String fieldName : mapping.stringConstValues)
                            constantVisitor.addStringField(fieldName);
                        for (String fieldName : mapping.classConstValues)
                            constantVisitor.addClassField(fieldName);

                        reader.accept(constantVisitor, 0);
                        FileOutputStream out = new FileOutputStream(NamingUtils.getFileFromClass(className, "tmp"));
                        out.write(writer.toByteArray());
                        out.flush();

                    } catch (Throwable e) {
                        logger.warn("Failed to extract constants from " + mapping.methodSignature.getName());
                    }
                }
            }

            if (modification.equals(TRAMPOLINE_MOD))
                FileUtils.copyFile(InterfaceCallerClassFile, InterfaceCallerCopyDestination);

            //-------------

            String modifiedName = args[0].replace(".jar", "") + "-modified" + ".jar";
            JarUtils.packJar(modifiedName, "tmp");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(modificationLog.toString());
    }

    public static void addMappings(Stmt stmt, MethodConstantMapping mapping) {
        if (stmt instanceof AbstractDefinitionStmt) {
            AbstractDefinitionStmt definitionStmt = (AbstractDefinitionStmt) stmt;
            if (definitionStmt.getRightOp() instanceof StringConstant) {
                StringConstant constant = (StringConstant) definitionStmt.getRightOp();
                mapping.addStringConstant(constant.getValue());
            } else if (definitionStmt.getRightOp() instanceof ClassConstant) {
                ClassConstant constant = (ClassConstant) definitionStmt.getRightOp();
                mapping.addClassConstant(constant.getValue());
            } else if (definitionStmt.getRightOp() instanceof AbstractInvokeExpr) {
                AbstractInvokeExpr invokeExpr = (AbstractInvokeExpr) definitionStmt.getRightOp();
                for (Immediate arg : invokeExpr.getArgs() ) {
                    if (arg instanceof StringConstant)
                        mapping.addStringConstant(((StringConstant) arg).getValue());
                    else if (arg instanceof ClassConstant)
                        mapping.addStringConstant(((ClassConstant) arg).getValue());
                }
            } else if (definitionStmt.getRightOp() instanceof AbstractBinopExpr) {
                AbstractBinopExpr binopExpr = (AbstractBinopExpr) definitionStmt.getRightOp();
                if (binopExpr.getOp1() instanceof StringConstant)
                    mapping.addStringConstant(((StringConstant) binopExpr.getOp1()).getValue());
                if (binopExpr.getOp2() instanceof StringConstant)
                    mapping.addStringConstant(((StringConstant) binopExpr.getOp2()).getValue());
            }
        }
        else if (stmt.containsInvokeExpr()) {
            AbstractInvokeExpr invokeExpr = stmt.getInvokeExpr();
            for (Immediate arg : invokeExpr.getArgs() ) {
                if (arg instanceof StringConstant)
                    mapping.addStringConstant(((StringConstant) arg).getValue());
                else if (arg instanceof ClassConstant)
                    mapping.addStringConstant(((ClassConstant) arg).getValue());

            }
        }
    }


}