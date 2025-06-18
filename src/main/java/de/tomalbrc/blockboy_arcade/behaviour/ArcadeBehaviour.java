package de.tomalbrc.blockboy_arcade.behaviour;

import de.tomalbrc.blockboy_arcade.BlockBoyArcade;
import de.tomalbrc.blockboy_arcade.ConsoleInput;
import de.tomalbrc.blockboy_arcade.EmulatorSession;
import de.tomalbrc.filament.api.behaviour.DecorationBehaviour;
import de.tomalbrc.filament.decoration.block.entity.DecorationBlockEntity;
import de.tomalbrc.filament.decoration.holder.DecorationHolder;
import de.tomalbrc.filament.decoration.holder.FilamentDecorationHolder;
import de.tomalbrc.filament.decoration.util.SeatEntity;
import de.tomalbrc.filament.registry.EntityRegistry;
import de.tomalbrc.filament.util.DecorationUtil;
import de.tomalbrc.filament.util.FilamentConfig;
import de.tomalbrc.filament.util.Util;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData;
import eu.rekawek.coffeegb_mc.emulator.EmulationController;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ArcadeBehaviour implements DecorationBehaviour<ArcadeBehaviour.Config> {
    private final Config config;
    private DecorationBlockEntity blockEntity;
    private SeatEntity seatEntity;

    @NotNull
    final private ItemDisplayElement screenElement = new ItemDisplayElement();
    @NotNull
    private ItemStack screen = Items.PAPER.getDefaultInstance();
    @NotNull
    private ItemStack cartridge = ItemStack.EMPTY;

    @Nullable
    private EmulatorSession session;
    @Nullable
    private Gui gui;

    public ArcadeBehaviour(Config config) {
        this.config = config;

        this.screen.set(DataComponents.ITEM_MODEL, ResourceLocation.fromNamespaceAndPath("blockboy", "screen"));
        this.screen.set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY);
        this.screenElement.setViewRange(0.0245f);
        this.screenElement.setBrightness( new Brightness(config.brightness, config.brightness));
        this.screenElement.setItem(screen);
    }

    @Override
    public FilamentDecorationHolder createHolder(DecorationBlockEntity blockEntity) {
        var holder = new DecorationHolder(blockEntity::getItem) {
            @Override
            public void destroy() {
                super.destroy();
                if (session != null) {
                    session.stop();
                }
            }

            @Override
            public boolean stopWatching(ServerGamePacketListenerImpl player) {
                boolean r = super.stopWatching(player);
                if (this.getWatchingPlayers().isEmpty() && session != null) {
                    session.stop();
                    session = null;
                }
                return r;
            }
        };

        holder.addElement(this.screenElement);

        DecorationUtil.setupElements(holder, this.blockEntity.getDecorationData(), this.blockEntity.getDirection(), blockEntity.getVisualRotationYInDegrees(), this.blockEntity.visualItemStack(this.blockEntity.getBlockState()), blockEntity::interact);

        return holder;
    }

    @Override
    @NotNull
    public ArcadeBehaviour.Config getConfig() {
        return this.config;
    }

    @Override
    public void init(DecorationBlockEntity blockEntity) {
        DecorationBehaviour.super.init(blockEntity);

        this.blockEntity = blockEntity;
        this.screenElement.setYaw(blockEntity.getVisualRotationYInDegrees() - 180);

        Matrix4f transform = new Matrix4f();
        transform.rotateY(Mth.DEG_TO_RAD * (config.screenYaw));
        transform.rotateX(Mth.DEG_TO_RAD * config.screenPitch);
        transform.scale(0.75f);
        this.screenElement.setOffset(screenTranslation(blockEntity));
        this.screenElement.setTransformation(transform);
    }

    @Override
    public InteractionResult interact(ServerPlayer player, InteractionHand hand, Vec3 location, DecorationBlockEntity decorationBlockEntity) {
        if ((this.gui == null || !this.gui.isOpen()) && player.isPassenger() && player.getVehicle() == this.seatEntity) {
            this.gui = new Gui(MenuType.HOPPER, player, this);
            this.gui.open();
        } else if (!player.isShiftKeyDown() && !player.isPassenger() && hasAvailableSeat()) {
            this.seatPlayer(decorationBlockEntity, player);
            if (this.session == null) {
                this.startSession(player, decorationBlockEntity, true);
            } else {
                this.session.setPlayer(player);
                this.startSession(player, decorationBlockEntity, false);
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    public void onGuiClosed(ItemStack containerItem) {
        if (this.cartridge != containerItem) {
            if (!this.cartridge.isEmpty() || containerItem.isEmpty()) {
                this.screen.set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY);
                this.screenElement.getDataTracker().setDirty(DisplayTrackedData.Item.ITEM, true);
                this.screenElement.getHolder().tick();
            }
            this.cartridge = containerItem;

        }
        this.blockEntity.setChanged();
        this.gui = null;
    }

    public void seatPlayer(DecorationBlockEntity decorationBlockEntity, ServerPlayer player) {
        this.seatEntity = EntityRegistry.SEAT_ENTITY.create(player.level(), EntitySpawnReason.TRIGGERED);
        assert seatEntity != null;
        this.seatEntity.setPos(seatTranslation(decorationBlockEntity).add(decorationBlockEntity.getOrCreateHolder().getPos()));
        player.level().addFreshEntity(this.seatEntity);
        player.startRiding(this.seatEntity);
        this.seatEntity.setYRot((decorationBlockEntity.getVisualRotationYInDegrees() - config.seatDirection + (FilamentConfig.getInstance().alternativeBlockPlacement ? 180 : 0)));
    }

    private boolean hasAvailableSeat() {
        return this.seatEntity == null || this.seatEntity.isRemoved();
    }

    private Vec3 seatTranslation(DecorationBlockEntity decorationBlockEntity) {
        Vec3 v3 = new Vec3(config.seatTranslation).subtract(0, 0.3, 0).yRot((float) Math.toRadians(decorationBlockEntity.getVisualRotationYInDegrees() + (FilamentConfig.getInstance().alternativeBlockPlacement ? 0 : 180)));
        return new Vec3(-v3.x, v3.y, v3.z);
    }

    private Vec3 screenTranslation(DecorationBlockEntity decorationBlockEntity) {
        Vec3 v3 = new Vec3(config.screenTranslation).subtract(0, 0.3, 0).yRot((float) Math.toRadians(decorationBlockEntity.getVisualRotationYInDegrees() + (FilamentConfig.getInstance().alternativeBlockPlacement ? 0 : 180)));
        return new Vec3(-v3.x, v3.y, v3.z);
    }

    private SeatEntity getSeatEntity() {
        return this.seatEntity;
    }

    @Override
    public void destroy(DecorationBlockEntity decorationBlockEntity, boolean dropItem) {
        var seat = getSeatEntity();
        if (seat != null && seat.getFirstPassenger() != null) {
            seat.getFirstPassenger().stopRiding();
        }

        if (this.gui != null) {
            this.gui.close();
            this.cartridge = ItemStack.EMPTY;
        }

        if (!this.cartridge.isEmpty()) {
            Util.spawnAtLocation(decorationBlockEntity.getLevel(), decorationBlockEntity.getBlockPos().getCenter(), this.cartridge);
            this.cartridge = ItemStack.EMPTY;
        }
    }

    @Override
    public void read(ValueInput compoundTag, DecorationBlockEntity blockEntity) {
        if (blockEntity.getOrCreateHolder() != null) {
            FilamentDecorationHolder holder = blockEntity.getOrCreateHolder();
            if (holder == null) {
                return;
            }

            if (blockEntity.has(BlockBoyBehaviours.ARCADE)) { // it should 100% have the behaviour but just in case
                compoundTag.read("Cartridge", ItemStack.CODEC).ifPresent(x -> this.cartridge = x);
                compoundTag.read("Screen", ItemStack.CODEC).ifPresent(x -> this.screen = x);

                this.screenElement.setItem(screen);
            }
        }
    }

    @Override
    public void write(ValueOutput compoundTag, DecorationBlockEntity blockEntity) {
        if (blockEntity.getOrCreateHolder() != null) {
            if (!this.cartridge.isEmpty()) compoundTag.store("Cartridge", ItemStack.CODEC, this.cartridge);
            compoundTag.store("Screen", ItemStack.CODEC, this.screen.copy());
        }
    }

    public void clearSession() {
        if (this.session != null) {
            this.session.stop();

            if (this.blockEntity == null || hasAvailableSeat()) {
                BlockBoyArcade.ACTIVE_SESSIONS.remove(this.session.getPlayer());
                this.session = null;
            }
        }
    }

    public void pauseSession(ServerPlayer player, boolean remove) {
        if (this.session != null) {
            this.session.pause();
        }
        if (remove) BlockBoyArcade.ACTIVE_SESSIONS.remove(player);
    }

    public void resumeSession(ServerPlayer player, boolean add) {
        if (this.session != null) {
            this.session.resume();
        }
        if (add) BlockBoyArcade.ACTIVE_SESSIONS.put(player, this);
    }

    public void startSession(ServerPlayer player, DecorationBlockEntity blockEntity, boolean newSession) {
        if (newSession) {
            if (this.session != null) {
                this.session.stop();
            }

            this.session = new EmulatorSession(player, blockEntity, this.screen, this.cartridge, this.screenElement);
        } else if (this.session == null) {
            BlockBoyArcade.LOGGER.error("Trying to resume session but no session exists!");
        }

        BlockBoyArcade.ACTIVE_SESSIONS.put(player, this);

        if (!newSession)
            this.session.resume();
    }

    public ServerPlayer getPlayer() {
        return this.session != null ? this.session.getPlayer() : null;
    }

    public boolean isPlaying() {
        return this.session != null && this.session.getController() != null;
    }

    @NotNull public ItemStack getCartridge() {
        return this.cartridge;
    }

    public void play(ResourceLocation id) {
        if (this.session != null)
            this.session.playRom(BlockBoyArcade.ROMS.get(id));
    }

    public ConsoleInput getInput() {
        return this.session != null ? this.session.getInput() : ConsoleInput.EMPTY;
    }

    public void tick() {
        if (this.session != null) {
            this.session.tick();
        }
    }

    @Nullable
    public EmulationController getController() {
        return this.session != null ? this.session.getController() : null;
    }

    public static class Config {
        public float seatDirection;
        public Vector3f seatTranslation;

        public Vector3f screenTranslation;
        public float screenYaw;
        public float screenPitch;
        public int brightness = 10;
    }
}
