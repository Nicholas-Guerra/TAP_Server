import com.sun.org.apache.bcel.internal.generic.JsrInstruction;
import jdk.nashorn.internal.parser.JSONParser;

import multichain.command.MultiChainCommand;
import multichain.command.MultichainException;
import multichain.command.builders.QueryBuilderGrant;
import multichain.object.BalanceAssetBase;
import multichain.object.KeyPairs;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.crypto.Data;



public class ParseRequest {

    Database database;

    MultiChainCommand chain = new MultiChainCommand("localhost","2770","multichainrpc" ,"DptN427z6BB2wPhmB43d4R74SG5KRL93AwUkxfzATQgx");



    public ParseRequest(Database database) {
        this.database = database;
    }

    public void parseLogin(JSONObject request, PrintWriter out){

        System.out.println("Login Request");

            try {
                String userName = null;
                try {
                    userName = request.getString("userName");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String hashedPassword = null;
                try {
                    hashedPassword = request.getString("hashedPassword");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                ResultSet passwordCheck = database.runQuery("SELECT hashedPassword" +
                                "FROM AccountInfo" +
                                "WHERE userName = " + userName);
                if(!passwordCheck.next()) {
                    //username wrong
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("Status", "Incomplete")
                                .put("Message","Wrong UserName");
                        out.println(jsonObject.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else if ( !hashedPassword.equals(passwordCheck.getString("hashedPassword"))) {
                    //wrong pass
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("Status", "Incomplete")
                                .put("Message", "Wrong Password");
                        out.println(jsonObject.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("Status", "Complete")
                                  .put("Message", "Success!");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    public void parseNewUser(JSONObject request, PrintWriter out) {
        System.out.println("New User Request");

        try {
            String userName = request.getString("userName");
            String hashedPassword = request.getString("hashedPassword");
            String email = request.getString("email");
            String phoneNumber = request.getString("phoneNumber");



            ResultSet userCheck = database.runQuery("SELECT userName" +
                            " FROM AccountInfo" +
                            " WHERE userName = '" + userName + "'");

            ResultSet emailCheck = database.runQuery("SELECT email" +
                            " FROM AccountInfo" +
                            " WHERE email = '" + email + "'");

            if (!emailCheck.isBeforeFirst() && !userCheck.isBeforeFirst()){
                double start = 5;
                double end = 100;
                double random = new Random().nextDouble();
                double result = start + (random * (end - start));
                double balance = result;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                KeyPairs key = null;
                try {
                    List<KeyPairs> list = chain.getAddressCommand().createKeyPairs();
                    key = list.get(0);

                    chain.getGrantCommand().grant(key.getAddress(),1);
                    chain.getGrantCommand().grant(key.getAddress(),2);
                    chain.getGrantCommand().grant(key.getAddress(),3);
                } catch (MultichainException e) {
                    e.printStackTrace();
                }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

                Random rand = new Random();
                int value = rand.nextInt(1000000);
                String token = String.valueOf(value);

                ResultSet resultSet = database.runQuery("INSERT INTO AccountInfo(userName, hashedPassword,cryptoID,cryptoPrivateKey,cryptoPublicKey,balance,email,phoneNumber)" +
                " VALUES ('" + userName + "','" + hashedPassword + "','" + key.getAddress() + "','" + key.getPrivkey() + "','" + key.getPubkey() + "','"  + balance + "','" + email + "','" + phoneNumber + "','" + token + " ')" +
                " SELECT last_insert_rowid()");
                resultSet.next();

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("status", "verified")
                        .put("balance", balance);
                out.println(jsonObject.toString()); }

             else if (userCheck.isBeforeFirst()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("status", "error")
                        .put("message", "UserName already in use");
                out.println(jsonObject.toString());
            }else if (emailCheck.isBeforeFirst()) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("status", "error")
                            .put("message", "Email already in use");
                    out.println(jsonObject.toString());
                }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void parseTransaction(JSONObject request, PrintWriter out) {

        try {

            JSONObject send = new JSONObject();
            String senderID = request.getString("senderID");
            String receiverID = request.getString("receiverID");
            Double amount = request.getDouble("amount");
            Long time = System.currentTimeMillis();
            String status = "pending";

            ResultSet resultSet = database.runQuery("SELECT amount FROM AccountInfo WHERE userName = '" + senderID + "'");
            resultSet.next();

            if(resultSet.getDouble("amount") >= amount) {
//change amount to balance
                database.runUpdate("INSERT into Transactions (senderID, receiever, amount, time, status)" +

                        "Values( '" + senderID + "','" + receiverID + "', '" + amount + "','" + time + "', '" + status + "')");

                database.runUpdate("UPDATE AccountInfo SET amount = amount - '" + amount + "'" +
                        " WHERE userName = '" + senderID + "'");

                database.runUpdate("UPDATE AccountInfo SET amount = amount + '" + amount + "'" +
                        " WHERE userName = '" + receiverID + "'");

                ResultSet BlocksenderCryptID = database.runQuery("SELECT cryptoID FROM AccountInfo WHERE userName = '" + senderID + "'");
                ResultSet BlockreceiverCryptID = database.runQuery("SELECT cryptoID FROM AccountInfo WHERE userName = '" + receiverID + "'");
                String senderCryptID = BlocksenderCryptID.getString("cryptoID");
                String receiverCryptID = BlockreceiverCryptID.getString("cryptoID");

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                List<BalanceAssetBase> assets = new ArrayList<BalanceAssetBase>(1);
                BalanceAssetBase temp = new BalanceAssetBase();
                temp.setQty(amount);
                assets.add(temp);
                try {
                    chain.getWalletTransactionCommand().sendFromAddress(senderCryptID, receiverCryptID,assets);
                } catch (MultichainException e) {
                    e.printStackTrace();
                }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



                send.put("Status", "Complete");
                out.println(send.toString());
            } else{
                send.put("Status", "Incomplete")
                        .put("Message", "Insufficient funds");
                out.println(send.toString());
            }


        } catch (SQLException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    public void parseHistory(JSONObject request, PrintWriter out) {
        try {
            String userName = request.getString("userName");

            JSONArray array = new JSONArray();
            JSONObject object;

            ResultSet resultSet = database
                    .runQuery("SELECT Transactions.receiverID, Transactions.senderID, Transactions.transactionID, Transactions.time, Transactions.status, Transactions.amount, AccountInfo.username" +
                                        " FROM Transactions JOIN AccountInfo" +
                                        " ON Transactions.senderID = AccountInfo.userID" +
                                        " WHERE Transactions.sender != '" + userName  + "'" +
                                        " UNION" +
                                        " SELECT Transactions.receiverID, Transactions.senderID, Transactions.transactionID, Transactions.time, Transactions.status, Transactions.amount, AccountInfo.username" +
                                        " FROM Transactions JOIN AccountInfo" +
                                        " ON Transactions.receiverID = AccountInfo.userID" +
                                        " WHERE Transactions.receiverID  != '" + userName + "'");



            double amount;
            while (resultSet.next()) {
                object = new JSONObject();

                amount = resultSet.getDouble("amount");
                if (userName.equals(resultSet.getString("sender")))
                    amount *= -1;

                object.put("transactionID", resultSet.getString("transactionID"))
                        .put("to_from", resultSet.getString("userName"))
                        .put("status", resultSet.getString("status"))
                        .put("amount", amount)
                        .put("time", resultSet.getLong("time"));

                array.put(object);

            }

            object = new JSONObject();
            object.put("Status", "Complete");
            object.put("array", array);
            //send current balance

            System.out.println(object.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("History Request");
    }

    public void updateToken(JSONObject request, PrintWriter out){


        try {
            String userName = request.getString("userName");
            String token = request.getString("token");

            //update token variable = to string that Nick sent
            database.runUpdate("UPDATE AccountInfo SET token = " + token +
                    " WHERE userName = '" + userName +"'");

            JSONObject send = new JSONObject();
            send.put("Status", "Complete");
            out.println(send.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void userRequest(JSONObject request, PrintWriter out){
        //
        JSONArray array = new JSONArray();

        String search = null;
        try {
            search = request.getString("Search");
            search = "%" + search + "%";
            ResultSet  users = database.runQuery(
                    "SELECT userName" +
                                "FROM AccountInfo" +
                                "WHERE user LIKE " + search);


            JSONObject object;
            while (users.next()) {
                object = new JSONObject();
                object.put("userName", users.getString("userName"));
                array.put(object);

            }

            object = new JSONObject();
            object.put("Status", "Complete")
                    .put("Array", array);

            out.println(object.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void sendNotification(JSONObject request,  PrintWriter out){

        JSONObject send = new JSONObject();
        try {
            String from = request.getString("from");
            String to = request.getString("to");
            double amount = request.getDouble("amount");
            Long time = System.currentTimeMillis();

            ResultSet resultSet = database.runQuery("SELECT token " +
                                                "FROM AccountInfo " +
                                                "WHERE userName = " + to);

            resultSet.next();
            String token = resultSet.getString("token");

            database.runUpdate("INSERT into Transactions (senderID, receiverID, amount, time, status)" +
                    "Values('" + from + "','" + to + "', '" + amount + "','" + String.valueOf(time) + "', 'Waiting')");


            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL myurl = new URL("https://fcm.googleapis.com/fcm/send");
                        HttpsURLConnection con = (HttpsURLConnection)myurl.openConnection();
                        con.setDoOutput(true);
                        con.setRequestMethod("POST");

                        con.setRequestProperty("Content-Type", "application/json");
                        con.setRequestProperty("Authorization", "key=AAAAvDSzk-s:APA91bEpIUE5NHy-axptjiaSxB5_waxCyH95UuXzw0HM_Sg7UR33pJRrc_AzQH5AxrS38BAqgVlD1Vfj3OeB4oce2IWwfMmMpOiauv9Ssvm32PzgkhAG-Wdu_PpJmCWVnGT7OD2fAjIq");

                        JSONObject object = new JSONObject();
                        JSONObject data = new JSONObject();
                        data.put("User Name", "Sally")
                                .put("Amount", "41.28")
                                .put("Date", "1523831301798");

                        object.put("to", token)
                                .put("data", data);

                        OutputStream outputStream = con.getOutputStream();
                        outputStream.write(object.toString().getBytes());
                        outputStream.flush();

                        int responseCode = con.getResponseCode();
                        System.out.println("PostRequest: Sending 'POST' request");
                        System.out.println("PostRequest: Response Code : " + responseCode+" "+con.getResponseMessage());


                        outputStream.close();


                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            send.put("Status", "Complete");
            out.println(send.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


}