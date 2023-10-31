package d021248.cfl.cmd.cf;

import java.util.regex.Pattern;

public record Target(String endpoint, String version, String user, String org, String space) {

    private static final String PREFIX = "(.*\\n)*";
    private static final String TEMPLATE = "(.*):(\\s+)(?<%s>\\S+)";
    private static final String REGEXP_ENDPOINT = TEMPLATE.formatted("endpoint");
    private static final String REGEXP_VERSION = TEMPLATE.formatted("version");
    private static final String REGEXP_USER = TEMPLATE.formatted("user");
    private static final String REGEXP_ORG = TEMPLATE.formatted("org");
    private static final String REGEXP_SPACE = TEMPLATE.formatted("space");
    private static final String REGEXP_CRLF = "\\n";
    private static final String REGEXP_TARGET = PREFIX
            + REGEXP_ENDPOINT + REGEXP_CRLF
            + REGEXP_VERSION + REGEXP_CRLF
            + REGEXP_USER + REGEXP_CRLF
            + REGEXP_ORG + REGEXP_CRLF
            + REGEXP_SPACE;

    private static final Pattern PATTERN_TARGET = Pattern.compile(REGEXP_TARGET);

    public static boolean matches(String s) {
        return PATTERN_TARGET.matcher(s).matches();
    }

    public static Target from(String s) {

        var matcher = PATTERN_TARGET.matcher(s);
        matcher.matches();

        return new Target(
                matcher.group("endpoint"),
                matcher.group("version"),
                matcher.group("user"),
                matcher.group("org"),
                matcher.group("space"));
    }

}
