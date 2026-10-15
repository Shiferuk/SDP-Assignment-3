package provider;

public class EmailMessageSender implements MessageSender {

    private static final String SIGNATURE = "--\nNotification Gateway Team\nsupport@gateway.example";
    private boolean available;

    public EmailMessageSender() { this(true); }
    public EmailMessageSender(boolean available) { this.available = available; }
    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public boolean sendMessage(String recipient, String title, String body) {
        if (!available) return false;
        if (recipient == null || !recipient.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            System.out.println("[Email] REJECTED: invalid address: " + recipient);
            return false;
        }
        System.out.println("[Email] SMTP (simulated)");
        System.out.println("        To:      " + recipient);
        System.out.println("        Subject: " + title);
        System.out.println("        " + body.replace("\n", "\n        "));
        System.out.println("        " + SIGNATURE.replace("\n", "\n        "));
        return true;
    }

    @Override
    public boolean isAvailable() { return available; }

    @Override
    public String getChannelName() { return "Email"; }
}
