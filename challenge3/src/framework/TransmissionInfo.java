package framework;

/**
 * Instructs the physical layer as to what action should be taking during the current timeslot
 */
public class TransmissionInfo {
    private TransmissionType transmissionType;
    private int controlInformation;

    /**
     * @param transmissionType   Instructs the physical layer on whether or not to transmit
     * @param controlInformation If (transmissionType != TransmissionType.Silent), sets the control information that should be sent on the medium in the next timeslot
     */
    public TransmissionInfo(TransmissionType transmissionType, int controlInformation) {
        this.transmissionType = transmissionType;
        this.controlInformation = controlInformation;
    }

    /**
     * @return The transmission type
     */
    public TransmissionType GetTransmissionType() {
        return this.transmissionType;
    }

    /**
     * @return The control information
     */
    public int GetControlInformation() {
        return this.controlInformation;
    }
}
