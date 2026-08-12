package com.vaultdesk.backend.crypto;

public record EncryptedPayload(byte[] iv, byte[] cipherTextWithTag) {
}
