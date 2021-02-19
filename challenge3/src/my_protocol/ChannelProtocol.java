package my_protocol;

import framework.IMACProtocol;
import framework.MediumState;
import framework.TransmissionInfo;
import framework.TransmissionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * A fairly trivial Medium Access Control scheme.
 *
 * @author Eline Brader (s2674483) and Lars Ran (s1403192).
 * @version 17-02-2021
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
public class ChannelProtocol implements IMACProtocol {

	public static final int MAX_CLIENTS = 4;

	private int slot = -1;
	private int activeClients = 1;
	private int turnOrder = 0;
	private int clientNumber;
	private boolean numbersVerified = false;
	private boolean sending = false;

	ArrayList<Integer> verifier;

	/**
	 * Simple constructor to make sure the initial clientnumbers can be handed out.
	 */
	public ChannelProtocol() {
		clientNumber = new Random().nextInt((int) Math.pow(2,8)) + 1;
		verifier = new ArrayList<>();
	}

	@Override
	public TransmissionInfo TimeslotAvailable(MediumState previousMediumState,
											  int controlInformation, int localQueueLength) {
		slot++;
		if (!numbersVerified) {
			// First use a aloha slotted handshake to determine reference client numbers for the rest of the protocol.
			return verifyClientNumbers(controlInformation);
		} else {
			// Run the protocol

			// When a collision occurs reset the system state to MAX_CLIENTS amount of channels. Reset the turnOrder as
			// well.
			if (previousMediumState == MediumState.Collision) {
				activeClients = MAX_CLIENTS;
				turnOrder = clientNumber;
			}

			// If an idle state occurs this means a channel partner is not using the channel decrease the amount of
			// active channels and move all turn orders above the leaver down by one step.
			if (activeClients > 0 && previousMediumState == MediumState.Idle) {
				decreaseActiveClients();
			}

			if (localQueueLength == 0) {
				// No data to send, just be quiet
				sending = false;
				System.out.println("SLOT " + slot + " - No data to send. clientNumber: " + clientNumber);
				return new TransmissionInfo(TransmissionType.Silent, 0);
			} else if (!sending) {
				// Only run this once when it gets new data to send. Will probably result in a collision. But this is
				// needed to reset the system and the amount of channels.
				sending = true;
				activeClients++;
				return new TransmissionInfo(TransmissionType.Data, clientNumber);
			}

			// If the program arrived here it means it is already on a sending spree and nothing noteworthy has happened
			// this slot. Therefore it will keep sending if it is their turn.
			return sendOnTurn();
		}
	}


	/**
	 * Returns whether it should transmit based on the slot, the amount of activeclients and its turnorder.
	 */
	private TransmissionInfo sendOnTurn() {
		if (slot % activeClients == turnOrder) {
			System.out.println("SLOT " + slot + " - Sending data because it is my turn. clientNumber: " + turnOrder);
			return new TransmissionInfo(TransmissionType.Data, clientNumber);
		} else {
			System.out.println("SLOT " + slot + " - Not sending data because it is not my turn. clientNumber: " + turnOrder);
			return new TransmissionInfo(TransmissionType.Silent, 0);
		}
	}

	/**
	 * Decreases the count of active clients and changes own turnOrder accordingly.
	 */
	private void decreaseActiveClients() {
		int left = (slot - 1) % activeClients;
		activeClients--;
		if (left < turnOrder) {
			turnOrder--;
		}
	}

	/**
	 * This is run to verify the global client order. This is done by picking random numbers first, collecting all
	 * numbers and use an ALOHA protocol to exchange these. After all are received the clientNumbers will be based on
	 * the order of the random numbers.
	 * N.B. There is a really small chance that this will fail due to equality of chosen numbers
	 * N.N.B. Note that this chance increases rapidly with more than 4 clients due to the birthday paradox.
	 */
	private TransmissionInfo verifyClientNumbers(int controlInformation) {
		// Receive random number
		if (controlInformation != 0) {
			verifier.add(controlInformation);
		}
		if (verifier.size() != MAX_CLIENTS) {
			// Not all numbers are received yet
			if (!verifier.contains(clientNumber)) {
				// My number is not received yet, use ALOHA with chance based on missing numbers.
				if (new Random().nextInt(100) < 100 / (MAX_CLIENTS - verifier.size())) {
					return new TransmissionInfo(TransmissionType.NoData, clientNumber);
				}
			}
		} else {
			// Everything is verified, time to decide the order.
			numbersVerified = true;
			Collections.sort(verifier);
			clientNumber = verifier.indexOf(clientNumber);
		}
		// Be silent if not sending.
		return new TransmissionInfo(TransmissionType.Silent, 0);
	}
}
