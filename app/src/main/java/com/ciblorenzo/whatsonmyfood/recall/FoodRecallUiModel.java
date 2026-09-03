package com.ciblorenzo.whatsonmyfood.recall;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;

public final class FoodRecallUiModel {
    @StringRes public final int badgeText;
    @StringRes public final int titleText;
    @StringRes public final int messageText;
    @StringRes public final int primaryActionText;
    @StringRes public final int guidanceText;
    @ColorRes public final int statusColor;
    public final boolean showProgress;
    public final boolean showPrimaryAction;
    public final boolean showOfficialSource;
    public final boolean showFallbackPanel;
    public final boolean urgentAlert;

    FoodRecallUiModel(
            int badgeText,
            int titleText,
            int messageText,
            int primaryActionText,
            int guidanceText,
            int statusColor,
            boolean showProgress,
            boolean showPrimaryAction,
            boolean showOfficialSource,
            boolean showFallbackPanel,
            boolean urgentAlert
    ) {
        this.badgeText = badgeText;
        this.titleText = titleText;
        this.messageText = messageText;
        this.primaryActionText = primaryActionText;
        this.guidanceText = guidanceText;
        this.statusColor = statusColor;
        this.showProgress = showProgress;
        this.showPrimaryAction = showPrimaryAction;
        this.showOfficialSource = showOfficialSource;
        this.showFallbackPanel = showFallbackPanel;
        this.urgentAlert = urgentAlert;
    }
}
