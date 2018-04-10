
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Main {
    public static void main(String [] args)  {
        System.setProperty("javax.net.ssl.keyStore", "keystore.keystore");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        Database database = new Database();


        try {
            ServerSocketFactory serverSocketFactory = SSLServerSocketFactory.getDefault();
            SSLServerSocket server = (SSLServerSocket) serverSocketFactory.createServerSocket(9099);
            Socket client;

            while(true){
                server.setSoTimeout(0);
                client = server.accept();

                new Thread(new ServerThread(client, database)).start();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
