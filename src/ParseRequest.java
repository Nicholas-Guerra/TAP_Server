import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;


public class ParseRequest {

    Database database;

    public ParseRequest(Database database) {
        this.database = database;
    }


    public void parseLogin(JSONObject request, BufferedReader in, PrintWriter out) throws SQLException {

        System.out.println("Login Request");

        ResultSet resultSet = database.runQuery("Select * From AccountInfo");

        while (resultSet.next()) {
            try {
                int id = resultSet.getInt("userId");
                String password = resultSet.getString("hashedPassword");


                System.out.println("UserID = " + id);
                System.out.println("Hashed Password = " + password);
                System.out.println();


            } catch (SQLException e) {
                e.printStackTrace();
            }

        }

    }

    public void parseNewUser(JSONObject request, BufferedReader in, PrintWriter out) {
        System.out.println("New User Request");

        try {
            String userId = request.getString("userID");
            String hashedPassword = request.getString("hashedPassword");
            String email = request.getString("email");
            int phoneNumber = request.getInt("phoneNumber");
            //String cryptoID = request.getString( key: "cryptoID");
            //String cryptoPrivateKey = request.getString( key: "cryptoPrivateKey")
            //String cryptoPublicKey = request.getString("cryptoPublicKey");
            //Double balance = request.getString("balance");
            String userName = request.getString("userName");


            //database.runUpdate("INSERT INTO AccountInfo(userID,hashedPassword,cryptoID,cryptoPrivateKey,cryptoPublicKey,balance,email,userName,phoneNumber)" +
                    //"VALUES ('" + userId + "','" + hashedPassword + "','" + cryptoID + "','" + cryptoPrivateKey + "','" + cryptoPublicKey + "',' balance ','" + email + "','"userName"','phoneNumber' )");

        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    public void parseTransaction(JSONObject request, BufferedReader in, PrintWriter out) throws JSONException {
        System.out.println("Transaction Request");

        JSONObject send = new JSONObject();
        send.put("Status", "Completed Transaction");
        out.println(send.toString());
    }

    public void parseHistory(JSONObject request, BufferedReader in, PrintWriter out) {
        //transactionID, sender, receiver, status, amount, senderOrReceiver(receiveristrue)
        try {
            String userID = request.getString("UID");

            JSONArray array = new JSONArray();
            JSONObject object;

            ResultSet resultSet = database
                    .runQuery("SELECT Transactions.receiverID, Transactions.senderID, Transactions.transactionID, Transactions.time, Transactions.status, Transactions.amount, AccountInfo.username" +
                                        "FROM Transactions JOIN AccountInfo" +
                                        "ON Transactions.senderID = AccountInfo.userID" +
                                        "WHERE Transactions.senderID != " + userID  +
                                        "UNION" +
                                        "SELECT Transactions.receiverID, Transactions.senderID, Transactions.transactionID, Transactions.time, Transactions.status, Transactions.amount, AccountInfo.username" +
                                        "FROM Transactions JOIN AccountInfo" +
                                        "ON Transactions.receiverID = AccountInfo.userID" +
                                        "WHERE Transactions.receiverID  != " + userID);


            // Get transaction where (uid = senderId or uid = receiverID) && update = true
            double amount;
            while (resultSet.next()) {
                object = new JSONObject();

                amount = resultSet.getDouble("amount");
                if (userID.equals(resultSet.getString("senderID")))
                    amount *= -1;

                object.put("transactionID", resultSet.getString("transactionID"))
                        .put("to_from", resultSet.getString("Username"))
                        .put("status", resultSet.getString("status"))
                        .put("amount", amount);

                array.put(object);

            }

            object = new JSONObject();
            object.put("array", array);


            System.out.println(object.toString());
            //out.println(object.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }


        System.out.println("History Request");
    }
}
