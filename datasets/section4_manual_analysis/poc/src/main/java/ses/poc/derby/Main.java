package ses.poc.derby;

import org.apache.derby.client.BasicClientDataSource;

import java.sql.SQLException;

public class Main {

    public static void main(String[] args) throws SQLException {

        BasicClientDataSource dataSource = new BasicClientDataSource();
        dataSource.setTraceFile("shaman");
        dataSource.setTraceDirectory("target");

        dataSource.getConnection("foo", "bar");

    }
}
