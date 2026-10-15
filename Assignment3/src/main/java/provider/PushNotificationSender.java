package provider;

public class PushNotificationSender implements MessageSender {

    private static final int MAX_PAYLOAD = 256;
    private boolean available;

    public PushNotificationSender() { this(true); }
    public PushNotificationSender(boolean available) { this.available = available; }
    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public boolean sendMessage(String recipient, String title, String body) {
        if (!available) return false;
        if (recipient == null || !recipient.matches("^[A-Za-z0-9_-]{8,}$")) {
            System.out.println("[Push] REJECTED: invalid device token: " + recipient);
            return false;
        }
        String payload = "{\"to\":\"" + recipient + "\",\"notification\":{\"title\":\"" + esc(title)
                + "\",\"body\":\"" + esc(body) + "\"},\"badge\":1,\"sound\":\"default\"}";
        if (payload.length() > MAX_PAYLOAD) {
            System.out.println("[Push] REJECTED: payload " + payload.length() + " > " + MAX_PAYLOAD + " characters.");
            return false;
        }
        System.out.println("[Push] delivered to device " + recipient + " | " + payload);
        return true;
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    @Override
    public boolean isAvailable() { return available; }

    @Override
    public String getChannelName() { return "Push"; }
}
