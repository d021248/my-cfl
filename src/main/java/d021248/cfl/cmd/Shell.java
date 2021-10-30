package d021248.cfl.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.function.Consumer;

public class Shell extends Command {

    private Consumer<String> outConsumer = System.out::println;
    private Consumer<String> errConsumer = System.err::println;

    public Shell(String... cmd) {
        super(cmd);
    }

    public static Shell cmd(String... cmd) {
        return new Shell(cmd);
    }

    public Shell outConsumer(Consumer<String> outConsumer) {
        this.outConsumer = outConsumer;
        return this;
    }

    public Shell errConsumer(Consumer<String> errConsumer) {
        this.errConsumer = errConsumer;
        return this;
    }

    @Override
    public void run() {
        Consumer<InputStream> toOutConsumer = (this.outConsumer == null)
            ? this.in
            : is -> toConsumer(is, this.outConsumer);
        Consumer<InputStream> toErrConsumer = (this.errConsumer == null)
            ? this.err
            : is -> toConsumer(is, this.errConsumer);
        this.in(toOutConsumer);
        this.err(toErrConsumer);
        super.run();
    }

    private void toConsumer(InputStream is, Consumer<String> consumer) {
        try (var bufferedReader = new BufferedReader(new InputStreamReader(is))) {
            bufferedReader.lines().forEach(consumer::accept);
        } catch (IOException e) {
            Optional
                .ofNullable(this.errConsumer)
                .orElse(System.err::println)
                .accept(String.format("Error: %s", e.getMessage()));
        }
    }
}
