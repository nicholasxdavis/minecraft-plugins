package com.playpandora.petplugin.storage;

import com.playpandora.petplugin.PetPlugin;
import com.playpandora.petplugin.models.Pet;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * DataManager handles all pet data persistence.
 * 
 * Data Storage:
 * - All pet data is saved to: plugins/PetPlugin/data.yml
 * - Auto-saves every 30 seconds
 * - Saves immediately on important operations (add, remove, rename, revive, death)
 * - Final save on plugin disable
 * 
 * Saved Data Per Pet:
 * - Pet type (horse, dog, cat, wolf, parrot, fox)
 * - Generated name (unique identifier)
 * - Custom name (player-given name, optional)
 * - Max health
 * - Current health
 * - Death timestamp (if dead, null if alive)
 * 
 * Data Structure:
 * pets:
 *   <player-uuid>:
 *     pet0:
 *       type: cat
 *       generatedName: Fluffy
 *       customName: MyCat
 *       maxHealth: 20.0
 *       currentHealth: 18.5
 *       deathTimestamp: null
 *     pet1:
 *       ...
 */
public class DataManager {
    
    private final PetPlugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, List<Pet>> playerPets = new HashMap<>();
    private final Map<UUID, List<Pet>> deadPets = new HashMap<>(); // Player UUID -> Dead Pets
    private BukkitTask autoSaveTask;
    private boolean needsSave = false;
    
    public DataManager(PetPlugin plugin) {
        this.plugin = plugin;
        loadData();
        startAutoSave();
    }
    
    public void loadData() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create data.yml: " + e.getMessage());
                return;
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        // Load player pets
        if (dataConfig.contains("pets")) {
            for (String uuidString : dataConfig.getConfigurationSection("pets").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    List<Pet> pets = new ArrayList<>();
                    
                    for (String petKey : dataConfig.getConfigurationSection("pets." + uuidString).getKeys(false)) {
                        String path = "pets." + uuidString + "." + petKey;
                        String petType = dataConfig.getString(path + ".type");
                        String generatedName = dataConfig.getString(path + ".generatedName");
                        String customName = dataConfig.getString(path + ".customName");
                        Long deathTimestamp = dataConfig.contains(path + ".deathTimestamp") ? 
                            dataConfig.getLong(path + ".deathTimestamp") : null;
                        Double maxHealth = dataConfig.contains(path + ".maxHealth") ?
                            dataConfig.getDouble(path + ".maxHealth") : null;
                        Double currentHealth = dataConfig.contains(path + ".currentHealth") ?
                            dataConfig.getDouble(path + ".currentHealth") : null;
                        
                        Pet pet = new Pet(uuid, petType, generatedName);
                        if (customName != null) {
                            pet.setCustomName(customName);
                        }
                        if (maxHealth != null) {
                            pet.setMaxHealth(maxHealth);
                        }
                        if (currentHealth != null) {
                            pet.setCurrentHealth(currentHealth);
                        }
                        if (deathTimestamp != null) {
                            pet.setDeathTimestamp(deathTimestamp);
                            deadPets.computeIfAbsent(uuid, k -> new ArrayList<>()).add(pet);
                        } else {
                            pets.add(pet);
                        }
                    }
                    
                    playerPets.put(uuid, pets);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in data.yml: " + uuidString);
                }
            }
        }
        
        int totalPets = playerPets.values().stream().mapToInt(List::size).sum();
        int totalDeadPets = deadPets.values().stream().mapToInt(List::size).sum();
        plugin.getLogger().info("Loaded data for " + playerPets.size() + " players with " + 
            totalPets + " alive pets and " + totalDeadPets + " dead pets");
    }
    
    private void startAutoSave() {
        // Auto-save every 30 seconds (600 ticks) for better data persistence
        autoSaveTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (needsSave) {
                saveDataSync();
                needsSave = false;
            }
        }, 600L, 600L); // Every 30 seconds
        
        plugin.getLogger().info("Auto-save enabled (every 30 seconds)");
    }
    
    public void saveData() {
        // Mark as needing save and trigger immediate save
        needsSave = true;
        saveDataSync();
    }
    
    private void saveDataSync() {
        // Ensure we're on the main thread for file operations
        if (!plugin.getServer().isPrimaryThread()) {
            plugin.getServer().getScheduler().runTask(plugin, this::saveDataSync);
            return;
        }
        
        if (dataConfig == null || dataFile == null) {
            plugin.getLogger().warning("Cannot save data: dataConfig or dataFile is null!");
            return;
        }
        
        try {
            // Ensure data directory exists
            if (!dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }
            
            // Clear existing data
            dataConfig.set("pets", null);
            
            int totalPetsSaved = 0;
            
            // Save player pets (alive and dead)
            for (Map.Entry<UUID, List<Pet>> entry : playerPets.entrySet()) {
                UUID uuid = entry.getKey();
                List<Pet> pets = entry.getValue();
                
                if (pets == null || pets.isEmpty()) {
                    continue;
                }
                
                int index = 0;
                for (Pet pet : pets) {
                    if (pet == null) {
                        continue;
                    }
                    
                    String path = "pets." + uuid.toString() + ".pet" + index;
                    dataConfig.set(path + ".type", pet.getPetType());
                    dataConfig.set(path + ".generatedName", pet.getGeneratedName());
                    if (pet.getCustomName() != null && !pet.getCustomName().isEmpty()) {
                        dataConfig.set(path + ".customName", pet.getCustomName());
                    } else {
                        dataConfig.set(path + ".customName", null);
                    }
                    dataConfig.set(path + ".maxHealth", pet.getMaxHealth());
                    dataConfig.set(path + ".currentHealth", pet.getCurrentHealth());
                    if (pet.getDeathTimestamp() != null) {
                        dataConfig.set(path + ".deathTimestamp", pet.getDeathTimestamp());
                    } else {
                        dataConfig.set(path + ".deathTimestamp", null);
                    }
                    index++;
                    totalPetsSaved++;
                }
            }
            
            // Save dead pets
            for (Map.Entry<UUID, List<Pet>> entry : deadPets.entrySet()) {
                UUID uuid = entry.getKey();
                List<Pet> pets = entry.getValue();
                
                if (pets == null || pets.isEmpty()) {
                    continue;
                }
                
                int startIndex = playerPets.getOrDefault(uuid, new ArrayList<>()).size();
                for (int i = 0; i < pets.size(); i++) {
                    Pet pet = pets.get(i);
                    if (pet == null) {
                        continue;
                    }
                    
                    String path = "pets." + uuid.toString() + ".pet" + (startIndex + i);
                    dataConfig.set(path + ".type", pet.getPetType());
                    dataConfig.set(path + ".generatedName", pet.getGeneratedName());
                    if (pet.getCustomName() != null && !pet.getCustomName().isEmpty()) {
                        dataConfig.set(path + ".customName", pet.getCustomName());
                    } else {
                        dataConfig.set(path + ".customName", null);
                    }
                    dataConfig.set(path + ".maxHealth", pet.getMaxHealth());
                    dataConfig.set(path + ".currentHealth", pet.getCurrentHealth());
                    if (pet.getDeathTimestamp() != null) {
                        dataConfig.set(path + ".deathTimestamp", pet.getDeathTimestamp());
                    } else {
                        dataConfig.set(path + ".deathTimestamp", null);
                    }
                    totalPetsSaved++;
                }
            }
            
            // Save to file
            dataConfig.save(dataFile);
            plugin.getLogger().info("Data saved successfully: " + totalPetsSaved + " pets for " + 
                (playerPets.size() + deadPets.size()) + " players");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save data.yml: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            plugin.getLogger().severe("Unexpected error while saving data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void addPet(UUID uuid, Pet pet) {
        playerPets.computeIfAbsent(uuid, k -> new ArrayList<>()).add(pet);
        needsSave = true;
        saveData(); // Immediate save for important operations
    }
    
    public List<Pet> getPlayerPets(UUID uuid) {
        return playerPets.getOrDefault(uuid, new ArrayList<>());
    }
    
    public boolean hasPetType(UUID uuid, String petType) {
        // Check if player has permission-based unlock (bypasses purchase/level requirements)
        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(uuid);
        if (player != null) {
            // Check for specific pet permission
            if (player.hasPermission("petplugin.pet." + petType)) {
                return true;
            }
            // Check for all pets permission
            if (player.hasPermission("petplugin.pet.all")) {
                return true;
            }
        }
        // Fall back to normal ownership check
        List<Pet> pets = getPlayerPets(uuid);
        return pets.stream().anyMatch(pet -> pet.getPetType().equalsIgnoreCase(petType));
    }
    
    public Pet getPetByGeneratedName(UUID uuid, String generatedName) {
        List<Pet> pets = getPlayerPets(uuid);
        return pets.stream()
                .filter(pet -> pet.getGeneratedName().equalsIgnoreCase(generatedName))
                .findFirst()
                .orElse(null);
    }
    
    public void removePet(UUID uuid, Pet pet) {
        List<Pet> pets = getPlayerPets(uuid);
        pets.remove(pet);
        
        // Also remove from dead pets if it's there
        List<Pet> dead = deadPets.get(uuid);
        if (dead != null) {
            dead.remove(pet);
            if (dead.isEmpty()) {
                deadPets.remove(uuid);
            }
        }
        
        if (pets.isEmpty() && (dead == null || dead.isEmpty())) {
            playerPets.remove(uuid);
            deadPets.remove(uuid);
        }
        needsSave = true;
        saveData(); // Immediate save for important operations
    }
    
    public void markPetAsDead(UUID uuid, Pet pet) {
        // Remove from alive pets
        List<Pet> pets = getPlayerPets(uuid);
        pets.remove(pet);
        
        // Add to dead pets with timestamp
        pet.setDeathTimestamp(System.currentTimeMillis());
        deadPets.computeIfAbsent(uuid, k -> new ArrayList<>()).add(pet);
        
        needsSave = true;
        saveData(); // Immediate save for important operations
    }
    
    public List<Pet> getDeadPets(UUID uuid) {
        return deadPets.getOrDefault(uuid, new ArrayList<>());
    }
    
    public Pet getDeadPetByName(UUID uuid, String petName) {
        List<Pet> dead = getDeadPets(uuid);
        return dead.stream()
                .filter(pet -> pet.getDisplayName().equalsIgnoreCase(petName) || 
                             pet.getGeneratedName().equalsIgnoreCase(petName))
                .findFirst()
                .orElse(null);
    }
    
    public void revivePet(UUID uuid, Pet pet) {
        // Remove from dead pets
        List<Pet> dead = deadPets.get(uuid);
        if (dead != null) {
            dead.remove(pet);
            if (dead.isEmpty()) {
                deadPets.remove(uuid);
            }
        }
        
        // Revive and add back to alive pets
        pet.revive();
        playerPets.computeIfAbsent(uuid, k -> new ArrayList<>()).add(pet);
        
        needsSave = true;
        saveData(); // Immediate save for important operations
    }
    
    public void updatePetHealth(UUID uuid, Pet pet) {
        // Update pet health in memory (will be saved on next auto-save or explicit save)
        needsSave = true;
    }
    
    /**
     * Update pet data (for name changes, etc.)
     * This triggers an immediate save to ensure data persistence
     */
    public void updatePet(UUID uuid, Pet pet) {
        // Pet is already in the list, just mark for save
        needsSave = true;
        saveData(); // Immediate save for important updates
    }
    
    public void close() {
        // Cancel auto-save task
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        
        // Force final save of all data (even if needsSave is false)
        needsSave = true;
        saveDataSync();
        plugin.getLogger().info("All data saved on plugin disable");
    }
    
    /**
     * Get statistics about saved data
     * @return String with data statistics
     */
    public String getDataStatistics() {
        int totalPlayers = playerPets.size() + deadPets.size();
        int totalAlivePets = playerPets.values().stream().mapToInt(List::size).sum();
        int totalDeadPets = deadPets.values().stream().mapToInt(List::size).sum();
        return String.format("Players: %d | Alive Pets: %d | Dead Pets: %d | Total: %d", 
            totalPlayers, totalAlivePets, totalDeadPets, totalAlivePets + totalDeadPets);
    }
    
    /**
     * Get the data file path
     * @return Path to data.yml file
     */
    public String getDataFilePath() {
        return dataFile != null ? dataFile.getAbsolutePath() : "Not initialized";
    }
}

