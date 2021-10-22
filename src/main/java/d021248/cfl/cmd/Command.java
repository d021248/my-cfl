package d021248.cfl.cmd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class Command {

	private static List<Command> runnig = new ArrayList<>();

	private Consumer<InputStream> in = null;
	private Consumer<OutputStream> out = null;
	private Consumer<InputStream> err = null;

	private String cmd;
	private Process process;
	private Thread tin = null;
	private Thread tout = null;
	private Thread terr = null;

	public Command(Consumer<InputStream> in, Consumer<OutputStream> out, Consumer<InputStream> err) {
		this.in = in;
		this.out = out;
		this.err = err;
		this.process = null;
	}

	public static void stopAll() {
		runnig.stream().forEach(Command::stop);
	}

	public void stop() {
		process.destroy();
		if (tin != null) {
			tin.stop();
		}
		if (tout != null) {
			tout.stop();
		}
		if (terr != null) {
			terr.stop();
		}
	}

	public void start(String... command) throws IOException {

		if (cmd != null) {
			throw new IOException(String.format("Instance already in use: %s", this));
		}

		runnig.add(this);
		cmd = Arrays.asList(command).stream().collect(Collectors.joining(" "));
		process = new ProcessBuilder().command(command).start();

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
	}

	@Override
	public String toString() {
		return String.format("%s [ %s ]", this.getClass().getSimpleName(), (cmd == null) ? "<initial>" : cmd);
	}
}
