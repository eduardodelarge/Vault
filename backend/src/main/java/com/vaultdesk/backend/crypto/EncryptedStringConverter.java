package com.vaultdesk.backend.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverters sao instanciados pelo proprio provider (Hibernate), nao pelo Spring,
 * entao nao ha injecao de dependencia normal disponivel aqui. Em vez de depender da SPI interna
 * do Hibernate (BeanContainer), que muda de pacote entre versoes, usamos um holder estatico
 * populado por um bean Spring simples no startup do contexto.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static volatile AesGcmEncryptionService encryptionService;

    @Component
    static class EncryptionServiceInjector {
        EncryptionServiceInjector(AesGcmEncryptionService service) {
            EncryptedStringConverter.encryptionService = service;
        }
    }

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        EncryptedPayload payload = service().encrypt(plaintext);
        byte[] iv = payload.iv();
        byte[] cipherTextWithTag = payload.cipherTextWithTag();
        byte[] combined = new byte[iv.length + cipherTextWithTag.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherTextWithTag, 0, combined, iv.length, cipherTextWithTag.length);
        return Base64.getEncoder().encodeToString(combined);
    }

    @Override
    public String convertToEntityAttribute(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        byte[] combined = Base64.getDecoder().decode(dbValue);
        byte[] iv = Arrays.copyOfRange(combined, 0, AesGcmEncryptionService.IV_LENGTH_BYTES);
        byte[] cipherTextWithTag =
                Arrays.copyOfRange(combined, AesGcmEncryptionService.IV_LENGTH_BYTES, combined.length);
        return service().decrypt(iv, cipherTextWithTag);
    }

    private AesGcmEncryptionService service() {
        AesGcmEncryptionService service = encryptionService;
        if (service == null) {
            throw new IllegalStateException(
                    "AesGcmEncryptionService ainda nao foi inicializado pelo contexto Spring");
        }
        return service;
    }
}
