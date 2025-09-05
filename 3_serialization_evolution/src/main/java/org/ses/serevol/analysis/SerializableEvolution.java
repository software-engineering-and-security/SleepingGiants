package org.ses.serevol.analysis;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import sootup.core.model.SootClass;
import sootup.core.model.SourceType;
import sootup.core.signatures.MethodSignature;
import sootup.core.signatures.MethodSubSignature;
import sootup.core.signatures.PackageName;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.VoidType;
import sootup.core.views.View;
import sootup.java.bytecode.frontend.inputlocation.ArchiveBasedAnalysisInputLocation;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;

import java.io.File;
import java.io.IOException;
import java.util.*;
public class SerializableEvolution {


    private static final Logger logger = LogManager.getLogger(SerializableEvolution.class.getName());





    private static void updatedMagicMethodMap(Map<MethodSubSignature, Integer> magicMethodMap,  View view) {
        for (MethodSubSignature signature: magicMethodMap.keySet())
            magicMethodMap.put(signature, getMagicMethodCount(view, signature));

    }


    public static boolean getEvolution(File versionOldJar, File versionNewJar) throws IOException {

        boolean sootCouldNotFindSerializable = false;

        Map<ClassType, Set<ChangeEvent>> evolution = new HashMap<>();
        long serializableClassCnt = 0;


        View viewOld = new JavaView(new JavaClassPathAnalysisInputLocation(versionOldJar.getPath(), SourceType.Library));

        View viewNew = null;
        if (versionNewJar != null)
            viewNew = new JavaView(new JavaClassPathAnalysisInputLocation(versionNewJar.getPath(), SourceType.Library));

        try {
            serializableClassCnt = getSerializableCount(viewOld);
            if (viewNew != null) {
                SerializableEvolution.getSerializableChanges(viewOld, viewNew, evolution);
                Map<ChangeEvent.ChangeType, Integer> changeEventCntMap = new HashMap<>();
                for (ClassType classType : evolution.keySet()) {
                    for (ChangeEvent event : evolution.get(classType) ) {
                        changeEventCntMap.put(event.changeType, changeEventCntMap.getOrDefault(event.changeType, 0) + 1);
                    }
                }
                for (ChangeEvent.ChangeType changeType : changeEventCntMap.keySet()) {
                    System.out.println(changeType + ": " + changeEventCntMap.get(changeType));
                }
            }

            System.out.println("SERIALIZABLE_CNT: " + serializableClassCnt);


        } catch (Exception e) {
            if (e.getMessage().contains("java.io.Serializable")) sootCouldNotFindSerializable = true;
            logger.warn(e.getMessage());
            e.printStackTrace();
        }

        return sootCouldNotFindSerializable;
    }

    private static long getSerializableCount(View view) {
        // sometimes the same class exists twice in the view, we need to alleviate this by avoiding duplicates in a set.
        Set<ClassType> serializableClasses = new HashSet<>();
        ClassType serializableClassType = view.getIdentifierFactory().getClassType("java.io.Serializable");

        view.getTypeHierarchy().implementersOf(serializableClassType).forEach(serializableClasses::add);
        return serializableClasses.size();
    }

    private static int getMagicMethodCount(View view, MethodSubSignature subSignature) {
        ClassType serializableClassType = view.getIdentifierFactory().getClassType("java.io.Serializable");
        Set<ClassType> implementingClasses = new HashSet<>();

        view.getTypeHierarchy().implementersOf(serializableClassType).forEach(classType -> {
            MethodSignature signature = view.getIdentifierFactory().getMethodSignature(classType, subSignature);
            if (view.getMethod(signature).isPresent()) implementingClasses.add(classType);
        });

        return implementingClasses.size();
    }

    private static void getSerializableChanges(View viewOld, View viewNew, Map<ClassType, Set<ChangeEvent>> evolution) {
        ClassType serializableClassTypeOld = viewOld.getIdentifierFactory().getClassType("java.io.Serializable");
        ClassType serializableClassTypeNew = viewOld.getIdentifierFactory().getClassType("java.io.Serializable");

        viewOld.getTypeHierarchy().implementersOf(serializableClassTypeOld).forEach(classType -> {
            if (!evolution.containsKey(classType)) evolution.put(classType, new HashSet<>());
            SootClass classNew = viewNew.getClass(classType).orElse(null);
            if (classNew == null) {
                evolution.get(classType).add(new ChangeEvent(ChangeEvent.ChangeType.REMOVE_CLASS));
            } else if (!viewNew.getTypeHierarchy().isSubtype(serializableClassTypeNew, classType)) {
                SootClass classOld = viewOld.getClass(classType).orElse(null);
                if (classOld != null && !classOld.implementsInterface(serializableClassTypeOld)) {
                    // figure out where the serializable interface is comming from ...
                    ChangeEvent changeEvent = new ChangeEvent(ChangeEvent.ChangeType.REMOVE_SERIALIZABLE_INDIRECT);
                    changeEvent.serializableProvider = getSerializableProvider(classType, viewOld);

                    evolution.get(classType).add(changeEvent);
                }
                else
                    evolution.get(classType).add(new ChangeEvent(ChangeEvent.ChangeType.REMOVE_SERIALIZABLE));
            }
        });

        viewNew.getTypeHierarchy().implementersOf(serializableClassTypeNew).forEach(classType -> {
            if (!evolution.containsKey(classType)) evolution.put(classType, new HashSet<>());

            SootClass classOld = viewOld.getClass(classType).orElse(null);
            if (classOld == null) {
                evolution.get(classType).add(new ChangeEvent(ChangeEvent.ChangeType.ADD_CLASS));
            } else if (!viewOld.getTypeHierarchy().isSubtype(serializableClassTypeOld, classType)) {
                SootClass classNew = viewNew.getClass(classType).orElse(null);
                if (classNew != null && !classNew.implementsInterface(serializableClassTypeNew)) {

                    // figure out where the serializable interface is comming from ...
                    ChangeEvent changeEvent = new ChangeEvent(ChangeEvent.ChangeType.ADD_SERIALIZABLE_INDIRECT);
                    changeEvent.serializableProvider = getSerializableProvider(classType, viewNew);

                    evolution.get(classType).add(changeEvent);
                }

                else
                    evolution.get(classType).add(new ChangeEvent(ChangeEvent.ChangeType.ADD_SERIALIZABLE));
            }
        });
    }

    private static ClassType getSerializableProvider(ClassType classType, View view) {

        ClassType superType = classType;
        ClassType returnType;

        while (superType != null) {
            returnType = getSerializableProviderInInterfaces(superType, view);
            if (returnType != null) return returnType;
            superType = view.getTypeHierarchy().superClassOf(superType).orElse(null);
        }

        return null;
    }

    private static ClassType getSerializableProviderInInterfaces(ClassType classType, View view) {
        ClassType serializableClassType = view.getIdentifierFactory().getClassType("java.io.Serializable");
        ClassType returnType = null;

        ClassType[] ifaces;

        if (view.getTypeHierarchy().isInterface(classType)) {
            ifaces = view.getTypeHierarchy().directlyExtendedInterfacesOf(classType).toArray(ClassType[]::new);
        } else {
            ifaces = view.getTypeHierarchy().directlyImplementedInterfacesOf(classType).toArray(ClassType[]::new);
        }

        for (ClassType iface : ifaces) {
            if (iface.equals(serializableClassType)) {
                returnType = classType;
                break;
            };
            returnType = getSerializableProviderInInterfaces(iface, view);
            if (returnType != null) break;
        }

        return returnType;
    }




}
