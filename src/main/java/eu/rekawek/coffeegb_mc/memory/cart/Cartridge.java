package eu.rekawek.coffeegb_mc.memory.cart;

import de.tomalbrc.blockboy_arcade.RomWrapper;
import eu.rekawek.coffeegb_mc.AddressSpace;
import eu.rekawek.coffeegb_mc.memory.BootRom;
import eu.rekawek.coffeegb_mc.memory.cart.battery.Battery;
import eu.rekawek.coffeegb_mc.memory.cart.battery.ItemStackBattery;
import eu.rekawek.coffeegb_mc.memory.cart.type.*;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Cartridge implements AddressSpace, Serializable {

    public enum GameboyTypeFlag {
        UNIVERSAL, CGB, NON_CGB;

        private static GameboyTypeFlag getFlag(int value) {
            if (value == 0x80) {
                return UNIVERSAL;
            } else if (value == 0xc0) {
                return CGB;
            } else {
                return NON_CGB;
            }
        }
    }

    public enum GameboyType {
        AUTOMATIC, FORCE_DMG, FORCE_CGB
    }

    private static final Logger LOG = LoggerFactory.getLogger(Cartridge.class);

    private final MemoryController addressSpace;

    private final boolean gbc;
    private final String title;
    private final Battery battery;
    private final boolean useBootstrap;

    private int dmgBoostrap;

    public Cartridge(RomWrapper romFile, ItemStack cartridgeItem) throws IOException {
        this(romFile, cartridgeItem, true, GameboyType.AUTOMATIC, false);
    }

    public Cartridge(RomWrapper romFile, ItemStack cartridgeItem, boolean supportBatterySaves, GameboyType overrideGameboyType, boolean useBootstrap) throws IOException {
        int[] rom = loadFile(new ByteArrayInputStream(romFile.data()));
        CartridgeType type = CartridgeType.getById(rom[0x0147]);
        String anyTitle = getTitle(rom);
        if (anyTitle.isBlank()) {
            anyTitle = romFile.name();
        }
        this.title = anyTitle;
        LOG.debug("Cartridge {}, type: {}", title, type);

        GameboyTypeFlag gameboyType = GameboyTypeFlag.getFlag(rom[0x0143]);
        int romBanks = getRomBanks(rom[0x0148]);
        int ramBanks = getRamBanks(rom[0x0149]);
        if (ramBanks == 0 && type.isRam()) {
            LOG.warn("RAM bank is defined to 0. Overriding to 1.");
            ramBanks = 1;
        }
        LOG.debug("ROM banks: {}, RAM banks: {}", romBanks, ramBanks);

        if (type.isBattery() && supportBatterySaves) {
            this.battery = new ItemStackBattery(cartridgeItem, 0x2000 * ramBanks);
        } else {
            this.battery = Battery.NULL_BATTERY;
        }

        if (type.isMbc1()) {
            this.addressSpace = new Mbc1(rom, this.battery, romBanks, ramBanks);
        } else if (type.isMbc2()) {
            this.addressSpace = new Mbc2(rom, this.battery);
        } else if (type.isMbc3()) {
            this.addressSpace = new Mbc3(rom, this.battery, ramBanks);
        } else if (type.isMbc5()) {
            this.addressSpace = new Mbc5(rom, this.battery, ramBanks);
        } else {
            this.addressSpace = new Rom(rom, type, romBanks, ramBanks);
        }

        this.dmgBoostrap = useBootstrap ? 0 : 1;
        if (overrideGameboyType == GameboyType.FORCE_CGB) {
            this.gbc = true;
        } else if (gameboyType == Cartridge.GameboyTypeFlag.NON_CGB) {
            this.gbc = false;
        } else { // UNIVERSAL
            this.gbc = overrideGameboyType != GameboyType.FORCE_DMG;
        }
        this.useBootstrap = useBootstrap;
    }

    private String getTitle(int[] rom) {
        StringBuilder t = new StringBuilder();
        for (int i = 0x0134; i < 0x0143; i++) {
            char c = (char) rom[i];
            if (c == 0) {
                break;
            }
            t.append(c);
        }
        return t.toString();
    }

    public String getTitle() {
        return title;
    }

    public boolean isGbc() {
        return gbc;
    }

    public boolean isUseBootstrap() {
        return useBootstrap;
    }

    @Override
    public boolean accepts(int address) {
        return addressSpace.accepts(address) || address == 0xff50;
    }

    @Override
    public void setByte(int address, int value) {
        if (address == 0xff50) {
            dmgBoostrap = 1;
        } else {
            addressSpace.setByte(address, value);
        }
    }

    @Override
    public int getByte(int address) {
        if (dmgBoostrap == 0 && !gbc && (address >= 0x0000 && address < 0x0100)) {
            return BootRom.GAMEBOY_CLASSIC[address];
        } else if (dmgBoostrap == 0 && gbc && address >= 0x000 && address < 0x0100) {
            return BootRom.GAMEBOY_COLOR[address];
        } else if (dmgBoostrap == 0 && gbc && address >= 0x200 && address < 0x0900) {
            return BootRom.GAMEBOY_COLOR[address - 0x0100];
        } else if (address == 0xff50) {
            return 0xff;
        } else {
            return addressSpace.getByte(address);
        }
    }

    public void flushBattery() {
        addressSpace.flushRam();
    }

    private static int[] loadFile(InputStream file) throws IOException {
        return load(file, file.available());
    }

    private static int[] loadFile(File file) throws IOException {
        String ext = FilenameUtils.getExtension(file.getName());
        try (InputStream is = Files.newInputStream(file.toPath())) {
            if ("zip".equalsIgnoreCase(ext)) {
                try (ZipInputStream zis = new ZipInputStream(is)) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        String name = entry.getName();
                        String entryExt = FilenameUtils.getExtension(name);
                        if (Stream.of("gb", "gbc", "rom").anyMatch(e -> e.equalsIgnoreCase(entryExt))) {
                            return load(zis, (int) entry.getSize());
                        }
                        zis.closeEntry();
                    }
                }
                throw new IllegalArgumentException("Can't find ROM file inside the zip.");
            } else {
                return load(is, (int) file.length());
            }
        }
    }

    private static int[] load(InputStream is, int length) throws IOException {
        byte[] byteArray = IOUtils.toByteArray(is, length);
        int[] intArray = new int[byteArray.length];
        for (int i = 0; i < byteArray.length; i++) {
            intArray[i] = byteArray[i] & 0xff;
        }
        return intArray;
    }

    private static int getRomBanks(int id) {
        return switch (id) {
            case 0 -> 2;
            case 1 -> 4;
            case 2 -> 8;
            case 3 -> 16;
            case 4 -> 32;
            case 5 -> 64;
            case 6 -> 128;
            case 7 -> 256;
            case 0x52 -> 72;
            case 0x53 -> 80;
            case 0x54 -> 96;
            default -> throw new IllegalArgumentException("Unsupported ROM size: " + Integer.toHexString(id));
        };
    }

    private static int getRamBanks(int id) {
        return switch (id) {
            case 0 -> 0;
            case 1, 2 -> 1;
            case 3 -> 4;
            case 4 -> 16;
            default -> throw new IllegalArgumentException("Unsupported RAM size: " + Integer.toHexString(id));
        };
    }
}
