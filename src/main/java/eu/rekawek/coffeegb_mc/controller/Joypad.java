package eu.rekawek.coffeegb_mc.controller;

import eu.rekawek.coffeegb_mc.AddressSpace;
import eu.rekawek.coffeegb_mc.cpu.InterruptManager;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class Joypad implements AddressSpace, Serializable {

    private final Set<ButtonListener.Button> buttons = new CopyOnWriteArraySet<>();
    private final InterruptManager interruptManager;
    private int p1;

    public Joypad(InterruptManager interruptManager) {
        this.interruptManager = interruptManager;
    }

    public void pressedButton(ButtonListener.Button b) {
        interruptManager.requestInterrupt(InterruptManager.InterruptType.P10_13);
        buttons.add(b);
    }
    public void releaseButton(ButtonListener.Button b) {
        buttons.remove(b);
    }

    @Override
    public boolean accepts(int address) {
        return address == 0xff00;
    }

    @Override
    public void setByte(int address, int value) {
        p1 = value & 0b00110000;
    }

    @Override
    public int getByte(int address) {
        int result = p1 | 0b11001111;
        for (ButtonListener.Button b : buttons) {
            if ((b.getLine() & p1) == 0) {
                result &= 0xff & ~b.getMask();
            }
        }
        return result;
    }
}
