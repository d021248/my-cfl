package d021248.cfl.cmd.cf;

import java.util.regex.Pattern;

public record App(String name, String state, String processes, String routes) {

    private static final String REGEXP_NAME = "(?<name>\\S+)";
    private static final String REGEXP_STATE = "(?<state>\\S+)";
    private static final String REGEXP_PROCESS = "(?<process>web:\\d+/\\d+(,\\s+task:\\d+/\\d+)?)";
    private static final String REGEXP_ROUTES = "(?<routes>.+)";
    private static final String REGEXP_WS = "\\s+";
    private static final String REGEXP_APP = "^"
            + REGEXP_NAME + REGEXP_WS
            + REGEXP_STATE + REGEXP_WS
            + REGEXP_PROCESS + REGEXP_WS
            + REGEXP_ROUTES
            + "$";

    private static final Pattern PATTERN_APP = Pattern.compile(REGEXP_APP);

    public static boolean matches(String s) {
        return PATTERN_APP.matcher(s).matches();
    }

    public static App from(String s) {

        var matcher = PATTERN_APP.matcher(s);
        matcher.matches();

        return new App(
                matcher.group("name"),
                matcher.group("state"),
                matcher.group("process"),
                matcher.group("routes"));
    }
}
