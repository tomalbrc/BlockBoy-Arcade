package de.tomalbrc.blockboy_arcade;

import net.minecraft.core.Direction;

public class ConsoleInput {
    public static ConsoleInput EMPTY = new ConsoleInput();

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
