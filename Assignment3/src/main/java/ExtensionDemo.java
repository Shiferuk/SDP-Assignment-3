import notification.EncryptedNotification;
import notification.MarketingNotification;
import notification.Notification;
import notification.ScheduledBatchNotification;
import notification.UrgentNotification;
import provider.EmailMessageSender;
import provider.KafkaEventSender;
import provider.MessageSender;
import provider.PushNotificationSender;
import provider.SmsMessageSender;
import provider.TelegramMessageSender;

import java.util.List;

public class ExtensionDemo {

    public static void main(String[] args) {
        TelegramMessageSender telegram = new TelegramMessageSender();
        EmailMessageSender email = new EmailMessageSender();
        SmsMessageSender sms = new SmsMessageSender();
        KafkaEventSender kafka = new KafkaEventSender();
        PushNotificationSender push = new PushNotificationSender();

        System.out.println("=== A. NEW provider (Push) with EXISTING notification types ===");
        Notification urgent = new UrgentNotification(push);
        urgent.send("TEST_DEVICE_TOKEN_NOT_REAL", "Server is down.");

        System.out.println();
        new EncryptedNotification(push).send("TEST_DEVICE_TOKEN_NOT_REAL", "Code 482915");

        System.out.println();
        new ScheduledBatchNotification(push,
                List.of("TEST_DEVICE_TOKEN_0001", "TEST_DEVICE_TOKEN_0002", "bad")).send("promo-batch", "Hello!");

        System.out.println("\n=== A2. Push as a fallback for an existing Urgent notification ===");
        telegram.setAvailable(false);
        new UrgentNotification(telegram)
                .addFallback(push, "TEST_DEVICE_TOKEN_NOT_REAL")
                .send("@cyr0th", "Payment service is down.");
        telegram.setAvailable(true);

        System.out.println("\n=== B. NEW notification type (Marketing) with EXISTING providers ===");
        List<MessageSender> existing = List.of(telegram, email, sms, kafka, push);
        List<String> recipients = List.of("@cyr0th", "adrakula379@gmail.com", "+77775712100", "user-42", "TEST_DEVICE_TOKEN_NOT_REAL");
        for (int i = 0; i < existing.size(); i++) {
            Notification marketing = new MarketingNotification(existing.get(i), "Autumn sale");
            System.out.println("\n-> " + marketing.describe());
            marketing.send(recipients.get(i), "-30% today.");
        }

        System.out.println("\n=== B2. Marketing: opt-out and runtime switch ===");
        MarketingNotification promo = new MarketingNotification(email, "Autumn sale");
        promo.unsubscribe("bob@example.com");
        promo.send("bob@example.com", "-30% today.");
        promo.setSender(push);
        promo.send("TEST_DEVICE_TOKEN_NOT_REAL", "-30% today.");
    }
}
