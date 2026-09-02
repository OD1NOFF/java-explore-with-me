package ru.practicum.util;

import java.time.format.DateTimeFormatter;

public final class AppConstants {

    public static final String APP_NAME = "ewm-main-service";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AppConstants() {
    }
}
