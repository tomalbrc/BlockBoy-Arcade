package de.tomalbrc.blockboy_arcade;

import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData;
import eu.rekawek.coffeegb.CartridgeOptions;
import eu.rekawek.coffeegb.controller.ButtonListener;
import eu.rekawek.coffeegb.emulator.BlockBoyDisplay;
import eu.rekawek.coffeegb.emulator.EmulationController;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import org.jetbrains.annotations.Nullable;

public class EmulatorSession {
    @Nullable
    private EmulationController controller;

    private ServerPlayer player;

    private final ItemStack screenDataItem;
    private final ItemStack cartridgeItem;
    private final ItemDisplayElement screenElement;

    private Runnable onClose;
    private Runnable onTick;

    public EmulatorSession(ServerPlayer player, ItemStack screenDataItem, ItemStack cartridgeItem, ItemDisplayElement screenElement) {
        this.player = player;
        this.screenDataItem = screenDataItem;
        this.cartridgeItem = cartridgeItem;
        this.screenElement = screenElement;
        BlockBoyArcade.ACTIVE_SESSIONS.put(player, this);
    }

    public void setPlayer(ServerPlayer player) {
        this.player = player;
        if (this.controller != null)
            this.controller.setPlayer(player);
    }

    @Nullable
    public EmulationController getController() {
        return controller;
    }

    public void playRom(RomWrapper rom, Runnable onTick, Runnable onClose) {
        this.controller = new EmulationController(new CartridgeOptions(), rom, player, cartridgeItem);
        this.controller.startEmulation();
        this.onClose = onClose;
        this.onTick = onTick;
    }

    public void tick() {
        this.onPlayerInput(player.getLastClientInput());
        this.draw();
        this.screenElement.getDataTracker().setDirty(DisplayTrackedData.Item.ITEM, true);
        this.screenElement.getHolder().tick();
        this.onTick.run();
    }

    public void stop() {
        if (this.controller != null) {
            this.controller.stopEmulation();
            this.onClose.run();
            this.controller = null;
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

    ConsoleInput input = new ConsoleInput();
    static public class ConsoleInput {
        private boolean pressingA = false;
        private boolean pressingB = false;
        private boolean pressingStart = false;
        private boolean pressingSelect = false;

        public void didPressA() {
            this.pressingA = true;
        }

        public void didPressB() {
            this.pressingB = true;
        }

        public void didPressStart() {
            this.pressingStart = true;
        }

        public void didPressSelect() {
            this.pressingSelect = true;
        }

        public boolean isPressingSelect() {
            if (pressingSelect) {
                pressingSelect = false;
                return true;
            }
            return false;
        }

        public boolean isPressingStart() {
            if (pressingStart) {
                pressingStart = false;
                return true;
            }
            return false;
        }

        public boolean isPressingB() {
            if (pressingB) {
                pressingB = false;
                return true;
            }
            return false;
        }

        public boolean isPressingA() {
            if (pressingA) {
                pressingA = false;
                return true;
            }
            return false;
        }

        public int pressedA = -1;
        public int pressedB = -1;
        public int pressedStart = -1;
        public int pressedSelect = -1;
        public Direction zdirection = Direction.UP;
        public Direction xdirection = Direction.UP;
    }

    public ConsoleInput getInput() {
        return this.input;
    }

    public void onPlayerInput(Input input) {
        if (controller == null)
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

        if (input.forward()) {
            this.input.zdirection = Direction.NORTH;
            this.controller.pressed(ButtonListener.Button.UP);
        } else if (this.input.zdirection == Direction.NORTH) {
            this.controller.released(ButtonListener.Button.UP);
            this.input.zdirection = Direction.UP; // abuse up as noop, only use n,e,s,w
        }

        if (input.backward()) {
            this.input.zdirection = Direction.SOUTH;
            this.controller.pressed(ButtonListener.Button.DOWN);
        } else if (this.input.zdirection == Direction.SOUTH) {
            this.controller.released(ButtonListener.Button.DOWN);
            this.input.zdirection = Direction.UP; // abuse up as noop, only use n,e,s,w
        }

        if (input.right()) {
            this.input.xdirection = Direction.EAST;
            this.controller.pressed(ButtonListener.Button.RIGHT);
        } else if (this.input.xdirection == Direction.EAST) {
            this.controller.released(ButtonListener.Button.RIGHT);
            this.input.xdirection = Direction.UP; // abuse up as noop, only use n,e,s,w
        }

        if (input.left()) {
            this.input.xdirection = Direction.WEST;
            this.controller.pressed(ButtonListener.Button.LEFT);
        } else if (this.input.xdirection == Direction.WEST) {
            this.controller.released(ButtonListener.Button.LEFT);
            this.input.xdirection = Direction.UP; // abuse up as noop, only use n,e,s,w
        }
    }

    private void draw() {
        CustomModelData image = null;
        if (controller == null) {
            image = CustomModelData.EMPTY;
        } else {
            image = controller.getDisplay().render(BlockBoyDisplay.DISPLAY_WIDTH, BlockBoyDisplay.DISPLAY_HEIGHT);
        }

        this.screenDataItem.set(DataComponents.CUSTOM_MODEL_DATA, image);
    }
}
