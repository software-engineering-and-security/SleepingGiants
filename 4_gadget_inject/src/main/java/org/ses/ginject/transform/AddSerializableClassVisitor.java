package org.ses.ginject.transform;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassVisitor;
import ses.ginject.Main;

public class AddSerializableClassVisitor extends ClassVisitor {

    private static final Logger logger = LogManager.getLogger(AddSerializableClassVisitor.class.getName());
    public static final String SERIALIZABLE_INTERFACE = "java/io/Serializable";

    public boolean addedSerializable = false;

    public AddSerializableClassVisitor(int api, ClassVisitor visitor) {
        super(api, visitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {

        boolean containsSerializable = false;

        for (int i = 0; i < interfaces.length; i++) {
            if (interfaces[i].equals(SERIALIZABLE_INTERFACE)) {
                containsSerializable = true;
                break;
            }
        }

        if (!containsSerializable) {
            String[] newInterfaces = new String[interfaces.length + 1];
            for (int i = 0; i < interfaces.length; i++) newInterfaces[i] = interfaces[i];
            newInterfaces[newInterfaces.length - 1] = SERIALIZABLE_INTERFACE;
            logger.info("Adding Serializable interface to " + name);
            addedSerializable = true;
            super.visit(version, access, name, signature, superName, newInterfaces);
        } else {
            super.visit(version, access, name, signature, superName, interfaces);
        }

    }

}
