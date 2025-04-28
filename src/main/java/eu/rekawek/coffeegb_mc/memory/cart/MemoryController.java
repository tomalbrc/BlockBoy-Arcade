package eu.rekawek.coffeegb_mc.memory.cart;

import eu.rekawek.coffeegb_mc.AddressSpace;

import java.io.Serializable;

public interface MemoryController extends AddressSpace, Serializable {
    default void flushRam() {}
}
