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
        new Thread(Shell.cmd(cmd).stdoutConsumer(logger)).start();
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
        Shell.cmd("cf", "apps").stdoutConsumer(lines::add).run();
        var appList = lines
                .stream()
                .peek(logger::accept) // workaround: logger.andThen(lines::add) does not work
                .filter(App::matches)
                .map(App::from)
                .toList();
        return appList;
    }

    public static List<App> apps() {
        return apps(System.err::println);
    }

    public static String env(String app, Consumer<String> logger) {
        var postfix = String.format("%s}", CRLF);
        var lines = new ArrayList<String>();
        Shell.cmd("cf", "env", app).stdoutConsumer(lines::add).run();
        return lines
                .stream()
                .peek(logger::accept) // workaround: logger.andThen(lines::add) does not work
                .collect(Collectors.joining(CRLF, "", postfix));
    }

    public static String env(String app) {
        return env(app, System.err::println);
    }

    public static void logs(Consumer<String> logger) {
        Cf.apps(logger).stream().forEach(app -> Cf.logs(app.name(), logger));
    }

    public static void logs(String appName, Consumer<String> logger) {
        Cf.stopLogs(appName);
        var logAppCommand = Shell
                .cmd("cf", "logs", appName)
                .stdoutConsumer(line -> logger.accept(line.isEmpty() ? "" : String.format("%s %s", appName, line)));
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
        Cf.apps().stream().map(App::name).forEach(Cf::stopLogs);
    }
}
