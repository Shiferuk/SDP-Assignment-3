import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import provider.EmailMessageSender;
import provider.KafkaEventSender;
import provider.MessageSender;
import provider.PushNotificationSender;
import provider.SmsMessageSender;
import provider.TelegramMessageSender;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SenderTest {

    private boolean lastResult;

    private String run(java.util.function.BooleanSupplier call) {
        return OutputCapture.capture(() -> lastResult = call.getAsBoolean());
    }

    @Test
    @DisplayName("SMS: text longer than 160 characters is truncated to exactly 160")
    void smsTruncatesLongText() {
        SmsMessageSender sms = new SmsMessageSender();
        String longBody = "x".repeat(400);

        String out = run(() -> sms.sendMessage("+77775712100", "Title", longBody));

        assertTrue(lastResult);
        assertTrue(out.contains("(160/160)"), out);
        assertTrue(out.contains("truncating"));
        assertTrue(out.contains("..."));
    }

    @Test
    @DisplayName("SMS: short text is sent unchanged, no truncation warning")
    void smsShortTextUntouched() {
        SmsMessageSender sms = new SmsMessageSender();

        String out = run(() -> sms.sendMessage("+77775712100", "Hi", "Short"));

        assertTrue(lastResult);
        assertFalse(out.contains("truncating"));
        assertTrue(out.contains("Hi: Short"));
    }

    @Test
    @DisplayName("SMS: invalid phone number is rejected (returns false)")
    void smsRejectsInvalidNumber() {
        String out = run(() -> new SmsMessageSender().sendMessage("12345", "T", "B"));

        assertFalse(lastResult);
        assertTrue(out.contains("REJECTED"));
    }

    @Test
    @DisplayName("Email: title becomes the subject and a signature is appended")
    void emailHasSubjectAndSignature() {
        String out = run(() -> new EmailMessageSender().sendMessage("adrakula379@gmail.com", "Hello", "Body"));

        assertTrue(lastResult);
        assertTrue(out.contains("Subject: Hello"));
        assertTrue(out.contains("Notification Gateway Team"));
    }

    @Test
    @DisplayName("Email: invalid address is rejected")
    void emailRejectsInvalidAddress() {
        run(() -> new EmailMessageSender().sendMessage("not-an-email", "T", "B"));

        assertFalse(lastResult);
    }

    @Test
    @DisplayName("Telegram: message is formatted with a bold Markdown title")
    void telegramFormatsMarkdown() {
        String out = run(() -> new TelegramMessageSender().sendMessage("@cyr0th", "Alert", "Body"));

        assertTrue(lastResult);
        assertTrue(out.contains("*Alert*"));
        assertTrue(out.contains("parse_mode=Markdown"));
    }

    @Test
    @DisplayName("Kafka: publishes a JSON event and the offset grows with every event")
    void kafkaPublishesEventsWithGrowingOffset() {
        KafkaEventSender kafka = new KafkaEventSender();

        String first = run(() -> kafka.sendMessage("user-1", "T", "B"));
        String second = run(() -> kafka.sendMessage("user-1", "T", "B"));

        assertTrue(first.contains("offset=0") && first.contains("\"type\":\"NOTIFICATION\""));
        assertTrue(second.contains("offset=1"));
    }

    @Test
    @DisplayName("Kafka: blank key is rejected; JSON special characters are escaped")
    void kafkaValidationAndEscaping() {
        KafkaEventSender kafka = new KafkaEventSender();

        run(() -> kafka.sendMessage(" ", "T", "B"));
        assertFalse(lastResult);

        String out = run(() -> kafka.sendMessage("k", "T", "say \"hi\""));
        assertTrue(lastResult);
        assertTrue(out.contains("say \\\"hi\\\""));
    }

    @Test
    @DisplayName("Every provider reports isAvailable()=false after being switched off, and refuses to send")
    void unavailableProvidersRefuseToSend() {
        TelegramMessageSender telegram = new TelegramMessageSender(false);
        EmailMessageSender email = new EmailMessageSender(false);
        SmsMessageSender sms = new SmsMessageSender(false);
        KafkaEventSender kafka = new KafkaEventSender(false);
        PushNotificationSender push = new PushNotificationSender(false);

        List<MessageSender> all = List.of(telegram, email, sms, kafka, push);
        for (MessageSender s : all) {
            assertFalse(s.isAvailable(), s.getChannelName());
        }
        assertFalse(telegram.sendMessage("@cyr0th", "T", "B"));
        assertFalse(email.sendMessage("adrakula379@gmail.com", "T", "B"));
        assertFalse(sms.sendMessage("+77775712100", "T", "B"));
        assertFalse(kafka.sendMessage("key", "T", "B"));
        assertFalse(push.sendMessage("TEST_DEVICE_TOKEN_NOT_REAL", "T", "B"));
    }

    @Test
    @DisplayName("Push: valid device token is delivered with a JSON payload")
    void pushDeliversToValidToken() {
        String out = run(() -> new PushNotificationSender().sendMessage("TEST_DEVICE_TOKEN_NOT_REAL", "Hello", "World"));

        assertTrue(lastResult);
        assertTrue(out.contains("\"title\":\"Hello\""));
        assertTrue(out.contains("\"body\":\"World\""));
    }

    @Test
    @DisplayName("Push: invalid token and oversized payload are rejected")
    void pushRejectsBadInput() {
        PushNotificationSender push = new PushNotificationSender();

        run(() -> push.sendMessage("bad", "T", "B"));
        assertFalse(lastResult);

        run(() -> push.sendMessage("TEST_DEVICE_TOKEN_NOT_REAL", "T", "x".repeat(300)));
        assertFalse(lastResult);
    }

    @Test
    @DisplayName("All five providers expose distinct channel names")
    void channelNamesAreDistinct() {
        Set<String> names = new HashSet<>();
        for (MessageSender s : List.of(new TelegramMessageSender(), new EmailMessageSender(),
                new SmsMessageSender(), new KafkaEventSender(), new PushNotificationSender())) {
            names.add(s.getChannelName());
        }
        assertEquals(5, names.size());
    }
}
