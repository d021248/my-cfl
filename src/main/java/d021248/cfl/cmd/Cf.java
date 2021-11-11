package d021248.cfl.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Cf {

    // is correct, but does not work: private static final Pattern TARGET_PATTERN =
    // Pattern.compile("^\\S+:\\s+(\\S+)$");
    private static final Pattern TARGET_PATTERN = Pattern.compile(".*:(.*)");
    private static final Pattern APPS_PATTERN = Pattern.compile(
        "^(\\S+)\\s+(\\S+)\\s+(\\d+/\\d+)\\s+(\\d+\\S+)\\s+(\\d+\\S+)\\s?(.*)$"
    );

    private static final String CRLF = System.getProperty("line.separator", "\n");

    private Cf() {}

    public static void run(Consumer<String> logger, String... command) {
        String[] cmd = new String[command.length + 2];
        cmd[0] = "CMD";
        cmd[1] = "/C";
        for (int i = 0; i < command.length; i++) {
            cmd[i + 2] = command[i];
        }
        new Thread(Shell.cmd(cmd).stdoutConsumer(logger).stderrConsumer(logger)).start();
    }

    public static Target target(Consumer<String> logger) {
        var lines = new ArrayList<String>();
        Shell.cmd("cf", "target").stdoutConsumer(lines::add).stderrConsumer(logger).run();

        var attrList = new ArrayList<String>();
        lines
            .stream()
            .peek(logger::accept) // workaround: logger.andThen(lines::add) does not work
            .map(TARGET_PATTERN::matcher)
            .filter(Matcher::matches)
            .map(matcher -> matcher.group(1))
            .map(String::trim)
            .forEach(attrList::add);
        return new Target(attrList.get(0), attrList.get(1), attrList.get(2), attrList.get(3), attrList.get(4));
    }

    public static Target target() {
        return target(System.err::println);
    }

    public static List<App> apps(Consumer<String> logger) {
        var lines = new ArrayList<String>();
        Shell.cmd("cf", "apps").stdoutConsumer(lines::add).stderrConsumer(logger).run();
        return lines
            .stream()
            .peek(logger::accept) // workaround: logger.andThen(lines::add) does not work
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
            .collect(Collectors.toList());
    }

    public static List<App> apps() {
        return apps(System.err::println);
    }

    public static String env(String app, Consumer<String> logger) {
        var postfix = String.format("%s}", CRLF);
        var lines = new ArrayList<String>();
        Shell.cmd("cf", "env", app).stdoutConsumer(lines::add).stderrConsumer(logger).run();
        var envJson = lines
            .stream()
            .peek(logger::accept) // workaround: logger.andThen(lines::add) does not work
            .dropWhile(line -> !line.equals("{"))
            .takeWhile(line -> !line.equals("}"))
            .collect(Collectors.joining(CRLF, "", postfix));
        if (envJson == null || envJson.equals(postfix)) {
            envJson = "{}";
        }
        return envJson;
    }

    public static String env(String app) {
        return env(app, System.err::println);
    }

    public static void logs(Consumer<String> logger) {
        Cf.apps(logger).stream().forEach(app -> Cf.logs(app.name, logger));
    }

    public static void logs(String appName, Consumer<String> logger) {
        Cf.stopLogs(appName);
        UnaryOperator<String> lineFormatter = line -> {
            if (line.isEmpty()) {
                return "";
            }
            line = line.trim();
            if (line.startsWith(">")) {
                return line;
            }
            return String.format("%-22s: %s", appName, line);
        };

        Consumer<String> outConsumer = line -> logger.accept(lineFormatter.apply(line));
        var logAppCommand = Shell.cmd("cf", "logs", appName).stdoutConsumer(outConsumer).stderrConsumer(logger);
        new Thread(logAppCommand).start();
    }

    public static void stopLogs(String appname) {
        Command
            .activeList()
            .stream()
            .filter(c -> c.cmd().equals(String.format("cf logs %s", appname)))
            .forEach(Command::stop);
    }

    public static void stopLogs() {
        Cf.apps().stream().map(a -> a.name).forEach(Cf::stopLogs);
    }
}
