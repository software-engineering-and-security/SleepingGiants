package ses.poc.redisson;

import org.redisson.mapreduce.ReducerTask;
import ses.poc.Caller;
import ses.poc.SerializationUtils;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        ReducerTask reducerTask = new ReducerTask("", null, Main.class, "", 0);
        Caller caller = new Caller();
        caller.runnable = reducerTask;

        SerializationUtils.serializeDeserialize(caller);

    }

    public Main() {
        System.out.println("<init> called on " + Main.class);
    }

}
