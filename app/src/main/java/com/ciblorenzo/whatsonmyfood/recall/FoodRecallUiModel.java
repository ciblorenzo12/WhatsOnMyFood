package com.ciblorenzo.whatsonmyfood.recall;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;

public final class FoodRecallUiModel {
    @StringRes public final int badgeText;
    @StringRes public final int titleText;
    @StringRes public final int messageText;
    @StringRes public final int primaryActionText;
    @ColorRes public final int statusColor;
    public final boolean showProgress;
    public final boolean showPrimaryAction;
    public final boolean showOfficialSource;

    FoodRecallUiModel(
            int badgeText,
            int titleText,
            int messageText,
            int primaryActionText,
            int statusColor,
            boolean showProgress,
            boolean showPrimaryAction,
            boolean showOfficialSource
    ) {
        this.badgeText = badgeText;
        this.titleText = titleText;
        this.messageText = messageText;
        this.primaryActionText = primaryActionText;
        this.statusColor = statusColor;
        this.showProgress = showProgress;
        this.showPrimaryAction = showPrimaryAction;
        this.showOfficialSource = showOfficialSource;
    }
}
