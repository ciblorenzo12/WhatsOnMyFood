package com.ciblorenzo.whatsonmyfood.analysis;

public final class AiSummaryFormatter {

    private AiSummaryFormatter() {
    }

    public static String format(String raw) {
        if (raw == null) return "";

        return raw.trim()
                .replace("**", "")
                .replace("###", "")
                .replace("\r\n", "\n")
                .replace("\n\n", "<br><br>")
                .replace("\n", "<br>")
                .replaceAll("(?i)(^|<br>\\s*)verdict:?\\s*", "$1<b>Verdict</b><br>")
                .replaceAll("(?i)(^|<br>\\s*)quick take:?\\s*", "$1<b>Quick take</b><br>")
                .replaceAll("(?i)(^|<br>\\s*)what stands out:?\\s*", "$1<b>What stands out</b><br>")
                .replaceAll("(?i)(^|<br>\\s*)why it matters:?\\s*", "$1<b>Why it matters</b><br>")
                .replaceAll("(?i)(^|<br>\\s*)product overview:?\\s*", "$1<b>Product overview</b><br>")
                .replaceAll("(?i)(^|<br>\\s*)ingredient notes:?\\s*", "$1<b>Ingredient notes</b><br>")
                .replaceAll("(?i)(^|<br>\\s*)ingredient insights:?\\s*", "$1<b>Ingredient notes</b><br>")
                .replaceAll("(?i)(^|<br>\\s*)nutrition summary:?\\s*", "$1<b>Nutrition summary</b><br>")
                .replaceAll("(?i)(^|<br>\\s*)things to consider:?\\s*", "$1<b>Things to consider</b><br>")
                .replaceAll("(?i)(^|<br>\\s*)potential benefits:?\\s*", "$1<b>Potential benefits</b><br>")
                .replaceAll("(?i)(^|<br>\\s*)bottom line:?\\s*", "$1<b>Bottom line</b><br>")
                .replaceAll("(?i)(^|<br>\\s*)conclusion:?\\s*", "$1<b>Bottom line</b><br>")
                .replaceAll("(?i)(^|<br>\\s*)final summary:?\\s*", "$1<b>Bottom line</b><br>")
                .replaceAll("(?m)(^|<br>)\\s*-\\s+([^<]+)", "$1<ul><li>$2</li></ul>");
    }
}
