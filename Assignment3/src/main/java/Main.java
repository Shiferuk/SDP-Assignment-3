import notification.EncryptedNotification;
import notification.MarketingNotification;
import notification.Notification;
import notification.ScheduledBatchNotification;
import notification.UrgentNotification;
import provider.EmailMessageSender;
import provider.PushNotificationSender;
import provider.SmsMessageSender;
import provider.TelegramMessageSender;

import java.time.LocalDateTime;
import java.util.List;

public class Main {

    static final String TELEGRAM_USER = "@cyr0th";
    static final String EMAIL_USER = "adrakula379@gmail.com";
    static final String PHONE_USER = "+77775712100";
    static final String DEVICE_TOKEN = "TEST_DEVICE_TOKEN_NOT_REAL";

    public static void main(String[] args) {
        TelegramMessageSender telegram = new TelegramMessageSender();
        EmailMessageSender email = new EmailMessageSender();
        SmsMessageSender sms = new SmsMessageSender();
        PushNotificationSender push = new PushNotificationSender();

        title("1. UrgentNotification + Telegram");
        new UrgentNotification(telegram).send(TELEGRAM_USER, "Database latency is high.");

        title("2. EncryptedNotification + Email");
        EncryptedNotification encrypted = new EncryptedNotification(email, "test-key");
        encrypted.send(EMAIL_USER, "Your test verification code is 482915");
        System.out.println("(decrypted for verification: "
                + encrypted.decrypt(encrypted.encrypt("Your test verification code is 482915")) + ")");

        title("3. ScheduledBatchNotification + SMS (10 recipients, 2 invalid numbers)");
        List<String> phones = List.of(
                "+77775712100", "+77001110002", "+77001110003", "+77001110004", "+77001110005",
                "+77001110006", "12345", "+77001110008", "not-a-number", "+77001110010");
        new ScheduledBatchNotification(sms, phones, LocalDateTime.now().plusHours(1))
                .send("weekly-digest", "Your weekly report is ready.");

        title("4. Runtime switching: Telegram -> Email (same Notification object)");
        Notification notification = new UrgentNotification(telegram);
        System.out.println("-> " + notification.describe());
        notification.send(TELEGRAM_USER, "Deployment finished.");
        notification.setSender(email);
        System.out.println("-> " + notification.describe());
        notification.send(EMAIL_USER, "Deployment finished.");

        title("5. Telegram unavailable -> fallback to SMS");
        telegram.setAvailable(false);
        new UrgentNotification(telegram)
                .addFallback(sms, PHONE_USER)
                .send(TELEGRAM_USER, "Payment service is down.");
        telegram.setAvailable(true);

        title("6. MarketingNotification + PushNotificationSender");
        new MarketingNotification(push, "Autumn sale").send(DEVICE_TOKEN, "-30% on everything today.");
    }

    private static void title(String text) {
        System.out.println("\n==================================================");
        System.out.println("SCENARIO " + text);
        System.out.println("==================================================");
    }
}
