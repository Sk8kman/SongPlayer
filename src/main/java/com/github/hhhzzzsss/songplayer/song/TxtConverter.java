package com.github.hhhzzzsss.songplayer.song;

import com.github.hhhzzzsss.songplayer.SongPlayer;

import java.io.*;

public class TxtConverter {
    public static Song getSong(File file) throws IOException {
        if (!file.getName().endsWith(".txt")) {
            return null;
        }
        Song song = new Song(file.getName());
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        int lasttick = 0;
        int linen = 0;
        while ((line = br.readLine()) != null) {
            linen++;
            if (line.startsWith("#")) continue;
            String[] parts = line.split(":");
            if (parts.length != 3) {
                continue;
            }
            int tick;
            int pitch;
            int instrument;
            try {
                tick = Integer.parseInt(parts[0]);
                pitch = Integer.parseInt(parts[1]);
                instrument = Integer.parseInt(parts[2]);
            } catch(NumberFormatException | ClassCastException | NullPointerException e) {
                SongPlayer.addChatMessage("Found incorrectly formatted line at line #" + linen);
                continue;
            }
            if (tick < 0 || pitch < 0 || instrument < 0 || pitch > 24 || instrument > 15) {
                SongPlayer.addChatMessage("Found incorrectly formatted line at line #" + linen);
                continue;
            }
            if (tick > lasttick) {
                lasttick = tick;
            }
            song.add(new Note((instrument * 25 + pitch), tick * 50, (byte) 127, (short) 0));
        }
        song.length = lasttick * 50;
        return song;
    }
}
