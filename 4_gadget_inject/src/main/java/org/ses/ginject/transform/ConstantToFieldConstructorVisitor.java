package org.ses.ginject.transform;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Map;

public class ConstantToFieldConstructorVisitor extends MethodVisitor {

    Map<String, String> stringConstantToFieldNamesMap;
    Map<String, String> classConstantToFieldNamesMap;
    String owner;
    int latestVarIndex = 0;

    protected ConstantToFieldConstructorVisitor(int api, MethodVisitor methodVisitor, String owner, int latestVarIndex,
                                                Map<String, String> stringConstantToFieldNamesMap, Map<String, String> classConstantToFieldNamesMap) {
        super(api, methodVisitor);
        this.latestVarIndex = latestVarIndex;
        this.stringConstantToFieldNamesMap = stringConstantToFieldNamesMap;
        this.classConstantToFieldNamesMap = classConstantToFieldNamesMap;
        this.owner = owner;
    }
    @Override
    public void visitInsn(int opcode) {
        if (opcode == Opcodes.RETURN) {

            for (String fieldName : this.stringConstantToFieldNamesMap.values()) {
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitVarInsn(Opcodes.ALOAD, this.latestVarIndex);
                super.visitFieldInsn(Opcodes.PUTFIELD, this.owner, fieldName, "Ljava/lang/String;");
                this.latestVarIndex++;
            }
            for (String fieldName : this.classConstantToFieldNamesMap.values()) {
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitVarInsn(Opcodes.ALOAD, this.latestVarIndex);
                super.visitFieldInsn(Opcodes.PUTFIELD, this.owner, fieldName, "Ljava/lang/Class;");
                this.latestVarIndex++;
            }

        }

        super.visitInsn(opcode);
    }

}
