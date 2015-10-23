package org.ecocean.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtils {
    private DateUtils() {
        // prevent instantiation
    }

    public static LocalDateTime dateToLDT(final Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public static long ldtToMillis(final LocalDateTime ldt) {
        return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * Shouldn't use this I would think but just in case you find that you absolutely need it
     * here it is.
     */
    public static Date ldtToDate(final LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }


    public static LocalDateTime epochMilliSecToLDT(final long epochMilliSecond) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochMilliSecond/1000), ZoneId.systemDefault());
    }

    /**
     * @param epochMilliSecond milliseconds since the epoch
     * @return date as string (e.g. "1986-04-08 12:30")
     */
    public static String epochMilliSecToString(final long epochMilliSecond) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return epochMilliSecToLDT(epochMilliSecond).format(formatter);
    }

//    public static long nowEpochSec() {
//        return LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
//    }

    public static String format(final LocalDate date, final OffsetTime start, final OffsetTime end) {
        if (date == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(date.toString());

        if (start != null) {
            builder.append(" ").append(start.toString());
        }

        if (end != null) {
            builder.append("-").append(end.toString());
        }

        return builder.toString();
    }
}
