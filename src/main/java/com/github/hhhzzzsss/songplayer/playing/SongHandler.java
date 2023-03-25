package com.github.hhhzzzsss.songplayer.playing;

import com.github.hhhzzzsss.songplayer.FakePlayerEntity;
import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.Util;
import com.github.hhhzzzsss.songplayer.song.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Objects;

public class SongHandler {
    public static ItemStack oldItemHeld = null;
    private static SongHandler instance = new SongHandler();
    public static SongHandler getInstance() {
        return instance;
    }


    public static SongLoaderThread loaderThread = null;
    public LinkedList<Song> songQueue = new LinkedList<>();
    public Song currentSong = null;
    public Stage stage = null;
    public boolean building = false;

    public void onRenderIngame(boolean tick) {
        if (currentSong == null && songQueue.size() > 0) {
            setSong(songQueue.poll());
        }
        if (loaderThread != null && !loaderThread.isAlive()) {
            if (loaderThread.exception != null) {
                SongPlayer.addChatMessage("§cFailed to load song: §4" + loaderThread.exception.getMessage());
                Util.advancePlaylist();
            } else {
                if (currentSong == null) {
                    setSong(loaderThread.song);
                } else {
                    queueSong(loaderThread.song);
                }
            }
            loaderThread = null;
        }

        if (currentSong == null) {
            if (songQueue.isEmpty() && Util.playlistSongs.isEmpty()) {
                if (stage != null || SongPlayer.fakePlayer != null) {
                    if (stage != null) {
                        stage.movePlayerToStagePosition(false, false, false);
                    }
                    cleanup(false);
                }
            }
            return;
        }

        if (stage == null) {
            stage = new Stage();
            if (songQueue.isEmpty() && Util.playlistSongs.isEmpty()) {
                stage.movePlayerToStagePosition(false, false, false);
            }
        }
        if (SongPlayer.showFakePlayer && SongPlayer.fakePlayer == null && SongPlayer.switchGamemode) {
            SongPlayer.fakePlayer = new FakePlayerEntity();
            SongPlayer.fakePlayer.copyStagePosAndPlayerLook();
        }
        if (!SongPlayer.showFakePlayer && SongPlayer.fakePlayer != null) {
            SongPlayer.removeFakePlayer();
        }
        checkCommandCache();
        if (SongPlayer.switchGamemode) {
            SongPlayer.MC.player.getAbilities().allowFlying = true;
        }
        if (building && SongPlayer.switchGamemode) {
            if (tick) {
                handleBuilding();
            }
        } else {
            // Check if stage was broken
            handlePlaying(tick);
        }
    }

    public void loadSong(String location, File dir) {
        if (loaderThread != null) {
            SongPlayer.addChatMessage("§cAlready loading a song, cannot load another");
            return;
        }
        try {
            loaderThread = new SongLoaderThread(location, dir);
            if (Util.currentPlaylist.isEmpty()) {
                SongPlayer.addChatMessage("§6Loading §3" + location + "");
            }
            loaderThread.start();
        } catch (IOException e) {
            SongPlayer.addChatMessage("§cFailed to load song: §4" + e.getMessage());
        }
    }

    public void setSong(Song song) {
        currentSong = song;
        if (SongPlayer.useCommandsForPlaying || !SongPlayer.switchGamemode) {
            building = false;
            return;
        }
        if (stage == null) {
            stage = new Stage();
            stage.movePlayerToStagePosition();
        }
        stage.movePlayerToStagePosition(true, false, true);
        stage.checkBuildStatus(currentSong);
        if (stage.nothingToBuild()) {
            setSurvivalIfNeeded();
            return;
        }
        SongPlayer.addChatMessage("§6Building noteblocks");
        building = true;
        setCreativeIfNeeded();
    }

    private void queueSong(Song song) {
        if (!Util.playlistSongs.isEmpty() || !Util.currentPlaylist.isEmpty()) {
            SongPlayer.addChatMessage("§cUnable to add song to queue; Playlist is in progress");
            return;
        }
        songQueue.add(song);
        if (Util.currentPlaylist.isEmpty()) {
            SongPlayer.addChatMessage("§6Added song to queue: §3" + song.name);
        }
    }

    // Runs every tick
    private int buildStartDelay = 0;
    private int buildEndDelay = 0;
    private int buildCooldown = 0;
    private int updatePlayerPosCooldown = 0;
    private void handleBuilding() {
        setBuildProgressDisplay();
        if (buildStartDelay > 0) {
            buildStartDelay--;
            return;
        }
        if (buildCooldown > 0) {
            buildCooldown--;
            return;
        }
        if (!SongPlayer.useCommandsForPlaying && !SongPlayer.switchGamemode) {
            if (updatePlayerPosCooldown > 0) {
                updatePlayerPosCooldown--;
            }
        }
        ClientWorld world = SongPlayer.MC.world;
        if (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.CREATIVE) {
            return;
        }
        PlayerInventory inventory = SongPlayer.MC.player.getInventory();

        if (stage.nothingToBuild()) {
            if (buildEndDelay > 0) {
                buildEndDelay--;
                if (oldItemHeld != null) {
                    inventory.setStack(inventory.selectedSlot, oldItemHeld);
                    SongPlayer.MC.interactionManager.clickCreativeStack(SongPlayer.MC.player.getStackInHand(Hand.MAIN_HAND), 36 + inventory.selectedSlot);
                    oldItemHeld = null;
                }
                return;
            } else {
                stage.checkBuildStatus(currentSong);
            }
        }

        if (!stage.requiredBreaks.isEmpty()) {
            for (int i=0; i<5; i++) {
                if (stage.requiredBreaks.isEmpty()) break;
                BlockPos bp = stage.requiredBreaks.poll();
                SongPlayer.MC.interactionManager.attackBlock(bp, Direction.UP);
            }
            buildEndDelay = 40;
            return;
        }

        if (stage.missingNotes.isEmpty()) {
            if (oldItemHeld != null) {
                inventory.main.set(inventory.selectedSlot, oldItemHeld);
                SongPlayer.MC.interactionManager.clickCreativeStack(SongPlayer.MC.player.getStackInHand(Hand.MAIN_HAND), 36 + inventory.selectedSlot);
                oldItemHeld = null;
            }
            building = false;
            setSurvivalIfNeeded();
            //stage.movePlayerToStagePosition();
            SongPlayer.addChatMessage("§6Now playing §3" + currentSong.name);
            return;
        }
        //rebuilding?

        int desiredNoteId = stage.missingNotes.pollFirst();
        BlockPos bp = stage.noteblockPositions.get(desiredNoteId);
        if (bp == null) {
            return;
        }
        int blockId = Block.getRawIdFromState(world.getBlockState(bp));
        if (blockId % 2 == 1) {
            blockId += 1;
        }
        int currentNoteId = (blockId-SongPlayer.NOTEBLOCK_BASE_ID)/2;
        if (currentNoteId != desiredNoteId) {
            holdNoteblock(desiredNoteId);
            if (blockId != 0) {
                attackBlock(bp);
            }
            placeBlock(bp);
        }
        buildCooldown = 4;
        buildEndDelay = 40;
    }
    private void setBuildProgressDisplay() {
        MutableText text = Text.empty()
                .append(Text.literal("Building noteblocks | " ).formatted(Formatting.GOLD))
                .append(Text.literal((stage.totalMissingNotes - stage.missingNotes.size()) + "/" + stage.totalMissingNotes).formatted(Formatting.DARK_AQUA));
        ProgressDisplay.getInstance().setText(text);
    }

    // Runs every frame
    private void handlePlaying(boolean tick) {
        if (tick) {
            setPlayProgressDisplay();
        }

        if (SongPlayer.switchGamemode) {
            if (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.SURVIVAL) {
                currentSong.pause();
                return;
            }
        }

        if (tick && SongPlayer.switchGamemode) {
            if (stage.hasBreakingModification()) {
                stage.checkBuildStatus(currentSong);
            }
            if (!stage.nothingToBuild()) {
                //oldItems.clear();
                building = true;
                setCreativeIfNeeded();
                Util.enableFlightIfNeeded();
                //stage.movePlayerToStagePosition();
                currentSong.pause();
                buildStartDelay = 40;
                //System.out.println("Total missing notes: " + stage.missingNotes.size());
                //for (int note : stage.missingNotes) {
                //    int pitch = note % 25;
                //    int instrumentId = note / 25;
                //    System.out.println("Missing note: " + Instrument.getInstrumentFromId(instrumentId).name() + ":" + pitch);
                //}
                SongPlayer.addChatMessage("§6Stage was altered. Rebuilding!");
                return;
            }
        }

        if (Calendar.getInstance().getTime().getTime() < Util.playcooldown) {
            return;
        }

        //cooldown is over!

        currentSong.play();
        boolean somethingPlayed = false;
        currentSong.advanceTime();
        ClientPlayerEntity player = SongPlayer.MC.player;
        if (player.isCreative() || player.isSpectator()) {
            setSurvivalIfNeeded();
        }
        if (updatePlayerPosCooldown < 1) {
            updatePlayerPosCooldown = 10;
            Util.playerPosX = player.getX();
            Util.playerPosZ = player.getZ();
        }
        SoundEvent[] soundlist = {SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value(), SoundEvents.BLOCK_NOTE_BLOCK_SNARE.value(), SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundEvents.BLOCK_NOTE_BLOCK_FLUTE.value(), SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundEvents.BLOCK_NOTE_BLOCK_GUITAR.value(), SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundEvents.BLOCK_NOTE_BLOCK_XYLOPHONE.value(), SoundEvents.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE.value(), SoundEvents.BLOCK_NOTE_BLOCK_COW_BELL.value(), SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO.value(), SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundEvents.BLOCK_NOTE_BLOCK_BANJO.value(), SoundEvents.BLOCK_NOTE_BLOCK_PLING.value()};
        World world = SongPlayer.MC.player.world;
        while (currentSong.reachedNextNote()) {
            Note note = currentSong.getNextNote();

            if (SongPlayer.useCommandsForPlaying) {
                String[] instrumentNames = {"harp", "basedrum", "snare", "hat", "bass", "flute", "bell", "guitar", "chime", "xylophone", "iron_xylophone", "cow_bell", "didgeridoo", "bit", "banjo", "pling"};
                int instrument = note.noteId / 25;
                int pitchID = (note.noteId % 25);
                double pitch = SongPlayer.pitchGlobal[pitchID];
                if (pitch > 2.0) pitch = 2.0;
                if (pitch < 0.5) pitch = 0.5;
                String command = SongPlayer.playSoundCommand.replace("{type}", instrumentNames[instrument]).replace("{volume}", "1").replace("{pitch}", Double.toString(pitch));
                SongPlayer.MC.getNetworkHandler().sendCommand(command);
                somethingPlayed = true;
            } else if (SongPlayer.switchGamemode) {
                BlockPos bp = stage.noteblockPositions.get(note.noteId);
                if (bp != null) {
                    attackBlock(bp);
                    somethingPlayed = true;
                }
            } else { //play client-side
                //player.playSound(soundlist[note.noteId / 25], SoundCategory.RECORDS, 1F, (float) SongPlayer.pitchGlobal[(note.noteId % 25)]);

                world.playSound(Util.playerPosX, 10000000, Util.playerPosZ, soundlist[note.noteId / 25], SoundCategory.BLOCKS, 30000000.0F, (float) SongPlayer.pitchGlobal[(note.noteId % 25)], false);
            }
        }

        if (somethingPlayed && !SongPlayer.useCommandsForPlaying && SongPlayer.switchGamemode) {
            stopAttack();
        }

        if (currentSong.finished()) {
            Util.playcooldown = Calendar.getInstance().getTime().getTime() + 1500;
            if (Util.currentPlaylist.isEmpty()) {
                SongPlayer.addChatMessage("§6Done playing §3" + currentSong.name);
            }
            Util.disableFlightIfNeeded();
            currentSong = null;
            Util.advancePlaylist();
        }
    }

    public void setPlayProgressDisplay() {
        long currentTime = Math.min(currentSong.time, currentSong.length);
        long totalTime = currentSong.length;
        MutableText text = Text.empty()
                .append(Text.literal("Now playing ").formatted(Formatting.GOLD))
                .append(Text.literal(currentSong.name).formatted(Formatting.BLUE))
                .append(Text.literal(" | ").formatted(Formatting.GOLD))
                .append(Text.literal(String.format("%s/%s", Util.formatTime(currentTime), Util.formatTime(totalTime))).formatted(Formatting.DARK_AQUA));
        if (currentSong.looping) {
            if (currentSong.loopCount > 0) {
                text.append(Text.literal(String.format(" | Loop (%d/%d)", currentSong.currentLoop, currentSong.loopCount)).formatted(Formatting.GOLD));
            } else {
                text.append(Text.literal(" | Looping enabled").formatted(Formatting.GOLD));
            }
        }
        ProgressDisplay.getInstance().setText(text);
        if (SongPlayer.useCommandsForPlaying && !Util.lastTimeExecuted.equalsIgnoreCase(Util.formatTime(currentTime))) {
            Util.lastTimeExecuted = Util.formatTime(currentTime);
            String midiname = currentSong.name;
            String rawcommand = SongPlayer.showProgressCommand;
            String command = rawcommand.replace("{MIDI}", midiname).replace("{CurrentTime}", Util.formatTime(currentTime)).replace("{SongTime}", Util.formatTime(totalTime));
            int cmdlength = command.length();
            if (cmdlength > 254) {
                while (cmdlength > 250) {
                    midiname = midiname.substring(0, midiname.length() - 1);
                    cmdlength -= 1;
                }
                midiname = midiname + "...";
                command = rawcommand.replace("{MIDI}", midiname).replace("{CurrentTime}", Util.formatTime(currentTime)).replace("{SongTime}", Util.formatTime(totalTime));
            }

            SongPlayer.MC.getNetworkHandler().sendCommand(command);
        }
    }

    public void cleanup(boolean includePlaylist) {
        if (!includePlaylist && Util.playlistSongs.size() > 0) {
            return;
        }
        currentSong = null;
        songQueue.clear();
        stage = null;
        Util.playlistSongs.clear();
        Util.currentPlaylist = "";
        Util.playlistIndex = 0;
        Util.loopPlaylist = false;
        SongPlayer.removeFakePlayer();
    }

    public void onNotIngame() {
        currentSong = null;
        songQueue.clear();
        Util.playlistSongs.clear();
        Util.currentPlaylist = "";
        Util.playlistIndex = 0;
    }

    private long lastCommandTime = System.currentTimeMillis();
    private String cachedCommand = null;
    private void sendGamemodeCommand(String command) {
        cachedCommand = command;
    }
    private void checkCommandCache() {
        //does not handle useCommandsForPlaying mode
        if (cachedCommand != null && System.currentTimeMillis() >= lastCommandTime + 1500) {
            SongPlayer.MC.getNetworkHandler().sendCommand(cachedCommand);
            cachedCommand = null;
            lastCommandTime = System.currentTimeMillis();
        }
    }
    private void setCreativeIfNeeded() {
        cachedCommand = null;
        if (SongPlayer.useCommandsForPlaying || !SongPlayer.switchGamemode) {
            return;
        }
        if (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.CREATIVE) {
            sendGamemodeCommand(SongPlayer.creativeCommand);
        }
    }
    private void setSurvivalIfNeeded() {
        cachedCommand = null;
        if (SongPlayer.useCommandsForPlaying || !SongPlayer.switchGamemode) {
            return;
        }
        if (oldItemHeld != null) {
            CreativeInventoryActionC2SPacket packet = new CreativeInventoryActionC2SPacket(SongPlayer.MC.player.getInventory().selectedSlot + 36, oldItemHeld);
            SongPlayer.MC.player.networkHandler.sendPacket(packet);
            PlayerInventory inventory = SongPlayer.MC.player.getInventory();
            SongPlayer.MC.player.getInventory().main.set(inventory.selectedSlot, oldItemHeld.copy());
            SongPlayer.MC.interactionManager.clickCreativeStack(SongPlayer.MC.player.getStackInHand(Hand.MAIN_HAND), 36 + inventory.selectedSlot);
            oldItemHeld = null;
        }
        if (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.SURVIVAL) {
            sendGamemodeCommand(SongPlayer.survivalCommand);
        }
    }

    private final String[] instrumentNames = {"harp", "basedrum", "snare", "hat", "bass", "flute", "bell", "guitar", "chime", "xylophone", "iron_xylophone", "cow_bell", "didgeridoo", "bit", "banjo", "pling"};
    private void holdNoteblock(int id) {
        PlayerInventory inventory = SongPlayer.MC.player.getInventory();
        if (oldItemHeld == null) {
            oldItemHeld = inventory.getMainHandStack();
        }
        int instrument = id/25;
        int note = id%25;
        NbtCompound nbt = new NbtCompound();
        nbt.putString("id", "minecraft:note_block");
        nbt.putByte("Count", (byte) 1);
        NbtCompound tag = new NbtCompound();
        NbtCompound bsTag = new NbtCompound();
        bsTag.putString("instrument", instrumentNames[instrument]);
        bsTag.putString("note", Integer.toString(note));
        tag.put("BlockStateTag", bsTag);
        nbt.put("tag", tag);
        inventory.main.set(inventory.selectedSlot, ItemStack.fromNbt(nbt));
        SongPlayer.MC.interactionManager.clickCreativeStack(SongPlayer.MC.player.getStackInHand(Hand.MAIN_HAND), 36 + inventory.selectedSlot);
    }

    private void placeBlock(BlockPos bp) {
        double fx = Math.max(0.0, Math.min(1.0, (stage.position.getX() - bp.getX())));
        double fy = Math.max(0.0, Math.min(1.0, (stage.position.getY() + 0.0 - bp.getY())));
        double fz = Math.max(0.0, Math.min(1.0, (stage.position.getZ() - bp.getZ())));
        fx += bp.getX();
        fy += bp.getY();
        fz += bp.getZ();
        SongPlayer.MC.interactionManager.interactBlock(SongPlayer.MC.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(fx, fy, fz), Direction.UP, bp, false));
        if (SongPlayer.swing) {
            SongPlayer.MC.player.swingHand(SongPlayer.MC.player.getActiveHand());
            if (SongPlayer.fakePlayer != null) {
                SongPlayer.fakePlayer.swingHand(SongPlayer.fakePlayer.getActiveHand());
            }
        }
        if (SongPlayer.rotate) {
            float[] pitchandyaw = Util.getAngleAtBlock(bp);
            PlayerMoveC2SPacket packet = new PlayerMoveC2SPacket.LookAndOnGround(pitchandyaw[1], pitchandyaw[0], true);
            SongPlayer.MC.player.networkHandler.sendPacket(packet);
        }
    }
    private void attackBlock(BlockPos bp) {
        ClientPlayerEntity player = SongPlayer.MC.player;
        //
        //if (player.isCreative() || player.isSpectator()) {
        //    setSurvivalIfNeeded();
        //}

        SongPlayer.MC.interactionManager.attackBlock(bp, Direction.UP);
        if (SongPlayer.swing) {
            player.swingHand(SongPlayer.MC.player.getActiveHand());
            if (SongPlayer.fakePlayer != null) {
                SongPlayer.fakePlayer.swingHand(SongPlayer.fakePlayer.getActiveHand());
            }
        }
        if (SongPlayer.rotate) {
            float[] pitchandyaw = Util.getAngleAtBlock(bp);
            PlayerMoveC2SPacket packet = new PlayerMoveC2SPacket.LookAndOnGround(pitchandyaw[1], pitchandyaw[0], true);
            player.networkHandler.sendPacket(packet);
        }
    }
    private void stopAttack() {
        SongPlayer.MC.interactionManager.cancelBlockBreaking();
    }
}