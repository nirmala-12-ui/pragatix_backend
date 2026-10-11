package jjcet.PragatiX.integrations.neopat.util;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filter and parser for incoming Neopat assessment test names.
 * Supported batches: 2027, 2028, 2029, 2030.
 * Rule:
 * 1. Must start with one of the supported batch years.
 * 2. Must contain the WEEKLY_ASSESSMENT designation.
 * 3. Daily assessments, unsupported batch years, or non-weekly test names must be skipped.
 */
public final class NeopatBatchAssessmentFilter {

    public static final Set<Integer> SUPPORTED_BATCHES = Set.of(2027, 2028, 2029, 2030);

    // Matches beginning with 4-digit batch year followed by separator, and contains WEEKLY_ASSESSMENT
    private static final Pattern BATCH_YEAR_PREFIX_PATTERN = Pattern.compile("^(\\d{4})[_-].*", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEEKLY_ASSESSMENT_PATTERN = Pattern.compile(".*WEEKLY[_-]ASSESSMENT.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern DAILY_ASSESSMENT_PATTERN = Pattern.compile(".*DAILY[_-]ASSESSMENT.*", Pattern.CASE_INSENSITIVE);

    public record FilterResult(boolean accepted, Integer batchYear, String reason) {
        public static FilterResult accept(int batchYear) {
            return new FilterResult(true, batchYear, "ACCEPTED");
        }

        public static FilterResult skip(String reason) {
            return new FilterResult(false, null, reason);
        }
    }

    private NeopatBatchAssessmentFilter() {
    }

    /**
     * Evaluates the test name against weekly batch processing criteria.
     *
     * @param rawTestName The test name or test ID from Neopat payload
     * @return FilterResult indicating acceptance or skip reason
     */
    public static FilterResult evaluate(String rawTestName) {
        if (rawTestName == null || rawTestName.trim().isEmpty()) {
            return FilterResult.skip("EMPTY_OR_NULL_TEST_NAME");
        }

        String testName = rawTestName.trim();

        // 1. Explicitly reject daily assessments first
        if (DAILY_ASSESSMENT_PATTERN.matcher(testName).matches()) {
            return FilterResult.skip("DAILY_ASSESSMENT_SKIPPED");
        }

        // 2. Extract batch year from prefix
        Matcher prefixMatcher = BATCH_YEAR_PREFIX_PATTERN.matcher(testName);
        if (!prefixMatcher.matches()) {
            return FilterResult.skip("MISSING_OR_INVALID_BATCH_YEAR_PREFIX");
        }

        int extractedYear;
        try {
            extractedYear = Integer.parseInt(prefixMatcher.group(1));
        } catch (NumberFormatException e) {
            return FilterResult.skip("INVALID_BATCH_YEAR_FORMAT");
        }

        if (!SUPPORTED_BATCHES.contains(extractedYear)) {
            return FilterResult.skip("UNSUPPORTED_BATCH_YEAR_" + extractedYear);
        }

        // 3. Must contain WEEKLY_ASSESSMENT designation
        if (!WEEKLY_ASSESSMENT_PATTERN.matcher(testName).matches()) {
            return FilterResult.skip("NOT_A_WEEKLY_ASSESSMENT");
        }

        return FilterResult.accept(extractedYear);
    }
}
