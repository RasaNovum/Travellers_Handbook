<h1 align="center">Traveller's Handbook<br>
<a href="https://www.curseforge.com/minecraft/mc-mods/travellers-handbook"><img src="https://img.shields.io/badge/CurseForge-1.20.1%20%7C%201.21.1-orange"></a>
<a href="https://modrinth.com/mod/travellers-handbook"><img src="https://img.shields.io/badge/Modrinth-1.20.1%20%7C%201.21.1-green"></a>
<a href="https://twitter.com/Rasa_Novum"><img src="https://img.shields.io/badge/Socials-Xitter-black"></a>
<a href="https://discord.gg/WGh4mq6W5U"><img src="https://img.shields.io/badge/Socials-Discord-5865F2"></a>
</h1>
Traveller's Handbook is a player progression tracking mod that adds a player journal to the game. This journal, or handbook, is filled with entries that can either act as a quest log or as expositional pieces to reveal information to the player as they discover things around them.

The journal entries are data-driven, with each entry being a .json file in the resources of the mod. No entries are loaded by default unless you enable the config option to "load default entries", which loads an internal datapack. We encourage you to create your own entries to tune your experience to exactly what your use case is. A template datapack is provided on our [Github](https://github.com/RasaNovum/Travellers_Handbook) page.

Entries will be visible by default unless hidden behind a lock requirement, in which case they will be obfuscated by enchanting runes. These runes randomize/animate by default, which can be disabled for accessibility purposes in config.

## Anatomy of a Journal Entry

An entry can contain any combination of advancement, item, mine, craft, structure, and entry requirements. All requirements on an entry must be fulfilled to complete the entry. Requirements are tracked per player and can use either `lock` or `goal` mode. Lock obfuscates the entry until it is complete. Goals remain visible, show an empty grey circle, and receive a green checkmark when complete. A category receives a checkmark when all of its entries are complete.

### Category

The displayed category the entry is sorted under. A maximum of one `subcategory` is permitted to further group entries under a given category.

### Description

`description` is the text that makes up the body of the entry. An optional `incompleteDescription` argument will show a different text body while the entry is incomplete. Once all requirements are complete, the journal automatically switches to `description`. If `incompleteDescription` is omitted, `description` is displayed in both states. Locked entries remain obfuscated until their lock requirements are satisfied.

### Requirement

You can set any of `advancement`, `item`, `mine`, `craft`, `structure`, or `entry` to be the trigger or requirement for an entry. `Advancement` ties the entry to the player unlocking a specific advancement, `Item` can be used to track the possession of specific items, while `mine` and `craft` can specify this further to enforce getting an item through those methods. `Structure` gates the requirement behind entering a given structure for the first time, and `entry` requires the completion of another entry to satisfy the requirement.

Multiple unique requirement arguments can be provided, use a requirement array when order is important for the multiple requirements being added.

#### Id

This is the specified id for the given requirement, be it a block id, advancement id, entry id, etc. This is where you point towards what you want the requirement to be for this entry.

#### Display Name

An optional argument that will display instead of the requirement's default representation.

#### Type

`type` sets the default mode for requirements that do not specify one. If it is omitted, requirements use `lock`, so existing entries with only a top-level advancement string remain locked for compatibility. Set the entry-level type to `goal`, or use an advancement object to override it, when an advancement should be a goal.

For multi-requirement entries, entries that are intended to be prerequisites to the subsequent steps/requirements can be set to `lock`, while the subsequent requirements can be set to `goal`.

#### Amount

`amount` can be used for `item`, `mine`, and `craft` requirement types. This is optional, as by default `amount` will be equal to 1 if not specified. Counts toward `amount` will not increment until any and all prerequisites to this requirement are complete for a given entry.

#### Requirement Array

A `requirements` array can be constructed to enforce a specific order to a multi-requirement entry. This uses a new argument `class` to define the class of requirement you intend to use for each element of the array, such as `item`, `advancement`, etc.

The array otherwise is populated with the same arguments that can usually be provided for a given requirement.

## Examples

A simple entry that captures the capability of what entries were limited to at the launch of Traveller's Handbook (TH).

```json
{
  "category": "Progression",
  "title": "Ironclad",
  "description":  "Now this is some serious protection! I should be well equipped to face the many monsters of the night.",
  "advancement": "minecraft:story/obtain_armor"
}
```

An expansion of that simple entry to showcase the `goal` type and how that can begin to expand the functionality of TH.

```json
{
  "category": "Progression",
  "title": "Ironclad",
  "description":  "Now this is some serious protection! I should be well equipped to face the many monsters of the night.",
  "advancement": {
    "id": "minecraft:story/obtain_armor",
    "displayName": "Craft some iron armour.",
    "type": "goal"
  }
}
```

This can be further expanded upon with changing descriptions, where the following entry has two descriptions for when the entry requirements are complete or incomplete.

```json
{
  "category": "Progression",
  "title": "Rock and Stone",
  "incompleteDescription": "I'm going to need more than simple wooden tools... Let me try to mine some stone.",
  "description":  "Now that I've got some stone, let me try to make some better tools!",
  "advancement": {
    "id": "minecraft:story/mine_stone",
    "displayName": "Use a pickaxe to mine stone for the first time.",
    "type": "goal"
  }
}
```

Another requirement is `mine` that uses Minecraft's native block-mined statistics. The ID can name a block, such as `minecraft:stone`, or a registered block tag such as `#minecraft:base_stone_overworld`. The amount counts blocks broken by the player, including blocks that are placed and broken repeatedly.

```json
{
  "category": "Mining",
  "title": "The Quarry",
  "description": "I have quarried enough stone for the next road.",
  "incompleteDescription": "Mine 200 stone to expand the road network",
  "mine": {
    "id": "minecraft:stone",
    "amount": 200,
    "type": "goal"
  }
}
```
Alternatively, replace "mine" with the following to use a tag as the `id`.

```json
{
  "mine": {"id": "#minecraft:base_stone_overworld", "amount": 200, "type": "goal"}
}
```

`craft` is another requirement, this time that uses Minecraft's native item-crafted statistics. The ID is always an exact item ID; item tags are not accepted. The amount is the quantity of output items crafted, rather than the number of times a recipe was executed.

```json
{
  "category": "Crafting",
  "title": "The Lidless Eyes",
  "description": "These eyes should seek out the Stronghold for me. I just hope I have enough...",
  "incompleteDescription": "I need a way to find the Stronghold. I wonder if I can reactivate pearls dropped by Endermen...",
  "craft": {
    "id": "minecraft:ender_eye",
    "amount": 12,
    "type": "goal",
    "displayName": "Craft Ender Eyes"
  }
}
```

`item` can be used to require the player to acquire a specific item, rather than specifying the need to acquire it by crafting or mining. All of `item`, `mine`, and `craft` requirements that are given an `amount` must be a positive whole number less than `2147483647`. If no `amount` is specified, the default is `1`. In the case of the following ungated `item` entry, counts are always recorded since nothing is a prerequisite for this entry. Ungated `mine` and `craft` requirements use the player's lifetime vanilla statistics, which will include progress recorded before the entry (or TH itself) was installed. For a `goal` behind one or more locks, progress only begins to count after the prerequisites are completed (see "A Baker's Dozen" example below).

```json
{
  "category": "Farming",
  "title": "A Bountiful Harvest",
  "description": "I have gathered enough wheat to feed a village.",
  "incompleteDescription": "Gather 1000 wheat to feed a village.",
  "item": {
    "id": "minecraft:wheat",
    "amount": 1000,
    "type": "goal",
    "displayName": "Collect wheat"
  }
}
```
An example of a `craft` requirement with no specified `amount`, therefore defaulting to `1`. 

```json
{
  "category": "Crafting",
  "title": "A Better Furnace",
  "description": "I built a blast furnace.",
  "type": "goal",
  "craft": "minecraft:blast_furnace"
}
```

Structure requirements complete when the player visits the specified structure. Structure IDs can use a structure tag such as `#minecraft:village`. These, like every other requirement, can be either `lock` or `goal`.

```json
{
  "category": "Exploration",
  "title": "Delved Too Deep",
  "description": "I should be prepared before I go any further.",
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
  "incompleteDescription": "I swear I saw a massive building in the distance through the trees. I should try and find it.",
  "structure": {
    "id": "minecraft:mansion",
    "type": "goal",
    "displayName": "Find a Woodland Mansion"
  }
}
```

An `entry` requirement refers to another entry by its datapack resource ID. It defaults to `lock`, so it can reveal a follow-up entry after the prerequisite is complete. The pair shown below demonstrates a village visit unlocking a goal to acquire wheat and craft bread.

`exploration_example.json`
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

`farming_example.json`
```json
{
  "category": "Farming",
  "title": "A Baker's Dozen",
  "description": "I've made all that wheat into bread, so now I've got food for a while! Hopefully it doesn't spoil...",
  "incompleteDescription": "The baker in the village showed me how to make my own bread. I should try it out, that way I can get my hands on a consistent food source.",
  "entry": {
    "id": "travellers_handbook:quest_village",
    "type": "lock",
    "displayName": "Find a village"
  },
  "item": {
    "id": "minecraft:wheat",
    "amount": 40,
    "type": "goal",
    "displayName": "Collect wheat"
  },
  "craft": {
    "id": "minecraft:bread",
    "amount": 13,
    "type": "goal",
    "displayName": "Make bread"
  }
}
```

When an entry has mixed requirements, every incomplete `lock` keeps the entry hidden and freezes its goal progress. After the locks are complete, the entry becomes visible and its goals can advance. Entry prerequisites require all requirements on the referenced entry to be complete. 

`adventure_example.json`
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

When a specific order is desired for entry requirements, using a `requirement` array can provide this functionality, where `class` is the class of requirement being referenced, such as `structure`, `id` or `item`.

```json
{
  "category": "Adventure",
  "title": "The Journey to the Nether",
  "description": "Now that I am prepared, there are several milestones ahead for my journey into this hellish landscape.",
  "requirements": [
    {"class":  "entry", "id": "travellers_handbook:adventure_example", "type": "lock", "displayName": "Prepare for the Nether"},
    {"class": "structure", "id": "minecraft:fortress", "type": "goal", "displayName": "Find a Nether fortress"},
    {"class": "advancement", "id": "minecraft:nether/return_to_sender", "type": "goal", "displayName": "Return to sender"},
    {"class": "item", "id": "minecraft:blaze_rod", "amount": 8, "type": "goal", "displayName": "Collect Blaze rods"}
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
