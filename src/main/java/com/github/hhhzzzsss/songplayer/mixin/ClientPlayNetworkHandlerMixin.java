package com.github.hhhzzzsss.songplayer.mixin;

import com.github.hhhzzzsss.songplayer.CommandProcessor;
import com.github.hhhzzzsss.songplayer.FakePlayerEntity;
import com.github.hhhzzzsss.songplayer.Util;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.playing.Stage;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.hhhzzzsss.songplayer.SongPlayer;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
	/*

	@Inject(at = @At("HEAD"), method = "sendPacket(Lnet/minecraft/network/packet/Packet;)V", cancellable = true)
	private void onSendPacket(Packet<?> packet, PacketCallbacks pc, CallbackInfo ci) {
		Stage stage = SongHandler.getInstance().stage;
		if (stage == null || !SongPlayer.useNoteblocksWhilePlaying || SongHandler.getInstance().paused) {
			return;
		}

		if (packet instanceof PlayerMoveC2SPacket) { //override any movement packets to the stage position, as well as rotation if needed.
			PlayerMoveC2SPacket movePacket = (PlayerMoveC2SPacket) packet;
			if (SongPlayer.rotate) {
				if (Util.lastPitch == Util.pitch && Util.lastYaw == Util.yaw) { //prevent sending rotations if you are already facing that direction
					ci.cancel();
				} else {
					connection.send(new PlayerMoveC2SPacket.Full(stage.position.getX() + 0.5, stage.position.getY(), stage.position.getZ() + 0.5, Util.yaw, Util.pitch, true));
				}
			} else {
				if ((movePacket).changesLook()) {
					connection.send(new PlayerMoveC2SPacket.Full(stage.position.getX() + 0.5, stage.position.getY(), stage.position.getZ() + 0.5, SongPlayer.MC.player.getYaw(), SongPlayer.MC.player.getPitch(), true));
				} else {
					connection.send(new PlayerMoveC2SPacket.PositionAndOnGround(stage.position.getX() + 0.5, stage.position.getY(), stage.position.getZ() + 0.5, true));
				}
			}
			if (SongPlayer.fakePlayer != null) {
				SongPlayer.fakePlayer.copyStagePosAndPlayerLook();
			}
			ci.cancel();
		} else if (packet instanceof VehicleMoveC2SPacket) { //prevents moving in a boat or whatever while playing
			Util.pauseSongIfNeeded();
			Util.lagBackCounter = 0;
			SongPlayer.addChatMessage("§6Your song has been paused because you were moved away from your stage. Go back to your stage and type §3" + SongPlayer.prefix + "resume");
		} else if (packet instanceof TeleportConfirmC2SPacket) { //prevents lagbacks client side
			Util.lagBackCounter++;
			TeleportConfirmC2SPacket tpacket = (TeleportConfirmC2SPacket) packet;
			SongPlayer.addChatMessage(tpacket.getTeleportId() + " - tpacket ID");
			if (Util.lagBackCounter > 3) {
				Util.pauseSongIfNeeded();
				SongPlayer.addChatMessage("§6Your song has been paused because you were moved away from your stage. Go back to your stage and type §3" + SongPlayer.prefix + "resume");
				Util.lagBackCounter = 0;
			} else {
				ci.cancel();
			}
		} else if (packet instanceof ClientCommandC2SPacket) { //prevents sprinting while playing
			ClientCommandC2SPacket sprinting = (ClientCommandC2SPacket) packet;
			ClientCommandC2SPacket.Mode mode = sprinting.getMode();
			if (mode.equals(ClientCommandC2SPacket.Mode.START_SPRINTING) || mode.equals(ClientCommandC2SPacket.Mode.STOP_SPRINTING)) {
				ci.cancel();
			}
		}/* else if (packet instanceof TeleportConfirmC2SPacket) {
			SongPlayer.addChatMessage("tp c2s packet sent");
		}
	}
	*/

	@Inject(at = @At("HEAD"), method = "sendChatMessage", cancellable=true)
	private void onSendChatMessage(String content, CallbackInfo ci) {
		boolean isCommand = CommandProcessor.processChatMessage(content);
		if (isCommand) {
			ci.cancel();
			if (content.startsWith(SongPlayer.prefix + "author")) { // lol watermark moment
				SongPlayer.MC.getNetworkHandler().sendChatMessage("SongPlayer made by hhhzzzsss, modified by Sk8kman, and tested by Lizard16");
			}
		}
	}

	@Inject(at = @At("TAIL"), method = "onGameJoin(Lnet/minecraft/network/packet/s2c/play/GameJoinS2CPacket;)V")
	public void onOnGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
		//fixes fakeplayer not rendering the first time
		SongPlayer.fakePlayer = new FakePlayerEntity();
		SongPlayer.removeFakePlayer();
		if (SongHandler.getInstance().paused) {
			return;
		}
		if (!SongPlayer.useNoteblocksWhilePlaying) {
			return;
		}
		SongHandler.getInstance().cleanup(true);
	}

	@Inject(at = @At("TAIL"), method = "onPlayerRespawn(Lnet/minecraft/network/packet/s2c/play/PlayerRespawnS2CPacket;)V")
	public void onOnPlayerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
		//fixes fakeplayer not rendering the first time
		if (SongPlayer.fakePlayer == null) {
			SongPlayer.fakePlayer = new FakePlayerEntity();
			SongPlayer.removeFakePlayer();
		}
		if (SongHandler.getInstance().paused) {
			return;
		}
		if (!SongPlayer.useNoteblocksWhilePlaying) {
			return;
		}
		if (SongHandler.getInstance().currentSong != null && !SongHandler.getInstance().paused) {
			Util.pauseSongIfNeeded();
			SongPlayer.addChatMessage("§6Your song has been paused because you died.\n §6Go back to your stage or find a new location and type §3" + SongPlayer.prefix + "resume§6 to resume playing.");
			return;
		}
		//this shouldn't run but if it does at least stuff won't break
		SongHandler.getInstance().cleanup(true);
	}
}
