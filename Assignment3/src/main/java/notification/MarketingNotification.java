package notification;

import provider.MessageSender;

import java.util.HashSet;
import java.util.Set;

public class MarketingNotification extends Notification {

    private final String campaign;
    private final Set<String> optedOut = new HashSet<>();

    public MarketingNotification(MessageSender sender, String campaign) {
        super(sender);
        this.campaign = campaign;
    }

    public void unsubscribe(String recipient) {
        optedOut.add(recipient);
    }

    @Override
    public void send(String recipient, String message) {
        if (optedOut.contains(recipient)) {
            System.out.println("[Marketing] " + recipient + " has opted out - skipped.");
            return;
        }
        deliver(recipient, "[PROMO] " + campaign, message + " Reply STOP to opt out.");
    }
}
