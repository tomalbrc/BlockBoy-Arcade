package eu.rekawek.coffeegb_mc.controller;

public interface Controller {

    void setButtonListener(ButtonListener listener);

    Controller NULL_CONTROLLER = listener -> {};
}
