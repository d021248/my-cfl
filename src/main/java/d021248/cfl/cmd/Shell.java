package d021248.cfl.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Optional;
import java.util.function.Consumer;

public class Shell extends Command {

    private Consumer<String> stdoutConsumer = s -> {
    };
    private Consumer<String> stderrConsumer = s -> {
    };
    private InputStream stdin = System.in;

    protected Shell(String... cmd) {
        super(cmd);
    }

    public static Shell cmd(String... cmd) {
        return new Shell(cmd);
    }

    public Shell stdin(InputStream stdin) {
        this.stdin = stdin;
        return this;
    }

    public Shell stdoutConsumer(Consumer<String> stdoutConsumer) {
        this.stdoutConsumer = stdoutConsumer;
        return this;
    }

    public Shell stderrConsumer(Consumer<String> stderrConsumer) {
        this.stderrConsumer = stderrConsumer;
        return this;
    }

    @Override
    public void run() {

        this.stdinHandler(os -> this.handle(this.stdin, os));
        this.stdoutHandler(is -> this.handle(is, this.stdoutConsumer));
        this.stderrHandler(is -> this.handle(is, this.stderrConsumer));

        this.stdoutConsumer.accept(this.cmd());
        super.run();
    }

    private void handle(InputStream is, Consumer<String> consumer) {
        try (var bufferedReader = new BufferedReader(new InputStreamReader(is))) {
            bufferedReader.lines().forEach(consumer::accept);
        } catch (IOException e) {
            Optional
                    .ofNullable(this.stderrConsumer)
                    .orElse(System.err::println)
                    .accept(String.format("Error: %s", e.getMessage()));
        }
    }

    private void handle(InputStream is, OutputStream os) {
        int c;
        try {
            while ((c = is.read()) > -1) {
                os.write(c);
                os.flush();
            }
        } catch (IOException e) {
            Optional
                    .ofNullable(this.stderrConsumer)
                    .orElse(System.err::println)
                    .accept(String.format("Error: %s", e.getMessage()));
        }
    }
}
