package wordsurvive;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

final class SentenceBank {
    static final int LEVELS = 25, MAX_TIER = 5;
    private static final Pattern MARK = Pattern.compile("^(.*?)\\[(.+?)\\](.*)$");

    private record Info(String meaning, int tier) { }

    private final HashMap<String, Info> vocab = new HashMap<>();
    private final HashMap<Integer, ArrayList<String>> sentences = new HashMap<>();
    private final Random rnd = new Random();
    private final HashMap<Integer, ArrayDeque<String>> stageQueues = new HashMap<>();
    private final String[] lastStageSentence = new String[LEVELS + 1];
    private final ArrayDeque<String> rushQueue = new ArrayDeque<>();
    private int rushTier = 0;

    SentenceBank() {
        for (int t = 1; t <= MAX_TIER; t++) {
            sentences.put(t, new ArrayList<>());
            stageQueues.put(t, new ArrayDeque<>());
        }
        read("vocab.csv", (p) -> {
            String w = p[1].trim().toLowerCase();
            vocab.put(w, new Info(p[2].trim(), tierOfWord(w)));
        });
        read("sentences.csv", (p) -> {
            int t = Integer.parseInt(p[0].trim());
            String s = p[1].trim();
            Matcher m = Pattern.compile("\\[(.+?)\\]").matcher(s);
            while (m.find()) {
                String w = m.group(1).toLowerCase();
                if (!vocab.containsKey(w))
                    System.err.println("sentences.csv: no meaning in vocab.csv for '" + m.group(1) + "'");
                else if (tierOfWord(w) != t)
                    System.err.println("sentences.csv: word '" + m.group(1) + "' length " + w.length() + " does not match tier " + t);
            }
            sentences.computeIfAbsent(t, k -> new ArrayList<>()).add(s);
        });
    }

    private interface Row { void take(String[] parts); }

    private void read(String file, Row row) {
        try (InputStream in = Assets.open(file)) {
            if (in == null) return;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\|", file.equals("vocab.csv") ? 3 : 2);
                try { row.take(p); } catch (RuntimeException e) { System.err.println(file + ": bad row: " + line); }
            }
        } catch (IOException e) { System.err.println(file + ": " + e.getMessage()); }
    }

    int sentenceCount() { int n = 0; for (ArrayList<String> l : sentences.values()) n += l.size(); return n; }
    int wordCount() { return vocab.size(); }

    static int tierOf(int level) { return Math.min(MAX_TIER, 1 + (level - 1) / 5); }

    static int tierOfWord(String word) {
        int len = word.length();
        if (len <= 5) return 1;
        if (len == 6) return 2;
        if (len == 7) return 3;
        if (len == 8) return 4;
        return 5;
    }

    static int blanksOf(int level) { return 2 + (level - 1) / 5; }

    Puzzle forLevel(int level) {
        int tier = tierOf(level);
        return build(pickStage(level, tier), tier, blanksOf(level), (level - 1) % 5);
    }

    Puzzle forRush(int index) {
        if (index == 0) { rushTier = 0; rushQueue.clear(); }
        int tier = Math.min(MAX_TIER, 2 + index / 2);
        if (tier != rushTier) { rushTier = tier; rushQueue.clear(); }
        if (rushQueue.isEmpty()) {
            List<String> all = new ArrayList<>(sentences.get(tier));
            Collections.shuffle(all, rnd);
            rushQueue.addAll(all);
        }
        return build(rushQueue.poll(), tier, Math.min(6, 3 + index / 2), 2);
    }

    private String pickStage(int level, int tier) {
        ArrayDeque<String> q = stageQueues.computeIfAbsent(tier, k -> new ArrayDeque<>());
        if (q.isEmpty()) refillStageQueue(tier, q);
        String prev = level > 1 ? lastStageSentence[level - 1] : null;
        String self = lastStageSentence[level];
        if (q.size() > 1 && (q.peek().equals(prev) || q.peek().equals(self))) {
            q.addLast(q.pollFirst());
        }
        String picked = q.pollFirst();
        lastStageSentence[level] = picked;
        return picked;
    }

    private void refillStageQueue(int tier, ArrayDeque<String> q) {
        List<String> all = new ArrayList<>(sentences.get(tier));
        Collections.shuffle(all, rnd);
        q.addAll(all);
    }

    private Puzzle build(String sentence, int tier, int wantBlanks, int subLevel) {
        Puzzle pz = new Puzzle(tier);
        List<List<String[]>> raw = new ArrayList<>();
        List<String> markerWords = new ArrayList<>();
        for (String token : sentence.split(" ")) {
            List<String[]> parts = new ArrayList<>();
            String rest = token;
            Matcher m;
            while ((m = MARK.matcher(rest)).matches()) {
                if (!m.group(1).isEmpty()) parts.add(new String[]{m.group(1), null});
                parts.add(new String[]{m.group(2), m.group(2)});
                markerWords.add(m.group(2));
                rest = m.group(3);
            }
            if (!rest.isEmpty()) parts.add(new String[]{rest, null});
            raw.add(parts);
        }
        int markers = markerWords.size();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < markers; i++) order.add(i);
        Collections.shuffle(order, rnd);
        if (subLevel <= 1) order.sort(Comparator.comparingInt(i -> markerWords.get(i).length()));
        else if (subLevel >= 3) order.sort((a, b) -> Integer.compare(markerWords.get(b).length(), markerWords.get(a).length()));
        Set<Integer> chosen = new HashSet<>(order.subList(0, Math.min(wantBlanks, markers)));
        int seen = 0;
        for (List<String[]> parts : raw) {
            List<Puzzle.Part> chunk = new ArrayList<>();
            for (String[] p : parts) {
                if (p[1] == null) { chunk.add(new Puzzle.Part(p[0], -1)); continue; }
                if (chosen.contains(seen++)) {
                    Info info = vocab.getOrDefault(p[1].toLowerCase(), new Info("(no meaning found)", tier));
                    pz.blanks.add(new Puzzle.Blank(p[1].toLowerCase(), info.meaning(), info.tier()));
                    chunk.add(new Puzzle.Part(p[1], pz.blanks.size() - 1));
                } else chunk.add(new Puzzle.Part(p[1], -1));
            }
            pz.chunks.add(chunk);
        }
        return pz;
    }
}
