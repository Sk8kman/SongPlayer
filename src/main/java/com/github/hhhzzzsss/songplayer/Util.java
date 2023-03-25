package com.github.hhhzzzsss.songplayer;

import com.github.hhhzzzsss.songplayer.config.ModProperties;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.playing.Stage;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.*;

public class Util {
    public static String currentPlaylist = "";
    public static int playlistIndex = 0;
    public static boolean loopPlaylist = false;
    public static String playlistOrder = "addedfirst";
    public static ArrayList<String> playlistSongs = new ArrayList<>();
    public static long playcooldown = Calendar.getInstance().getTime().getTime();
    public static String lastTimeExecuted = "";
    public static float pitch = 0;
    public static float yaw = 0;
    public static double playerPosX = 0;
    public static double playerPosZ = 0;
    
    public static void advancePlaylist() {
        if (Util.currentPlaylist.isEmpty()) {
            return;
        }
        Util.playlistIndex += 1;
        if (Util.playlistIndex >= Util.playlistSongs.size()) {
            if (Util.loopPlaylist) {
                Util.playlistIndex = 0;
            } else {
                SongPlayer.addChatMessage("§6Done playing playlist §3" + Util.currentPlaylist);
                SongHandler.getInstance().cleanup(true);
                return;
            }
        }
        int bruh = Util.playlistIndex + 1;
        SongPlayer.addChatMessage("§6Loading §3" + Util.playlistSongs.get(Util.playlistIndex) + "§6 from playlist §3" + Util.currentPlaylist + "§6 | §9" + bruh + "/" + Util.playlistSongs.size());
        SongHandler.getInstance().loadSong(Util.playlistSongs.get(Util.playlistIndex), new File("SongPlayer/playlists/" + Util.currentPlaylist));
    }
    public static float[] getAngleAtBlock(BlockPos to) {
        BlockPos playerPos = Stage.position;
        double dx = to.getX() - playerPos.getX();
        double dy = to.getY() - (playerPos.getY() + 1);
        double dz = to.getZ() - playerPos.getZ();
        //double distance = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2) + Math.pow(dz, 2));
        float yaw = (float) ((Math.atan2(dz, dx) * 180 / PI) - 90);//Math.atan2(dz, dx);
        float pitch = (float) (Math.atan2(dy, Math.sqrt(Math.pow(dx, 2) + Math.pow(dz, 2))) * 180 / PI);//Math.atan2(Math.sqrt(dz * dz + dx * dx), dy) + Math.PI;
        Util.pitch = -pitch;
        Util.yaw = yaw;
        return (new float[] {-pitch, yaw});
    }
    public static void updateStageLocationToPlayer() {
        if (SongHandler.getInstance().stage == null) {
            return;
        }
        Stage.position = SongPlayer.MC.player.getBlockPos();
        SongHandler.getInstance().stage.checkBuildStatus(SongHandler.getInstance().currentSong);
    }
    public static String formatTime(long milliseconds) {
        long temp = abs(milliseconds);
        temp /= 1000;
        long seconds = temp % 60;
        temp /= 60;
        long minutes = temp % 60;
        temp /= 60;
        long hours = temp;
        StringBuilder sb = new StringBuilder();
        if (milliseconds < 0) {
            sb.append("-");
        }
        if (hours > 0) {
            sb.append(String.format("%d:", hours));
            sb.append(String.format("%02d:", minutes));
        } else {
            sb.append(String.format("%d:", minutes));
        }
        sb.append(String.format("%02d", seconds));
        return sb.toString();
    }

    public static Pattern timePattern = Pattern.compile("(?:(\\d+):)?(\\d+):(\\d+)");
    public static long parseTime(String timeStr) throws IOException {
        Matcher matcher = timePattern.matcher(timeStr);
        if (matcher.matches()) {
            long time = 0;
            String hourString = matcher.group(1);
            String minuteString = matcher.group(2);
            String secondString = matcher.group(3);
            if (hourString != null) {
                time += Integer.parseInt(hourString) * 60 * 60 * 1000;
            }
            time += Integer.parseInt(minuteString) * 60 * 1000;
            time += Double.parseDouble(secondString) * 1000.0;
            return time;
        } else {
            throw new IOException("Invalid time pattern");
        }
    }

    public static void disableFlightIfNeeded() {
        if (SongPlayer.MC.interactionManager.getCurrentGameMode() == GameMode.CREATIVE || SongPlayer.MC.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR || !SongPlayer.switchGamemode) {
            return;
        }
        ClientPlayerEntity player = SongPlayer.MC.player;
        player.getAbilities().allowFlying = false;
        player.getAbilities().flying = false;
    }

    public static void enableFlightIfNeeded() {
        if (SongPlayer.useCommandsForPlaying || !SongPlayer.switchGamemode) {
            return;
        }
        if (SongHandler.getInstance().currentSong == null) {
            return;
        }
        ClientPlayerEntity player = SongPlayer.MC.player;
        player.getAbilities().allowFlying = true;
        player.getAbilities().flying = true;
    }

    public static void updateValuesToConfig() {
        SongPlayer.creativeCommand = ModProperties.getInstance().getConfig().getProperty("creativeCommand", "gamemode creative");
        SongPlayer.survivalCommand = ModProperties.getInstance().getConfig().getProperty("survivalCommand", "gamemode survival");
        SongPlayer.playSoundCommand = ModProperties.getInstance().getConfig().getProperty("playSoundCommand", "playsound minecraft:block.note_block.{type} record @a ~ ~ ~ 100 {pitch}");
        SongPlayer.stageType = ModProperties.getInstance().getConfig().getProperty("stageType", "default");
        SongPlayer.rotate = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("rotate", "false"));
        SongPlayer.swing = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("swing", "false"));
        SongPlayer.useCommandsForPlaying = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("useCommandsForPlaying", "false"));
        SongPlayer.switchGamemode = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("switchGamemode", "true"));
        SongPlayer.showProgressCommand = ModProperties.getInstance().getConfig().getProperty("showProgressCommand",
                "title @a actionbar [" +
                        "{\"color\":\"gold\",\"text\":\"Now playing \"}," +
                        "{\"color\":\"blue\",\"text\":\"{MIDI}\"}," +
                        "{\"color\":\"gold\",\"text\":\" | \"}," +
                        "{\"color\":\"dark_aqua\",\"text\":\"{CurrentTime}/{SongTime}\"}]");
        SongPlayer.prefix = ModProperties.getInstance().getConfig().getProperty("prefix", "$");
        if (SongPlayer.prefix.startsWith("/")) { //prevent prefix from starting with '/'
            ModProperties.getInstance().updateValue("prefix", "$");
            SongPlayer.prefix = "$";
        }
    }
}
