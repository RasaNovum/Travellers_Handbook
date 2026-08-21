# Entry Obfuscation

If an entry is not unlocked by the player yet, the title and description of the entry will be obfuscated or scrambled in the GUI.

This is done by randomly translating the text in the string to the `Minecraft.ALT_FONT`, or the enchanting runes.

By default, this obfuscation is animated, constantly changing and re-scrambling itself.

This can be disabled in the config via setting `reduceRuneMotion` to `false`.