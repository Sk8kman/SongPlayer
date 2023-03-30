package com.github.hhhzzzsss.songplayer.mixin;

import com.github.hhhzzzsss.songplayer.CommandProcessor;
import com.github.hhhzzzsss.songplayer.FakePlayerEntity;
import com.github.hhhzzzsss.songplayer.Util;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.playing.Stage;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.hhhzzzsss.songplayer.SongPlayer;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
	@Shadow
	private final ClientConnection connection;

	public ClientPlayNetworkHandlerMixin() {
		connection = null;
	}


	@Inject(at = @At("HEAD"), method = "sendPacket(Lnet/minecraft/network/packet/Packet;)V", cancellable = true)
	private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
		Stage stage = SongHandler.getInstance().stage;
		//check if any packets need to be messed with before proceeding
		if (stage == null || !SongPlayer.switchGamemode) {
			return;
		}
		if (packet instanceof PlayerMoveC2SPacket) { //override any movement packets to the stage position, as well as rotation if needed.
			if (SongPlayer.rotate) {
				connection.send(new PlayerMoveC2SPacket.Full(stage.position.getX() + 0.5, stage.position.getY(), stage.position.getZ() + 0.5, Util.yaw, Util.pitch, true));
			} else {
				if (((PlayerMoveC2SPacket) packet).changesLook()) {
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
			ci.cancel();
			if (SongPlayer.MC.player != null) { //does this even matter? how am I even going to send a packet if the player is null?
				PlayerInputC2SPacket sneak = new PlayerInputC2SPacket(0f, 0f, false, true);
				PlayerInputC2SPacket unsneak = new PlayerInputC2SPacket(0f, 0f, false, false);
				connection.send(sneak);
				connection.send(unsneak);
				SongPlayer.MC.player.dismountVehicle();
			}
		/*} else if (packet instanceof PlayerInteractEntityC2SPacket) { //prevents getting in boats, camels, minecarts, etc... while playing
			try {
				PlayerInteractEntityC2SPacket entityPacket = (PlayerInteractEntityC2SPacket) packet;
				MinecraftServer server = MinecraftClient.getInstance().getNetworkHandler().getWorld().getServer();
				Entity targetEntity = entityPacket.getEntity(server.getWorld(server.getOverworld().getRegistryKey()));
				if (targetEntity == null) {
					ci.cancel();
					return;
				}
				if (targetEntity.isLiving()) {
					return;
				}
				ci.cancel();
			} catch(NullPointerException e) {
				SongPlayer.addChatMessage(e.getMessage());
				e.printStackTrace();
				System.out.println("dang it");
			}*/
		} else if (packet instanceof TeleportConfirmC2SPacket) { //prevents lagbacks client side
			ci.cancel();
		} else if (packet instanceof ClientCommandC2SPacket) { //prevents sprinting while playing
			ClientCommandC2SPacket sprinting = (ClientCommandC2SPacket) packet;
			ClientCommandC2SPacket.Mode mode = sprinting.getMode();
			if (mode.equals(ClientCommandC2SPacket.Mode.START_SPRINTING) || mode.equals(ClientCommandC2SPacket.Mode.STOP_SPRINTING)) {
				ci.cancel();
			}
		}
	}

	@Inject(at = @At("HEAD"), method = "sendChatMessage", cancellable=true)
	private void onSendChatMessage(String content, CallbackInfo ci) {
		boolean isCommand = CommandProcessor.processChatMessage(content);
		if (isCommand) {
			ci.cancel();
		}
	}


	@Inject(at = @At("TAIL"), method = "onGameJoin(Lnet/minecraft/network/packet/s2c/play/GameJoinS2CPacket;)V")
	public void onOnGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
		if (!SongPlayer.useCommandsForPlaying && !SongPlayer.switchGamemode) {
			return;
		}
		SongHandler.getInstance().cleanup(true);
		SongPlayer.fakePlayer = new FakePlayerEntity();
		SongPlayer.removeFakePlayer(); //fixes fakeplayer not rendering the first time
	}

	@Inject(at = @At("TAIL"), method = "onPlayerRespawn(Lnet/minecraft/network/packet/s2c/play/PlayerRespawnS2CPacket;)V")
	public void onOnPlayerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
		if (!SongPlayer.useCommandsForPlaying && SongPlayer.switchGamemode) {
			SongHandler.getInstance().cleanup(true);
		}
		SongPlayer.fakePlayer = new FakePlayerEntity();
		SongPlayer.removeFakePlayer(); //fixes fakeplayer not rendering the first time
	}
}
