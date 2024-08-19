package com.devbyheart.chatmosphere.utilities;

import java.util.HashMap;

// Ensure that all fields are appropriately mapped to the corresponding fields in your Firebase server's database.

// The value of KEY_DEFAULT_DP should be the base64-encoded string of your profile photo placeholder.

// Put your own server key for getRemoteMsgHeaders() method

public class Constants {
    public static final String KEY_COLLECTION_USERS = "";
    public static final String KEY_FNAME = "";
    public static final String KEY_EMAIL = "";
    public static final String KEY_PASSWORD = "";
    public static final String KEY_PREFERENCE_NAME = "";
    public static final String KEY_IS_SIGNED_IN = "";
    public static final String KEY_LOCKER = "";
    public static final String KEY_IS_CHAT_LOCKED = "";
    public static final String KEY_USER_ID = "";
    public static final String KEY_DEFAULT_DP = "";
    public static final String KEY_IMAGE = "";
    public static final String KEY_FCM = "";
    public static final String KEY_ABOUT = "";
    public static final String KEY_USER = "";
    public static final String KEY_COLLECTION_CHAT = "";
    public static final String KEY_SENDER_ID = "";
    public static final String KEY_RECEIVER_ID = "";
    public static final String KEY_MESSAGE = "";
    public static final String KEY_TIMESTAMP = "";
    public static final String KEY_COLLECTION_CONVERSATIONS = "";
    public static final String KEY_SENDER_NAME = "";
    public static final String KEY_RECEIVER_NAME = "";
    public static final String KEY_SENDER_IMAGE = "";
    public static final String KEY_RECEIVER_IMAGE = "";
    public static final String KEY_LAST_MESSAGE = "";
    public static final String KEY_AVAILABILITY = "";
    public static final String REMOTE_MESSAGE_AUTHORIZATION = "";
    public static final String REMOTE_MESSAGE_CONTENT_TYPE = "";
    public static final String REMOTE_MESSAGE_DATA = "";
    public static final String REMOTE_MESSAGE_REGISTRATION_ID = "";
    public static HashMap<String, String> remoteMsgHeaders = null;

    public static HashMap<String, String> getRemoteMsgHeaders() {
        if (remoteMsgHeaders == null) {
            remoteMsgHeaders = new HashMap<>();
            remoteMsgHeaders.put(REMOTE_MESSAGE_AUTHORIZATION,
                    "<Cloud Messaging API (Legacy) -- Server Key>");
            remoteMsgHeaders.put(REMOTE_MESSAGE_CONTENT_TYPE,
                    "application/json");

        }
        return remoteMsgHeaders;
    }
}