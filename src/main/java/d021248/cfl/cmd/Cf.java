package d021248.cfl.cmd;

import java.util.ArrayList;
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
    private static final String CRLF = System.getProperty("line.separator", "\n");
    private static Consumer<String> outLogger2 = System.out::println;
    private static Consumer<String> errLogger = System.err::println;

    private Cf() {}

    public static Target getTarget() {
        var lines = new ArrayList<String>();
        Shell.cmd("cf", "target").stdoutConsumer(lines::add).run();
        var attrList = lines
            .stream()
            .map(TARGET_PATTERN::matcher)
            .filter(Matcher::matches)
            .map(matcher -> matcher.group(1))
            .collect(Collectors.toList());
        return new Target(attrList.get(0), attrList.get(1), attrList.get(2), attrList.get(3), attrList.get(4));
    }

    public static List<App> getApps() {
        var lines = new ArrayList<String>();
        Shell.cmd("cf", "apps").stdoutConsumer(lines::add).run();
        return lines
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
            .collect(Collectors.toList());
    }

    public static String getEnv(String app) {
        var postfix = String.format("%s}", CRLF);
        var lines = new ArrayList<String>();
        Shell.cmd("cf", "env", app).stdoutConsumer(lines::add).run();
        var envJson = lines
            .stream()
            .dropWhile(line -> !line.equals("{"))
            .takeWhile(line -> !line.equals("}"))
            .collect(Collectors.joining(CRLF, "", postfix));
        if (envJson == null || envJson.equals(postfix)) {
            envJson = "{}";
        }
        return envJson;
    }

    public static void logs(Consumer<String> logger) {
        Cf.getApps().stream().forEach(app -> Cf.logs(app.name, logger));
    }

    public static void logs(String appName, Consumer<String> logger) {
        Consumer<String> outConsumer = line ->
            logger.accept(line.isEmpty() ? "" : String.format("%s %s", appName, line.trim()));
        var logAppCommand = Shell.cmd("cf", "logs", appName).stdoutConsumer(outConsumer);
        Command.activeList().stream().filter(c -> c.cmd().equals(logAppCommand.cmd())).forEach(Command::stop);
        new Thread(logAppCommand).start();
    }
}
