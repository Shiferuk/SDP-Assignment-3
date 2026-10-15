package provider;

public class TelegramMessageSender implements MessageSender {

    private static final int MAX_LENGTH = 4096;
    private boolean available;

    public TelegramMessageSender() { this(true); }
    public TelegramMessageSender(boolean available) { this.available = available; }
    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public boolean sendMessage(String recipient, String title, String body) {
        if (!available) return false;
        if (recipient == null || !recipient.startsWith("@")) {
            System.out.println("[Telegram] REJECTED: recipient must be a username like @name, got: " + recipient);
            return false;
        }
        String formatted = "*" + title + "*\n" + body;
        if (formatted.length() > MAX_LENGTH) {
            System.out.println("[Telegram] REJECTED: message exceeds " + MAX_LENGTH + " characters.");
            return false;
        }
        System.out.println("[Telegram] sendMessage(chat=" + recipient + ", parse_mode=Markdown)");
        System.out.println("           " + formatted.replace("\n", "\n           "));
        return true;
    }

    @Override
    public boolean isAvailable() { return available; }

    @Override
    public String getChannelName() { return "Telegram"; }
}
