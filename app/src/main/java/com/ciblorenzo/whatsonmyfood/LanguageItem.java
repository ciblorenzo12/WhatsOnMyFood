package com.ciblorenzo.whatsonmyfood;

public class LanguageItem {
    private final String languageName;
    private final String languageCode;
    private final int flagImage;

    public LanguageItem(String languageName, int flagImage) {
        this(languageName, languageName.toLowerCase(), flagImage);
    }

    public LanguageItem(String languageName, String languageCode, int flagImage) {
        this.languageName = languageName;
        this.languageCode = languageCode;
        this.flagImage = flagImage;
    }

    public String getLanguageName() {
        return languageName;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public int getFlagImage() {
        return flagImage;
    }
}
