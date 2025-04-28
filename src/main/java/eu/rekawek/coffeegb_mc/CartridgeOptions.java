package eu.rekawek.coffeegb_mc;

public class CartridgeOptions {

    private final boolean forceDmg;

    private final boolean forceCgb;

    private final boolean useBootstrap;

    private final boolean disableBatterySaves;

    public CartridgeOptions() {
        this.forceDmg = false;
        this.forceCgb = false;
        this.useBootstrap = false;
        this.disableBatterySaves = false;
    }


    public boolean isForceDmg() {
        return forceDmg;
    }

    public boolean isForceCgb() {
        return forceCgb;
    }

    public boolean isUsingBootstrap() {
        return useBootstrap;
    }

    public boolean isSupportBatterySaves() {
        return !disableBatterySaves;
    }


}
