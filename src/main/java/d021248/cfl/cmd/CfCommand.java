package d021248.cfl.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

	public static List<Application> apps() {
		var command = new CfCommand("cf", "apps");
		var appList = new ArrayList<Application>();
		Consumer<InputStream> toApplicationList = is -> getApplicationList(is, appList);

		command.sync().in(toApplicationList).err(CfCommand::toLogger);
		command.start();
		return appList;
	}

	private static List<Application> getApplicationList(InputStream is, ArrayList<Application> appList) {
		final String APPS_FOUND_INDICATOR = "^name.*requested.*state.*instances.*memory.*disk.*urls.*";
		String line;
		var isApp = false;
		try (var br = new BufferedReader(new InputStreamReader(is))) {
			while ((line = br.readLine()) != null) {
				CfCommand.logger.accept(line);
				if (isApp) {
					var app = new Application();
					var a = line.split("\\s+");
					app.setName(saveGet(a, 0));
					app.setState(saveGet(a, 1));
					app.setInstances(saveGet(a, 2));
					app.setMemory(saveGet(a, 3));
					app.setDisk(saveGet(a, 4));
					app.setUrls(saveGet(a, 5));
					appList.add(app);
				}
				if (line.matches(APPS_FOUND_INDICATOR)) {
					isApp = true;
				}
			}
		} catch (IOException e) {

			CfCommand.logger.accept("Error: " + e.getMessage());
		}
		Collections.sort(appList, (l, r) -> l.getName().compareTo(r.getName()));
		return appList;
	}

	private static String saveGet(String[] a, int i) {
		return (a == null || i >= a.length || i < 0) ? "" : a[i];
	}

	public static CfCommand logs(String app) {
		var command = new CfCommand("cf", "logs", app);
		command.async().in(CfCommand::toLogger).err(CfCommand::toLogger);
		command.start();
		return command;
	}

	private static void toLogger(InputStream is) {

		String line;
		try (var br = new BufferedReader(new InputStreamReader(is))) {
			while ((line = br.readLine()) != null) {
				if (!line.isEmpty()) {
					logger.accept(line.trim());
				}
			}
		} catch (IOException e) {
			logger.accept(String.format("Error: %s", e.getMessage()));
		}
	}
}
