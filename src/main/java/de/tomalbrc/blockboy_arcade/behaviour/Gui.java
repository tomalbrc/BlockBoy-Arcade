package de.tomalbrc.blockboy_arcade.behaviour;

import de.tomalbrc.blockboy_arcade.BlockBoyArcade;
import de.tomalbrc.blockboy_arcade.component.BlockBoyComponents;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

public class Gui extends SimpleGui {
    private final SimpleContainer container;
    private final ArcadeBehaviour arcadeBehaviour;
    private final FilteringSlot slot;

    public Gui(MenuType<?> type, ServerPlayer player, ArcadeBehaviour arcadeBehaviour) {
        super(type, player, false);
        this.container = new SimpleContainer(1);
        this.container.setItem(0, arcadeBehaviour.getCartridge());
        this.arcadeBehaviour = arcadeBehaviour;

        this.slot = new FilteringSlot(BlockBoyComponents.ROM, this.container, 0, !arcadeBehaviour.isPlaying());

        this.setSlot(0, new GuiElementBuilder(Items.STAINED_GLASS_PANE.gray()).hideTooltip());
        this.setSlot(1, new GuiElementBuilder(Items.STAINED_GLASS_PANE.gray()).hideTooltip());
        this.setSlot(
                2,
                this.slot
        );
        this.setSlot(3, new GuiElementBuilder(Items.STAINED_GLASS_PANE.gray()).hideTooltip());

        boolean hasValidItem = !arcadeBehaviour.getCartridge().isEmpty() && arcadeBehaviour.getCartridge().has(BlockBoyComponents.ROM);
        boolean canStartStop = arcadeBehaviour.getPlayer() == player;
        if (arcadeBehaviour.isPlaying() && hasValidItem && canStartStop) {
            this.setOffSlot();
        } else {
            if (canStartStop && hasValidItem && BlockBoyArcade.ACTIVE_SESSIONS.containsKey(player)) {
                this.setOnSlot();
            } else {
                this.setDefaultSlot();
            }
        }
    }

    private void setOnSlot() {
        this.setSlot(4, new GuiElementBuilder(Items.CONCRETE.green()).setItemName(Component.literal("Turn On")).setCallback(() -> {
            ItemStack item = this.getContainer().getItem(0);
            if (item.has(BlockBoyComponents.ROM)) {
                this.arcadeBehaviour.setCartridge(item);
                this.arcadeBehaviour.play(item.get(BlockBoyComponents.ROM));
                this.close();
            }
        }));
    }

    private void setOffSlot() {
        this.setSlot(4, new GuiElementBuilder(Items.CONCRETE.red()).setItemName(Component.literal("Turn Off")).setCallback(() -> {
            this.arcadeBehaviour.clearSession();
            this.slot.setAllowModification(true);
            this.setDefaultSlot();
            this.updateTitle();
        }));
    }

    private void setDefaultSlot() {
        this.setSlot(4, new GuiElementBuilder(Items.STAINED_GLASS_PANE.gray()).hideTooltip());
    }

    @Override
    public void onOpen() {
        super.onOpen();

        this.arcadeBehaviour.pauseSession(player, false);
        this.updateTitle();
    }

    private void updateTitle() {
        this.setTitle(Component.literal("BlockBoy " + (this.arcadeBehaviour.isPlaying() ? "§c" : this.arcadeBehaviour.getController() == null ? "§e" : "§6") + "[" + (this.arcadeBehaviour.isPlaying() ? "Running" : this.arcadeBehaviour.getController() == null ? "Off" : "Paused") + "]"));
    }

    @Override
    public void onRemoved() {
        this.player.getInventory().placeItemBackInInventory(this.player.containerMenu.getCarried());
        this.player.containerMenu.setCarried(ItemStack.EMPTY);

        var cart = arcadeBehaviour.getCartridge();

        for (int i = 0; i < this.container.getContainerSize() - 1; i++) {
            if (!this.container.getItem(i).isEmpty())
                this.player.getInventory().placeItemBackInInventory(this.container.removeItemNoUpdate(i));
        }

        this.arcadeBehaviour.onGuiClosed(this.container.getItem(0));

        if (cart == this.container.getItem(0))
            this.arcadeBehaviour.resumeSession(player, false);

        this.container.removeAllItems();
    }

    public SimpleContainer getContainer() {
        return this.container;
    }

    public class FilteringSlot extends Slot {
        private final DataComponentType<?> componentType;
        private boolean allowModification;

        public void setAllowModification(boolean allowModification) {
            this.allowModification = allowModification;
        }

        public FilteringSlot(DataComponentType<?> componentType, Container container, int index, boolean allowModification) {
            super(container, index, index, 0);
            this.componentType = componentType;
            this.allowModification = allowModification;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (slot.allowModification) {
                if (!this.getItem().isEmpty() && BlockBoyArcade.ACTIVE_SESSIONS.containsKey(player))
                    setOnSlot();
                else
                    setDefaultSlot();
            }
        }

        @Override
        public boolean mayPlace(@NonNull ItemStack stack) {
            return this.allowModification && stack.has(this.componentType);
        }

        @Override
        public boolean mayPickup(@NonNull Player player) {
            return this.allowModification;
        }

        @Override
        public boolean allowModification(@NonNull Player player) {
            return this.allowModification;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
