package com.devbyheart.chatmosphere.utilities;

import com.scottyab.aescrypt.AESCrypt;

import java.security.GeneralSecurityException;

public class EncryptionManager {
    public String encrypt(String KEY_LOCK, String message) throws GeneralSecurityException {
        return AESCrypt.encrypt(KEY_LOCK, message);
    }
}