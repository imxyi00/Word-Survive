package wordsurvive;

final class User {
    static final int STARTING_COINS = 300;

    String name, salt, hash, bio = "", created;
    int avatar, coins = STARTING_COINS;

    final int[] best = new int[SentenceBank.LEVELS];
    int bgVol = 70, sfxVol = 80, music = 0, theme = 0;

    User(String name, String salt, String hash, String created) {
        this.name = name; this.salt = salt; this.hash = hash; this.created = created;
    }

    int cleared() { int n = 0; while (n < best.length && best[n] > 0) n++; return n; }
    int stageScore() { int sum = 0; for (int b : best) sum += b; return sum; }
}
