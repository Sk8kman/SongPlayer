package com.github.hhhzzzsss.songplayer;

import java.io.File;

import com.github.hhhzzzsss.songplayer.config.ModProperties;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

public class SongPlayer implements ModInitializer {
	public static final MinecraftClient MC = MinecraftClient.getInstance();
	public static final int NOTEBLOCK_BASE_ID = Block.getRawIdFromState(Blocks.NOTE_BLOCK.getDefaultState());
	public static final File SONG_DIR = new File("songs");
	public static final File CONFIG_FILE = new File("SongPlayer/songPlayer.properties");
	public static final File PLAYLISTS_DIR = new File("SongPlayer/playlists");
	public static boolean showFakePlayer = false;
	public static FakePlayerEntity fakePlayer;
	public static String creativeCommand = ModProperties.getInstance().getConfig().getProperty("creativeCommand", "gamemode creative");
	public static String survivalCommand = ModProperties.getInstance().getConfig().getProperty("survivalCommand", "gamemode survival");
	public static String playSoundCommand = ModProperties.getInstance().getConfig().getProperty("playSoundCommand", "execute at @a run playsound minecraft:block.note_block.{type} record @p ~ ~300000000 ~ 3000000000 {pitch} 1");
	public static String stageType = ModProperties.getInstance().getConfig().getProperty("stageType", "default");
	public static boolean rotate = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("rotate", "false"));
	public static boolean swing = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("swing", "false"));
	public static boolean useCommandsForPlaying = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("useCommandsForPlaying", "false"));
	public static boolean switchGamemode = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("switchGamemode", "true"));
	public static String showProgressCommand = ModProperties.getInstance().getConfig().getProperty("showProgressCommand",
			"title @a actionbar [" +
			"{\"color\":\"gold\",\"text\":\"Now playing \"}," +
			"{\"color\":\"blue\",\"text\":\"{MIDI}\"}," +
			"{\"color\":\"gold\",\"text\":\" | \"}," +
			"{\"color\":\"dark_aqua\",\"text\":\"{CurrentTime}/{SongTime}\"}]");
	public static String prefix = ModProperties.getInstance().getConfig().getProperty("prefix", "$");
	public static double[] pitchGlobal = { //used for /playsound
		0.5, 0.529732, 0.561231, 0.594604, 0.629961, 0.66742, 0.707107, 0.749154, 0.793701, 0.840896, 0.890899, 0.943874, 1.0, 1.059463, 1.122462, 1.189207, 1.259921, 1.33484, 1.414214, 1.498307, 1.587401, 1.681793, 1.781797, 1.887749, 2.0};


	@Override
	public void onInitialize() {
		System.out.println("Loading SongPlayer v3.1.2 made by hhhzzzsss, forked by Sk8kman, and tested by Lizard16");
		CommandProcessor.initCommands();
		PLAYLISTS_DIR.mkdirs(); //make directories for everything
		ModProperties.getInstance().setup(); //set up config file
		Util.updateValuesToConfig(); //update values from config file
	}
	
	public static void addChatMessage(String message) {
		MC.player.sendMessage(Text.of(message), false);
	}

	public static void removeFakePlayer() {
		if (fakePlayer != null) {
			fakePlayer.remove(Entity.RemovalReason.DISCARDED);
			fakePlayer = null;
		}
	}
}
