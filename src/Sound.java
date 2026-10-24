package wordsurvive;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

final class Sound {
    static final String[] TRACKS = {"Panda Window", "Bikes At The Pier"};
    private static final String[] FILES = {"music/PandaWindow.wav", "music/Bikes_At_the_Pier.wav"};
    private static int generation;
    private static final float RATE = 22050f;
    private static final Map<String, int[]> SFX = new HashMap<>();
    static {
        SFX.put("click", new int[]{84}); SFX.put("shuffle", new int[]{79, 76, 79, 83});
        SFX.put("correct", new int[]{72, 76, 79, 84}); SFX.put("wrong", new int[]{47, 44});
        SFX.put("hint", new int[]{88, 93}); SFX.put("win", new int[]{72, 76, 79, 84, 79, 84, 88, 91});
        SFX.put("lose", new int[]{60, 57, 53, 48}); SFX.put("select", new int[]{86});
    }

    static int bgVol = 70, sfxVol = 80, track = 0;
    private static Clip music;
    private static boolean broken;

    private Sound() { }

    static void sfx(String name) {
        int[] notes = SFX.get(name);
        if (notes == null || sfxVol == 0 || broken) return;
        double len = name.equals("click") || name.equals("select") ? 0.04 : name.equals("lose") || name.equals("wrong") ? 0.16 : 0.07;
        play(synth(notes, len), sfxVol);
    }

    static void startMusic() {
        stopMusic();
        if (broken) return;
        final int token;
        synchronized (Sound.class) { token = ++generation; }
        final String file = FILES[Math.floorMod(track, FILES.length)];
        Thread loader = new Thread(() -> {
            try (InputStream raw = Assets.open(file)) {
                if (raw == null) { System.err.println("[Word Survive] music file not found: " + file); return; }
                AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(raw));
                AudioFormat f = in.getFormat();
                if (f.getEncoding() != AudioFormat.Encoding.PCM_SIGNED || f.getSampleSizeInBits() != 16) {
                    AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, f.getSampleRate(), 16, f.getChannels(), f.getChannels() * 2, f.getSampleRate(), false);
                    in = AudioSystem.getAudioInputStream(target, in);
                }
                Clip clip = AudioSystem.getClip();
                clip.open(in);
                synchronized (Sound.class) {
                    if (token != generation) { clip.close(); return; }
                    music = clip;
                    gain(clip, bgVol);
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                }
            } catch (Throwable e) { System.err.println("[Word Survive] could not play music: " + e); }
        }, "music-loader");
        loader.setDaemon(true);
        loader.start();
    }

    static void stopMusic() {
        synchronized (Sound.class) {
            generation++;
            if (music != null) { music.stop(); music.close(); music = null; }
        }
    }

    static void setBg(int v) { bgVol = v; if (music != null) gain(music, v); }

    private static void play(byte[] pcm, int vol) {
        try {
            AudioFormat fmt = new AudioFormat(RATE, 16, 1, true, false);
            Clip clip = AudioSystem.getClip();
            clip.open(fmt, pcm, 0, pcm.length);
            gain(clip, vol);
            clip.start();
            clip.addLineListener(e -> { if (e.getType() == LineEvent.Type.STOP) clip.close(); });
        } catch (Throwable t) { broken = true; }
    }

    private static void gain(Clip clip, int vol) {
        try {
            FloatControl c = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            c.setValue(vol <= 0 ? c.getMinimum() : Math.max(c.getMinimum(), (float) (20 * Math.log10(vol / 100.0))));
        } catch (Throwable ignored) { }
    }

    private static byte[] synth(int[] notes, double noteSec) {
        int per = (int) (RATE * noteSec);
        byte[] out = new byte[notes.length * per * 2];
        for (int n = 0; n < notes.length; n++) {
            if (notes[n] == 0) continue;
            double f = 440 * Math.pow(2, (notes[n] - 69) / 12.0);
            for (int i = 0; i < per; i++) {
                double t = i / RATE, env = Math.min(1, (per - i) / (per * 0.6)) * Math.min(1, i / 60.0);
                double v = Math.signum(Math.sin(2 * Math.PI * f * t)) * 0.22;
                short s = (short) (v * env * 32767);
                out[(n * per + i) * 2] = (byte) (s & 0xFF);
                out[(n * per + i) * 2 + 1] = (byte) (s >> 8);
            }
        }
        return out;
    }
}
