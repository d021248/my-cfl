package d021248.cfl.cmd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Command implements Runnable {

    protected static List<Command> activeList = Collections.synchronizedList(new ArrayList<>());

    private Process process = null;
    private Thread tin = null;
    private Thread tout = null;
    private Thread terr = null;
    private final String[] cmd;
    private final String commandString;

    protected Consumer<OutputStream> stdinHandler = null;
    protected Consumer<InputStream> stdoutHandler = null;
    protected Consumer<InputStream> stderrHandler = null;

    protected Command(String... cmd) {
        this.cmd = cmd;
        this.commandString = Arrays.asList(cmd).stream().collect(Collectors.joining(" "));
    }

    public static Command cmd(String... cmd) {
        return new Command(cmd);
    }

    public Command stdinHandler(Consumer<OutputStream> stdinHandler) {
        this.stdinHandler = stdinHandler;
        return this;
    }

    public Command stdoutHandler(Consumer<InputStream> stdoutHandler) {
        this.stdoutHandler = stdoutHandler;
        return this;
    }

    public Command stderrHandler(Consumer<InputStream> stderrHandler) {
        this.stderrHandler = stderrHandler;
        return this;
    }

    public static void stopAll() {
        while (!activeList.isEmpty()) {
            activeList.get(0).stop();
        }
    }

    public void stop() {
        if (!activeList.contains(this)) {
            return;
        }

        System.out.println("stopping: " + commandString);

        if (process != null) {
            process.destroy();
        }

        if (tin != null) {
            tin.stop();
        }

        if (tout != null) {
            tout.stop();
        }

        if (terr != null) {
            terr.stop();
        }

        activeList.remove(this);
    }

    public void run() {
        System.out.println("starting: " + commandString);

        try {
            run(stdinHandler, stdoutHandler, stderrHandler);
        } catch (IOException | InterruptedException e) {
            System.err.println(e);
            stop();
        }
    }

    protected int run(
        Consumer<OutputStream> stdinHandler,
        Consumer<InputStream> stdoutHandler,
        Consumer<InputStream> stderrHandler
    ) throws IOException, InterruptedException {
        if (process != null) {
            throw new IOException(String.format("Command already started: %s", this));
        }

        activeList.add(this);
        process = new ProcessBuilder().command(cmd).start();

        if (stdinHandler != null) {
            tout = new Thread(() -> stdinHandler.accept(process.getOutputStream()));
            tout.start();
        }

        if (stdoutHandler != null) {
            tin = new Thread(() -> stdoutHandler.accept(process.getInputStream()));
            tin.start();
        }

        if (stderrHandler != null) {
            terr = new Thread(() -> stderrHandler.accept(process.getErrorStream()));
            terr.start();
        }

        var result = process.waitFor();
        stop();
        return result;
    }

    public String cmd() {
        return commandString;
    }

    @Override
    public String toString() {
        return String.format("%s(\"%s\")", this.getClass().getSimpleName(), commandString);
    }

    public static List<Command> activeList() {
        return new ArrayList<>(activeList);
    }
}
