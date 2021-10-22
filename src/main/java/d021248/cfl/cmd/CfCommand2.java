package d021248.cfl.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class CfCommand2 extends Command {

	private static final int MAX_NAME_LENGTH = 23;
	private static Logger LOGGER = System.out::println;
	private final static String CF = System.getProperty("CF_LOGGER", "cf");

	private CfCommand(String commandString) {
		super(cleanupCfCommandString(commandString));
	}

	private static String cleanupCfCommandString(String s) {
		return s.trim().replace("  ", " ").replace("cf", CF);
	}

	private static String adjustStringLength(String s, int length) {
		String name = s.trim();
		while (name.length() < length) {
			name = name + " ";
		}
		return s;
	}

	// --------------------------------------------------------------------------------------------
	//
	// --------------------------------------------------------------------------------------------
	public static void logCommand(String commandString) {
		LOGGER.log(">" + commandString);
		String tmp = "";
		if (commandString.startsWith("cf logs")) {
			tmp = commandString.substring("cf logs".length()) + " ";
			tmp = String.format("%-20s", tmp);
		}
		final String prefix = tmp;
		new Thread(
				() -> new CfCommand(commandString).execute(is -> CfCommand.toLogger(prefix, is), CfCommand::toLogger))
						.start();
	}

	private static Void toLogger(InputStream is) {
		return toLogger("", is);
	}

	private static Void toLogger(String prefix, InputStream is) {
		String line;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			while ((line = br.readLine()) != null) {
				if (line.length() > 1) {
					line = (prefix + " " + line).trim();
					LOGGER.log(line);
				}
			}
		} catch (IOException e) {
			LOGGER.log("Error: " + e.getMessage());
		}
		return null;
	}

	// --------------------------------------------------------------------------------------------
	final static String APPS_FOUND_INDICATOR = "^name.*requested.*state.*instances.*memory.*disk.*urls.*";

	public static List<Application> getApplicationList() {
		return new CfCommand("cf a").execute(CfCommand::getApplicationList, CfCommand::toLogger);
	}

	private static List<Application> getApplicationList(InputStream is) {
		String line;
		boolean isApp = false;
		List<Application> apps = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			while ((line = br.readLine()) != null) {
				// LOGGER.log(line);
				if (isApp) {
					Application app = new Application();
					String[] a = line.split("\\s+");
					app.setName(saveGet(a, 0));
					app.setState(saveGet(a, 1));
					app.setInstances(saveGet(a, 2));
					app.setMemory(saveGet(a, 3));
					app.setDisk(saveGet(a, 4));
					app.setUrls(saveGet(a, 5));
					apps.add(app);
				}
				if (line.matches(APPS_FOUND_INDICATOR)) {
					isApp = true;
				}
			}
		} catch (IOException e) {
			LOGGER.log("Error: " + e.getMessage());
		}
		Collections.sort(apps, (l, r) -> l.getName().compareTo(r.getName()));
		return apps;
	}

	private static String saveGet(String[] a, int i) {
		return (a == null || i >= a.length || i < 0) ? "" : a[i];
	}

	static boolean isActive = false;

	public static void unlogApplication(String applicationName) {
		getCommandList().stream().filter(command -> command.getCommandString().startsWith("cf logs "))
				.filter(command -> command.getCommandString().contains(applicationName))
				.forEach(command -> command.destroy());
	}

	public static void logApplication(String applicationName) {
		CfCommand.logCommand("cf logs " + adjustStringLength(applicationName, MAX_NAME_LENGTH));
	}

	public static void logApplicationList() {
		unlogApplicationList();
		getApplicationList().forEach(application -> CfCommand.logApplication(application.getName()));
	}

	public static void unlogApplicationList() {
		getCommandList().stream().filter(command -> command.getCommandString().startsWith("cf logs "))
				.forEach(command -> command.destroy());
	}

	public static void setLogger(Logger logger) {
		LOGGER = logger;
	}

}
