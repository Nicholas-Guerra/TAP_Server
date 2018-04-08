import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class ParseRequest {

    public static void parseLogin(JSONObject request, BufferedReader in, PrintWriter out) throws JSONException {
        System.out.println("Login Request");
    }

    public static void parseNewUser(JSONObject request, BufferedReader in, PrintWriter out){
        System.out.println("New User Request");
    }

    public static void parseTransaction(JSONObject request, BufferedReader in, PrintWriter out) throws JSONException {
        System.out.println("Transaction Request");

        JSONObject send = new JSONObject();
        send.put("Status", "Completed Transaction");
        out.println(send.toString());
    }

    public static void parseHistory(JSONObject request, BufferedReader in, PrintWriter out){
        System.out.println("History Request");
    }

}
