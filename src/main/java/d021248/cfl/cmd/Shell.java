package d021248.cfl.cmd;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Shell extends Command {

    private static final Consumer<String> DEFAULT_STDERR_CONSUMER = System.err::println;

    private Consumer<String> stdoutConsumer = s -> {
    };
    private Consumer<String> stderrConsumer = s -> {
    };

    private Supplier<InputStream> stdinSupplier = () -> System.in;

    protected Shell(String... cmd) {
        super(cmd);
    }

    public static Shell cmd(String... cmd) {
        return new Shell(cmd);
    }

    public Shell stdinSupplier(Supplier<InputStream> stdinSupplier) {
        Objects.requireNonNull(stdinSupplier);
        this.stdinSupplier = stdinSupplier;
        return this;
    }

    public Shell stdoutConsumer(Consumer<String> stdoutConsumer) {
        Objects.requireNonNull(stdoutConsumer);
        this.stdoutConsumer = stdoutConsumer;
        return this;
    }

    public Shell stderrConsumer(Consumer<String> stderrConsumer) {
        Objects.requireNonNull(stderrConsumer);
        this.stderrConsumer = stderrConsumer;
        return this;
    }

    @Override
    public void run() {

        this.stdinHandler(os -> this.pipe(this.stdinSupplier, os));
        this.stdoutHandler(is -> this.pipe(is, this.stdoutConsumer));
        this.stderrHandler(is -> this.pipe(is, this.stderrConsumer));

        this.stdoutConsumer.accept(this.cmd());
        super.run();
    }

    private void pipe(InputStream is, Consumer<String> consumer) {
        try (var bufferedReader = new BufferedReader(new InputStreamReader(is))) {
            bufferedReader.lines().forEach(consumer::accept);
        } catch (IOException e) {
            Optional
                    .ofNullable(this.stderrConsumer)
                    .orElse(DEFAULT_STDERR_CONSUMER)
                    .accept(String.format("Error: %s", e.getMessage()));
        }
    }

    private void pipe(Supplier<InputStream> supplier, OutputStream os) {
        int c;
        try (var is = supplier.get()) {
            while ((c = is.read()) > -1) {
                os.write(c);
                os.flush();
            }
        } catch (IOException e) {
            Optional
                    .ofNullable(this.stderrConsumer)
                    .orElse(DEFAULT_STDERR_CONSUMER)
                    .accept(String.format("Error: %s", e.getMessage()));
        }
    }
}
