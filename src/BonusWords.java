package wordsurvive;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

final class BonusWords {
    private static Set<String> words;

    private BonusWords() { }

    static boolean valid(String w) {
        if (words == null) load();
        return words.contains(w.trim().toLowerCase());
    }

    private static void load() {
        words = new HashSet<>();
        loadWords("bonus_words.txt");
        loadWords("vocab.csv");
        loadWords("sentences.csv");
    }

    private static void loadWords(String asset) {
        try (InputStream in = Assets.open(asset)) {
            if (in == null) return;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (line.isEmpty() || line.startsWith("#")) continue;
                for (String tok : line.split("[^a-z]+")) {
                    if (tok.length() >= GameSession.MIN_BONUS_LEN) words.add(tok);
                }
            }
        } catch (IOException e) { System.err.println(asset + ": " + e.getMessage()); }
    }
}
