package my_protocol;

import framework.DRDTChallengeClient;
import framework.IRDTProtocol;
import framework.NetworkLayer;
import framework.Utils;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Entry point of the program. Starts the client and links the used MAC
 * protocol.
 *
 * @author Jaco ter Braak & Frans van Dijk, University of Twente
 * @version 12-02-2018
 */
public class Program {

    // Change to your group authentication token
    private static String groupToken = "0729ab87-031e-45d2-bac8-e4d0fb51c2e8";

    // Choose ID of test file to transmit: 1, 2, 3, 4, 5 or 6
    // Sizes in bytes are: 248, 2085, 6267, 21067, 53228, 141270
    private static int file = 6;

    // Change to your protocol implementation
    private static IRDTProtocol protocolImpl = new QueueProtocol();

    // Challenge server address
    // See the website for the hostname of the server
    private static String serverAddress = "networkingchallenges.ewi.utwente.nl";

    // Challenge server port
    private static int serverPort = 8002;

    // *                                                          *
    // **                                                        **
    // ***             DO NOT EDIT BELOW THIS LINE!             ***
    // ****                                                    ****
    // ************************************************************
    // ************************************************************

    public static void main(String[] args) {
        DRDTChallengeClient client = null;
        Thread sender = null;
        boolean fail = false;
        long timestamp = System.currentTimeMillis();
        try {
            System.out.print("[FRAMEWORK] Starting client... ");

            // Create the client
            client = new DRDTChallengeClient(serverAddress, serverPort, groupToken);

            System.out.println("Done.");

            System.out.println("[FRAMEWORK] Press Enter to start the simulation as sender...");
            System.out.println("[FRAMEWORK] (Simulation will be started automatically as receiver " +
                    "when the other client in the group issues the start command)");

            boolean startRequested = false;
            InputStream inputStream = new BufferedInputStream(System.in);
            while (!client.isSimulationStarted() && !client.isSimulationFinished()) {
                if (!startRequested && inputStream.available() > 0) {
                    // Request start as sender.
                    client.requestStart(file);
                    startRequested = true;
                }
                Thread.sleep(10);
            }

            if (client.isSimulationFinished()) {
                // Finished before actually started indicated failure to start.
                fail = true;
            } else {
                System.out.println("[FRAMEWORK] Simulation started!");

                protocolImpl.setNetworkLayer(new NetworkLayer(client));
                protocolImpl.setFileID(client.getFileID());
                if (startRequested) {
                    System.out.println("[FRAMEWORK] Running protocol implementation as sender...");
                    sender = new Thread(() -> protocolImpl.sender());
                    sender.start();
                } else {
                    System.out.println("[FRAMEWORK] Running protocol implementation as receiver...");
                    Integer[] fileContents = protocolImpl.receiver();
                    Utils.setFileContents(fileContents, client.getFileID(), timestamp);
                    client.sendChecksumOut(timestamp);
                }
            }

        } catch (IOException e) {
            System.out.println("[FRAMEWORK] Could not start the client, because: ");
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("[FRAMEWORK] Operation interrupted.");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("[FRAMEWORK] Unexpected Exception: ");
            e.printStackTrace();
        } finally {
            if (client != null) {
                if (fail) {
                    client.stop();
                } else {
                    while (!client.isSimulationFinished()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                    System.out.print("[FRAMEWORK] Shutting down client... ");
                    if (sender != null) {
                        sender.stop(); // We just want to kill it
                    }
                    client.stop();
                    System.out.println("[FRAMEWORK] Done.");
                }
            }
        }
    }
}
