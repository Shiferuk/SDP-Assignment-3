package provider;

public class SmsMessageSender implements MessageSender {

    public static final int MAX_LENGTH = 160;
    private boolean available;

    public SmsMessageSender() { this(true); }
    public SmsMessageSender(boolean available) { this.available = available; }
    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public boolean sendMessage(String recipient, String title, String body) {
        if (!available) return false;
        if (recipient == null || !recipient.matches("^\\+?\\d{10,15}$")) {
            System.out.println("[SMS] REJECTED: invalid phone number: " + recipient);
            return false;
        }
        String text = title + ": " + body;
        if (text.length() > MAX_LENGTH) {
            System.out.println("[SMS] WARNING: " + text.length() + " chars, truncating to " + MAX_LENGTH);
            text = text.substring(0, MAX_LENGTH - 3) + "...";
        }
        System.out.println("[SMS] to=" + recipient + " (" + text.length() + "/" + MAX_LENGTH + ") " + text);
        return true;
    }

    @Override
    public boolean isAvailable() { return available; }

    @Override
    public String getChannelName() { return "SMS"; }
}
