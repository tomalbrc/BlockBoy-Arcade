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

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void blockboy$handlePlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (!BlockBoyArcade.ACTIVE_SESSIONS.containsKey(player)) return;

        var input = BlockBoyArcade.ACTIVE_SESSIONS.get(player).getInput();

        switch (packet.getAction()) {
            case START_DESTROY_BLOCK -> {
                input.didPressB();
                ci.cancel();
            }

            case DROP_ITEM, DROP_ALL_ITEMS -> {
                input.didPressSelect();
                ci.cancel();
            }

            case SWAP_ITEM_WITH_OFFHAND -> {
                input.didPressStart();
                ci.cancel();
            }
        }
    }

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void blockboy$handleDisconnect(DisconnectionDetails disconnectionDetails, CallbackInfo ci) {
        if (BlockBoyArcade.ACTIVE_SESSIONS.containsKey(player)) {
            BlockBoyArcade.ACTIVE_SESSIONS.get(player).clearSession();
        }
    }
}
