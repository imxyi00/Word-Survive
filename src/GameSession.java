package wordsurvive;

import java.util.*;

final class GameSession {
    enum Mode { STAGE, RUSH }
    enum Result { INCOMPLETE, CORRECT, WRONG, REPEATED }

    static final int MAX_LIVES = 3, HINT_COST = 50, RUSH_PENALTY_MS = 5000;

    final Mode mode;
    final int level;
    private final SentenceBank bank;
    private final Random rnd = new Random();

    Puzzle puzzle;
    int selected = -1, lives = MAX_LIVES, points = 0, puzzlesDone = 0;
    long timeLeftMs;

    private GameSession(Mode mode, int level, int seconds, SentenceBank bank) {
        this.mode = mode; this.level = level; this.bank = bank; this.timeLeftMs = seconds * 1000L;
        loadPuzzle();
    }

    static GameSession stage(SentenceBank bank, int level) { return new GameSession(Mode.STAGE, level, 0, bank); }
    static GameSession rush(SentenceBank bank, int seconds) { return new GameSession(Mode.RUSH, 0, seconds, bank); }

    void loadPuzzle() {
        puzzle = mode == Mode.STAGE ? bank.forLevel(level) : bank.forRush(puzzlesDone);
        selected = -1;
        select(0);
    }

    Puzzle.Blank current() { return selected < 0 ? null : puzzle.blanks.get(selected); }

    void select(int i) {
        if (i < 0 || i >= puzzle.blanks.size() || puzzle.blanks.get(i).solved) return;
        selected = i;
        Puzzle.Blank b = puzzle.blanks.get(i);
        if (b.wheel == null) { b.wheel = b.word.toUpperCase().toCharArray(); shuffle(); }
    }

    void selectNextUnsolved() {
        selected = -1;
        for (int i = 0; i < puzzle.blanks.size(); i++) if (!puzzle.blanks.get(i).solved) { select(i); return; }
    }

    void shuffle() {
        Puzzle.Blank b = current();
        if (b == null) return;
        char[] before = b.wheel.clone();
        List<Character> list = new ArrayList<>();
        for (char c : b.wheel) list.add(c);
        for (int tries = 0; tries < 12; tries++) {
            Collections.shuffle(list, rnd);
            char[] now = new char[list.size()];
            for (int i = 0; i < now.length; i++) now[i] = list.get(i);
            b.wheel = now;
            if (!Arrays.equals(now, before) && !new String(now).equalsIgnoreCase(b.word)) break;
        }
    }

    String pattern() {
        Puzzle.Blank b = current();
        if (b == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.word.length(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(b.revealed[i] ? Character.toUpperCase(b.word.charAt(i)) : '_');
        }
        return sb.toString();
    }

    boolean canHint(User u) {
        Puzzle.Blank b = current();
        if (b == null || u.coins < HINT_COST) return false;
        int hidden = 0;
        for (boolean r : b.revealed) if (!r) hidden++;
        return hidden > 1;
    }

    boolean useHint(User u) {
        if (!canHint(u)) return false;
        Puzzle.Blank b = current();
        List<Integer> hidden = new ArrayList<>();
        for (int i = 0; i < b.revealed.length; i++) if (!b.revealed[i]) hidden.add(i);
        b.revealed[hidden.get(rnd.nextInt(hidden.size()))] = true;
        b.hints++;
        u.coins -= HINT_COST;
        return true;
    }

    Result guess(String attempt) {
        Puzzle.Blank b = current();
        if (b == null || attempt.length() != b.word.length()) return Result.INCOMPLETE;
        if (attempt.equalsIgnoreCase(b.word)) {
            b.solved = true;
            points += wordPoints(b);
            return Result.CORRECT;
        }
        if (!b.tried.add(attempt.toUpperCase())) return Result.REPEATED;
        b.wrong++;
        if (mode == Mode.STAGE) lives--; else timeLeftMs = Math.max(0, timeLeftMs - RUSH_PENALTY_MS);
        return Result.WRONG;
    }

    void tick(long ms) {
        Puzzle.Blank b = current();
        if (b != null && !b.solved) b.millis += ms;
        if (mode == Mode.RUSH) timeLeftMs = Math.max(0, timeLeftMs - ms);
    }

    int wordPoints(Puzzle.Blank b) {
        int base = 10 * b.word.length() + 15 * b.tier;
        int speed = (int) Math.max(0, 30 - b.millis / 1000);
        return Math.max(10, base + speed - 10 * b.wrong - 20 * b.hints);
    }

    int clearBonus() { return mode == Mode.STAGE ? 50 + 25 * lives : 25; }

    boolean puzzleDone() { return puzzle.done(); }
    boolean outOfLives() { return mode == Mode.STAGE && lives <= 0; }
    boolean timeUp() { return mode == Mode.RUSH && timeLeftMs <= 0; }

    static int coinsFor(int points, boolean full) { return Math.max(1, full ? points / 10 : points / 20); }
}
