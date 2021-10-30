package d021248.cfl.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Cf extends Shell {

    private static final Pattern TARGET_PATTERN = Pattern.compile("^\\S+:\\s+(\\S+)$");
    private static final Pattern APPS_PATTERN = Pattern.compile(
        "^(\\S+)\\s+(\\S+)\\s+(\\d+/\\d+)\\s+(\\d+\\S+)\\s+(\\d+\\S+)\\s?(.*)$"
    );
    private static final String CF = System.getProperty("CFL", "cf");
    private static final String CRLF = System.getProperty("line.separator", "\n");
    private static Consumer<String> outLogger = System.out::println;
    private static Consumer<String> errLogger = System.err::println;

    private Cf() {}

    private Cf(String... cmd) {
        super(cmd);
        this.outConsumer(Cf.outLogger);
        this.errConsumer(Cf.errLogger);
    }

    public static Cf cmd(String... cmd) {
        return new Cf(cmd);
    }

    public static Target getTarget() {
        var lines = new ArrayList<String>();
        Cf.cmd("cf", "target").outConsumer(lines::add).run();
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
        Cf.cmd("cf", "apps").outConsumer(lines::add).run();
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
        Cf.cmd("cf", "env", app).outConsumer(lines::add).run();
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

    public static void logs() {
        Cf.getApps().stream().forEach(app -> Cf.logs(app.name));
    }

    public static void logs(String appName) {
        Consumer<String> outConsumer = line -> Cf.outLogger.accept(String.format("%s %s", appName, line));
        var logAppCommand = Shell.cmd("cf", "logs", appName).outConsumer(outConsumer);
        Command.activeList().stream().filter(c -> c.cmd().equals(logAppCommand.cmd())).forEach(Command::stop);
        new Thread(logAppCommand).start();
    }

    public static void setOutLogger(Consumer<String> logger) {
        Cf.outLogger = logger;
    }

    public static void setErrLogger(Consumer<String> logger) {
        Cf.errLogger = logger;
    }
}
