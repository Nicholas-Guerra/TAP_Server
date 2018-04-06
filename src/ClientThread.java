import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;


public class ClientThread implements Runnable {
    private Socket client;
    BufferedReader in;
    PrintWriter out;


    public ClientThread( Socket client) {
        this.client = client;
    }

    @Override
    public void run() {


        try {
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(client.getOutputStream(), true);

            JSONObject object = new JSONObject(in.readLine());

            String request = object.getString("Request");
            if(request.equalsIgnoreCase("Login")){
                ParseRequest.parseLogin(object, in, out);
            } else if(request.equalsIgnoreCase("NewUser")){
                ParseRequest.parseNewUser(object, in, out);
            } else if(request.equalsIgnoreCase("Transaction")){
                ParseRequest.parseTransaction(object, in, out);
            } else if(request.equalsIgnoreCase("History")){
                ParseRequest.parseHistory(object, in, out);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }


}
