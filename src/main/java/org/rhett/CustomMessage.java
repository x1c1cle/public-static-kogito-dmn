package org.rhett;

public class CustomMessage {

    public String severity;
    public String message;
    public String messageType;
    public String sourceId;

    public CustomMessage(String severity, String message, String messageType, String sourceId) {
        this.severity = severity;
        this.message = message;
        this.messageType = messageType;
        this.sourceId = sourceId;
    }
}
