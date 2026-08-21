# Journal Entries

*Entries* are what Traveller's Handbook will display to the user in game through the GUI.

### Creating an entry

Entries have simple formatting with only 4 fields: Category, Title, Description, and optionally an Advancement field.

If an entry is created with a given advancement specified, the entry will be *locked* behind this advancement.

```json
{
  "category": "The group that the entry will go in",
  "title": "The title that the entry will have in the GUI",
  "description": "The text that goes on the right page of the GUI when expanding an entry.",
  "advancement": "example_namespace:example/example_advancement"
}
```

Entries made by the end user can be placed in a datapack with the path `data/travellers-handbook/traveller_entries/your_entry_here.json`

### Default entries

There are a set of default entries shipped with the mod that can be enabled or disabled via config.

If you wish to include these entries, they are enabled by default. If you wish to remove them, simply set `loadDefaultEntries` to `false` in config.

You can also overwrite any entry that you wish in your own datapack if you wish to only change a few from the defaults.
To do so, simply add your modified version of the entry with the same name as the default entry into your datapack to be loaded.