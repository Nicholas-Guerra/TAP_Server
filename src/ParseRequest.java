import com.sun.org.apache.bcel.internal.generic.JsrInstruction;
import jdk.nashorn.internal.parser.JSONParser;

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
                                .put("Message","Wrong Username");
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
                            "FROM AccountInfo" +
                            "WHERE userName = " + userName);

            ResultSet emailCheck = database.runQuery("SELECT email" +
                            "FROM AccountInfo" +
                            "WHERE email = " + email);

            if (!emailCheck.isBeforeFirst() && !userCheck.isBeforeFirst()){
                double start = 5;
                double end = 100;
                double random = new Random().nextDouble();
                double result = start + (random * (end - start));
                double balance = result;



                JSONObject results = sendRPC("createkeypairs"); //fill parameter list, just add in method name
               // JSONObject results = sendRPC();
                Random rand = new Random();
                int value = rand.nextInt(10000);
                String token = String.valueOf(value);


                ResultSet resultSet = database.runQuery("INSERT INTO AccountInfo(userName, hashedPassword,cryptoID,cryptoPrivateKey,cryptoPublicKey,balance,email,phoneNumber)" +
                " VALUES (" + userName + "','" + hashedPassword + "','" + results.get("address") + "','" + results.get("privkey") + "','" + results.get("pubkey") + "','"  + balance + "','" + email + "','" + phoneNumber + "','" + token + " )" +
                        " SELECT last_insert_rowid()");
                //resultSet.next();
                //String id = resultSet.getString("userID");

            }
                /*JSONObject jsonObject = new JSONObject();
                jsonObject.put("status", "verified")
                        .put("balance", balance);
                out.println(jsonObject.toString());*/
             else if (userCheck.isBeforeFirst()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("status", "error")
                        .put("message", "Username already in use");
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
            String sender = request.getString("sender");
            String receiver = request.getString("receiver");
            Double amount = request.getDouble("amount");
            Long time = System.currentTimeMillis();
            String status = "pending";

            ResultSet resultSet = database.runQuery("SELECT amount FROM AccountInfo WHERE userName = '" + sender + "'");
            resultSet.next();

            if(resultSet.getDouble("amount") >= amount) {
//change amount to balance
                database.runUpdate("INSERT into Transactions (sender, receiever, amount, time, status)" +

                        "Values( " + sender + "," + receiver + ", " + amount + "," + time + ", " + status + ")");

                database.runUpdate("UPDATE AccountInfo SET amount = amount - " + amount +
                        " WHERE userName = '" + sender + "'");

                database.runUpdate("UPDATE AccountInfo SET amount = amount + " + amount +
                        " WHERE userName = '" + receiver + "'");

                ResultSet BlocksenderCryptID = database.runQuery("SELECT cryptoID FROM AccountInfo WHERE userName = '" + sender + "'");
                ResultSet BlockreceiverCryptID = database.runQuery("SELECT cryptoID FROM AccountInfo WHERE userName = '" + receiver + "'");
                String senderCryptID = BlocksenderCryptID.getString("cryptoID");
                String receiverCryptID = BlockreceiverCryptID.getString("cryptoID");
                List<String> rpcRequestList = new ArrayList();

                rpcRequestList.add(senderCryptID);
                rpcRequestList.add(receiverCryptID);
                rpcRequestList.add(Double.toString(amount));
                JSONObject transactionBlock = sendRPC(senderCryptID,"sendfrom",rpcRequestList);
                //call rpc here. i will need to query the database with the username to get the ID



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
            String username = request.getString("username");

            JSONArray array = new JSONArray();
            JSONObject object;

            ResultSet resultSet = database
                    .runQuery("SELECT Transactions.receiverID, Transactions.senderID, Transactions.transactionID, Transactions.time, Transactions.status, Transactions.amount, AccountInfo.username" +
                                        " FROM Transactions JOIN AccountInfo" +
                                        " ON Transactions.senderID = AccountInfo.userID" +
                                        " WHERE Transactions.sender != '" + username  + "'" +
                                        " UNION" +
                                        " SELECT Transactions.receiverID, Transactions.senderID, Transactions.transactionID, Transactions.time, Transactions.status, Transactions.amount, AccountInfo.username" +
                                        " FROM Transactions JOIN AccountInfo" +
                                        " ON Transactions.receiverID = AccountInfo.userID" +
                                        " WHERE Transactions.receiver  != '" + username + "'");



            double amount;
            while (resultSet.next()) {
                object = new JSONObject();

                amount = resultSet.getDouble("amount");
                if (username.equals(resultSet.getString("sender")))
                    amount *= -1;

                object.put("transactionID", resultSet.getString("transactionID"))
                        .put("to_from", resultSet.getString("username"))
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

            database.runUpdate("INSERT into Transactions (sender, receiever, amount, time, status)" +
                    "Values('" + from + "','" + to + "', " + amount + "," + String.valueOf(time) + ", 'Waiting')");


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

    public JSONObject sendRPC(String id, String method, List<String> params) throws JSONException {

        String mainURLL = "127.0.0.1";
        int portNum = 2770;
        String userName = "multichainrpc";
        String password = "DptN427z6BB2wPhmB43d4R74SG5KRL93AwUkxfzATQgx";
        String post = "http://tapchain:DptN427z6BB2wPhmB43d4R74SG5KRL93AwUkxfzATQgx@127.0.0.1:2770";
        String chainName = "tapchain";
        DefaultHttpClient httpclient = new DefaultHttpClient();


        JSONObject json = new JSONObject();
        json.put("id",id);
        json.put("chain_name",chainName);
        json.put("method",method);
        if(null != params){
            JSONArray array = new JSONArray();
            for(int i = 1; i <= params.size(); i++) {
                array.optString(params.indexOf(i));
            }
            json.put("params",array);

        }


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

    public JSONObject sendRPC(String method) throws JSONException {

        String mainURLL = "127.0.0.1";
        int portNum = 2770;
        String userName = "multichainrpc";
        String password = "DptN427z6BB2wPhmB43d4R74SG5KRL93AwUkxfzATQgx";
        String post = "http://tapchain:DptN427z6BB2wPhmB43d4R74SG5KRL93AwUkxfzATQgx@127.0.0.1:2770";
        String chainName = "tapchain";
        DefaultHttpClient httpclient = new DefaultHttpClient();


        JSONObject json = new JSONObject();
        json.put("chain_name", chainName);
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





        String ID = responseJSONObj.getString("id");


        JSONObject json2 = new JSONObject();
        json.put("id",ID);
        json.put("chain_name",chainName);
        json.put("method",method);


        String con = "connect";
        JSONArray array = new JSONArray();
        array.optString(1,"connect");
        array.optString(2,"send");
        array.optString(3,"receive");
        json.put("params",array);



        JSONObject response2JSONObj = null;

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
}