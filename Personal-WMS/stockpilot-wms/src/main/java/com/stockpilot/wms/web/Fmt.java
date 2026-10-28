package com.stockpilot.wms.web;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Display helpers for templates, used as ${@fmt.qty(x)}.
 * Formatting only changes how a value looks, never the value itself.
 */
@Component("fmt")
public class Fmt {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US);

    /** 600.0000 -> "600", 2.5000 -> "2.5" */
    public String qty(BigDecimal value) {
        if (value == null) {
            return "";
        }
        BigDecimal stripped = value.stripTrailingZeros();
        return NumberFormat.getNumberInstance(Locale.US).format(stripped.scale() < 0 ? stripped.setScale(0) : stripped);
    }

    /** 12.5 -> "$12.50" */
    public String money(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return NumberFormat.getCurrencyInstance(Locale.US).format(value.setScale(2, RoundingMode.HALF_UP));
    }

    /** True when the value is below zero. Used to highlight negative stock. */
    public boolean neg(BigDecimal value) {
        return value != null && value.signum() < 0;
    }

    /** True when the value is above zero. */
    public boolean pos(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    public String dateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME);
    }

    /** PARTIALLY_RECEIVED -> "Partially received" */
    public String label(Enum<?> value) {
        if (value == null) {
            return "";
        }
        String text = value.name().replace('_', ' ').toLowerCase(Locale.US);
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /** CSS class for a status pill, e.g. "status-partially-received" */
    public String statusClass(Enum<?> value) {
        return value == null ? "" : "status-" + value.name().toLowerCase(Locale.US).replace('_', '-');
    }
}
