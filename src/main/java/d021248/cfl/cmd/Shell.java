package d021248.cfl.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Optional;
import java.util.function.Consumer;

public class Shell extends Command {

    private Consumer<String> stdoutConsumer = s -> {};
    private Consumer<String> stderrConsumer = s -> {};
    private InputStream stdin = null;

    public Shell(String... cmd) {
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
        this.stdin = Optional.ofNullable(this.stdin).orElse(System.in);
        Consumer<OutputStream> toStdinConsumer = os -> this.transferTo(this.stdin, os);
        this.stdinHandler(toStdinConsumer);

        Consumer<InputStream> toStdoutConsumer = is -> toConsumer(is, this.stdoutConsumer);
        this.stdoutHandler(toStdoutConsumer);

        Consumer<InputStream> toStderrConsumer = is -> toConsumer(is, this.stderrConsumer);
        this.stderrHandler(toStderrConsumer);

        this.stdoutConsumer.accept(this.cmd());
        super.run();
    }

    private void toConsumer(InputStream is, Consumer<String> consumer) {
        try (var bufferedReader = new BufferedReader(new InputStreamReader(is))) {
            bufferedReader.lines().forEach(consumer::accept);
        } catch (IOException e) {
            Optional
                .ofNullable(this.stderrConsumer)
                .orElse(System.err::println)
                .accept(String.format("Error: %s", e.getMessage()));
        }
    }

    private void transferTo(InputStream is, OutputStream os) {
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
