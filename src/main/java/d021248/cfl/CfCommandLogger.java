package d021248.cfl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import d021248.cfl.cmd.Command;

public class CfCommandLogger {

	private final static String CF = System.getProperty("CF_LOGGER", "cf");

	public static void logCommand(Logger logger, String commandString) {		
		new Thread(() -> logCommandSync(logger, commandString)).start();
	}
	
	public static void logCommandSync(Logger logger, String commandString) {
		String command = commandString.trim().replace("  ", " ").replace("cf", CF);
		String appName = command.startsWith("cf logs") ? String.format("%-20s", command.substring("cf logs".length()) + " ") : "";
		logger.log(">" + command);
		new Command(command).execute(is -> toLogger(logger, appName, is), is -> toLogger(logger, "error: ", is));
	}

	private static Void toLogger(Logger logger, String prefix, InputStream is) {
		String line;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			while ((line = br.readLine()) != null) {
				if (line.length() > 0) {
					line = (prefix + " " + line);//.trim();
					logger.log(line);
				}
			}
		} catch (IOException e) {
			logger.log("error: " + e.getMessage());
		}
		return null;
	}


	
	// --------------------------------------------------------------------------------------------
	// log application list
	// --------------------------------------------------------------------------------------------
		final static String APPS_FOUND_INDICATOR = "^name.*requested.*state.*instances.*memory.*disk.*urls.*";

		public static List<Application> getApplicationList(Logger logger) {
			return new Command("cf a").execute(is->getApplicationList(logger, is), is->toLogger(logger, "error: ", is));
		}

		private static List<Application> getApplicationList(Logger logger, InputStream is) {
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
				logger.log("Error: " + e.getMessage());
			}
			Collections.sort(apps, (l, r) -> l.getName().compareTo(r.getName()));
			return apps;
		}

		private static String saveGet(String[] a, int i) {
			return (a == null || i >= a.length || i < 0) ? "" : a[i];
		}

		public static void logApplication(Logger logger, String appName) {
			logCommand(logger, "cf logs " + adjustStringLength(appName, MAX_NAME_LENGTH));			
		}

		public static void unlogApplication(String appName) {
			Command.getCommandList().stream().filter(command -> command.getCommandString().startsWith("cf logs ")).filter(command -> command.getCommandString().contains(appName)).forEach(command -> command.destroy());
			
		}

		public static void unlogApplicationList() {
			Command.getCommandList().stream().filter(command -> command.getCommandString().startsWith("cf logs ")).forEach(command -> command.destroy());			
		}
		
		public static void logApplicationList(Logger logger) {
			unlogApplicationList();
			getApplicationList(logger).forEach(application -> logApplication(logger, application.getName()));
		}

		private static final int MAX_NAME_LENGTH = 23;
		private static String adjustStringLength(String s, int length) {		
			String name = s.trim();
			while (name.length() < length) {
				name = name + " ";
			}
			return s;
		}

}
