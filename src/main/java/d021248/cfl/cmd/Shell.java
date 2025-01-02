package d021248.cfl.cmd;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Shell extends Command {

    private Consumer<String> stdoutConsumer = System.out::println;
    private Consumer<String> stderrConsumer = System.err::println;
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
        } catch (Exception e) {
            this.stderrConsumer.accept("Error while piping stream: " + e.getMessage());
        }
    }

    private void pipe(Supplier<InputStream> supplier, OutputStream os) {
        try (var inputStream = supplier.get()) {
            inputStream.transferTo(os);
        } catch (Exception e) {
            this.stderrConsumer.accept("Error while piping stream: " + e.getMessage());
        }
    }
}