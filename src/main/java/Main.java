import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by stefano
 */
public class Main {

    public static void main(String[] args) throws IOException {

        final CommandLineParser commandLine = CommandLineParser.fromAgs(args);

        if (commandLine == null){
            System.exit(1);
        }
        ServerSocket socket = new ServerSocket(commandLine.getListenPort());

        Thread serverThread = new Thread(() -> {

            while (true){

                try {
                    Socket connection = socket.accept();
                    Thread clientThread = new Thread(() -> {
                        try {
                            new InputMultiplexor(connection, commandLine.getForwardAddress(), commandLine.getForwardPort()).startPipe();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    clientThread.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }


        });

        serverThread.start();




    }


}
