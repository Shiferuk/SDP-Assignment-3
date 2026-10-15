import notification.BatchSummary;
import notification.EncryptedNotification;
import notification.MarketingNotification;
import notification.Notification;
import notification.ScheduledBatchNotification;
import notification.UrgentNotification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import provider.PushNotificationSender;
import provider.SmsMessageSender;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationBridgeTest {

    @Test
    @DisplayName("Urgent: adds [URGENT] and delivers through an available sender")
    void urgentDeliversWithMarker() {
        RecordingSender telegram = new RecordingSender("Telegram");

        new UrgentNotification(telegram).send("@cyr0th", "DB is down");

        assertEquals(1, telegram.sent.size());
        assertEquals("@cyr0th", telegram.sent.get(0).recipient());
        assertEquals("[URGENT] DB is down", telegram.sent.get(0).body());
    }

    @Test
    @DisplayName("Unavailable provider and no fallback: nothing is sent")
    void unavailableWithoutFallbackSendsNothing() {
        RecordingSender telegram = new RecordingSender("Telegram", false);

        new UrgentNotification(telegram).send("@cyr0th", "DB is down");

        assertTrue(telegram.sent.isEmpty());
    }

    @Test
    @DisplayName("Non-urgent types also refuse to send through an unavailable provider")
    void encryptedDoesNotSendThroughUnavailableProvider() {
        RecordingSender email = new RecordingSender("Email", false);

        new EncryptedNotification(email).send("a@b.com", "secret");

        assertTrue(email.sent.isEmpty());
    }

    @Test
    @DisplayName("Fallback: unavailable primary -> SMS receives the message with its own recipient")
    void fallbackUsedWhenPrimaryUnavailable() {
        RecordingSender telegram = new RecordingSender("Telegram", false);
        RecordingSender sms = new RecordingSender("SMS");

        new UrgentNotification(telegram).addFallback(sms, "+77775712100").send("@cyr0th", "Payment down");

        assertTrue(telegram.sent.isEmpty());
        assertEquals(1, sms.sent.size());
        assertEquals("+77775712100", sms.sent.get(0).recipient());
        assertEquals("[URGENT] Payment down", sms.sent.get(0).body());
    }

    @Test
    @DisplayName("Fallback chain skips unavailable fallbacks and uses the first available one")
    void fallbackChainSkipsUnavailable() {
        RecordingSender primary = new RecordingSender("Telegram", false);
        RecordingSender fb1 = new RecordingSender("SMS", false);
        RecordingSender fb2 = new RecordingSender("Kafka");
        RecordingSender fb3 = new RecordingSender("Email");

        new UrgentNotification(primary).addFallback(fb1).addFallback(fb2).addFallback(fb3).send("u", "x");

        assertTrue(fb1.sent.isEmpty());
        assertEquals(1, fb2.sent.size());
        assertTrue(fb3.sent.isEmpty(), "only one fallback should deliver");
    }

    @Test
    @DisplayName("Fallback is NOT used while the primary sender is available")
    void fallbackNotUsedWhenPrimaryAvailable() {
        RecordingSender primary = new RecordingSender("Telegram");
        RecordingSender fallback = new RecordingSender("SMS");

        new UrgentNotification(primary, fallback).send("@cyr0th", "hello");

        assertEquals(1, primary.sent.size());
        assertTrue(fallback.sent.isEmpty());
    }

    @Test
    @DisplayName("Failure scenario: primary and all fallbacks unavailable -> nothing delivered anywhere")
    void everythingDown() {
        RecordingSender primary = new RecordingSender("Telegram", false);
        RecordingSender fallback = new RecordingSender("SMS", false);

        new UrgentNotification(primary, fallback).send("@cyr0th", "hello");

        assertTrue(primary.sent.isEmpty());
        assertTrue(fallback.sent.isEmpty());
    }

    @Test
    @DisplayName("Encrypted: message is transformed before sending and can be decrypted")
    void encryptedMessageIsTransformed() {
        RecordingSender email = new RecordingSender("Email");
        EncryptedNotification notification = new EncryptedNotification(email, "key-1");

        notification.send("a@b.com", "Code 482915");

        String sentBody = email.sent.get(0).body();
        assertTrue(sentBody.startsWith("ENC:"));
        assertFalse(sentBody.contains("482915"), "plain text must not appear in the sent body");
        assertEquals("Code 482915", notification.decrypt(sentBody));
    }

    @Test
    @DisplayName("Encrypted: a different key does not recover the plain text")
    void encryptedWrongKeyFails() {
        String cipher = new EncryptedNotification(new RecordingSender("Email"), "abc").encrypt("Top secret");

        String attempt = new EncryptedNotification(new RecordingSender("Email"), "xyz").decrypt(cipher);

        assertNotEquals("Top secret", attempt);
    }

    @Test
    @DisplayName("Failure scenario: empty encryption key is rejected")
    void encryptedEmptyKeyRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new EncryptedNotification(new RecordingSender("Email"), ""));
    }

    @Test
    @DisplayName("Batch + real SMS: 10 recipients, 2 invalid numbers -> Total 10 / Successful 8 / Failed 2")
    void batchSummaryWithSms() {
        List<String> phones = List.of(
                "+77001110001", "+77001110002", "+77001110003", "+77001110004", "+77001110005",
                "+77001110006", "12345", "+77001110008", "not-a-number", "+77001110010");
        ScheduledBatchNotification batch = new ScheduledBatchNotification(new SmsMessageSender(), phones);

        OutputCapture.capture(() -> batch.send("weekly-digest", "Report ready."));

        BatchSummary summary = batch.getLastSummary();
        assertEquals(10, summary.total());
        assertEquals(8, summary.successful());
        assertEquals(2, summary.failed());
        assertEquals("Total: 10\nSuccessful: 8\nFailed: 2", summary.toString());
    }

    @Test
    @DisplayName("Batch: every recipient receives the message with the batch label in the title")
    void batchSendsToEveryRecipient() {
        RecordingSender sender = new RecordingSender("Kafka");
        ScheduledBatchNotification batch = new ScheduledBatchNotification(sender, List.of("a", "b", "c"));
        batch.addRecipient("d");

        OutputCapture.capture(() -> batch.send("promo", "Hi"));

        assertEquals(4, sender.sent.size());
        assertEquals("Batch: promo", sender.sent.get(0).title());
        assertEquals(4, batch.getLastSummary().successful());
    }

    @Test
    @DisplayName("Failure scenario: batch through an unavailable provider -> all recipients fail")
    void batchWithUnavailableProviderFailsEverything() {
        RecordingSender sender = new RecordingSender("SMS", false);
        ScheduledBatchNotification batch = new ScheduledBatchNotification(sender, List.of("a", "b", "c"));

        OutputCapture.capture(() -> batch.send("promo", "Hi"));

        assertEquals(3, batch.getLastSummary().total());
        assertEquals(0, batch.getLastSummary().successful());
        assertEquals(3, batch.getLastSummary().failed());
    }

    @Test
    @DisplayName("Runtime switching: the same Notification object delivers through the new sender after setSender")
    void runtimeSwitching() {
        RecordingSender telegram = new RecordingSender("Telegram");
        RecordingSender email = new RecordingSender("Email");
        Notification notification = new UrgentNotification(telegram);

        notification.send("@cyr0th", "first");
        notification.setSender(email);
        notification.send("adrakula379@gmail.com", "second");

        assertEquals(1, telegram.sent.size());
        assertEquals(1, email.sent.size());
        assertEquals("[URGENT] second", email.sent.get(0).body());
        assertEquals("UrgentNotification via Email", notification.describe());
    }

    @Test
    @DisplayName("Failure scenario: null sender is rejected by constructor and by setSender")
    void nullSenderRejected() {
        assertThrows(NullPointerException.class, () -> new UrgentNotification((provider.MessageSender) null));
        Notification notification = new UrgentNotification(new RecordingSender("Telegram"));
        assertThrows(NullPointerException.class, () -> notification.setSender(null));
    }

    @Test
    @DisplayName("Marketing: [PROMO] title, opt-out hint, works with any sender")
    void marketingFormatsMessage() {
        RecordingSender sender = new RecordingSender("Email");

        new MarketingNotification(sender, "Autumn sale").send("a@b.com", "-30% today.");

        RecordingSender.Message m = sender.sent.get(0);
        assertEquals("[PROMO] Autumn sale", m.title());
        assertEquals("-30% today. Reply STOP to opt out.", m.body());
    }

    @Test
    @DisplayName("Marketing: opted-out recipients are skipped, others still receive the message")
    void marketingSkipsOptedOut() {
        RecordingSender sender = new RecordingSender("Email");
        MarketingNotification marketing = new MarketingNotification(sender, "Sale");
        marketing.unsubscribe("bob@example.com");

        marketing.send("bob@example.com", "Offer");
        marketing.send("adrakula379@gmail.com", "Offer");

        assertEquals(1, sender.sent.size());
        assertEquals("adrakula379@gmail.com", sender.sent.get(0).recipient());
    }

    @Test
    @DisplayName("Marketing + new PushNotificationSender work together without modifying either class")
    void marketingWithPush() {
        PushNotificationSender push = new PushNotificationSender();

        String out = OutputCapture.capture(() ->
                new MarketingNotification(push, "Autumn sale").send("TEST_DEVICE_TOKEN_NOT_REAL", "-30% today."));

        assertTrue(out.contains("[Push] delivered to device TEST_DEVICE_TOKEN_NOT_REAL"));
        assertTrue(out.contains("[PROMO] Autumn sale"));
    }

    @Test
    @DisplayName("Push works as a provider for Urgent and Encrypted notifications")
    void pushWithExistingTypes() {
        PushNotificationSender push = new PushNotificationSender();

        String urgentOut = OutputCapture.capture(() ->
                new UrgentNotification(push).send("TEST_DEVICE_TOKEN_NOT_REAL", "Server down"));
        String encryptedOut = OutputCapture.capture(() ->
                new EncryptedNotification(push).send("TEST_DEVICE_TOKEN_NOT_REAL", "Code 1234"));

        assertTrue(urgentOut.contains("[URGENT] Server down"));
        assertTrue(encryptedOut.contains("ENC:"));
        assertFalse(encryptedOut.contains("Code 1234"));
    }

    @Test
    @DisplayName("Push can be the fallback of an Urgent notification")
    void pushAsFallback() {
        RecordingSender telegram = new RecordingSender("Telegram", false);
        PushNotificationSender push = new PushNotificationSender();

        String out = OutputCapture.capture(() ->
                new UrgentNotification(telegram).addFallback(push, "TEST_DEVICE_TOKEN_NOT_REAL").send("@cyr0th", "Alert"));

        assertTrue(out.contains("Falling back to Push"));
        assertTrue(out.contains("[Push] delivered to device TEST_DEVICE_TOKEN_NOT_REAL"));
    }
}
