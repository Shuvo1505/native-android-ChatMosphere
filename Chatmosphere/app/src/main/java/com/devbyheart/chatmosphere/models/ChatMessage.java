package com.devbyheart.chatmosphere.models;

import java.util.Date;

public class ChatMessage {
    public String senderID, receiverID, message, dateTime;
    public Date dateObject;
    public String conversationName, conversationID, conversationImage;
    public Boolean isDeletedForEveryone = false;
}