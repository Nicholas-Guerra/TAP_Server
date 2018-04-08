
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.Socket;

public class Main {
    public static void main(String [] args){
        System.setProperty("javax.net.ssl.keyStore", "keystore.keystore");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        try {
            ServerSocketFactory serverSocketFactory = SSLServerSocketFactory.getDefault();
            SSLServerSocket server = (SSLServerSocket) serverSocketFactory.createServerSocket(9099);
            Socket client;

            while(true){
                server.setSoTimeout(0);
                client = server.accept();

                new Thread(new ServerThread(client)).start();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
