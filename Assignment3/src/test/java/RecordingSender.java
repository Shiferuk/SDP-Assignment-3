import provider.MessageSender;

import java.util.ArrayList;
import java.util.List;

public class RecordingSender implements MessageSender {

    public record Message(String recipient, String title, String body) {}

    private final String name;
    private boolean available;
    public final List<Message> sent = new ArrayList<>();

    public RecordingSender(String name) { this(name, true); }

    public RecordingSender(String name, boolean available) {
        this.name = name;
        this.available = available;
    }

    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public boolean sendMessage(String recipient, String title, String body) {
        sent.add(new Message(recipient, title, body));
        return true;
    }

    @Override
    public boolean isAvailable() { return available; }

    @Override
    public String getChannelName() { return name; }
}
