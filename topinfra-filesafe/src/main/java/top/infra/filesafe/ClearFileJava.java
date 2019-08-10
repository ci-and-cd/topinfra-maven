package top.infra.filesafe;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.DSYNC;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import top.infra.logging.Logger;

public class ClearFileJava extends AbstractResource implements ClearFile {

    private final Map<String, String> environment;

    public ClearFileJava(final Logger logger, final Path path) {
        super(logger, path);

        this.environment = Collections.emptyMap();
    }

    @Override
    protected Map<String, String> getEnvironment() {
        return this.environment;
    }

    @Override
    public void encrypt(final String passphrase, final Path targetPath) {
        try {
            final byte[] encryptedBytes = encrypt(this.getPath(), passphrase, targetPath);
        } catch (final GeneralSecurityException | IOException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private static byte[] encrypt(
        final Path inPath,
        final String passphrase,
        final Path targetPath
    ) throws IOException, GeneralSecurityException {
        final byte[] inBytes = Files.readAllBytes(inPath);

        final byte[] magicBytes = "Salted__".getBytes(StandardCharsets.US_ASCII);

        final byte[] saltBytes = new byte[8];
        new Random().nextBytes(saltBytes);
        final byte[] passphraseBytes = passphrase.getBytes(StandardCharsets.US_ASCII);
        final byte[] passphraseAndSaltBytes = concat(passphraseBytes, saltBytes);

        byte[] hash = new byte[0];
        byte[] keyAndIv = new byte[0];
        for (int i = 0; i < 3; i++) {
            final byte[] data = concat(hash, passphraseAndSaltBytes);
            final MessageDigest md = MessageDigest.getInstance("MD5");
            hash = md.digest(data);
            keyAndIv = concat(keyAndIv, hash);
        }

        final byte[] keyValue = Arrays.copyOfRange(keyAndIv, 0, 32);
        final byte[] iv = Arrays.copyOfRange(keyAndIv, 32, 48);
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        final SecretKeySpec key = new SecretKeySpec(keyValue, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        final byte[] secretBytes = cipher.doFinal(inBytes, 0, inBytes.length);

        final Base64.Encoder encoder = Base64.getEncoder();
        final byte[] outBytes = encoder.encode(concat(concat(magicBytes, saltBytes), secretBytes));
        Files.write(targetPath, outBytes, CREATE, TRUNCATE_EXISTING, WRITE, DSYNC);
        return outBytes;
    }

    private static byte[] concat(final byte[] a, final byte[] b) {
        final byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
}
