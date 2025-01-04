package d021248.cfl.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import d021248.cfl.cmd.cf.App;
import d021248.cfl.cmd.cf.Target;

public class Cf {

    private static final String CRLF = "\n";

    private Cf() {
    }

    public static void run(Consumer<String> logger, String... command) {
        String[] cmd = new String[command.length + 2];
        cmd[0] = "CMD";
        cmd[1] = "/C";
        for (int i = 0; i < command.length; i++) {
            cmd[i + 2] = command[i];
        }
        Shell.cmd(cmd).stdoutConsumer(logger).run();
        // Thread.ofVirtual().start(Shell.cmd(cmd).stdoutConsumer(logger));
    }

    public static Target target(Consumer<String> logger) {
        var lines = new ArrayList<String>();
        Shell.cmd("cf", "target").stdoutConsumer(lines::add).run();
        var s = lines.stream().collect(Collectors.joining(CRLF));
        logger.accept(s);
        return Target.from(s);

    }

    public static Target target() {
        return target(System.err::println);
    }

    public static List<App> apps(Consumer<String> logger) {
        var lines = new ArrayList<String>();

        // does not work: concurrency problem
        // Shell.cmd("cf", "apps").stdoutConsumer(logger.andThen(lines::add)).run();

        Shell.cmd("cf", "apps").stdoutConsumer(lines::add).run();
        return lines
                .stream()
                .sorted()
                .filter(App::matches)
                .map(App::from)
                .toList();
    }

    public static List<App> apps() {
        return apps(System.err::println);
    }

    public static String env(String app, Consumer<String> logger) {
        var postfix = String.format("%s}", CRLF);
        var lines = new ArrayList<String>();
        Shell.cmd("cf", "env", app).stdoutConsumer(logger.andThen(lines::add)).run();
        return lines
                .stream()
                .collect(Collectors.joining(CRLF, "", postfix));
    }

    public static String env(String app) {
        return env(app, System.err::println);
    }

    public static void logs(Consumer<String> logger) {
        Cf.apps(logger).forEach(app -> Cf.logs(app.name(), logger));
    }

    public static void logs(String appName, Consumer<String> logger) {
        // logger.accept(appName);
        Thread.ofVirtual().start(() -> {
            Cf.stopLogs(appName);
            Shell.cmd("cf", "logs", appName)
                    .stdoutConsumer(line -> logger.accept(line.isEmpty() ? "" : String.format("%s %s", appName, line)))
                    .run();
        });

    }

    public static void stopLogs(String appname) {
        Command
                .activeList()
                .stream()
                .filter(c -> c.cmd().equals(String.format("cf logs %s", appname)))
                .forEach(Command::stop);
    }

    public static void stopLogs() {
        Cf.apps().stream().map(App::name).forEach(Cf::stopLogs);
    }
}
