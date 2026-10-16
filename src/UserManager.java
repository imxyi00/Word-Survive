package wordsurvive;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.*;

final class UserManager {
    private final HashMap<String, User> users = new HashMap<>();
    private final Path file = Paths.get("data", "users.tsv");

    UserManager() { load(); }

    static String validName(String name) {
        return name.matches("[A-Za-z0-9._@+-]{3,30}") ? null : "Use 3-30 letters, numbers or . _ @ + -";
    }

    String register(String name, String password) {
        name = name.trim();
        String bad = validName(name);
        if (bad != null) return bad;
        if (password.length() < 4) return "Password needs at least 4 characters.";
        if (users.containsKey(name.toLowerCase())) return "That name is already taken.";
        String salt = randomSalt();
        users.put(name.toLowerCase(), new User(name, salt, hash(salt, password), LocalDate.now().toString()));
        save();
        return null;
    }

    User login(String name, String password) {
        User u = users.get(name.trim().toLowerCase());
        return (u != null && u.hash.equals(hash(u.salt, password))) ? u : null;
    }

    User get(String name) { return users.get(name.toLowerCase()); }

    String rename(User u, String newName) {
        newName = newName.trim();
        if (newName.equals(u.name)) return null;
        String bad = validName(newName);
        if (bad != null) return bad;
        String key = newName.toLowerCase();
        if (!key.equals(u.name.toLowerCase()) && users.containsKey(key)) return "That name is already taken.";
        users.remove(u.name.toLowerCase());
        u.name = newName;
        users.put(key, u);
        save();
        return null;
    }

    void save() {
        try {
            Files.createDirectories(file.getParent());
            List<String> lines = new ArrayList<>();
            for (User u : users.values()) {
                StringBuilder best = new StringBuilder();
                for (int i = 0; i < u.best.length; i++) best.append(i > 0 ? "," : "").append(u.best[i]);
                lines.add(String.join("\t", enc(u.name), u.salt, u.hash, String.valueOf(u.avatar), enc(u.bio), u.created,
                        String.valueOf(u.coins), best.toString(), String.valueOf(u.bgVol), String.valueOf(u.sfxVol),
                        String.valueOf(u.music), String.valueOf(u.theme)));
            }
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) { System.err.println("Could not save users: " + e.getMessage()); }
    }

    private void load() {
        if (!Files.exists(file)) return;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] p = line.split("\t", -1);
                if (p.length < 12) continue;
                try {
                    User u = new User(dec(p[0]), p[1], p[2], p[5]);
                    u.avatar = Integer.parseInt(p[3]); u.bio = dec(p[4]); u.coins = Integer.parseInt(p[6]);
                    String[] b = p[7].split(",");
                    for (int i = 0; i < Math.min(b.length, u.best.length); i++) u.best[i] = Integer.parseInt(b[i]);
                    u.bgVol = Integer.parseInt(p[8]); u.sfxVol = Integer.parseInt(p[9]);
                    u.music = Integer.parseInt(p[10]); u.theme = Integer.parseInt(p[11]);
                    users.put(u.name.toLowerCase(), u);
                } catch (NumberFormatException ignored) { }
            }
        } catch (IOException e) { System.err.println("Could not read users: " + e.getMessage()); }
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private static String dec(String s) { return URLDecoder.decode(s, StandardCharsets.UTF_8); }

    private static String randomSalt() {
        byte[] b = new byte[8]; new SecureRandom().nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    private static String hash(String salt, String pw) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest((salt + pw).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
