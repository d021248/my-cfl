package d021248.cfl.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Cf {

    private static final Pattern TARGET_PATTERN = Pattern.compile("^\\S+:\\s+(\\S+)$");
    private static final Pattern APPS_PATTERN = Pattern.compile(
        "^(\\S+)\\s+(\\S+)\\s+(\\d+/\\d+)\\s+(\\d+\\S+)\\s+(\\d+\\S+)\\s?(.*)$"
    );
    private static final String CF = System.getProperty("CFL", "cf");
    private static final String CRLF = String.format("%n");
    private static Consumer<String> outLogger = System.out::println;
    private static Consumer<String> errLogger = System.err::println;

    private Cf() {}

    public static List<String> run(String... cmd) {
        return run(false, cmd);
    }

    public static List<String> run(boolean silently, String... cmd) {
        var lines = new ArrayList<String>();
        Consumer<InputStream> toStringBuilder = is -> toConsumer(is, lines::add);
        Command.cmd(cmd).in(toStringBuilder).err(toStringBuilder).start();
        if (!silently) {
            Cf.outLogger.accept(Arrays.asList(cmd).stream().collect(Collectors.joining(" ")));
            Cf.outLogger.accept(toString(lines));
            Cf.outLogger.accept(CRLF);
        }
        return lines;
    }

    public static List<String> target() {
        return Cf.run("cf", "target");
    }

    public static Target getTarget() {
        var attrList = new ArrayList<String>();
        Cf
            .target()
            .stream()
            .map(TARGET_PATTERN::matcher)
            .filter(Matcher::matches)
            .map(matcher -> matcher.group(1))
            .forEach(attrList::add);
        return new Target(attrList.get(0), attrList.get(2), attrList.get(2), attrList.get(3), attrList.get(4));
    }

    public static List<String> apps() {
        return Cf.run("cf", "apps");
    }

    public static List<App> getApps() {
        var appList = new ArrayList<App>();
        Cf
            .apps()
            .stream()
            .map(APPS_PATTERN::matcher)
            .filter(Matcher::matches)
            .map(
                matcher ->
                    new App(
                        matcher.group(1),
                        matcher.group(2),
                        matcher.group(3),
                        matcher.group(4),
                        matcher.group(5),
                        matcher.group(6)
                    )
            )
            .forEach(appList::add);
        return appList;
    }

    public static List<String> env(String app) {
        return Cf.run("cf", "env", app);
    }

    public static String getEnv(String app) {
        var postfix = String.format("%s}", CRLF);
        var envJson = Cf
            .env(app)
            .stream()
            .dropWhile(line -> !line.equals("{"))
            .takeWhile(line -> !line.equals("}"))
            .collect(Collectors.joining(CRLF, "", postfix));
        if (envJson == null || envJson.equals(postfix)) {
            envJson = "{}";
        }
        return envJson;
    }

    public static void logs() {
        Cf.getApps().stream().forEach(app -> Cf.logs(app.name));
    }

    public static void logs(String appName) {
        String cmd = String.format("cf logs %s", appName);
        Command.activeList().stream().filter(c -> c.cmd().equals(cmd)).forEach(c -> c.stop());
        new Thread(() -> Command.cmd("cf", "logs", appName).in(Cf::toOutLogger).err(Cf::toErrLogger).start()).start();
    }

    private static void toOutLogger(InputStream is) {
        toConsumer(is, Cf.outLogger);
    }

    private static void toErrLogger(InputStream is) {
        toConsumer(is, Cf.errLogger);
    }

    private static synchronized void toConsumer(InputStream is, Consumer<String> consumer) {
        try (var bufferedReader = new BufferedReader(new InputStreamReader(is))) {
            bufferedReader.lines().forEach(consumer::accept);
        } catch (IOException e) {
            Cf.errLogger.accept(String.format("Error: %s", e.getMessage()));
        }
    }

    private static String toString(List<String> lines) {
        return lines.stream().collect(Collectors.joining(CRLF));
    }

    public static void setOutLogger(Consumer<String> logger) {
        Cf.outLogger = logger;
    }

    public static Consumer<String> getOutLogger() {
        return Cf.outLogger;
    }

    public static void setErrLogger(Consumer<String> logger) {
        Cf.errLogger = logger;
    }

    public static Consumer<String> getErrLogger() {
        return Cf.errLogger;
    }
}
