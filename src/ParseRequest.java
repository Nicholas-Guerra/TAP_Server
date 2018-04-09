import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;


public class ParseRequest {

    Database database;

    public ParseRequest(Database database){
        this.database = database;
    }



    public void parseLogin(JSONObject request, BufferedReader in, PrintWriter out) throws SQLException {

        System.out.println("Login Request");

        ResultSet resultSet = database.runQuery("Select * From AccountInfo");

        while ( resultSet.next() ) {
            try {
                int id = resultSet.getInt("userId");
                String password = resultSet.getString("hashedPassword");


                System.out.println( "UserID = " + id );
                System.out.println( "Hashed Password = " + password );
                System.out.println();


            } catch (SQLException e) {
                e.printStackTrace();
            }

        }

    }

    public void parseNewUser(JSONObject request, BufferedReader in, PrintWriter out){
        System.out.println("New User Request");


        database.runUpdate("INSERT INTO AccountInfo(userID,hashedPassword,cryptoID,cryptoPrivateKey,cryptoPublicKey,balance,email,userName,phoneNumber)" +
                "VALUES (1234, 'kjsbfskajbd','1234','privates','publics',12.00,'bill@gamil.com','Bill',9727898762 )");
    }

    public void parseTransaction(JSONObject request, BufferedReader in, PrintWriter out) throws JSONException {
        System.out.println("Transaction Request");

        JSONObject send = new JSONObject();
        send.put("Status", "Completed Transaction");
        out.println(send.toString());
    }

    public void parseHistory(JSONObject request, BufferedReader in, PrintWriter out){
        System.out.println("History Request");
    }

}
