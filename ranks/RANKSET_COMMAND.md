# RankSet Command Documentation

## Overview
The `/rankset` command is a user-friendly way to set player ranks in SlimeRanks. It integrates with LuckPerms to manage rank groups and permissions.

## Usage
```
/rankset <player> <rankname>
```

## Available Ranks
- `pandora` - Top rank (Yellow)
- `obsidian` - Rank 2 (Dark Purple)
- `imperator` - Rank 3 (Gold)
- `default` - Default/Player rank (Gray)

## Aliases
- `/rankset`
- `/setrank`
- `/rank`

## Permissions
- `slimeranks.admin` - Required to use this command

## Features

### Setting a Rank
When you set a player to a rank:
1. Removes all existing rank groups
2. Sets the new rank as primary group in LuckPerms
3. Adds the rank group inheritance node
4. Reloads displays for all online players
5. Notifies the target player if online
6. Triggers rank unlock listener for auto-unlocking perks/pets (if applicable)

### Setting to Default
When setting a player to `default` rank:
1. **Removes all rank groups** (group.pandora, group.obsidian, group.imperator)
2. **Clears all rank-related permissions**:
   - `group.pandora.*`
   - `group.obsidian.*`
   - `group.imperator.*`
   - `perkshop.*` (perk permissions)
   - `slimeranks.rank.*` (rank permissions)
3. **Revokes rank benefits** (logs perk count for reference)
4. **Notifies the player** if online
5. **Reloads displays**

### Edge Cases Handled
- ✅ Player already has the rank (shows friendly message)
- ✅ Player not found (shows error with helpful message)
- ✅ Invalid rank name (shows available ranks)
- ✅ Offline players (works with UUID lookup)
- ✅ LuckPerms not available (shows error)
- ✅ Async operations (properly handles LuckPerms async API)

## Examples

### Set player to Pandora rank
```
/rankset PlayerName pandora
```
Output: `&e&lPandora &8» &aSet &7PlayerName &ato &e&lPandora &arank!`

### Set player to Default rank
```
/rankset PlayerName default
```
Output: 
```
&e&lPandora &8» &aSet &7PlayerName &ato &7Default &arank!
&7All rank-related permissions, perks, and pets have been cleared.
```

### Set player who already has the rank
```
/rankset PlayerName pandora
```
Output: `&e&lPandora &8» &7PlayerName &7already has the &e&lPandora &7rank!`

## Tab Completion
- First argument: Tab completes online player names
- Second argument: Tab completes rank names (pandora, obsidian, imperator, default)

## Integration with RankUnlockListener
When a player is set to Pandora, Obsidian, or Imperator:
- Automatically unlocks all perks (if applicable)
- Automatically unlocks all pets (if applicable for Pandora/Obsidian)
- Distributes monthly keys (if 30 days have passed)
- Grants all necessary permissions

## Technical Details

### LuckPerms Integration
- Uses `User.setPrimaryGroup()` to set primary group
- Uses `Node.builder("group.rankname").build()` for inheritance nodes
- Properly removes old rank groups before setting new one
- Uses `NodeType.INHERITANCE` for proper node removal

### Permission Cleanup
When setting to default, the following permissions are removed:
- All rank group nodes (`group.pandora`, `group.obsidian`, `group.imperator`)
- All perkshop permissions (`perkshop.*`)
- All slimeranks rank permissions (`slimeranks.rank.*`)

### Display Updates
- Automatically reloads displays after rank change
- Updates tab list, chat format, and name tags
- Works for both online and offline players

## Error Messages

### Player Not Found
```
&cPlayer &7PlayerName &cnot found!
&7Make sure the player name is spelled correctly.
```

### Invalid Rank
```
&cInvalid rank! Available ranks: &epandora&7, &5obsidian&7, &6imperator&7, &7default
```

### LuckPerms Not Available
```
&cLuckPerms is not available! Please install LuckPerms to use this command.
```

## Notes
- The command works with both online and offline players
- All operations are async-safe
- Proper error handling and logging
- User-friendly messages with Pandora styling
- No data is deleted, only permissions are modified






