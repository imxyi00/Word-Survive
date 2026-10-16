package wordsurvive;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

final class ScoreBoard {
    static final class Entry {
        final String mode, name; int score;
        Entry(String mode, String name, int score) { this.mode = mode; this.name = name; this.score = score; }
    }

    private final ArrayList<Entry> entries = new ArrayList<>();
    private final Path file = Paths.get("data", "scores.csv");

    ScoreBoard() { load(); }

    void submit(String mode, String name, int score) {
        for (Entry e : entries) {
            if (e.mode.equals(mode) && e.name.equalsIgnoreCase(name)) {
                if (score > e.score) { e.score = score; save(); }
                return;
            }
        }
        entries.add(new Entry(mode, name, score));
        save();
    }

    List<Entry> top(String mode, int n) {
        List<Entry> list = new ArrayList<>();
        for (Entry e : entries) if (e.mode.equals(mode)) list.add(e);
        list.sort((a, b) -> Integer.compare(b.score, a.score));
        return list.subList(0, Math.min(n, list.size()));
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            List<String> lines = new ArrayList<>();
            for (Entry e : entries) lines.add(e.mode + "," + e.name + "," + e.score);
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException ex) { System.err.println("Could not save scores: " + ex.getMessage()); }
    }

    private void load() {
        if (!Files.exists(file)) return;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] p = line.split(",");
                if (p.length == 3) entries.add(new Entry(p[0], p[1], Integer.parseInt(p[2])));
            }
        } catch (IOException | NumberFormatException e) { System.err.println("Could not read scores: " + e.getMessage()); }
    }
}
