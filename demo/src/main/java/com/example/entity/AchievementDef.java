package com.example.entity;

/**
 * 成就定义表实体
 */
public class AchievementDef {
    private String id;
    private String name;
    private String description;
    private String category;    // reading/social/collection/hidden
    private String rarity;      // bronze/silver/gold/legendary
    private String conditionJson; // {"type":"total_books","threshold":10}
    private Integer expReward;

    public AchievementDef() {}

    public AchievementDef(String id, String name, String description, String category,
                          String rarity, String conditionJson, Integer expReward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.rarity = rarity;
        this.conditionJson = conditionJson;
        this.expReward = expReward;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getRarity() { return rarity; }
    public void setRarity(String rarity) { this.rarity = rarity; }
    public String getConditionJson() { return conditionJson; }
    public void setConditionJson(String conditionJson) { this.conditionJson = conditionJson; }
    public Integer getExpReward() { return expReward; }
    public void setExpReward(Integer expReward) { this.expReward = expReward; }

    @Override
    public String toString() {
        return "AchievementDef [id=" + id + ", name=" + name + ", category=" + category + ", rarity=" + rarity + "]";
    }
}
