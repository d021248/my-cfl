package d021248.cfl.cmd;

import java.io.BufferedReader;
import java.io.IOException;
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
        this.stdinSupplier = Objects.requireNonNull(stdinSupplier);
        return this;
    }

    public Shell stdoutConsumer(Consumer<String> stdoutConsumer) {
        this.stdoutConsumer = Objects.requireNonNull(stdoutConsumer);
        return this;
    }

    public Shell stderrConsumer(Consumer<String> stderrConsumer) {
        this.stderrConsumer = Objects.requireNonNull(stderrConsumer);
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
            handleException(e);
        }
    }

    private void pipe(Supplier<InputStream> supplier, OutputStream os) {
        try (var is = supplier.get()) {
            transferData(is, os);
        } catch (Exception e) {
            handleException(e);
        }
    }

    private void transferData(InputStream is, OutputStream os) throws IOException {
        int c;
        while ((c = is.read()) > -1) {
            os.write(c);
            if (c == '\n') {
                os.flush();
            }
        }
        os.flush();
    }

    private void handleException(Exception e) {
        if (stderrConsumer != null) {
            stderrConsumer.accept(String.format("Error: %s", e.getMessage()));
        }
        throw new RuntimeException(e);
    }
}
