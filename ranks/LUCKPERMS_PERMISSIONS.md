# LuckPerms Permissions for Pandora Ranks

## Overview
This document lists all LuckPerms permissions that should be assigned to each rank group.

## Rank Groups
- `group.pandora` - Top rank (Yellow)
- `group.obsidian` - Rank 2 (Dark Purple)
- `group.imperator` - Rank 3 (Gold)
- `group.default` - Default/Player rank (Gray)

## Essentials Permissions

### All Paid Ranks (Pandora, Obsidian, Imperator)
These permissions should be granted to all three paid ranks:

```
essentials.fly
essentials.fly.*
essentials.feed
essentials.feed.*
essentials.heal
essentials.heal.*
essentials.hat
essentials.nick
essentials.nick.*
essentials.itemname
essentials.itemname.*
```

### Per-Rank Permissions

#### Pandora (group.pandora)
```
# Essentials
essentials.fly
essentials.fly.*
essentials.feed
essentials.feed.*
essentials.heal
essentials.heal.*
essentials.hat
essentials.nick
essentials.nick.*
essentials.itemname
essentials.itemname.*

# PerkShop (auto-unlocked via RankUnlockListener)
perkshop.heal
perkshop.feed
perkshop.night
perkshop.repair

# SlimeRanks
slimeranks.rank.pandora
```

#### Obsidian (group.obsidian)
```
# Essentials
essentials.fly
essentials.fly.*
essentials.feed
essentials.feed.*
essentials.heal
essentials.heal.*
essentials.hat
essentials.nick
essentials.nick.*
essentials.itemname
essentials.itemname.*

# PerkShop (auto-unlocked via RankUnlockListener)
perkshop.heal
perkshop.feed
perkshop.night
perkshop.repair

# SlimeRanks
slimeranks.rank.obsidian
```

#### Imperator (group.imperator)
```
# Essentials
essentials.fly
essentials.fly.*
essentials.feed
essentials.feed.*
essentials.heal
essentials.heal.*
essentials.hat
essentials.nick
essentials.nick.*
essentials.itemname
essentials.itemname.*

# PerkShop (auto-unlocked via RankUnlockListener)
perkshop.heal
perkshop.feed
perkshop.night
perkshop.repair

# SlimeRanks
slimeranks.rank.imperator
```

#### Default (group.default)
```
# SlimeRanks (no permission needed - default rank)
# No special permissions
```

## Monthly Key Distribution
Monthly keys are automatically distributed via the `RankUnlockListener`:
- Pandora: 1 of each crate key (default, imperator, obsidian, pandora) every 30 days
- Obsidian: 1 of each crate key (default, imperator, obsidian, pandora) every 30 days
- Imperator: 1 of each crate key (default, imperator, obsidian, pandora) every 30 days

## Auto-Unlock Features

### Pandora Rank
- ✅ All perks unlocked automatically (heal, feed, night, repair)
- ✅ All pets unlocked automatically (horse, dog, cat, wolf, parrot, fox)
- ✅ Receives kit cannon (1 of each: basic, scattershot, sniper, shotgun)
- ✅ Receives 5 creeper eggs
- ✅ Monthly crate keys (1 of each)

### Obsidian Rank
- ✅ All perks unlocked automatically (heal, feed, night, repair)
- ✅ All pets unlocked automatically (horse, dog, cat, wolf, parrot, fox)
- ✅ Receives kit cannon (1 of each: basic, scattershot, sniper, shotgun)
- ✅ Monthly crate keys (1 of each)

### Imperator Rank
- ✅ All perks unlocked automatically (heal, feed, night, repair)
- ✅ Monthly crate keys (1 of each)

## Implementation Notes

1. The `RankUnlockListener` automatically handles perk and pet unlocking when players get promoted to these ranks
2. Monthly keys are checked on player join and distributed if 30 days have passed
3. Essentials permissions should be set up in LuckPerms groups
4. The listener uses LuckPerms events to detect rank changes






