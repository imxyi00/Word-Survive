package wordsurvive;

import java.util.*;

final class Puzzle {

    static final class Part {
        final String text; final int blank;
        Part(String text, int blank) { this.text = text; this.blank = blank; }
    }

    static final class Blank {
        final String word, meaning; final int tier;
        boolean solved;
        final boolean[] revealed;
        final HashSet<String> tried = new HashSet<>();
        int wrong, hints;
        long millis;
        char[] wheel;

        Blank(String word, String meaning, int tier) {
            this.word = word; this.meaning = meaning; this.tier = tier;
            this.revealed = new boolean[word.length()];
        }
    }

    final List<List<Part>> chunks = new ArrayList<>();
    final List<Blank> blanks = new ArrayList<>();

    int solvedCount() { int n = 0; for (Blank b : blanks) if (b.solved) n++; return n; }
    boolean done() { return solvedCount() == blanks.size(); }
}
