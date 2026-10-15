package notification;

import provider.MessageSender;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScheduledBatchNotification extends Notification {

    private final List<String> recipients = new ArrayList<>();
    private final LocalDateTime scheduledFor;
    private BatchSummary lastSummary;

    public ScheduledBatchNotification(MessageSender sender, List<String> recipients) {
        this(sender, recipients, null);
    }

    public ScheduledBatchNotification(MessageSender sender, List<String> recipients, LocalDateTime scheduledFor) {
        super(sender);
        this.recipients.addAll(recipients);
        this.scheduledFor = scheduledFor;
    }

    public void addRecipient(String recipient) {
        recipients.add(recipient);
    }

    public BatchSummary getLastSummary() {
        return lastSummary;
    }

    @Override
    public void send(String recipient, String message) {
        String title = "Batch: " + recipient;
        System.out.println("[Batch] '" + recipient + "' scheduled for "
                + (scheduledFor == null ? "immediate delivery" : scheduledFor)
                + ", " + recipients.size() + " recipients via " + sender.getChannelName());

        int ok = 0;
        for (String to : recipients) {
            if (deliver(to, title, message)) ok++;
        }
        lastSummary = new BatchSummary(recipients.size(), ok, recipients.size() - ok);
        System.out.println("[Batch] Summary:\n" + lastSummary);
    }
}
