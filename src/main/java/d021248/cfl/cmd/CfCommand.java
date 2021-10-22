package d021248.cfl.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

class CfCommand extends Command {

	private final static String CF = System.getProperty("CFL", "cf");
	private static Consumer<String> logger = System.out::println;

	private CfCommand(String... cmd) {
		super(cmd);
	}

	@Override
	public int start() {
		try {
			return super.start();
		} catch (IOException | InterruptedException e) {
			logger.accept(String.format("Error: %s", e.getMessage()));
		}
		return -1;
	}

	public static CfCommand apps() {
		var command = new CfCommand("cf", "apps");
		command.sync().in(CfCommand::toLogger).err(CfCommand::toLogger);
		command.start();
		return command;
	}

	public static CfCommand logs(String app) {
		var command = new CfCommand("cf", "logs", app);
		command.async().in(CfCommand::toLogger).err(CfCommand::toLogger);
		command.start();
		return command;
	}

	private static void toLogger(InputStream is) {
		String prefix = ">";
		String line;
		try (var br = new BufferedReader(new InputStreamReader(is))) {
			while ((line = br.readLine()) != null) {
				if (!line.isEmpty()) {
					logger.accept(String.format("%s %s", prefix, line.trim()));
				}
			}
		} catch (IOException e) {
			logger.accept(String.format("Error: %s", e.getMessage()));
		}
	}
}
