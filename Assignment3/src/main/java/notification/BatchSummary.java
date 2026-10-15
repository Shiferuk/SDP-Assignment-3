package notification;

public record BatchSummary(int total, int successful, int failed) {
    @Override
    public String toString() {
        return "Total: " + total + "\nSuccessful: " + successful + "\nFailed: " + failed;
    }
}
