package com.timeplace.backend.util;

public final class TextUtils {

    private TextUtils() {
    }

    /** Wikimedia/Europeana metadata often embeds HTML (links, <span> wrappers); keep plain text only. */
    public static String stripHtml(String value) {
        if (value == null) {
            return null;
        }
        String withoutTags = value.replaceAll("<[^>]*>", " ");
        String collapsed = withoutTags.replaceAll("\\s+", " ").trim();
        return collapsed.isBlank() ? null : collapsed;
    }
}
