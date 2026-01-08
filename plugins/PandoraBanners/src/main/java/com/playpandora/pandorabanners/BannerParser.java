package com.playpandora.pandorabanners;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BannerParser {
    
    private final File bannerFile;
    private final Map<String, String[]> banners;
    
    public BannerParser(String filePath) {
        this.bannerFile = new File(filePath);
        this.banners = new HashMap<>();
    }
    
    public boolean loadBanners() {
        if (!bannerFile.exists()) {
            return false;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(bannerFile))) {
            String line;
            String currentBanner = null;
            StringBuilder bannerContent = new StringBuilder();
            
            while ((line = reader.readLine()) != null) {
                // Check if this is a banner header (## DIRECTION)
                if (line.startsWith("## ")) {
                    // Save previous banner if exists
                    if (currentBanner != null && bannerContent.length() > 0) {
                        banners.put(currentBanner, parseBannerContent(bannerContent.toString()));
                    }
                    
                    // Start new banner
                    currentBanner = line.substring(3).trim().toUpperCase();
                    bannerContent = new StringBuilder();
                } else if (currentBanner != null && !line.trim().isEmpty() && !line.trim().startsWith("#")) {
                    // Extract ASCII art from line, removing gradient tags
                    String cleanLine = line.trim();
                    
                    // Remove opening gradient tag
                    if (cleanLine.contains("<gradient:")) {
                        int startIdx = cleanLine.indexOf(">");
                        if (startIdx != -1) {
                            cleanLine = cleanLine.substring(startIdx + 1);
                        } else {
                            cleanLine = cleanLine.replaceAll("<gradient:[^>]*>", "");
                        }
                    }
                    
                    // Remove closing gradient tag
                    cleanLine = cleanLine.replaceAll("</gradient>", "").trim();
                    
                    if (!cleanLine.isEmpty()) {
                        bannerContent.append(cleanLine).append("\n");
                    }
                }
            }
            
            // Save last banner
            if (currentBanner != null && bannerContent.length() > 0) {
                banners.put(currentBanner, parseBannerContent(bannerContent.toString()));
            }
            
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private String[] parseBannerContent(String content) {
        // Split by newlines and filter out empty lines
        String[] lines = content.trim().split("\n");
        
        // Clean up and format lines for better Minecraft display
        List<String> cleanedLines = new ArrayList<>();
        for (String line : lines) {
            String cleaned = cleanLine(line);
            if (!cleaned.isEmpty()) {
                cleanedLines.add(cleaned);
            }
        }
        
        return cleanedLines.toArray(new String[0]);
    }
    
    private String cleanLine(String line) {
        // Remove leading/trailing whitespace but preserve internal spacing
        // Don't normalize spaces as it will break ASCII art formatting
        line = line.trim();
        
        // Keep all characters as-is for proper ASCII art display
        // Box-drawing characters and spacing are important for the art
        return line;
    }
    
    public String[] getBanner(String direction) {
        return banners.get(direction.toUpperCase());
    }
    
    public boolean hasBanner(String direction) {
        return banners.containsKey(direction.toUpperCase());
    }
    
    public Map<String, String[]> getAllBanners() {
        return new HashMap<>(banners);
    }
}

