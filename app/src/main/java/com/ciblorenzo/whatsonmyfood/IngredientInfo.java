package com.ciblorenzo.whatsonmyfood;

public class IngredientInfo {
    private String name;
    private String purpose;
    private String healthNote;
    private String category;

    public IngredientInfo(String name, String purpose, String healthNote, String category) {
        this.name = name;
        this.purpose = purpose;
        this.healthNote = healthNote;
        this.category = category;
    }

    public String getName() { return name; }
    public String getPurpose() { return purpose; }
    public String getHealthNote() { return healthNote; }
    public String getCategory() { return category; }
}
