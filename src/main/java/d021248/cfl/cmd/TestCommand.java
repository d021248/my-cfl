package d021248.cfl.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class TestCommand {

    public static void main(String[] args) throws IOException, InterruptedException {

        Shell.cmd("cmd").run();
        System.out.println();

        Cf.apps();
        System.out.println();

        Cf.target();
        System.out.println();

        Shell.cmd("cf", "apps").run();
        System.out.println();

        Command
                .cmd("cf env mkv-srv")
                .stdinHandler(TestCommand::stdinHandler)
                .stdoutHandler(TestCommand::stdoutHandler)
                .stderrHandler(TestCommand::stderrHandler)
                .run();
        System.out.println();

        Command
                .cmd("cmd")
                .stdinHandler(TestCommand::stdinHandler)
                .stdoutHandler(TestCommand::stdoutHandler)
                .stderrHandler(TestCommand::stderrHandler)
                .run();
        System.out.println();

        Command
                .cmd("notepad")
                .stdinHandler(TestCommand::stdinHandler)
                .stdoutHandler(TestCommand::stdoutHandler)
                .stderrHandler(TestCommand::stderrHandler)
                .run();
        System.out.println();

        Thread.sleep(15_000);
        Command.stopAll();

        // Thread.sleep(15_000);
        System.out.println("done");
    }

    private static void stdinHandler(OutputStream os) {
        System.out.println("starting stdinHandler");

        int c;
        try {
            while ((c = System.in.read()) > -1) {
                os.write(c);
                if (c == '\n') {
                    os.flush();
                }
            }
            os.flush();
        } catch (IOException e) {
            System.err.println(String.format("Error: %s", e.getMessage()));
        }
    }

    private static void stderrHandler(InputStream is) {
        System.out.println("starting stderrHandler");
        try (var bufferedReader = new BufferedReader(new InputStreamReader(is))) {
            bufferedReader.lines().forEach(line -> System.out.println("err>>" + line));
        } catch (Exception e) {
            System.err.println(String.format("Error: %s", e.getMessage()));
        }
    }

    private static void stdoutHandler(InputStream is) {
        System.out.println("starting stdoutHandler");
        try (var bufferedReader = new BufferedReader(new InputStreamReader(is))) {
            bufferedReader.lines().forEach(line -> System.out.println("out>>" + line));
        } catch (Exception e) {
            System.err.println(String.format("Error: %s", e.getMessage()));
        }
    }
}
