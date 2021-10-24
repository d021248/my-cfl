package d021248.cfl.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

class CfCommand extends Command {

	private static Pattern targetPattern = Pattern.compile("^\\S+:\\s+(\\S+)$");

	private static Pattern appsPattern = Pattern
			.compile("^(\\S+)\\s+(\\S+)\\s+(\\d+/\\d+)\\s+(\\d+\\S+)\\s+(\\d+\\S+)\\s?(.*)$");
	private final static String CF = System.getProperty("CFL", "cf");
	private static Consumer<String> outLogger = System.out::println;
	private static Consumer<String> errLogger = System.err::println;

	private CfCommand(String... cmd) {
		super(cmd);
	}

	@Override
	public int start() {
		try {
			CfCommand.errLogger.accept(this.toString());
			return super.start();
		} catch (IOException | InterruptedException e) {
			CfCommand.errLogger.accept(String.format("Error: %s", e.getMessage()));
		}
		return -1;
	}

	public static Target target() {

		var attrList = new ArrayList<String>();
		Consumer<InputStream> getTarget = is -> {
			String line;
			int i = 0;
			try (var br = new BufferedReader(new InputStreamReader(is))) {
				while ((line = br.readLine()) != null) {

					var matcher = targetPattern.matcher(line);
					if (matcher.matches()) {
						attrList.add(matcher.group(1));
					}
				}
			} catch (IOException e) {
				CfCommand.errLogger.accept(String.format("Error: %s", e.getMessage()));
			}
		};

		var command = new CfCommand("cf", "target");
		command.sync().in(getTarget).err(CfCommand::toErrLogger);
		command.start();
		return new Target(attrList.get(0), attrList.get(2), attrList.get(2), attrList.get(3), attrList.get(4));
	}

	public static List<App> apps() {

		var appList = new ArrayList<App>();
		Consumer<InputStream> getAppList = is -> {
			String line;
			try (var br = new BufferedReader(new InputStreamReader(is))) {
				while ((line = br.readLine()) != null) {
					var matcher = appsPattern.matcher(line);
					if (matcher.matches()) {
						var app = new App(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4),
								matcher.group(5), matcher.group(6));
						appList.add(app);
					}
				}
			} catch (IOException e) {
				CfCommand.errLogger.accept(String.format("Error: %s", e.getMessage()));
			}
			Collections.sort(appList, (l, r) -> l.name.compareTo(r.name));
		};

		var command = new CfCommand("cf", "apps");
		command.sync().in(getAppList).err(CfCommand::toErrLogger);
		command.start();
		return appList;
	}

	public static CfCommand logs(String app) {
		var command = new CfCommand("cf", "logs", app);
		command.async().in(CfCommand::toOutLogger).err(CfCommand::toErrLogger);
		command.start();
		return command;
	}

	public static CfCommand env(String app) {
		var command = new CfCommand("cf", "env", app);
		command.async().in(CfCommand::toOutLogger).err(CfCommand::toErrLogger);
		command.start();
		return command;
	}

	private static void toOutLogger(InputStream is) {
		toLogger(is, CfCommand.outLogger);
	}

	private static void toErrLogger(InputStream is) {
		toLogger(is, CfCommand.errLogger);
	}

	private static void toLogger(InputStream is, Consumer<String> logger) {

		String line;
		try (var br = new BufferedReader(new InputStreamReader(is))) {
			while ((line = br.readLine()) != null) {
				if (!line.isEmpty()) {
					logger.accept("-------" + line.trim());
				}
			}
		} catch (IOException e) {
			CfCommand.errLogger.accept(String.format("Error: %s", e.getMessage()));
		}
	}
}
