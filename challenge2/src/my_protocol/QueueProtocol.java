package my_protocol;

import framework.IRDTProtocol;
import framework.Utils;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * @version 10-07-2019
 *
 * Copyright University of Twente, 2013-2019
 *
 **************************************************************************
 *                            Copyright notice                            *
 *                                                                        *
 *             This file may ONLY be distributed UNMODIFIED.              *
 * In particular, a correct solution to the challenge must NOT be posted  *
 * in public places, to preserve the learning effect for future students. *
 **************************************************************************
 *
 * @Authors Anne van den Boom (s2674475) and Lars Ran (s1403192)
 */
public class QueueProtocol extends IRDTProtocol {

  // change the following as you wish:
  static final int HEADERSIZE = 2;   // number of header bytes in each packet
  static final int PACKETSIZE = 200; // max bytes in a packet

  static final int DATASIZE = PACKETSIZE - HEADERSIZE;   // max. number of user data bytes in each packet

  // The time between sending each packet (important as the program runs faster than transmission.
  private long packetInterval = 50;

  // Keeping track of the packets that should be sent.
  private Integer[][] allPackets;
  private int totalPackets;

  private AckReceiver receiver;

  Integer[] fileContents;

  public ArrayDeque<Integer> packetQueue;

  @Override
  public void sender() {
    System.out.println("Sending...");

    // read from the input file
    fileContents = Utils.getFileContents(getFileID());

    // declare filesize and packetNumbers
    int filesize = fileContents.length;
    totalPackets = 1 + (filesize / DATASIZE);

    // create a new Integer array to store all the packets
    allPackets = new Integer[totalPackets][];

    System.out.println("Sending a file of " + filesize + " bytes divided over "
        + totalPackets + " packets.");

    packetQueue = new ArrayDeque<>();

    // create a new receiver that checks for ACKs. Run in a separate thread.
    receiver = new AckReceiver(getNetworkLayer(), totalPackets, this);
    new Thread(receiver).start();

    // Send all packets once first and save them to an array.
    for(int i = 0; i < totalPackets; i++){
      int packetLength = Math.min(DATASIZE, filesize - i * DATASIZE);

      // create a new packet of appropriate size
      Integer[] packet = new Integer[HEADERSIZE + packetLength];

      // assign the header with a sequenceNumber
      packet[0] = i / (int) Math.pow(2,8);
      packet[1] = i % (int) Math.pow(2,8);

      // copy databytes from the input file into data part of the packet, i.e., after the header
      System.arraycopy(fileContents, i * DATASIZE, packet, HEADERSIZE, packetLength);

      // send the packet to the network layer
      getNetworkLayer().sendPacket(packet);
      packetQueue.add(i);

      // add this packet to the allPackets array
      allPackets[i] = packet;

      System.out.println("Sent one packet with header="+i);

      try {
        Thread.sleep(packetInterval);
      } catch (InterruptedException ignored) {
      }
    }

    // Keep on sending unacknowledged packets until infinity.
    while(true){
      if(!packetQueue.isEmpty()){
        int sequenceNumber = packetQueue.poll();
        if(!receiver.getAcksReceived(sequenceNumber)){
          getNetworkLayer().sendPacket(allPackets[sequenceNumber]);
          packetQueue.add(sequenceNumber);
          System.out.println("Sent packet " + sequenceNumber + " again on queue.");
          try {
            Thread.sleep(packetInterval);
          } catch (InterruptedException ignored) {
          }
        }
      }else{
        try {
          Thread.sleep(10);
        } catch (InterruptedException ignored) {
        }
      }
    }
  }

  @Override
  public void TimeoutElapsed(Object tag) {

  }

  @Override
  public Integer[] receiver() {
    System.out.println("Receiving...");

    // create the array that will contain the file contents
    // note: we don't know yet how large the file will be, so the easiest (but not most efficient)
    //   is to reallocate the array every time we find out there's more data
    fileContents = new Integer[0];

    // Keep track of what the maximum sequence number encountered is.
    int maximumSequenceNumber = -1;
    boolean maximumSequenceNumberFound = false;

    // Keep track of missing packets to determine end of transmission.
    Set<Integer> missingPackets = new HashSet();

    // loop until we are done receiving the file
    boolean stop = false;
    while (!stop) {

      // try to receive a packet from the network layer
      Integer[] packet = getNetworkLayer().receivePacket();

      // if we indeed received a packet
      if (packet != null) {

        int sequenceNumber = getSequenceNumber(packet);

        // send ACK
        getNetworkLayer().sendPacket(Arrays.copyOfRange(packet, 0, 2));

        // tell the user
        System.out.println("Received packet, length="+packet.length+"  first byte="+sequenceNumber );

        // append the packet's data part (excluding the header) to the fileContents array, first making it larger
        int datalen = packet.length - HEADERSIZE;

        // if the sequenceNumber is larger than the last recorded maxSequenceNumber
        if(sequenceNumber > maximumSequenceNumber){
          // add to missingPackets if it is not the sequenceNumber that is "next in line"
          for(int i = maximumSequenceNumber + 1; i <= sequenceNumber; i++){
            missingPackets.add(i);
          }

          // set maxSequenceNumberFound to true if packet length was smaller than set DATASIZE
          if(datalen < DATASIZE){
            maximumSequenceNumberFound = true;
          }

          // update maximumSequenceNumber and fileContents
          maximumSequenceNumber = sequenceNumber;
          fileContents = Arrays.copyOf(fileContents, maximumSequenceNumber  * DATASIZE + datalen);
        }
        // if missingPacket
        if(missingPackets.contains(sequenceNumber)){
          System.arraycopy(packet, HEADERSIZE, fileContents, sequenceNumber * DATASIZE , datalen);
        }

        // remove sequenceNumber from missingPackets list
        missingPackets.remove(sequenceNumber);

        // Stop if EOF is reached and all packets have been received
        if(maximumSequenceNumberFound && missingPackets.isEmpty()){
          System.out.println("File received.");
          stop = true;
        }
      }else{
        // wait ~10ms (or however long the OS makes us wait) before trying again
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          stop = true;
        }
      }
    }
    // return the output file
    return fileContents;
  }

  public static int getSequenceNumber(Integer[] packet){
    return packet[0] * (int) Math.pow(2, 8) + packet[1];
  }

}
