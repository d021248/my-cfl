package d021248.cfl.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
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

    public static List<String> cli(String... cmd) {
        return cli(true, cmd);
    }

    public static List<String> cli(boolean silently, String... cmd) {
        var lines = new ArrayList<String>();
        Consumer<InputStream> toStringBuilder = is -> toConsumer(is, UnaryOperator.identity(), lines::add);
        Command.cmd(cmd).in(toStringBuilder).err(toStringBuilder).run();
        if (!silently) {
            Cf.outLogger.accept(Arrays.asList(cmd).stream().collect(Collectors.joining(" ", ">", "")));
            Cf.outLogger.accept(toString(lines));
            Cf.outLogger.accept(CRLF);
        }
        return lines;
    }

    public static Target getTarget() {
        var attrList = new ArrayList<String>();
        Cf
            .cli("cf", "target")
            .stream()
            .map(TARGET_PATTERN::matcher)
            .filter(Matcher::matches)
            .map(matcher -> matcher.group(1))
            .forEach(attrList::add);
        return new Target(attrList.get(0), attrList.get(1), attrList.get(2), attrList.get(3), attrList.get(4));
    }

    public static List<App> getApps() {
        var appList = new ArrayList<App>();
        Cf
            .cli("cf", "apps")
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

    public static String getEnv(String app) {
        var postfix = String.format("%s}", CRLF);
        var envJson = Cf
            .cli("cf", "env", app)
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
        var cmd = String.format("cf logs %s", appName);
        UnaryOperator<String> formatter = s -> s.isEmpty() ? s : String.format("%s %s", appName, s.trim());
        Consumer<InputStream> toOutputlogger = is -> toConsumer(is, formatter, Cf.outLogger);
        Command.activeList().stream().filter(c -> c.cmd().equals(cmd)).forEach(Command::stop);
        new Thread(Command.cmd("cf", "logs", appName).in(toOutputlogger).err(toOutputlogger)).start();
    }

    private static void toConsumer(InputStream is, UnaryOperator<String> formatter, Consumer<String> consumer) {
        try (var bufferedReader = new BufferedReader(new InputStreamReader(is))) {
            bufferedReader.lines().map(formatter).forEach(consumer::accept);
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
