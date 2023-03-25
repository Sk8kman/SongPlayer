package com.github.hhhzzzsss.songplayer.config;

import com.github.hhhzzzsss.songplayer.SongPlayer;

import java.io.*;
import java.util.Properties;

public class ModProperties {
    private static ModProperties instance = new ModProperties();
    public static ModProperties getInstance() {
        return instance;
    }
    private File cfgfile = SongPlayer.CONFIG_FILE;
    private String cfgPath = cfgfile.getPath();
    private Properties config = new Properties();

    public Properties getConfig() {
        return config;
    }
    public void createValue(String setting, String value) {
        if (config.containsKey(setting)) {
            return;
        }
        config.setProperty(setting, value);
    }
    public void updateValue(String setting, String value) {
        if (!config.containsKey(setting)) {
            System.out.println("Dang it I just disappointed " + SongPlayer.MC.player.getName() + "... Sorry you have to see this error message.");
            SongPlayer.addChatMessage("§cThere was an error attempting to save your settings.");
            return;
        }
        config.setProperty(setting, value);
        save();
    }
    public void save() {
        try {
            config.store(new FileOutputStream(cfgPath), "config file for SongPlayer");
        } catch(IOException e) {
            e.printStackTrace();
            System.out.println("Dang it I just disappointed " + SongPlayer.MC.player.getName() + "... Sorry you have to see this error message.");
            SongPlayer.addChatMessage("§cThere was an error attempting to save your settings.");
        }
    }

    public void setup() {
        try {
            cfgfile.createNewFile();
            if (!SongPlayer.SONG_DIR.exists()) {
                SongPlayer.SONG_DIR.mkdir();
            }
            FileInputStream cfginput = new FileInputStream(cfgPath);
            config.load(cfginput);
            createValue("prefix", "$");
            createValue("creativeCommand", "gamemode creative");
            createValue("survivalCommand", "gamemode survival");
            createValue("playSoundCommand", "execute at @a run playsound minecraft:block.note_block.{type} record @p ~ ~300000000 ~ 3000000000 {pitch} 1");
            createValue("stageType", "default");
            createValue("showProgressCommand", "title @a actionbar [" +
                    "{\"color\":\"gold\",\"text\":\"Now playing \"}," +
                    "{\"color\":\"blue\",\"text\":\"{MIDI}\"}," +
                    "{\"color\":\"gold\",\"text\":\" | \"}," +
                    "{\"color\":\"dark_aqua\",\"text\":\"{CurrentTime}/{SongTime}\"}]");
            createValue("rotate", String.valueOf(false));
            createValue("swing", String.valueOf(false));
            createValue("useCommandsForPlaying", String.valueOf(false));
            createValue("switchGamemode", String.valueOf(true));
            save();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Dang it I just disappointed " + SongPlayer.MC.player.getName() + "... Sorry you have to see this error message.");
            SongPlayer.addChatMessage("§cThere was an error attempting to save your settings.");
        }
    }
}
