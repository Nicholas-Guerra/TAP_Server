import java.sql.*;

public class Database {
    Connection connection;

    public Database() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:ExternalDb.sqlite");

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public void runUpdate(String sqlStatement) {

        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(sqlStatement);

        } catch (SQLException e) {
            e.printStackTrace();
        }


    }



    public ResultSet runQuery(String sqlStatement){

        try {
            Statement statement = connection.createStatement();
            return statement.executeQuery(sqlStatement);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

    }

}
