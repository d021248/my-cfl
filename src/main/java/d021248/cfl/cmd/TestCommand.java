package d021248.cfl.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.function.Predicate;

public class TestCommand {

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println();
        Command
            .cmd("cf", "env", "mkv-srv")
            .in(TestCommand::handlerIn)
            .out(TestCommand::handlerOut)
            .err(TestCommand::handlerErr)
            .start();

        System.out.println();

        System.out.println();
        Command
            .cmd("cmd")
            .async()
            .in(TestCommand::handlerIn)
            .out(TestCommand::handlerOut)
            .err(TestCommand::handlerErr)
            .start();
        System.out.println();

        System.out.println();
        Command
            .cmd("notepad")
            .async()
            .in(TestCommand::handlerIn)
            .out(TestCommand::handlerOut)
            .err(TestCommand::handlerErr)
            .start();
        System.out.println();

        Thread.sleep(15_000);
        Command.stopAll();

        // Thread.sleep(15_000);
        System.out.println("done");
    }

    private static void handlerOut(OutputStream os) {
        System.out.println("starting handlerOut()");
        int c;
        try {
            while ((c = System.in.read()) > -1) {
                os.write(c);
                os.flush();
            }
        } catch (IOException e) {}
    }

    private static void handlerErr(InputStream is) {
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

    private static void handlerIn(InputStream is) {
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
