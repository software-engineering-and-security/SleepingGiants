package org.ses.ginject.transform;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import ses.ginject.util.ModificationLog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConstantToFieldClassVisitor extends ClassVisitor {

    private static final Logger logger = LogManager.getLogger(ConstantToFieldClassVisitor.class.getName());

    String methodName;
    String className;
    int api;
    Map<String, String> stringConstantToFieldNamesMap;
    Map<String, String> classConstantToFieldNamesMap;
    Set<String> currentFieldNames;
    public int constCount = 0;
    ClassVisitor cv;

    public boolean addConstructor = false;


    public ModificationLog modificationLog;

    public ConstantToFieldClassVisitor(int api, ClassVisitor cv, String methodName, String className, ModificationLog modificationLog) {
        super(api, cv);
        this.api = api;
        this.cv = cv;
        // sanitize <init>
        this.methodName = methodName.replace("<", "__").replace(">", "__");
        this.className = className.replace(".", "/");
        this.stringConstantToFieldNamesMap = new HashMap<>();
        this.classConstantToFieldNamesMap = new HashMap<>();
        this.currentFieldNames = new HashSet<>();
        this.modificationLog = modificationLog;
    }

    public void addStringField(String constantValue) {
        stringConstantToFieldNamesMap.put(constantValue, "constinject_" + this.methodName + "_" + constCount);
        constCount++;
    }

    public void addClassField(String constantValue) {
        if (!classConstantToFieldNamesMap.containsKey(constantValue)) {
            classConstantToFieldNamesMap.put(constantValue, "constinject_" + this.methodName + "_" + constCount);
            constCount++;
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature, String[] exceptions) {
        if (methodName.equals("<init>") && this.addConstructor) {

            if (stringConstantToFieldNamesMap.isEmpty() && classConstantToFieldNamesMap.isEmpty())
                return super.visitMethod(access, methodName, descriptor, signature, exceptions);

            StringBuilder args = new StringBuilder();

            for (int i = 0; i < stringConstantToFieldNamesMap.size(); i++)
                args.append("Ljava/lang/String;");
            for (int i = 0; i < classConstantToFieldNamesMap.size(); i++)
                args.append("Ljava/lang/Class;");

            String[] signatureSplit = signature.split("\\)");
            String[] descriptorSplit = descriptor.split("\\)");

            signatureSplit[0] += args.toString();
            descriptorSplit[0] += args.toString();
            String newSignature = String.join(")", signatureSplit);
            String newDescriptor = String.join(")", descriptorSplit);

            System.out.println(descriptor);
            int originalArgCount = descriptorSplit[0].split(";").length - 1;

            return new ConstantToFieldConstructorVisitor(this.api, super.visitMethod(access, methodName, newDescriptor, newSignature, exceptions),
                    this.className, originalArgCount, this.stringConstantToFieldNamesMap, this.classConstantToFieldNamesMap);
        }

        if (methodName.equals(this.methodName))
            return new ConstantToFieldMethodVisitor(this.api, super.visitMethod(access, methodName, descriptor, signature, exceptions),
                    this.className, stringConstantToFieldNamesMap, classConstantToFieldNamesMap);
        return super.visitMethod(access, methodName, descriptor, signature, exceptions);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        currentFieldNames.add(name);
        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public void visitEnd() {
        for (String stringConstant : stringConstantToFieldNamesMap.keySet()) {
            String newFieldName = stringConstantToFieldNamesMap.get(stringConstant);
            if (!currentFieldNames.contains(newFieldName)) {
                logger.info("Inserting String field " + newFieldName  + " into " + this.className);
                modificationLog.addedStringConstants++;
                modificationLog.constantModifications.add(this.className);

                FieldVisitor fv = cv.visitField(Opcodes.ACC_PUBLIC , newFieldName , ConstantToFieldMethodVisitor.STRING_TYPE , null, null);

                if (fv != null) fv.visitEnd();
            }
        }
        for (String classConstant : classConstantToFieldNamesMap.keySet()) {
            String newFieldName = classConstantToFieldNamesMap.get(classConstant);
            if (!currentFieldNames.contains(newFieldName)) {
                logger.info("Inserting Class field " + newFieldName  + " into " + this.className);
                modificationLog.addedClassConstants++;
                modificationLog.constantModifications.add(this.className);

                FieldVisitor fv = cv.visitField(Opcodes.ACC_PUBLIC , newFieldName , ConstantToFieldMethodVisitor.CLASS_TYPE , null, null);
                if (fv != null) fv.visitEnd();
            }
        }

        super.visitEnd();
    }
}
