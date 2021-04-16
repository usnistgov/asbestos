package gov.nist.asbestos.client.log;

public class ChannelDoesNotExistException extends RuntimeException {
    public ChannelDoesNotExistException(String message) {
        super(message);
    }
}
