package de.tomalbrc.blockboy_arcade.mixin;

import de.tomalbrc.blockboy_arcade.BlockBoyArcade;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void blockboy$handleDisconnect(DisconnectionDetails disconnectionDetails, CallbackInfo ci) {
        if (BlockBoyArcade.ACTIVE_SESSIONS.containsKey(player)) {
            BlockBoyArcade.ACTIVE_SESSIONS.get(player).clearSession();
        }
    }

    @Inject(method = "handlePlayerAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;handleBlockBreakAction(Lnet/minecraft/core/BlockPos;Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket$Action;Lnet/minecraft/core/Direction;II)V"), cancellable = true)
    private void blockboy$handleBreakAction(ServerboundPlayerActionPacket serverboundPlayerActionPacket, CallbackInfo ci) {
        if (BlockBoyArcade.ACTIVE_SESSIONS.containsKey(player)) {
            BlockBoyArcade.ACTIVE_SESSIONS.get(player).getInput().didPressB();
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSpectator()Z", ordinal = 1), cancellable = true)
    private void blockboy$handleDrop(ServerboundPlayerActionPacket serverboundPlayerActionPacket, CallbackInfo ci) {
        if (BlockBoyArcade.ACTIVE_SESSIONS.containsKey(this.player)) {
            BlockBoyArcade.ACTIVE_SESSIONS.get(player).getInput().didPressSelect();
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSpectator()Z", ordinal = 0), cancellable = true)
    private void blockboy$handleSwap(ServerboundPlayerActionPacket serverboundPlayerActionPacket, CallbackInfo ci) {
        if (BlockBoyArcade.ACTIVE_SESSIONS.containsKey(this.player)) {
            BlockBoyArcade.ACTIVE_SESSIONS.get(player).getInput().didPressStart();
            ci.cancel();
        }
    }
}
