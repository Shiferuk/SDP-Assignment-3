package notification;

import provider.MessageSender;

import java.util.Objects;

public abstract class Notification {

    protected MessageSender sender;

    protected Notification(MessageSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender cannot be null");
    }

    public void setSender(MessageSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender cannot be null");
    }

    public abstract void send(String recipient, String message);

    protected boolean deliver(String recipient, String title, String body) {
        return deliver(sender, recipient, title, body);
    }

    protected boolean deliver(MessageSender target, String recipient, String title, String body) {
        if (!target.isAvailable()) {
            System.out.println("[" + target.getChannelName() + "] UNAVAILABLE - message not sent.");
            return false;
        }
        return target.sendMessage(recipient, title, body);
    }

    public String describe() {
        return getClass().getSimpleName() + " via " + sender.getChannelName();
    }
}
