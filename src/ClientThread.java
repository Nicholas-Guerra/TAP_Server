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


            JSONArray read = new JSONArray(in.readLine());
            JSONObject object = read.getJSONObject(0);
            System.out.println(object.get("Test"));



        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }


}
