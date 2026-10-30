package tollmanagement.model;

public final class Admin {
    private final String username;
    public Admin(String username) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Admin username cannot be blank.");
        this.username = username.trim();
    }
    public String getUsername() { return username; }
}
