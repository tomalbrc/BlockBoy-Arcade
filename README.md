Arcade machines with full gameboy resolution and color support!

Fully server-side, using polymer!
[Checkout polymers authost feature](https://polymer.pb4.eu/latest/user/resource-pack-hosting/) to host the resource-pack (in polymer/resourcepack.zip) automagically!

There are not recipes, use `/polymer creative` to see the items or use `/give @s blockboy:arcade`

The mods' builtin datapack comes with 6 open source gameboy games:
- [Regegade Rush](https://quinnp.itch.io/renegade-rush)
- [Wyrmhole](https://quinnp.itch.io/wyrmhole)
- [Postie](https://invertedhat.itch.io/postie)
- [Dawn Will Come](https://eishiya.itch.io/dawn-will-come)
- [Tobu Tobu Girl Deluxe](https://tangramgames.itch.io/tobu-tobu-girl-deluxe)
- [Flooder](https://github.com/Obalfour/Flooder)

## Config

```json
{
  "brightness": 10,
  "dateFormat": "dd.MM yyyy, HH:mm",
  "sound": false
}
```

`brightness`: Brightness of the screen
`dateFormat`: Date format for rom save data tooltip
`sound`: Flag to enable sounds for the playing player using simple voice chat

## Adding roms / items

You can either add cartridge items using filament or just use the `blockboy:rom` component to give yourself an item that can play a given rom file:
`/give @s paper[blockboy:rom="namespace:game.gb"]`

Put the roms in `data/<namespace>/blockboy/<rom>.gb` and `data/<namespace>/blockboy/<rom>.gbc`
Make sure the rom file names are lowercase and only contain letters from `a` to `z`, `0-9` and/or underscores.

To add your own arcade model item + block use the filament mod and the `blockboy:arcade` behaviour for decorations.

Arcade example for filament (data/<namespace>/filament/decoration/<name>.json):
```json
{
  "id": "blockboy:arcade",
  "group": "blockboy:items",
  "blockTags": ["shulker_boxes"],
  "itemResource": {
    "models": {
      "default": "blockboy:item/arcade-item",
      "floor": "blockboy:item/arcade"
    }
  },
  "properties": {
    "rotate": true,
    "rotateSmooth": true,
    "stackSize": 1,
    "destroyTime": 2,
    "requiresTool": false
  },
  "behaviour": {
    "blockboy:arcade": {
      "seatTranslation": [0, 0.4, 0],
      "screenTranslation": [0, 0.95, -0.775],
      "screenPitch": -22.5
    }
  },
  "blocks": [
    {
      "origin": [0,0,-1],
      "size": [1,1,2]
    },
    {
      "origin": [0,1,-1],
      "size": [1,1,1]
    }
  ]
}
```

Cartridge / rom item example for filament (data/<namespace>/filament/item/<name>.json):
```json
{
  "id": "blockboy:flooder",
  "translations": {
    "en_us": "Flooder"
  },
  "group": "blockboy:items",
  "vanillaItem": "minecraft:paper",
  "itemResource":{
    "models": {
      "default": "blockboy:item/cartridge2"
    }
  },
  "properties": {
    "stackSize": 1
  },
  "components": {
    "blockboy:rom": "namespace:flooder.gb"
  }
}
```

Blockboy Arcade comes with 2 item textures/models for cartridges, feel free to modify them as you wish:
`blockboy:item/cartridge1` and `blockboy:item/cartridge2`


Permissions for Luckperms and friends: `blockboy_arcade.command`, with vanilla permission level 1

## Controls:
- WASD = DPAD
- Jump = A
- Left-Click = B
- F (swap held item) = Start
- Q (drop held item) = Select



---
This project uses code from the [coffee-gb gameboy emulator](https://github.com/trekawek/coffee-gb) and the [Image2Map Mod](https://github.com/Patbox/Image2Map) 
You can find the original licenses in the "licenses" directory.
My changes are also licensed under the MIT license.
