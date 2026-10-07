<h1 align="center">Traveller's Handbook<br>
<a href="https://www.curseforge.com/minecraft/mc-mods/travellers-handbook"><img src="https://img.shields.io/badge/CurseForge-1.20.1%20%7C%201.21.1-orange"></a>
<a href="https://modrinth.com/mod/travellers-handbook"><img src="https://img.shields.io/badge/Modrinth-1.20.1%20%7C%201.21.1-green"></a>
<a href="https://twitter.com/Rasa_Novum"><img src="https://img.shields.io/badge/Socials-Xitter-black"></a>
<a href="https://discord.gg/WGh4mq6W5U"><img src="https://img.shields.io/badge/Socials-Discord-5865F2"></a>
</h1>
Traveller's handbook is a quest-adjacent player progression tracking mod that adds a journal GUI to the game. This journal is filled with obfuscated or hidden entries that only reveal themselves when the player discovers something related to the entry. Entries typically describe the event that was observed and potentially provide some clues towards next steps for the player.

The journal entries are data-driven, with each entry being a .json file in the resources of the mod. No entries are loaded by default unless you enable the config option to "load default entries", which loads an internal datapack, or you add your own entries datapack. An template datapack is provided on our [Github](https://github.com/RasaNovum/Travellers_Handbook) page.

Entries will be visible by default unless hidden behind a lock requirement, in which case they will be obfuscated by enchanting runes. Goal requirements leave an entry visible and show an unfinished marker until every requirement is complete. These runes randomize/animate by default, which can be disabled for accessibility purposes in config.

## Quest requirements

An entry can contain any combination of advancement, item, structure, and entry requirements. All requirements on an entry must be complete. Requirements are tracked per player and can use either `lock` or `goal` mode. Locks obfuscate the entry until they are complete. Goals remain visible, show an empty grey circle, and receive a green checkmark when complete. A category receives a checkmark when all of its entries that have requirements are complete; entries without requirements do not count toward the category checkmark.

The entry-level `type` sets the default mode for requirements that do not specify one. If it is omitted, requirements use `lock`, so existing entries with only a top-level advancement string remain locked for compatibility. Set the entry-level type to `goal`, or use an advancement object to override it, when an advancement should be a goal:

```json
{
  "category": "Progression",
  "title": "A New Beginning",
  "description": "I have learned the basics of survival.",
  "advancement": {
    "id": "minecraft:story/mine_stone",
    "type": "goal"
  }
}
```

Each of `advancement`, `item`, `structure`, and `entry` can be a string ID or an object with an `id`. String item requirements use amount `1`; object requirements can set their mode with `type` and item objects can set `amount`. Requirement objects can provide an optional `displayName` as the progress label. For example, `"displayName": "Collect wheat"` produces `Collect wheat: 42/1000`; the counter is appended automatically. If it is omitted, the journal generates a label from the requirement type and ID. Advancement display names can make a technical ID readable:

```json
{
  "advancement": {
    "id": "minecraft:story/obtain_armor",
    "type": "goal",
    "displayName": "Make a set of iron armor"
  }
}
```

Ungated item requirements use Minecraft's cumulative pickup statistics in the current world or server, including pickups recorded before the entry was installed. They count items collected from the ground, including items dropped and picked up again. Crafting, taking items from containers, and `/give` do not increase this statistic. Spending, storing, or losing the items does not reduce progress. Item goals in an entry with lock requirements start counting only after all locks are completed, and keep their earned count if the entry becomes locked again. The amount defaults to `1` and must be a positive whole number no greater than `2147483647`. This standalone entry tracks 1000 wheat:

```json
{
  "category": "Farming",
  "title": "A Bountiful Harvest",
  "description": "I have gathered enough wheat to feed a village.",
  "item": {
    "id": "minecraft:wheat",
    "amount": 1000,
    "type": "goal",
    "displayName": "Collect wheat"
  }
}
```

Structure requirements complete when the player visits the configured structure. Structure IDs can use a structure tag such as `#minecraft:village`; tags are normalized to their canonical namespace. A lock and a goal can be written as follows:

```json
{
  "category": "Exploration",
  "title": "A Dangerous Place",
  "description": "I should be prepared before I enter this place.",
  "structure": {
    "id": "minecraft:ancient_city",
    "type": "lock",
    "displayName": "Find an ancient city"
  }
}
```

```json
{
  "category": "Exploration",
  "title": "Lost in the Woods",
  "description": "I found a woodland mansion.",
  "structure": {
    "id": "minecraft:mansion",
    "type": "goal",
    "displayName": "Find a woodland mansion"
  }
}
```

An `entry` requirement refers to another entry by its datapack resource ID. It defaults to `lock`, so it can reveal a follow-up entry after the prerequisite is complete. This pair shows a village visit unlocking a wheat goal:

`quest_village.json`

```json
{
  "category": "Exploration",
  "title": "A Village Ahead",
  "description": "I found a village.",
  "structure": {
    "id": "#minecraft:village",
    "type": "lock",
    "displayName": "Find a village"
  }
}
```

`quest_village_farming_1000_wheat.json`

```json
{
  "category": "Farming",
  "title": "A Village Harvest",
  "description": "I gathered enough wheat to feed the village.",
  "entry": {
    "id": "travellers-handbook:quest_village",
    "type": "lock",
    "displayName": "Find a village"
  },
  "item": {
    "id": "minecraft:wheat",
    "amount": 1000,
    "type": "goal",
    "displayName": "Collect wheat"
  }
}
```

When an entry has mixed requirements, every incomplete `lock` keeps the entry hidden and freezes its goal progress. After the locks are complete, the entry becomes visible and its goals can advance. Entry prerequisites require all requirements on the referenced entry to be complete. Missing or cyclic entry references remain locked and incomplete. Advancement goals evaluate the player's current vanilla advancement state when they unlock, while gated structure goals require a visit after unlocking. Progress persists through death, reconnects, and temporary relocking.

Multiple top-level requirements are combined with AND. For example, this entry is a visible goal that is only complete after the player has the advancement and has collected the item:

```json
{
  "category": "Adventure",
  "title": "Ready for the Nether",
  "description": "I have gathered the supplies and proven myself.",
  "type": "goal",
  "advancement": {
    "id": "minecraft:story/obtain_armor",
    "type": "goal"
  },
  "item": {
    "id": "minecraft:obsidian",
    "amount": 10
  }
}
```

Requirement progress persists with each player. Advancements use the normal vanilla advancement state, item progress uses the player's pickup statistics, and structure progress is recorded when a matching structure is discovered while the mod and requirement are installed. Datapack reloads apply the current entry definitions; they do not discard a player's saved progress. A compact `requirements` array is also accepted for entries that need several requirements of the same or different types:

```json
{
  "category": "Adventure",
  "title": "A Long Road",
  "description": "Several milestones remain.",
  "requirements": [
    {"type": "advancement", "id": "minecraft:nether/return_to_sender", "mode": "goal", "displayName": "Return to sender"},
    {"type": "item", "id": "minecraft:blaze_rod", "amount": 8, "mode": "goal", "displayName": "Blaze rods"},
    {"type": "structure", "id": "minecraft:fortress", "mode": "lock", "displayName": "Find a Nether fortress"}
  ]
}
```

## Accessibility

- Use the config option **reduceRuneMotion** to disable animation and keep obfuscated journal runes static.

## Dependencies

- **[Fabric API](https://modrinth.com/mod/fabric-api)** - For Fabric versions
- **[NeoForge](https://neoforged.net/)** - For NeoForge version
- **[MidnightLib](https://modrinth.com/mod/midnightlib)** - Provides config

## License

**Traveller's Handbook** is licensed under the **Responsive Source License (RSL) v1.0**. See the [LICENSE](LICENSE.md) file for full details.

Feel free to include this mod in your modpack as long as you download it from our official sources.
