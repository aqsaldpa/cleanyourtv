package com.gathin.tvhome;

import android.icu.util.Calendar;
import android.icu.util.IslamicCalendar;
import android.icu.util.ULocale;

import java.util.Locale;
import java.util.TimeZone;

/**
 * The five daily prayer times for one place, computed on the TV from the sun's position: no network.
 * Defaults follow Kemenag RI (Indonesia): Fajr at 20 degrees, Isha at 18 degrees, Asr when a shadow equals
 * its object plus noon shadow (Shafi'i), and two minutes of ihtiyat added to every time, rounded up.
 *
 * config.json: "prayer_times": true, "prayer_lat": -6.2, "prayer_lon": 106.8, optional "prayer_fajr_angle",
 * "prayer_isha_angle", "prayer_ihtiyat_minutes" and "prayer_hijri" (Hijri date at the start of the line).
 */
final class PrayerTimes {
    static final String[] NAMES_ID = {"Subuh", "Zuhur", "Asar", "Magrib", "Isya"};

    final double lat;
    final double lon;
    final double fajrAngle;
    final double ishaAngle;
    final int ihtiyat;
    final boolean hijri;

    PrayerTimes(double lat, double lon, double fajrAngle, double ishaAngle, int ihtiyat, boolean hijri) {
        this.lat = lat;
        this.lon = lon;
        this.fajrAngle = fajrAngle;
        this.ishaAngle = ishaAngle;
        this.ihtiyat = ihtiyat;
        this.hijri = hijri;
    }

    /** Minutes after local midnight for Subuh, Zuhur, Asar, Magrib and Isya on the given local date. */
    int[] minutes(java.util.Calendar day) {
        int y = day.get(java.util.Calendar.YEAR);
        int m = day.get(java.util.Calendar.MONTH) + 1;
        int d = day.get(java.util.Calendar.DAY_OF_MONTH);
        double tz = TimeZone.getDefault().getOffset(day.getTimeInMillis()) / 3_600_000.0;

        double jd = julianDay(y, m, d) - lon / 360.0;
        double n = jd + 0.5 - 2451545.0;
        double g = rad(fix(357.529 + 0.98560028 * n, 360));
        double q = fix(280.459 + 0.98564736 * n, 360);
        double l = rad(fix(q + 1.915 * Math.sin(g) + 0.020 * Math.sin(2 * g), 360));
        double e = rad(23.439 - 0.00000036 * n);
        double ra = Math.toDegrees(Math.atan2(Math.cos(e) * Math.sin(l), Math.cos(l))) / 15.0;
        double decl = Math.asin(Math.sin(e) * Math.sin(l));
        double eqt = q / 15.0 - fix(ra, 24);

        double noon = fix(12 + tz - lon / 15.0 - eqt, 24);
        double phi = rad(lat);
        double asrAlt = Math.atan(1.0 / (1 + Math.tan(Math.abs(phi - decl))));

        double[] hours = {
                noon - hourAngle(-rad(fajrAngle), phi, decl),
                noon,
                noon + hourAngle(asrAlt, phi, decl),
                noon + hourAngle(-rad(0.833), phi, decl),
                noon + hourAngle(-rad(ishaAngle), phi, decl),
        };
        int[] out = new int[5];
        for (int i = 0; i < 5; i++) out[i] = (int) Math.ceil(hours[i] * 60 + ihtiyat);
        return out;
    }

    /** "Asar 15:01 · 1 j 20 mnt lagi  ·  Subuh 04:12  Zuhur 11:45 ...": the next prayer first, then the day. */
    String line(java.util.Calendar now) {
        int[] t = minutes(now);
        int nowMin = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE);
        int next = -1;
        for (int i = 0; i < 5; i++) {
            if (t[i] >= 0 && t[i] > nowMin) {
                next = i;
                break;
            }
        }
        int nextAt;
        int nextIndex;
        if (next >= 0) {
            nextIndex = next;
            nextAt = t[next];
        } else {
            java.util.Calendar tomorrow = (java.util.Calendar) now.clone();
            tomorrow.add(java.util.Calendar.DAY_OF_MONTH, 1);
            nextIndex = 0;
            nextAt = minutes(tomorrow)[0] + 24 * 60;
        }
        StringBuilder sb = new StringBuilder();
        if (hijri) sb.append(hijriDate(now)).append("  ·  ");
        sb.append(NAMES_ID[nextIndex]).append(' ').append(clock(nextAt % (24 * 60)))
                .append(" (").append(until(nextAt - nowMin)).append(')');
        sb.append("  ·  ");
        for (int i = 0; i < 5; i++) {
            if (i > 0) sb.append("   ");
            sb.append(NAMES_ID[i]).append(' ').append(clock(t[i]));
        }
        return sb.toString();
    }

    static String hijriDate(java.util.Calendar now) {
        IslamicCalendar cal = new IslamicCalendar(ULocale.forLocale(new Locale("id")));
        cal.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
        cal.setTimeInMillis(now.getTimeInMillis());
        String[] months = {"Muharram", "Safar", "Rabiul Awal", "Rabiul Akhir", "Jumadil Awal", "Jumadil Akhir",
                "Rajab", "Syaban", "Ramadan", "Syawal", "Zulkaidah", "Zulhijah"};
        return cal.get(Calendar.DAY_OF_MONTH) + " " + months[cal.get(Calendar.MONTH)] + " " + cal.get(Calendar.YEAR) + " H";
    }

    private static String until(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        if (h == 0) return m + " mnt lagi";
        if (m == 0) return h + " j lagi";
        return h + " j " + m + " mnt lagi";
    }

    private static String clock(int minutes) {
        int mm = ((minutes % (24 * 60)) + 24 * 60) % (24 * 60);
        return String.format(Locale.ROOT, "%02d:%02d", mm / 60, mm % 60);
    }

    private static double hourAngle(double altitude, double phi, double decl) {
        double c = (Math.sin(altitude) - Math.sin(phi) * Math.sin(decl)) / (Math.cos(phi) * Math.cos(decl));
        c = Math.max(-1, Math.min(1, c));
        return Math.toDegrees(Math.acos(c)) / 15.0;
    }

    private static double julianDay(int y, int m, int d) {
        if (m <= 2) {
            y -= 1;
            m += 12;
        }
        double a = Math.floor(y / 100.0);
        double b = 2 - a + Math.floor(a / 4);
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + b - 1524.5;
    }

    private static double rad(double deg) {
        return Math.toRadians(deg);
    }

    private static double fix(double v, double range) {
        v = v - range * Math.floor(v / range);
        return v < 0 ? v + range : v;
    }
}
