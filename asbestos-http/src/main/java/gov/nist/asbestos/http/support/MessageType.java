package gov.nist.asbestos.http.support;
/**
 *
 */
public class MessageType {
    MessageTechnology messageTechnology;
    String soapAction;

    public MessageType(MessageTechnology messageTechnology1, String soapAction) {
        this.messageTechnology = messageTechnology1;
        this.soapAction = soapAction;
    }
}
