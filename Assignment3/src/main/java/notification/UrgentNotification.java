package notification;

import provider.MessageSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UrgentNotification extends Notification {

    private static final String TITLE = "Urgent alert";

    private record Fallback(MessageSender sender, String recipient) {}

    private final List<Fallback> fallbacks = new ArrayList<>();

    public UrgentNotification(MessageSender primary) {
        super(primary);
    }

    public UrgentNotification(MessageSender primary, MessageSender fallback) {
        this(primary);
        addFallback(fallback);
    }

    public UrgentNotification(MessageSender primary, MessageSender fallback, String fallbackRecipient) {
        this(primary);
        addFallback(fallback, fallbackRecipient);
    }

    public UrgentNotification addFallback(MessageSender fallback) {
        return addFallback(fallback, null);
    }

    public UrgentNotification addFallback(MessageSender fallback, String recipient) {
        fallbacks.add(new Fallback(Objects.requireNonNull(fallback, "fallback cannot be null"), recipient));
        return this;
    }

    @Override
    public void send(String recipient, String message) {
        String urgentMessage = "[URGENT] " + message;

        if (sender.isAvailable()) {
            deliver(sender, recipient, TITLE, urgentMessage);
            return;
        }

        System.out.println("[Urgent] Primary channel " + sender.getChannelName() + " is unavailable.");
        for (Fallback fb : fallbacks) {
            if (fb.sender().isAvailable()) {
                System.out.println("[Urgent] Falling back to " + fb.sender().getChannelName() + ".");
                String to = fb.recipient() != null ? fb.recipient() : recipient;
                deliver(fb.sender(), to, TITLE, urgentMessage);
                return;
            }
            System.out.println("[Urgent] Fallback " + fb.sender().getChannelName() + " is unavailable too.");
        }
        System.out.println("[Urgent] No fallback available - urgent message NOT delivered.");
    }
}
