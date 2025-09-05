package org.ses.ginject.transform;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import ses.ginject.Main;

import java.util.Map;

public class ConstantToFieldMethodVisitor extends MethodVisitor {

    public static final String STRING_TYPE = "Ljava/lang/String;";
    public static final String CLASS_TYPE = "Ljava/lang/Class;";
    private static final Logger logger = LogManager.getLogger(Main.class.getName());

    Map<String, String> stringConstantToFieldNamesMap;
    Map<String, String> classConstantToFieldNamesMap;
    String owner;


    public ConstantToFieldMethodVisitor(int api, MethodVisitor methodVisitor, String owner,
                                        Map<String, String> stringConstantToFieldNamesMap, Map<String, String> classConstantToFieldNamesMap) {
        super(api, methodVisitor);
        this.stringConstantToFieldNamesMap = stringConstantToFieldNamesMap;
        this.classConstantToFieldNamesMap = classConstantToFieldNamesMap;
        this.owner = owner;
    }

    @Override
    public void visitLdcInsn(Object value) {
        String fieldName = null;
        String type = "";

        if (value instanceof String){
            fieldName = this.stringConstantToFieldNamesMap.get(value);
            type = STRING_TYPE;
        }
        else if (value instanceof Type)  {
            fieldName = this.classConstantToFieldNamesMap.get(value.toString());
            type = CLASS_TYPE;
        }
        if (fieldName != null) {
           logger.info("Replacing LDC [" + value + "] with ALOAD and GETFIELD");
            // load this onto the stack
            super.visitVarInsn(Opcodes.ALOAD, 0);
            super.visitFieldInsn(Opcodes.GETFIELD, this.owner, fieldName, type);
            return;
        }
        super.visitLdcInsn(value);
    }
}
