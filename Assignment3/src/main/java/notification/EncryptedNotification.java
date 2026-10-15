package notification;

import provider.MessageSender;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EncryptedNotification extends Notification {

    private static final String PREFIX = "ENC:";
    private static final String TITLE = "Encrypted message";

    private final byte[] key;

    public EncryptedNotification(MessageSender sender) {
        this(sender, "gateway-key");
    }

    public EncryptedNotification(MessageSender sender, String key) {
        super(sender);
        if (key == null || key.isEmpty()) throw new IllegalArgumentException("key cannot be empty");
        this.key = key.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void send(String recipient, String message) {
        deliver(recipient, TITLE, encrypt(message));
    }

    public String encrypt(String plain) {
        return PREFIX + Base64.getEncoder().encodeToString(xor(plain.getBytes(StandardCharsets.UTF_8)));
    }

    public String decrypt(String cipher) {
        String data = cipher.startsWith(PREFIX) ? cipher.substring(PREFIX.length()) : cipher;
        return new String(xor(Base64.getDecoder().decode(data)), StandardCharsets.UTF_8);
    }

    private byte[] xor(byte[] in) {
        byte[] out = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (byte) (in[i] ^ key[i % key.length]);
        }
        return out;
    }
}
