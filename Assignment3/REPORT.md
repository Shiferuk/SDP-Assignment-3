## 1. Problem: Class Explosion

With inheritance only, every combination of notification type and provider needs its own class:

```
SimpleTelegramNotification    SimpleEmailNotification    SimpleSmsNotification    SimpleKafkaNotification
UrgentTelegramNotification    UrgentEmailNotification    UrgentSmsNotification    UrgentKafkaNotification
EncryptedTelegramNotification EncryptedEmailNotification EncryptedSmsNotification EncryptedKafkaNotification
```

3 types x 4 providers = 12 classes. Adding one provider adds 3 classes; adding one type adds 3 more.
The urgent/encryption logic is also duplicated in every provider-specific class, and the provider
cannot be changed once the object exists.

## 2. Solution: Bridge

Two hierarchies vary independently:

* **Abstraction** - `Notification` and its subclasses decide *what* is sent (urgent with fallback, encrypted, scheduled batch).
* **Implementation** - `MessageSender` and its implementations decide *how* it is delivered (Telegram, Email, SMS, Kafka).

`Notification` holds a `MessageSender` field (the bridge) and delegates the final delivery to it.
Subclasses transform the message and call `sender.sendMessage(...)`; they never know which provider is behind the interface.

## 3. Result

| | Without Bridge | With Bridge |
|---|---|---|
| Initial design: 3 types × 4 providers | 12 combination classes | 7 classes (3 types + 4 providers) |
| Add a 4th provider to that design | +3 classes | +1 class |
| Add a 4th notification type to that design | +4 classes | +1 class |

After both independent extensions, the final design has 4 notification types and 5 providers:
20 possible combinations are supported by 9 classes (4 notification types + 5 providers).
| Switch provider at runtime | not possible | `setSender(...)` |

## 4. Principles
* **Open/Closed:** new providers/types are added without editing existing classes.
* **Single Responsibility:** formatting/encryption lives in notification types; transport lives in providers.
* **Composition over inheritance:** the link between hierarchies is a field, not a subclass.

## 5. Bridge vs Abstract Factory (Assignment 2)
Abstract Factory creates *families* of compatible objects (Mac/Windows/Gtk widgets). Bridge keeps two
dimensions of variation separate and combines them at runtime. They also combine well: a factory could build
the `MessageSender` that is passed into a `Notification`.

## 6. Implementor Hierarchy
`MessageSender` exposes `sendMessage(recipient, title, body)`, `isAvailable()` and `getChannelName()`.
Telegram formats Markdown, Email adds subject and signature, SMS enforces 160 characters, Kafka publishes a JSON event.

## 7. Abstraction Hierarchy
* **UrgentNotification** adds `[URGENT]`, checks `isAvailable()` and falls back to a second `MessageSender`
  (optionally with a different recipient, since a Telegram handle is not a valid phone number).
* **EncryptedNotification** transforms the message with XOR + Base64 before delegating (demo, not real crypto).
* **ScheduledBatchNotification** holds a list of recipients, sends to each, and produces a `BatchSummary`
  (`Total / Successful / Failed`). Its `send(recipient, message)` uses the first argument as a batch label.

None of these classes contains provider-specific code - they only use the `MessageSender` interface, which is the point of Bridge.

## 8. Runtime Switching and Fallback
`setSender(...)` replaces the implementor reference, so the same `Notification` object can deliver through
Telegram, then Email, then SMS. `UrgentNotification` keeps an ordered list of fallback `MessageSender`s and
picks the first available one using only `isAvailable()` - there is no `instanceof` check on a concrete provider.

## 9. Independent Extension
`PushNotificationSender` (new implementor) and `MarketingNotification` (new refined abstraction) were added as new
files only; no existing class was modified. 4 types x 5 providers = 20 combinations are covered by 9 concrete classes.

## 10. Demo and Tests
`Main` shows the six mandatory scenarios using only the existing classes. The 32 JUnit 5 tests cover delivery,
unavailable providers, fallback, the SMS limit, encryption, batches, runtime switching, the new Push provider,
the new Marketing type and several failure scenarios. A recording test double (`RecordingSender`) implements
`MessageSender`, so the abstraction is tested without any real provider - another benefit of Bridge.
