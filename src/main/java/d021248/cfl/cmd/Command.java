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

	private static List<Command> list = new ArrayList<>();

	private Process process = null;
	private Thread tin = null;
	private Thread tout = null;
	private Thread terr = null;
	private final String[] cmd;

	public Command(String... cmd) {
		this.cmd = cmd;
	}

	public static void stopAll() {
		list.stream().forEach(Command::stop);
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

	public void start() throws IOException {
		start(null, null, null);
	}

	public void start(Consumer<OutputStream> out) throws IOException {
		start(null, out, null);
	}

	public void start(Consumer<InputStream> in, Consumer<OutputStream> out, Consumer<InputStream> err)
			throws IOException {

		if (process != null) {
			throw new IOException(String.format("Command already started: %s", this));
		}

		list.add(this);
		process = new ProcessBuilder().command(cmd).start();

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
		var command = Arrays.asList(cmd).stream().collect(Collectors.joining(" "));
		return String.format("%s [ %s ]", this.getClass().getSimpleName(), command);
	}
}
