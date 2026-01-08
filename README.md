# Minecraft Plugins Collection

This repository contains a suite of custom Minecraft plugins, primarily developed for the **PlayPandora** network. It functions as a monorepo, hosting multiple independent plugin projects that cover various server mechanics, ranging from economy and shops to custom world generation and PvP features.

## Repository Structure

The `plugins/` directory contains the source code for the following projects:

### Core & Utilities
* **Hook**: A central API/dependency plugin likely used to bridge integrations between various other plugins in this suite.
* **PandoraMaster**: The core system plugin for the network, potentially handling hub menus and central management.
* **ChatManager**: Handles chat formatting, administration, and events.
* **Welcome**: Manages player join events, welcome messages, and tips.

### Economy & Shops
* **Banknotes**: Allows players to withdraw currency into physical note items.
* **BuyGUI / SellGUI**: Graphical user interfaces for buying and selling items.
* **CannonShop**: A specialized shop interface, likely for TNT or raiding supplies.
* **FarmShop**: A shop dedicated to farming-related items.
* **PerkShop**: A GUI for purchasing permanent or temporary player perks (e.g., Feed, Heal, Night vision).
* **PandoraCrates**: A custom crate system for rewards.

### Gameplay Enhancements
* **EssHats**: Provides cosmetic hats for players, possibly hooking into Essentials.
* **EssentialsGUI**: A GUI wrapper for common Essentials commands (Warps, Kits, Homes).
* **HomeBuffs**: Grants buffs to players when they are in their home region.
* **LevelPlugin / LevelEnchant**: A custom progression system that likely integrates with enchanting mechanics.
* **NoNerfs**: Removes certain vanilla nerfs or restrictions (e.g., explosion damage or potion effects).
* **PetPlugin**: A system for purchasing and managing cosmetic or functional pets.
* **TotemLimit**: Restricts the usage or cooldown of Totems of Undying.
* **CraftAccess**: Controls or adds permissions to specific crafting recipes or anvils.

### World & Environment
* **MoreCaves / MoreEnd / MoreNether**: Enhances vanilla dimensions with custom mechanics, spawning rules, or generation features.
* **MoreMobs**: Customizes mob attributes, drops, and spawn logic.
* **MoreWeather**: Introduces custom weather events and environmental effects.
* **PandoraCavern**: A specific cavern-related gameplay feature or world generator.
* **PandoraWorldLock**: Manages world access, locking specific worlds behind permissions or states.

### PvP & Competitions
* **PandoraOnevsOne**: A 1v1 dueling system with arenas and kits.
* **PandoraBanners**: Custom banner management, possibly for teams or factions.
* **PandoraEvents**: Manages automated or admin-triggered server events.
* **TreasureHunt**: A scavenger hunt event plugin.

### Special Mechanics
* **PandoraSpawners**: A comprehensive custom spawner system (stacking, upgrading, etc.).
* **PandoraEnchants**: A custom enchantment system with a GUI editor and builder.
* **PandoraMiner**: Likely adds custom mining mechanics or automated miners.
* **PandoraItems**: Manages custom items with special abilities or attributes.
* **PandoraCeggs**: Custom "Ceggs" (likely Custom Eggs or Creeper Eggs) mechanics.

## Build Instructions

This repository allows you to build all plugins simultaneously or individually.

### Prerequisites
* **Java Development Kit (JDK)**: Version 21 (recommended for 1.21+ compatibility) or at least JDK 17.
* **Maven**: Used for dependency management and building the Java projects.
