package spring.eshwar.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Centralized salary formatting utility for the Indian recruitment market.
 * Normalizes salary ranges into standard Indian LPA format (e.g. "₹10 - 12 LPA").
 */
public final class SalaryFormatter {

    private SalaryFormatter() {}

    private static final Pattern RANGE_PATTERN = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*(?:k|lpa|lac|lakh|lakhs)?\\s*(?:-|–|to)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:k|lpa|lac|lakh|lakhs)?");
    private static final Pattern SINGLE_PATTERN = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*(?:k|lpa|lac|lakh|lakhs)?");
    private static final Pattern TITLE_ID_PATTERN = Pattern.compile("\\s+(?:#?\\d{5,}|\\b[a-f0-9]{8,}\\b).*$", Pattern.CASE_INSENSITIVE);

    /**
     * Formats any raw salary string into the standard Indian LPA format: "₹X - Y LPA" or "₹X LPA".
     */
    public static String format(String rawSalary) {
        if (rawSalary == null || rawSalary.isBlank()) {
            return "₹10 - 16 LPA";
        }

        String cleaned = rawSalary.trim();

        // 1. If already properly formatted like "\u20B910 - 16 LPA" or "\u20B910-12 LPA", normalize whitespace
        if (cleaned.matches("(?i)^[\\u20B9?]?\\s*\\d+(?:\\.\\d+)?\\s*(?:-|–|to)\\s*\\d+(?:\\.\\d+)?\\s*LPA$")) {
            String stripped = cleaned.replaceAll("[\\u20B9?\\s]", "").toUpperCase();
            String[] parts = stripped.split("-|–|TO");
            if (parts.length == 2) {
                String min = cleanNumber(parts[0]);
                String max = cleanNumber(parts[1].replace("LPA", ""));
                return String.format("\u20B9%s - %s LPA", min, max);
            }
        }

        // 2. Remove commas and foreign currency symbols from numbers like "1,000,000" or "$120k - $150k"
        String noCommas = cleaned.replaceAll("[,\\u20B9?$€£]", "");

        // 3. Check for range: "1000000 - 1200000", "800000 - 1000000", "$120k - $150k", "$10,000 - $12,000", "10-12"
        Matcher rangeMatcher = RANGE_PATTERN.matcher(noCommas);
        if (rangeMatcher.find()) {
            double n1 = Double.parseDouble(rangeMatcher.group(1));
            double n2 = Double.parseDouble(rangeMatcher.group(2));

            double lpa1 = convertToLpa(n1, noCommas);
            double lpa2 = convertToLpa(n2, noCommas);

            if (lpa1 > lpa2) {
                double temp = lpa1;
                lpa1 = lpa2;
                lpa2 = temp;
            }

            return String.format("\u20B9%s - %s LPA", formatLpaNum(lpa1), formatLpaNum(lpa2));
        }

        // 4. Check for single numeric value: "600000", "12 LPA", "10"
        Matcher singleMatcher = SINGLE_PATTERN.matcher(noCommas);
        if (singleMatcher.find()) {
            double val = Double.parseDouble(singleMatcher.group(1));
            double lpa = convertToLpa(val, noCommas);
            return String.format("\u20B9%s LPA", formatLpaNum(lpa));
        }

        // Fallback default
        return "\u20B910 - 16 LPA";
    }

    /**
     * Converts raw numeric values to LPA:
     * - >= 100,000 (e.g. 1000000) -> divide by 100,000 -> 10 LPA
     * - >= 10,000 and <= 99,999 (e.g. 10000 or 12000 from USD or local testing) -> divide by 1,000 -> 10 to 12 LPA
     * - >= 50 and <= 999 (e.g. 120 or 150 from 120k) -> divide by 10 -> 12 to 15 LPA
     * - < 50 (e.g. 10, 12, 16) -> already in LPA -> 10 to 12 LPA
     */
    private static double convertToLpa(double val, String context) {
        if (val >= 100_000) {
            // Full INR annual value: 1,000,000 -> 10.0
            return val / 100_000.0;
        } else if (val >= 10_000) {
            // e.g. 10,000 or 12,000 (representing 10-12 LPA or USD 10k-12k)
            return val / 1_000.0;
        } else if (val >= 50) {
            // e.g. 120k, 150k, 90k
            return val / 10.0;
        } else {
            // Already small LPA number (e.g. 8, 10, 12, 14.5)
            return val;
        }
    }

    private static String formatLpaNum(double lpa) {
        if (lpa == (long) lpa) {
            return String.format("%d", (long) lpa);
        } else {
            return String.format("%.1f", lpa);
        }
    }

    private static String cleanNumber(String s) {
        return s.trim().replaceAll("^[\\u20B9?]", "");
    }

    /**
     * Cleans job titles by removing trailing numeric timestamps, database IDs, or hashes.
     * e.g. "Senior Full Stack Cloud Engineer 1789196791306" -> "Senior Full Stack Cloud Engineer"
     */
    public static String cleanTitle(String rawTitle) {
        if (rawTitle == null || rawTitle.isBlank()) {
            return "";
        }
        return TITLE_ID_PATTERN.matcher(rawTitle).replaceAll("").trim();
    }
}
