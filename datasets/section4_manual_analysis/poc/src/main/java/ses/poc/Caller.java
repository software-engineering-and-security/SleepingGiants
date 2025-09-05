package ses.poc;

import javax.sql.XADataSource;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Iterator;

public class Caller implements Serializable {

    public Iterator iterator;
    public Runnable runnable;
    public XADataSource dataSource;

    public PropertyChangeListener listener;

    public Caller() {}

    public Caller(Iterator iterator) {
        this.iterator = iterator;
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException, SQLException {
        ois.defaultReadObject();

        if (iterator != null)
            iterator.hasNext();
        if (runnable != null)
            runnable.run();

        if (dataSource != null)
            dataSource.getXAConnection();

        if(listener != null) {
            listener.propertyChange(new PropertyChangeEvent(this, "strings", "", ""));
        }
    }

}
