package gov.nist.asbestos.simapi.simCommon;


import gov.nist.asbestos.simapi.tk.actors.ActorType;

import java.util.List;

public class TransactionInstance  {
    String simId = null;
    String messageId = null;   // message id
    String labelInterpretedAsDate = null;
    String trans = null;    // actor type code
    gov.nist.asbestos.simapi.tk.actors.TransactionType nameInterpretedAsTransactionType = null;
    gov.nist.asbestos.simapi.tk.actors.ActorType actorType = null;
    String ipAddress;
    boolean isPif = false;

    public String toString() {
        return labelInterpretedAsDate + " " + nameInterpretedAsTransactionType + " " + ipAddress;
    }

    public TransactionInstance chooseFromList(String label, List<TransactionInstance> instances) {
        String[] parts = label.split(" ");
        if (parts.length != 3) return null;
        String date = parts[0];

        for (TransactionInstance ti : instances) {
            if (date.equals(ti.labelInterpretedAsDate)) return ti;
        }

        // could select on the other parts - is there a need?
        return null;
    }

    public String getTransactionTypeName() { return trans; }
    ActorType getActorType() { return actorType; }

    void setActorType(ActorType actorType) {
        this.actorType = actorType;
    }

    public static TransactionInstance copy(TransactionInstance src) {
        TransactionInstance ti = new TransactionInstance();
        ti.simId = src.simId;
        ti.messageId = src.messageId;
        ti.labelInterpretedAsDate = src.labelInterpretedAsDate;
        ti.trans = src.trans;
        ti.nameInterpretedAsTransactionType = src.nameInterpretedAsTransactionType;
        ti.actorType = src.actorType;
        ti.ipAddress = src.ipAddress;
        return ti;
    }

    public boolean isPif() {
        return isPif;
    }

    public String getMessageId() {
        return messageId;
    }
}
