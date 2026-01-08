




/pandoraitem give cannon basic [player]/pandoraitem give cannon scattershot [player]/pandoraitem give cannon sniper [player]/pandoraitem give cannon shotgun [player]
FARM Commands:
/pandoraitem give farm sugar-cane [player]/pandoraitem give farm melon [player]/pandoraitem give farm tree [player]/pandoraitem give farm chicken [player]/pandoraitem give farm honey [player]
PERK Commands:
/pandoraitem give perk heal [player]/pandoraitem give perk feed [player]/pandoraitem give perk night [player]/pandoraitem give perk repair [player]
PET Commands:
/pandoraitem give pet horse [player]/pandoraitem give pet dog [player]/pandoraitem give pet cat [player]/pandoraitem give pet wolf [player]/pandoraitem give pet parrot [player]/pandoraitem give pet fox [player]
Note: [player] is optional. If omitted, the command sender receives the item. If you're a player, you can omit it to give yourself the item.

Make the full plugin also 
include ALL items in f shop into crates (shown in pandorabases folder) 
add banknotes (from banknotes folder) 
give levels on every spin (levelplugin folder)
hook into hook notifactions (hook folder)
make perks winnable (perks folder)
make pets winnable (pets folder)
include items and hook into those plugins^


FULL CRATE SYSTEM — WITH YOUR KIT ITEMS INCLUDED
Crate Types You Should Make (based on your kit tiers)

To integrate your kit ecosystem cleanly:

1. Default Crate (Free-to-earn)
2. Imperator Crate ($15 tier loot)
3. Obsidian Crate ($25 tier loot)
4. Pandora Crate ($50 tier loot — highest tier)
5. Monthly Crate (1 per month, highest hype)

Each crate follows the exact same core system — holograms, particles, sounds, rarity rates — the only difference is the item tables.

1. Holograms Above Crates (DH-Style, 3 Lines, Merged)

For each crate:

Pandora Crate Hologram
&e&lPandora Crate ❖
&7Right-click with a &ePandora Key
&7Preview: &e/crate preview pandora


Same format for:

Imperator Crate ❖
Obsidian Crate ❖
Default Crate ❖
Monthly Crate ❖

Your plugin must support:

One hologram object per crate (NOT per line)

Auto-offset (0.8 blocks above crate)

Auto-reload when crate moves

Placeholders like:
%rewards%, %season%, %cratename%

Dev Notes

Store holograms in crateID → hologramObj map

Command: /crate setholo <crate>

String join the 3 lines into one hologram

2. Particles + Sounds + Fireworks (Premium Feel, Not Spam)
On Key Use (opening start):

Sound: ENTITY_PLAYER_LEVELUP

Particles: HAPPY_VILLAGER or PORTAL

Block crack: redstone block crack effect

During CS:GO spin:

Every tick:

Sound: UI_BUTTON_CLICK

Particle burst: CRIT small burst

On Final Reward Reveal:

Firework (color determined by rarity)

Sound: ENTITY_FIREWORK_ROCKET_BLAST

Particle circle

Rarity → Firework Color
Rarity	Color
Common	White
Uncommon	Green
Rare	Blue
Epic	Purple
Legendary	Gold
3. Keys (With NBT, Unstackable, Matching Crates)
Pandora Key

Item: Tripwire Hook

Name: &ePandora Key ✦

Lore:

&7Used to open the &ePandora Crate
&7Right-click on the crate to use


NBT: keyType=pandora

Unstackable

Commands you need:
/crate key give <player> <crate> <amount>
/crate key bundle <crate> <10|25|50>

Optional:

Key shards (10 = 1 key)

Virtual keys for webstore

4. Rarity Rates (Balanced for Factions)

Use this globally unless you explicitly override per-crate.

Common:      50–60%
Uncommon:    20–25%
Rare:        10–12%
Epic:        5–6%
Legendary:   ~2%

Extras:

Pity system: 50 opens guarantees Rare+

Broadcast only Rare/Epic/Legendary

GUI glow on Rare+

5. CRATE REWARD TABLES (WITH YOUR KIT ITEMS)

This is what you actually asked for — integrating your donor kit items into crate rewards.

🎁 DEFAULT CRATE LOOT TABLE

Common

Iron PvP pieces (Prot 1)

Iron tools

Steak (32)

Logs (16)

Arrows (16)

1 Ender Pearl

$2,500

Uncommon

Diamond Sword (Sharp 1)

Bow (Power 1)

Mining Pick (Eff2 Unb2)

5k money voucher

Kit token: Default General Kit

Rare

P2 Iron piece (single item)

Book: Tier 1 Random Enchant

1 Creeper Egg

10k money voucher

Epic

1 Random Imperator Item (Pulled from your Imperator kits)

1 normal Golden Apple

5 Creeper Eggs

Legendary (~2%)

Imperator FULL PvP Kit Voucher

Imperator FULL Mining Kit Voucher

🎁 IMPERATOR CRATE LOOT TABLE

Common

Diamond Sword (Sharp2)

Diamond Helmet (Prot1)

32 Porkchops

2 Speed pots

Uncommon

1 Gap

Bow (Power2)

Random Tier 1 book

Imperator General Kit token

Rare

Obsidian PvP piece (Prot2)

Strength pot (1)

16 Gold Blocks

15k–20k money voucher

Epic

Obsidian FULL Mining Kit voucher
(Eff4 Unb3 Pick, Eff3 Shovel, etc.)

2 Gaps

3 Ender Pearls

Legendary (~2%)

Obsidian FULL PvP Kit Voucher

Obsidian FULL General Kit Voucher

🎁 OBSIDIAN CRATE LOOT TABLE

Common

Diamond Sword (Sharp2 Unb2)

Prot2 Diamond piece

32 Steak

2 Strength pots

Uncommon

Bow (Power3)

2 Books (Tier 1–2)

16 Obsidian

25k voucher

Rare

Pandora-tier single items:

Eff5 Pick

Sharp3 Sword

Prot3 Boots

3 Gaps

3 Pearls

Epic

Pandora FULL Mining Kit voucher

Pandora FULL General Kit voucher

Legendary (~2%)

FULL Pandora PvP Kit Voucher
(Prot3, Sharp3, clean HCF gear)

🎁 PANDORA CRATE LOOT TABLE (Your highest tier)

Common

Prot3 single piece

Sharp3 sword

Eff5/Unb3 pick

64 Steak

Uncommon

Random Tier 2–3 book

3 Pearls

3 Gaps

32 Gold Blocks

Cosmetic Token

Rare

Full Pandora Mining Kit

Full Pandora General Kit

1 Debuff Set

50k voucher

Epic

Pandora PvP Kit voucher

Donor Monthly Kit Items (Sharp3/Prot3 equivalents)

16–24 Creeper Eggs

Legendary (~2%)

Full Pandora Rank Kit

1 Rank Upgrade (Obsidian → Pandora)

1 Monthly Crate Key

🎁 MONTHLY CRATE LOOT TABLE

This crate uses your monthly universal kit + donor monthly kit.

Common

Full Diamond Prot3/Unb2 piece

Eff4 Pick

Uncommon

Creeper Eggs (16)

TNT (64)

16 Diamond Blocks

16 Gold Blocks

Rare

Donor Monthly Kit Items

Eff5 Picks

Sharp3 Swords

24 Creeper Eggs

Epic

FULL Donor Monthly Kit Voucher

FULL Pandora Mining Kit

Legendary (~2%)

Rank Upgrade

1 MONTHLY crate key

Cosmetic bundle

6. FULL SYSTEM FLOW (Player Experience)

✔ Walks up → sees hologram
✔ Particles around crate
✔ Uses key → sound & particles
✔ CS:GO rolling animation
✔ Item slows → stops
✔ Firework (color = rarity)
✔ Reward delivered
✔ Broadcast if Rare+