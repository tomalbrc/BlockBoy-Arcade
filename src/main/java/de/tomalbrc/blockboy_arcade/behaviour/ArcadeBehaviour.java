package de.tomalbrc.blockboy_arcade.behaviour;

import de.tomalbrc.blockboy_arcade.BlockBoyArcade;
import de.tomalbrc.blockboy_arcade.EmulatorSession;
import de.tomalbrc.blockboy_arcade.component.BlockBoyComponents;
import de.tomalbrc.blockboy_arcade.config.ModConfig;
import de.tomalbrc.filament.api.behaviour.DecorationBehaviour;
import de.tomalbrc.filament.decoration.block.entity.DecorationBlockEntity;
import de.tomalbrc.filament.decoration.holder.DecorationHolder;
import de.tomalbrc.filament.decoration.util.SeatEntity;
import de.tomalbrc.filament.registry.EntityRegistry;
import de.tomalbrc.filament.util.FilamentConfig;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import eu.rekawek.coffeegb.emulator.BlockBoyDisplay;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class ArcadeBehaviour implements DecorationBehaviour<ArcadeBehaviour.Config> {
    private final Config config;
    private final Long2ObjectArrayMap<List<Gui>> map = new Long2ObjectArrayMap<>();

    @NotNull private ItemStack cartridge = Items.PAPER.getDefaultInstance();
    @NotNull private ItemStack screen = Items.PAPER.getDefaultInstance();
    @NotNull final private ItemDisplayElement screenElement = new ItemDisplayElement();

    @Nullable private EmulatorSession session;

    public ArcadeBehaviour(Config config) {
        this.config = config;

        this.screen.set(DataComponents.ITEM_MODEL, ResourceLocation.fromNamespaceAndPath("blockboy", "screen"));

        long time = System.currentTimeMillis();
        float baseHue = (time % 5000L) / 5000f;
        var list = new ObjectArrayList<Integer>();
        for (int y = 0; y < BlockBoyDisplay.DISPLAY_HEIGHT; y++) {
            for (int x = 0; x < BlockBoyDisplay.DISPLAY_WIDTH; x++) {
                float hue = (baseHue + (float) x / 160f) % 1.0f;
                int color = hsvToRgb(hue, 1.0f, 1.0f);
                list.add(color);
            }
        }
        this.screen.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), list));
        this.screenElement.setViewRange(0.025f);
        this.screenElement.setItem(screen);
    }

    @Override
    public ElementHolder createHolder(DecorationBlockEntity blockEntity) {
        return new DecorationHolder(blockEntity) {
            @Override
            public void destroy() {
                super.destroy();
                if (session != null) {
                    session.stop();
                }
            }
        };
    }

    public static int hsvToRgb(float h, float s, float v) {
        int rgb = Color.HSBtoRGB(h, s, v);
        return 0xFF000000 | (rgb & 0x00FFFFFF); // force full alpha
    }

    @Override
    @NotNull
    public ArcadeBehaviour.Config getConfig() {
        return this.config;
    }

    @Override
    public void init(DecorationBlockEntity blockEntity) {
        DecorationBehaviour.super.init(blockEntity);

        screenElement.setYaw(blockEntity.getVisualRotationYInDegrees()-180);

        Matrix4f transform = new Matrix4f();
        transform.rotateY(Mth.DEG_TO_RAD * (config.screenYaw));
        transform.rotateX(Mth.DEG_TO_RAD * config.screenPitch);
        transform.scale(0.75f);
        this.screenElement.setOffset(screenTranslation(blockEntity));
        this.screenElement.setTransformation(transform);
    }

    @Override
    public void onElementAttach(DecorationBlockEntity blockEntity, ElementHolder holder) {
        holder.addElement(this.screenElement);
    }

    @Override
    public InteractionResult interact(ServerPlayer player, InteractionHand hand, Vec3 location, DecorationBlockEntity decorationBlockEntity) {
        if (player.isShiftKeyDown() || player.getVehicle() instanceof SeatEntity) {
            Gui gui = new Gui(MenuType.HOPPER, player, session == null);
            gui.container.setItem(0, this.cartridge);
            final var key = decorationBlockEntity.getBlockPos().asLong();
            gui.open(() -> {
                this.map.get(key).remove(gui);
                if (this.map.get(key).isEmpty())
                    this.map.remove(key);

                var containerItem = gui.container.getItem(0);
                if (this.cartridge != containerItem) {
                    this.cartridge = gui.container.getItem(0);
                    this.screen.set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY);
                    this.screenElement.getDataTracker().setDirty(DisplayTrackedData.Item.ITEM, true);
                    this.screenElement.getHolder().tick();
                }
                decorationBlockEntity.setChanged();
            });

            if (this.map.containsKey(key)) {
                this.map.get(key).add(gui);
            } else {
                this.map.put(key, ObjectArrayList.of(gui));
            }
        } else if (player.getVehicle() == null && !hasSeatedPlayer(decorationBlockEntity)) {
            this.seatPlayer(decorationBlockEntity, player);
            if (this.session == null && this.cartridge.has(BlockBoyComponents.ROM)) {
                this.session = new EmulatorSession(player, this.screen, this.cartridge, this.screenElement);
                this.session.playRom(BlockBoyArcade.ROMS.get(cartridge.get(BlockBoyComponents.ROM)), () -> {
                    Level level = decorationBlockEntity.getLevel();
                    if (level != null && level.getGameTime() % 40 == 0)
                        decorationBlockEntity.setChanged();
                }, () -> {
                    this.session = null;
                    decorationBlockEntity.setChanged();
                });
            } else if (this.session != null) {
                this.session.setPlayer(player);
                this.session.resume();
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    public void seatPlayer(DecorationBlockEntity decorationBlockEntity, ServerPlayer player) {
        SeatEntity seatEntity = EntityRegistry.SEAT_ENTITY.create(player.serverLevel(), EntitySpawnReason.TRIGGERED);
        assert seatEntity != null;
        seatEntity.setPos(seatTranslation(decorationBlockEntity).add(decorationBlockEntity.getDecorationHolder().getPos()));
        player.level().addFreshEntity(seatEntity);
        player.startRiding(seatEntity);
        seatEntity.setYRot((decorationBlockEntity.getVisualRotationYInDegrees() - config.seatDirection + (FilamentConfig.getInstance().alternativeBlockPlacement ? 180 : 0)));
    }

    public boolean hasSeatedPlayer(DecorationBlockEntity decorationBlockEntity) {
        return !Objects.requireNonNull(decorationBlockEntity.getLevel()).getEntitiesOfClass(SeatEntity.class, AABB.ofSize(seatTranslation(decorationBlockEntity).add(decorationBlockEntity.getDecorationHolder().getPos()), 0.2, 0.2, 0.2), x -> true).isEmpty();
    }

    public Vec3 seatTranslation(DecorationBlockEntity decorationBlockEntity) {
        Vec3 v3 = new Vec3(config.seatTranslation).subtract(0, 0.3, 0).yRot((float) Math.toRadians(decorationBlockEntity.getVisualRotationYInDegrees()+(FilamentConfig.getInstance().alternativeBlockPlacement ? 0 : 180)));
        return new Vec3(-v3.x, v3.y, v3.z);
    }

    public Vec3 screenTranslation(DecorationBlockEntity decorationBlockEntity) {
        Vec3 v3 = new Vec3(config.screenTranslation).subtract(0, 0.3, 0).yRot((float) Math.toRadians(decorationBlockEntity.getVisualRotationYInDegrees()+(FilamentConfig.getInstance().alternativeBlockPlacement ? 0 : 180)));
        return new Vec3(-v3.x, v3.y, v3.z);
    }

    public SeatEntity getSeatEntity(DecorationBlockEntity decorationBlockEntity) {
        List<SeatEntity> entities = Objects.requireNonNull(decorationBlockEntity.getLevel()).getEntitiesOfClass(SeatEntity.class, AABB.ofSize(seatTranslation(decorationBlockEntity).add(decorationBlockEntity.getDecorationHolder().getPos()), 0.2, 0.2, 0.2), x -> true);
        if (!entities.isEmpty())
            return entities.getFirst();

        return null;
    }

    @Override
    public void destroy(DecorationBlockEntity decorationBlockEntity, boolean dropItem) {
        var seat = getSeatEntity(decorationBlockEntity);
        if (seat != null && seat.getFirstPassenger() != null) {
            seat.getFirstPassenger().stopRiding();
        }

        var key = decorationBlockEntity.getBlockPos().asLong();
        if (map.containsKey(key)) {
            for (Gui gui : map.get(key)) {
                gui.close();
            }
        }
    }

    public void read(CompoundTag compoundTag, HolderLookup.Provider provider, DecorationBlockEntity blockEntity) {
        if (blockEntity.getOrCreateHolder() != null) {
            DecorationHolder holder = (DecorationHolder) blockEntity.getDecorationHolder();
            if (holder == null) {
                return;
            }

            if (blockEntity.has(BlockBoyBehaviours.ARCADE)) { // it should 100% have the behaviour but just in case
                RegistryOps<Tag> registryOps = provider.createSerializationContext(NbtOps.INSTANCE);
                if (compoundTag.contains("Cartridge")) this.cartridge = compoundTag.read("Cartridge", ItemStack.CODEC, registryOps).orElse(ItemStack.EMPTY);
                if (compoundTag.contains("Screen")) this.screen = compoundTag.read("Screen", ItemStack.CODEC, registryOps).orElse(ItemStack.EMPTY);

                this.screenElement.setItem(screen);
            }
        }
    }

    public void write(CompoundTag compoundTag, HolderLookup.Provider provider, DecorationBlockEntity blockEntity) {
        if (blockEntity.getDecorationHolder() != null) {
            if (!this.cartridge.isEmpty()) compoundTag.put("Cartridge", this.cartridge.save(provider));
            compoundTag.put("Screen", this.screen.copy().save(provider));
        }
    }

    public static class Config {
        public float seatDirection;
        public Vector3f seatTranslation;

        public Vector3f screenTranslation;
        public float screenYaw;
        public float screenPitch;
    }

    public static class Gui extends SimpleGui {
        private static final int WIDTH = 4;
        private static final int HEIGHT = 6;
        private final SimpleContainer container;
        private Runnable onClose = null;

        public Gui(MenuType<?> type, ServerPlayer player, boolean canEdit) {
            super(type, player, false);
            this.container = new SimpleContainer(1);

            this.setSlot(0, new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).hideTooltip());
            this.setSlot(1, new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).hideTooltip());
            this.setSlotRedirect(
                    2,
                    new FilteringSlot(BlockBoyComponents.ROM, this.container, 0, canEdit)
            );
            this.setSlot(3, new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).hideTooltip());
            this.setSlot(4, new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).hideTooltip());
        }

        public void open(Runnable onClose) {
            this.onClose = onClose;
            this.open();
        }

        @Override
        public void onClose() {
            this.player.getInventory().placeItemBackInInventory(this.player.containerMenu.getCarried());
            this.player.containerMenu.setCarried(ItemStack.EMPTY);

            for (int i = 0; i < this.container.getContainerSize() - 1; i++) {
                if (!this.container.getItem(i).isEmpty())
                    this.player.getInventory().placeItemBackInInventory(this.container.removeItemNoUpdate(i));
            }

            if (this.onClose != null) {
                this.onClose.run();
            }

            this.container.removeAllItems();
        }

        private String buildGuiTitle(int column, int row, float percentage) {
            column = (WIDTH - 1) - column;

            String colA = "(".repeat(column);
            String colB = ")".repeat(column);
            String rowStr = row < 6 && row >= 0 ? Character.toString(0xF700 | row) : "___>";
            String scrollStr = percentage == -1 ? "\uF000" : Character.toString(0xF800 | (int) ((0x1C) * percentage));

            return String.format("<color:#ffffff><U<xx----%s%s%s____--%s<<xxxxxxxxx</color>", colA, rowStr, colB, scrollStr);
        }

        public static class FilteringSlot extends Slot {
            private final DataComponentType<?> componentType;
            private final boolean allowModification;

            public FilteringSlot(DataComponentType<?> componentType, Container container, int index, boolean allowModification) {
                super(container, index, index, 0);
                this.componentType = componentType;
                this.allowModification = allowModification;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                Item item = stack.getItem();
                return allowModification && stack.has(componentType);
            }

            @Override
            public boolean mayPickup(Player player) {
                return allowModification;
            }

            @Override
            public boolean allowModification(Player player) {
                return allowModification;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        }
    }
}
