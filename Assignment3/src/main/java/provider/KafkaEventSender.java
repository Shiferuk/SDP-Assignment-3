package provider;

public class KafkaEventSender implements MessageSender {

    private static final String TOPIC = "notifications.events";
    private static final int PARTITIONS = 3;

    private boolean available;
    private long offset = 0;

    public KafkaEventSender() { this(true); }
    public KafkaEventSender(boolean available) { this.available = available; }
    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public boolean sendMessage(String recipient, String title, String body) {
        if (!available) return false;
        if (recipient == null || recipient.isBlank()) {
            System.out.println("[Kafka] REJECTED: event key (recipient) is required.");
            return false;
        }
        String json = "{\"type\":\"NOTIFICATION\",\"key\":\"" + esc(recipient)
                + "\",\"title\":\"" + esc(title) + "\",\"body\":\"" + esc(body)
                + "\",\"timestamp\":" + System.currentTimeMillis() + "}";
        int partition = Math.floorMod(recipient.hashCode(), PARTITIONS);
        System.out.println("[Kafka] published to topic=" + TOPIC + " partition=" + partition
                + " offset=" + (offset++));
        System.out.println("        " + json);
        return true;
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    @Override
    public boolean isAvailable() { return available; }

    @Override
    public String getChannelName() { return "Kafka"; }
}
