
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.Socket;

public class Main {
    public static void main(String [] args){
        System.setProperty("javax.net.ssl.keyStore", "keystore.keystore");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        Database database = new Database();

        database.runUpdate("INSERT INTO AccountInfo(userID,hashedPassword,cryptoID,cryptoPrivateKey,cryptoPublicKey,balance,email,userName,phoneNumber)" +
                                       "VALUES ('kjsbfskajbd','12343','private','public',12.00,'bill@gamil.com','Bill',9727898762 );");

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
