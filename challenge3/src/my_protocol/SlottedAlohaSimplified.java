package my_protocol;

import framework.IMACProtocol;
import framework.MediumState;
import framework.TransmissionInfo;
import framework.TransmissionType;

import java.util.Random;

/**
 * A fairly trivial Medium Access Control scheme.
 *
 * @author Jaco ter Braak, University of Twente
 * @version 05-12-2013
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
 */
public class SlottedAlohaSimplified implements IMACProtocol {

    @Override
    public TransmissionInfo TimeslotAvailable(MediumState previousMediumState,
                                              int controlInformation, int localQueueLength) {
        // No data to send, just be quiet
        if (localQueueLength == 0) {
            System.out.println("SLOT - No data to send.");
            return new TransmissionInfo(TransmissionType.Silent, 0);
        }

        // Randomly transmit with 60% probability
        if (new Random().nextInt(100) < 25) {
            System.out.println("SLOT - Sending data and hope for no collision.");
            return new TransmissionInfo(TransmissionType.Data, 0);
        } else {
            System.out.println("SLOT - Not sending data to give room for others.");
            return new TransmissionInfo(TransmissionType.Silent, 0);
        }

    }

}
