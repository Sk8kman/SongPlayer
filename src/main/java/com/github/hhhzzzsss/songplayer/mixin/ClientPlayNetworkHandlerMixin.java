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
