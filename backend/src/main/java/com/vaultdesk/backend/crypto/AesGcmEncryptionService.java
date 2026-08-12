package com.vaultdesk.backend.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class AesGcmEncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    static final int IV_LENGTH_BYTES = 12;

    private final SecretKey masterKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmEncryptionService(CryptoProperties properties) {
        if (properties.masterKeyBase64() == null || properties.masterKeyBase64().isBlank()) {
            throw new IllegalStateException(
                    "vaultdesk.crypto.master-key-base64 (VAULTDESK_MASTER_KEY) nao foi configurado");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(properties.masterKeyBase64());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("VAULTDESK_MASTER_KEY deve ser uma string Base64 valida", e);
        }
        if (decoded.length != 32) {
            throw new IllegalStateException(
                    "VAULTDESK_MASTER_KEY deve decodificar para exatamente 32 bytes (AES-256), encontrado: "
                            + decoded.length);
        }
        this.masterKey = new SecretKeySpec(decoded, "AES");
    }

    public EncryptedPayload encrypt(String plaintext) {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherTextWithTag = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return new EncryptedPayload(iv, cipherTextWithTag);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao criptografar conteudo", e);
        }
    }

    public String decrypt(byte[] iv, byte[] cipherTextWithTag) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plain = cipher.doFinal(cipherTextWithTag);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            throw new EncryptedPayloadTamperedException(e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao decriptografar conteudo", e);
        }
    }
}
