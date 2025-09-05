package ses.poc;

import java.io.*;

public class SerializationUtils {

    public static void serializeDeserialize(Object o) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(o);
        out.flush();
        bos.flush();

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        in.readObject();
    }

    public static void serializeToFile(Object o) throws IOException, ClassNotFoundException {
        FileOutputStream fos = new FileOutputStream("poc.ser");
        ObjectOutputStream out = new ObjectOutputStream(fos);
        out.writeObject(o);
        out.flush();
        fos.flush();
    }

    public static void deserializeFromFile() throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream("poc.ser");
        ObjectInputStream in = new ObjectInputStream(fis);
        in.readObject();
    }

}
