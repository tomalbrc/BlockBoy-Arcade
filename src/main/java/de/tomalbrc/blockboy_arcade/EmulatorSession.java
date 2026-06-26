package de.tomalbrc.blockboy_arcade;

import de.tomalbrc.blockboy_arcade.config.ModConfig;
import de.tomalbrc.filament.decoration.block.entity.DecorationBlockEntity;
import eu.pb4.polymer.virtualentity.api.data.DisplayEntityData;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.rekawek.coffeegb_mc.CartridgeOptions;
import eu.rekawek.coffeegb_mc.controller.ButtonListener;
import eu.rekawek.coffeegb_mc.emulator.BlockBoyDisplay;
import eu.rekawek.coffeegb_mc.emulator.EmulationController;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class EmulatorSession {
    @Nullable
    private EmulationController controller;

    private ServerPlayer player;
    private ConsoleInput input = new ConsoleInput();

    private final ItemStack screenDataItem;
    private ItemStack cartridgeItem;
    private final ItemDisplayElement screenElement;

    private final DecorationBlockEntity blockEntity;

    public EmulatorSession(ServerPlayer player, DecorationBlockEntity blockEntity, ItemStack screenDataItem, ItemStack cartridgeItem, ItemDisplayElement screenElement) {
        this.player = player;
        this.screenDataItem = screenDataItem;
        this.cartridgeItem = cartridgeItem;
        this.screenElement = screenElement;
        this.blockEntity = blockEntity;
    }

    public void setPlayer(ServerPlayer player) {
        this.player = player;
        if (this.controller != null)
            this.controller.setPlayer(player);
    }

    public void setCartridgeItem(ItemStack cartridgeItem) {
        this.cartridgeItem = cartridgeItem;
    }

    public ServerPlayer getPlayer() {
        return this.player;
    }

    @Nullable
    public EmulationController getController() {
        return controller;
    }

    public void playRom(RomWrapper rom) {
        this.controller = new EmulationController(new CartridgeOptions(), rom, player, cartridgeItem);
        this.controller.startEmulation();
    }

    public void tick() {
        this.onPlayerInput(player.getLastClientInput());
        this.draw();
        this.screenElement.getSyncedData().setDirty(DisplayEntityData.Item.ITEM, true);
        this.screenElement.getHolder().tick();

        Level level = this.blockEntity.getLevel();
        if (level != null && level.getGameTime() % 40 == 0)
            this.blockEntity.setChanged();
    }

    public void stop() {
        if (this.controller != null) {
            this.controller.stopEmulation();
            this.controller = null;

            CustomModelData cmd = this.screenDataItem.get(DataComponents.CUSTOM_MODEL_DATA);
            float darkenFactor = 0.2f;
            for (int i = 0; i < cmd.colors().size(); i++) {
                int rgb = cmd.colors().get(i);
                int red   = (rgb >> 16) & 0xFF;
                int green = (rgb >>  8) & 0xFF;
                int blue  = (rgb      ) & 0xFF;

                red   = Math.max(0, Math.min(255, (int)(red   * darkenFactor)));
                green = Math.max(0, Math.min(255, (int)(green * darkenFactor)));
                blue  = Math.max(0, Math.min(255, (int)(blue  * darkenFactor)));

                int darkenedRgb = (red << 16) | (green << 8) | (blue);

                cmd.colors().set(i, darkenedRgb);
            }
            this.screenDataItem.set(DataComponents.CUSTOM_MODEL_DATA, cmd);
            this.screenElement.getSyncedData().setDirty(DisplayEntityData.Item.ITEM, true);
            this.screenElement.getHolder().tick();
        }
    }

    public boolean pause() {
        if (this.controller != null) {
            this.controller.pause();
            return true;
        }
        return false;
    }

    public boolean resume() {
        if (this.controller != null) {
            this.controller.resume();
            return true;
        }
        return false;
    }

    public ConsoleInput getInput() {
        return this.input;
    }

    public void onPlayerInput(Input input) {
        if (this.controller == null)
            return;

        if (input.jump()) this.input.didPressA();

        if (this.input.isPressingA()) {
            this.controller.pressed(ButtonListener.Button.A);
            this.input.pressedA = 2;
        } else if (this.input.pressedA == 0) {
            this.controller.released(ButtonListener.Button.A);
            this.input.pressedA = -1;
        } else this.input.pressedA--;

        if (this.input.isPressingB()) {
            this.input.pressedB = 2;
            this.controller.pressed(ButtonListener.Button.B);
        } else if (this.input.pressedB == 0) {
            this.controller.released(ButtonListener.Button.B);
            this.input.pressedB = -1;
        } else this.input.pressedB--;

        if (this.input.isPressingStart()) {
            this.input.pressedStart = 2;
            this.controller.pressed(ButtonListener.Button.START);
        } else if (this.input.pressedStart == 0) {
            this.controller.released(ButtonListener.Button.START);
            this.input.pressedStart = -1;
        } else this.input.pressedStart--;

        if (this.input.isPressingSelect()) {
            this.input.pressedSelect = 2;
            this.controller.pressed(ButtonListener.Button.SELECT);
        } else if (this.input.pressedSelect == 0) {
            this.controller.released(ButtonListener.Button.SELECT);
            this.input.pressedSelect = -1;
        } else this.input.pressedSelect--;

        boolean ensureNoOppositeDirection = ModConfig.getInstance().ensureNoOppositeDirection;

        if (input.forward() && (!ensureNoOppositeDirection || !input.backward())) {
            this.input.zdirection = Direction.NORTH;
            this.controller.pressed(ButtonListener.Button.UP);
        } else if (this.input.zdirection == Direction.NORTH) {
            this.controller.released(ButtonListener.Button.UP);
            this.input.zdirection = Direction.UP; // abuse up as noop, only use n,e,s,w
        }

        if (input.backward() && (!ensureNoOppositeDirection || !input.forward())) {
            this.input.zdirection = Direction.SOUTH;
            this.controller.pressed(ButtonListener.Button.DOWN);
        } else if (this.input.zdirection == Direction.SOUTH) {
            this.controller.released(ButtonListener.Button.DOWN);
            this.input.zdirection = Direction.UP; // abuse up as noop, only use n,e,s,w
        }

        if (input.right() && (!ensureNoOppositeDirection || !input.left())) {
            this.input.xdirection = Direction.EAST;
            this.controller.pressed(ButtonListener.Button.RIGHT);
        } else if (this.input.xdirection == Direction.EAST) {
            this.controller.released(ButtonListener.Button.RIGHT);
            this.input.xdirection = Direction.UP; // abuse up as noop, only use n,e,s,w
        }

        if (input.left() && (!ensureNoOppositeDirection || !input.right())) {
            this.input.xdirection = Direction.WEST;
            this.controller.pressed(ButtonListener.Button.LEFT);
        } else if (this.input.xdirection == Direction.WEST) {
            this.controller.released(ButtonListener.Button.LEFT);
            this.input.xdirection = Direction.UP; // abuse up as noop, only use n,e,s,w
        }
    }

    private void draw() {
        if (this.controller != null) {
            CustomModelData image = this.controller.getDisplay().render(BlockBoyDisplay.DISPLAY_WIDTH, BlockBoyDisplay.DISPLAY_HEIGHT);
            this.screenDataItem.set(DataComponents.CUSTOM_MODEL_DATA, image);
        }
    }
}
