import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;


public class ServerThread implements Runnable {
    private Socket client;
    BufferedReader in;
    PrintWriter out;
    Database database;


    public ServerThread(Socket client, Database database) {
        this.client = client;
        this.database = database;
    }

    @Override
    public void run() {

        System.out.println("THREAD ");
        try {
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(client.getOutputStream(), true);

            JSONObject object = new JSONObject(in.readLine());

            String request = object.getString("Request");

            if(request.equalsIgnoreCase("Login")){
                new ParseRequest(database).parseLogin(object, out);
            } else if(request.equalsIgnoreCase("NewUser")){
                new ParseRequest(database).parseNewUser(object, out);
            } else if(request.equalsIgnoreCase("Transaction")){
                new ParseRequest(database).parseTransaction(object, out);
            } else if(request.equalsIgnoreCase("History")){
                new ParseRequest(database).parseHistory(object, out);
            } else if(request.equalsIgnoreCase("updateToken")){
                new ParseRequest(database).updateToken(object, out);
            } else if(request.equalsIgnoreCase("userRequest")){
                new ParseRequest(database).userRequest(object, out);
            } else if(request.equalsIgnoreCase("sendNotification")) {
                new ParseRequest(database).sendNotification(object, out);
            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }


}
