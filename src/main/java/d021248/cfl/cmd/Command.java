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

    private static List<Command> activeList = Collections.synchronizedList(new ArrayList<>());

    private Process process = null;
    private Thread tin = null;
    private Thread tout = null;
    private Thread terr = null;
    private final String[] cmd;
    private final String command;

    private Consumer<InputStream> in = null;
    private Consumer<OutputStream> out = null;
    private Consumer<InputStream> err = null;

    private Command(String... cmd) {
        this.cmd = cmd;
        this.command = Arrays.asList(cmd).stream().collect(Collectors.joining(" "));
    }

    public static Command cmd(String... cmd) {
        return new Command(cmd);
    }

    public Command in(Consumer<InputStream> in) {
        this.in = in;
        return this;
    }

    public Command out(Consumer<OutputStream> out) {
        this.out = out;
        return this;
    }

    public Command err(Consumer<InputStream> err) {
        this.err = err;
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

        System.out.println("stopping: " + command);

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
        System.out.println("starting: " + command);

        try {
            run(in, out, err);
        } catch (IOException | InterruptedException e) {}
    }

    protected int run(Consumer<InputStream> in, Consumer<OutputStream> out, Consumer<InputStream> err)
        throws IOException, InterruptedException {
        if (process != null) {
            throw new IOException(String.format("Command already started: %s", this));
        }

        activeList.add(this);
        process = new ProcessBuilder().command(cmd).start();

        if (in != null) {
            tin = new Thread(() -> in.accept(process.getInputStream()));
            tin.start();
        }

        if (out != null) {
            tout = new Thread(() -> out.accept(process.getOutputStream()));
            tout.start();
        }

        if (err != null) {
            terr = new Thread(() -> err.accept(process.getErrorStream()));
            terr.start();
        }

        var result = process.waitFor();
        stop();
        return result;
    }

    public String cmd() {
        return command;
    }

    @Override
    public String toString() {
        return String.format("%s(\"%s\")", this.getClass().getSimpleName(), cmd());
    }

    public static List<Command> activeList() {
        return new ArrayList<>(activeList);
    }
}
