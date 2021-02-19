package my_protocol;

import framework.IRDTProtocol;
import framework.NetworkLayer;

/**
 * Utility class for the sender function in the protocol class.
 * Will receive acknowledgments sent by the "receiver" and keep track of the acknowledgements it got.
 * In the case of a window it will tell the sender once a new acknowledgement is received.
 *
 * @Authors Anne van den Boom (s2674475) and Lars Ran (s1403192)
 */
public class AckReceiver implements Runnable{

  private NetworkLayer network;
  private boolean[] acksReceived;
  private IRDTProtocol protocol;
  private int lastAckReceived;
  private int numberOfPackets;
  private int acks;

  /**
   * A constructor with all the info the receiver needs to know.
   */
  public AckReceiver(NetworkLayer network, int numberOfPackets, IRDTProtocol protocol){
      this.network = network;
      this.protocol = protocol;
      acksReceived = new boolean[numberOfPackets];
      this.numberOfPackets = numberOfPackets;
  }

  /**
   * Keep on receiving acks to infinity.
   */
  @Override
  public void run() {
    while(true){
      Integer[] packet = network.receivePacket();
      if(packet != null){
        int sequenceNumber = MyProtocol.getSequenceNumber(packet);
        // If it has not yet received this ack then add it and tell the sender to send the next.
        if (!acksReceived[sequenceNumber]) {
          acks++;
          // If protocol is myprotocol it needs to send the next packet.
          if(protocol instanceof MyProtocol){
            ((MyProtocol) protocol).sendNextPacket();
          }
          if(protocol instanceof QueueProtocol){
            ((QueueProtocol) protocol).packetQueue.remove(sequenceNumber);
             int top = sequenceNumber;
             if (sequenceNumber < lastAckReceived){
               top += numberOfPackets;
             }
             for(int i = lastAckReceived + 1; i < top; i++){
               if(!acksReceived[i % numberOfPackets]){
                  for(int j = -1; j < 10/(numberOfPackets - acks); j++){
                     ((QueueProtocol) protocol).packetQueue.addFirst(i % numberOfPackets);
                  }
               }
             }
          }
          acksReceived[sequenceNumber] = true;
          lastAckReceived = sequenceNumber;
        }
        System.out.println("Received ACK " + sequenceNumber);
      } else {
        // wait ~10ms (or however long the OS makes us wait) before trying again
        try {
          Thread.sleep(10);
        } catch (InterruptedException ignored) {
        }
      }
    }
  }

  /**
   * Return if the specified ack has been received by this class or not.
   */
  public boolean getAcksReceived(int sequenceNumber) {
    return acksReceived[sequenceNumber];
  }

  public int getLastAckReceived(){
    return lastAckReceived;
  }

}
