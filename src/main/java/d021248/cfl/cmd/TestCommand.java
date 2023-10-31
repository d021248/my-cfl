package d021248.cfl.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.function.Predicate;

import d021248.cfl.cmd.cf.Cf;

public class TestCommand {


    public static void main(String[] args) throws IOException, InterruptedException {
        // Shell.cmd("cmd").run();

        // Cf.apps();
        Cf.target();

        Shell.cmd("cf", "apps").run();

        System.out.println();
        Command
                .cmd("cf env mkv-srv")
                .stdinHandler(TestCommand::stdinHandler)
                .stdoutHandler(TestCommand::stdoutHandler)
                .stderrHandler(TestCommand::stderrHandler)
                .run();

        System.out.println();

        System.out.println();
        Command
                .cmd("cmd")
                .stdinHandler(TestCommand::stdinHandler)
                .stdoutHandler(TestCommand::stdoutHandler)
                .stderrHandler(TestCommand::stderrHandler)
                .run();
        System.out.println();

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
        System.out.println("starting handlerOut()");
        int c;
        try {
            while ((c = System.in.read()) > -1) {
                os.write(c);
                os.flush();
            }
        } catch (IOException e) {
        }
    }

    private static void stderrHandler(InputStream is) {
        System.out.println("starting handlerErr()");
        try (var bufferedReader = new BufferedReader(new InputStreamReader(is))) {
            bufferedReader
                    .lines()
                    .filter(Predicate.not(String::isEmpty))
                    .map(String::trim)
                    .forEach(line -> System.out.println("err>>" + line));
        } catch (IOException e) {
            System.err.println(String.format("Error: %s", e.getMessage()));
        }
    }

    private static void stdoutHandler(InputStream is) {
        System.out.println("starting handlerIn()");
        try (var bufferedReader = new BufferedReader(new InputStreamReader(is))) {
            bufferedReader
                    .lines()
                    .filter(Predicate.not(String::isEmpty))
                    .map(String::trim)
                    .forEach(line -> System.out.println("out>>" + line));
        } catch (IOException e) {
            System.err.println(String.format("Error: %s", e.getMessage()));
        }
    }
}
