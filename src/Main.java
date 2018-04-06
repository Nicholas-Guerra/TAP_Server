
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Main {
    public static void main(String [] args){
        System.setProperty("javax.net.ssl.keyStore", "keystore.keystore");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        execAdb();

        try {
            ServerSocketFactory serverSocketFactory = SSLServerSocketFactory.getDefault();
            SSLServerSocket server = (SSLServerSocket) serverSocketFactory.createServerSocket(9099);
            Socket client;

            while(true){
                server.setSoTimeout(0);
                client = server.accept();

                new Thread(new ClientThread(client)).start();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void execAdb() {
        // run the adb bridge
        try {


            Process p = Runtime.getRuntime().exec("adb reverse tcp:9099 tcp:9099");

            Scanner sc = new Scanner(p.getErrorStream());
            Scanner in = new Scanner(System.in);

            if (sc.hasNextLine()) {
                while (sc.hasNextLine()) {
                    System.out.println(sc.nextLine());
                }

                System.out.println("Device not connected: Try again? (Y/N)");

                while (true) {

                    String input = in.nextLine();
                    if (input.equalsIgnoreCase("Y")) {
                        execAdb();
                        break;
                    } else if (input.equalsIgnoreCase("N")) {
                        System.out.println("No connection: Exiting program!");
                        System.exit(0);
                    } else {
                        System.out.println("Invalid response: Try again");
                    }
                }

            } else {
                System.out.println("Device connected successfully!");
            }

        } catch (Exception e) {
            System.out.println("Fatal exception: " + e.toString());
        }
    }
}
