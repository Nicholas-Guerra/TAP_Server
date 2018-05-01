import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Random;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.net.ssl.HttpsURLConnection;


public class ParseRequest {

    Database database;


    public ParseRequest(Database database) {
        this.database = database;

    }

    public void parseLogin(JSONObject request, PrintWriter out){

        System.out.println("Login Request");

            try {
                String userName = request.getString("userName");

                String hashedPassword = request.getString("hashedPassword");
                ResultSet passwordCheck = database.runQuery("SELECT hashedPassword " +
                                " FROM AccountInfo " +
                                " WHERE userName = '" + userName + "' ");
                if(!passwordCheck.next()) {

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("Status", "Error")
                            .put("Message", "Wrong Username");
                    out.println(jsonObject.toString());

                } else if ( !hashedPassword.equals(passwordCheck.getString("hashedPassword"))) {
                    //wrong pass
                    JSONObject jsonObject = new JSONObject();

                    jsonObject.put("Status", "Error")
                            .put("Message", "Wrong Password");
                    out.println(jsonObject.toString());

                } else {
                    System.out.println("Success");
                    JSONObject jsonObject = new JSONObject();

                    ResultSet results = parseUserRefresh(userName);

                    jsonObject.put("Status", "Complete")
                            .put("Message", "Success!");
                    results.next();
                    jsonObject.put("phoneNumber", results.getString("phoneNumber"))
                                .put("email", results.getString("email"))
                                .put("balance", results.getDouble("balance"))
                                .put("firstName", results.getString("firstName"))
                                .put("lastName", results.getString("lastName"));

                    System.out.println(jsonObject.toString());

                    out.println(jsonObject.toString());

                }
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println(e.getMessage());

            } catch (JSONException e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
            }
    }


    public void parseNewUser(JSONObject request, PrintWriter out) {
        System.out.println("New User Request");

        try {
            String userName = request.getString("userName");
            String hashedPassword = request.getString("hashedPassword");
            String email = request.getString("email");
            String phoneNumber = request.getString("phoneNumber");
            String token = request.getString("token");
            String firstName = request.getString("firstName");
            String lastName = request.getString("lastName");


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
                double balance =  new Double(new DecimalFormat("#.##").format(result));

                String address = getNewAddress(balance); //fill parameter list, just add in method name


                database.runUpdate("INSERT INTO AccountInfo(userName, hashedPassword,cryptoID,balance,email,phoneNumber, token, firstName, lastName)" +
                        " VALUES ('" + userName + "','" + hashedPassword + "','" + address + "','"  + balance + "','" + email + "','" + phoneNumber + "','" + token + "','" + firstName + "','" + lastName + "')" );


                JSONObject jsonObject = new JSONObject();
                jsonObject.put("Status", "Complete")
                        .put("balance", balance);
                out.println(jsonObject.toString()); }

             else if (userCheck.isBeforeFirst()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("Status", "Error")
                        .put("message", "UserName already in use");
                out.println(jsonObject.toString());



            }else if (emailCheck.isBeforeFirst()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("Status", "Error")
                        .put("message", "Email already in use");
                out.println(jsonObject.toString());


            } else {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("status", "error")
                        .put("message", "Unknown");
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
            String sender = request.getString("sender");
            String receiver = request.getString("receiver");
            Double amount = request.getDouble("amount");
            Long time = System.currentTimeMillis();
            String status = "pending";

            database.runUpdate("INSERT into Transactions (senderID, receiverID, amount, time, status)" +
                    " Values( '" + sender + "','" + receiver + "', '" + amount + "','" + time + "', '" + status + "')");

            ResultSet resultSet = database.runQuery("SELECT balance FROM AccountInfo WHERE userName = '" + sender + "'");
            resultSet.next();

            if(resultSet.getDouble("balance") >= amount) {
                //change amount to balance
                status = "complete";
                database.runUpdate("INSERT into Transactions (status)" +
                        " Values( '" + status + "')");

                database.runUpdate("UPDATE AccountInfo SET balance = balance - '" + amount + "'" +
                        " WHERE userName = '" + sender + "'");

                database.runUpdate("UPDATE AccountInfo SET balance = balance + '" + amount + "'" +
                        " WHERE userName = '" + receiver + "'");

                ResultSet BlocksenderCryptID = database.runQuery("SELECT cryptoID FROM AccountInfo WHERE userName = '" + sender + "'");
                BlocksenderCryptID.next();
                String senderCryptID = BlocksenderCryptID.getString("cryptoID");

                ResultSet BlockreceiverCryptID = database.runQuery("SELECT cryptoID FROM AccountInfo WHERE userName = '" + receiver + "'");
                BlockreceiverCryptID.next();
                String receiverCryptID = BlockreceiverCryptID.getString("cryptoID");

                JSONObject transactionBlock = sendFrom(senderCryptID, receiverCryptID, amount);
                //call rpc here. i will need to query the database with the userName to get the ID



                send.put("Status", "Complete");
                out.println(send.toString());
            } else{
                status = "incomplete";
                database.runUpdate("INSERT into Transactions (status)" +
                        " Values( '" + status + "')");
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
            search = "'%" + search + "%'";
            ResultSet  users = database.runQuery(
                    "SELECT userName, token " +
                                " FROM AccountInfo " +
                                " WHERE userName LIKE " + search);

            JSONObject object;
            while (users.next()) {
                object = new JSONObject();
                object.put("userName", users.getString("userName"))
                        .put("token", users.getString("token"));
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
            String fromName = request.getString("fromName");
            String toName = request.getString("toName");
            String token = request.getString("to");
            double amount = request.getDouble("amount");
            Long time = System.currentTimeMillis();



            database.runUpdate("INSERT into Transactions (senderID, receiverID, amount, time, status)" +
                    "Values('" + fromName + "','" + toName + "', '" + amount + "','" + String.valueOf(time) + "', 'Waiting')");


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
                        data.put("User Name", fromName)
                                .put("Amount", String.valueOf(amount))
                                .put("Date", String.valueOf(time));

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
        }

    }

    public JSONObject sendFrom(String from, String to, Double amount) throws JSONException {

        String mainURLL = "localhost";
        int portNum = 6456;
        String userName = "multichainrpc";
        String password = "9iFLwr8Rydj6RBWeDBQX2aTsa3PXDrqtkHxskvDttgUv";
        String post = "http://localhost:6456";
        String chainName = "tapchain1.0";
        String method = "sendassetfrom";
        String asset = "TAPcoin";
        DefaultHttpClient httpclient = new DefaultHttpClient();


        JSONObject json = new JSONObject();
        json.put("id",chainName);
        json.put("method", method);

        JSONArray params = new JSONArray();
        params.put(from)
                .put(to)
                .put(asset)
                .put(amount);

        json.put("params", params);


        JSONObject responseJSONObj = null;

        try{
            httpclient.getCredentialsProvider().setCredentials( new AuthScope(mainURLL,portNum),
                    new UsernamePasswordCredentials(userName,password));
            StringEntity myString = new StringEntity(json.toString());
            System.out.println(json.toString());
            HttpPost myhttppost = new HttpPost(post);
            myhttppost.setEntity(myString);

            HttpResponse myresponse = httpclient.execute(myhttppost);
            HttpEntity myentity2 = myresponse.getEntity();
            System.out.println(myresponse.getStatusLine());
            if(myentity2 != null){
                System.out.println("Good Response");
            }

            String retJSON = EntityUtils.toString(myentity2);
            responseJSONObj = new JSONObject(retJSON);


        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            httpclient.getConnectionManager().shutdown();
        }


        return responseJSONObj;
    }

    public String getNewAddress(double amount) throws JSONException {

        String mainURLL = "localhost";
        int portNum = 6456;
        String userName = "multichainrpc";
        String password = "9iFLwr8Rydj6RBWeDBQX2aTsa3PXDrqtkHxskvDttgUv";
        String post = "http://localhost:6456";
        String chainName = "tapchain1.0";
        String method = "getnewaddress";
        DefaultHttpClient httpclient = new DefaultHttpClient();


        JSONObject json = new JSONObject();
        json.put("id", chainName);
        json.put("method", method);


        JSONObject responseJSONObj = null;

        try{
            httpclient.getCredentialsProvider().setCredentials( new AuthScope(mainURLL,portNum),
                    new UsernamePasswordCredentials(userName,password));
            StringEntity myString = new StringEntity(json.toString());
            System.out.println(json.toString());
            HttpPost myhttppost = new HttpPost(post);
            myhttppost.setEntity(myString);

            HttpResponse myresponse = httpclient.execute(myhttppost);
            HttpEntity myentity2 = myresponse.getEntity();
            System.out.println(myresponse.getStatusLine());
            if(myentity2 != null){
                System.out.println("Good Response");
            }

            String retJSON = EntityUtils.toString(myentity2);

            responseJSONObj = new JSONObject(retJSON);
            System.out.println(responseJSONObj.toString());


        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        String address = responseJSONObj.getString("result");

        JSONObject json2 = new JSONObject();
        json2.put("id", chainName);
        json2.put("method", "issuemore");
        JSONArray params = new JSONArray();
        params.put(address)
                .put("TAPcoin")
                .put(amount);

        json2.put("params", params);


        JSONObject response2JSONObj;

        try{
            httpclient.getCredentialsProvider().setCredentials( new AuthScope(mainURLL,portNum),
                    new UsernamePasswordCredentials(userName,password));
            StringEntity myString = new StringEntity(json2.toString());
            System.out.println(json2.toString());
            HttpPost myhttppost = new HttpPost(post);
            myhttppost.setEntity(myString);

            HttpResponse myresponse = httpclient.execute(myhttppost);
            HttpEntity myentity2 = myresponse.getEntity();
            System.out.println(myresponse.getStatusLine());
            if(myentity2 != null){
                System.out.println("Good Response");
            }

            String retJSON = EntityUtils.toString(myentity2);

            response2JSONObj = new JSONObject(retJSON);
            System.out.println(response2JSONObj.toString());


        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            httpclient.getConnectionManager().shutdown();
        }

        System.out.println(address);
        return address;
    }

    public ResultSet parseUserRefresh(String username){
        return database.runQuery("SELECT email, phoneNumber, balance, firstName, lastName" +
                " FROM AccountInfo" +
                " WHERE username = '" + username + "'");



    }
}