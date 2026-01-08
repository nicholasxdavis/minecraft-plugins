package com.playpandora.welcome.data;

import java.util.*;

public class PlayerData {
    
    private UUID uuid;
    private String name;
    private long firstJoin;
    private long lastSeen;
    private boolean hasReceivedStarterItems;
    private boolean hasReceivedBooklet;
    private List<String> pets;
    private Map<String, Object> customData;
    
    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.firstJoin = System.currentTimeMillis();
        this.lastSeen = System.currentTimeMillis();
        this.hasReceivedStarterItems = false;
        this.hasReceivedBooklet = false;
        this.pets = new ArrayList<>();
        this.customData = new HashMap<>();
    }
    
    public PlayerData() {
        this.pets = new ArrayList<>();
        this.customData = new HashMap<>();
    }
    
    // Getters and Setters
    public UUID getUuid() {
        return uuid;
    }
    
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public long getFirstJoin() {
        return firstJoin;
    }
    
    public void setFirstJoin(long firstJoin) {
        this.firstJoin = firstJoin;
    }
    
    public long getLastSeen() {
        return lastSeen;
    }
    
    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    public boolean hasReceivedStarterItems() {
        return hasReceivedStarterItems;
    }
    
    public void setHasReceivedStarterItems(boolean hasReceivedStarterItems) {
        this.hasReceivedStarterItems = hasReceivedStarterItems;
    }
    
    public boolean hasReceivedBooklet() {
        return hasReceivedBooklet;
    }
    
    public void setHasReceivedBooklet(boolean hasReceivedBooklet) {
        this.hasReceivedBooklet = hasReceivedBooklet;
    }
    
    public List<String> getPets() {
        return pets;
    }
    
    public void setPets(List<String> pets) {
        this.pets = pets != null ? pets : new ArrayList<>();
    }
    
    public void addPet(String pet) {
        if (pet != null && !pets.contains(pet)) {
            pets.add(pet);
        }
    }
    
    public void removePet(String pet) {
        pets.remove(pet);
    }
    
    public Map<String, Object> getCustomData() {
        return customData;
    }
    
    public void setCustomData(Map<String, Object> customData) {
        this.customData = customData != null ? customData : new HashMap<>();
    }
    
    public void setCustomData(String key, Object value) {
        if (value == null) {
            customData.remove(key);
        } else {
            customData.put(key, value);
        }
    }
    
    public Object getCustomData(String key) {
        return customData.get(key);
    }
}

