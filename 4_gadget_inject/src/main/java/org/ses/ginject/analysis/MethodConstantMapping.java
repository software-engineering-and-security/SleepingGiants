package org.ses.ginject.analysis;

import sootup.core.signatures.MethodSignature;

import java.util.ArrayList;
import java.util.List;

public class MethodConstantMapping {

    public MethodSignature methodSignature;
    public List<String> stringConstValues;
    public List<String> classConstValues;

    public MethodConstantMapping(MethodSignature signature) {
        this.methodSignature = signature;
        this.stringConstValues = new ArrayList<>();
        this.classConstValues = new ArrayList<>();
    }

    public void addStringConstant(String stringConstant) {this.stringConstValues.add(stringConstant);}
    public void addClassConstant(String classConstant) {this.classConstValues.add(classConstant);}



}
