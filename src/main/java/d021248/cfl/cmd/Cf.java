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
import java.util.stream.Collectors;

class Cf {

	private static Pattern targetPattern = Pattern.compile("^\\S+:\\s+(\\S+)$");

	private static Pattern appsPattern = Pattern
			.compile("^(\\S+)\\s+(\\S+)\\s+(\\d+/\\d+)\\s+(\\d+\\S+)\\s+(\\d+\\S+)\\s?(.*)$");
	private final static String CF = System.getProperty("CFL", "cf");
	private static Consumer<String> outLogger = System.out::println;
	private static Consumer<String> errLogger = System.err::println;

	private Cf() {

	}

	public static Target target() {

		var attrList = new ArrayList<String>();
		Consumer<InputStream> getTarget = is -> {
			String line;
			try (var br = new BufferedReader(new InputStreamReader(is))) {
				while ((line = br.readLine()) != null) {
					var matcher = targetPattern.matcher(line);
					if (matcher.matches()) {
						attrList.add(matcher.group(1));
					}
				}
			} catch (IOException e) {
				Cf.errLogger.accept(String.format("Error: %s", e.getMessage()));
			}
		};

		Command.cmd("cf", "target").in(getTarget).err(Cf::toErrLogger).start();
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
				Cf.errLogger.accept(String.format("Error: %s", e.getMessage()));
			}
			Collections.sort(appList, (l, r) -> l.name.compareTo(r.name));
		};

		Command.cmd("cf", "apps").in(getAppList).err(Cf::toErrLogger).start();
		return appList;
	}

	public static void logs() {
		Cf.apps().stream().forEach(app -> Cf.logs(app.name));
	}

	public static void logs(String appName) {
		Command.cmd("cf", "logs", appName).async().in(Cf::toOutLogger).err(Cf::toErrLogger).start();
	}

	public static String env(String app) {
		var env = new String[1];
		Consumer<InputStream> getEnc = is -> {
			env[0] = new BufferedReader(new InputStreamReader(is)).lines().dropWhile(line -> !line.equals("{"))
					.takeWhile(line -> !line.equals("}")).collect(Collectors.joining("\n", "", "\n}"));
			if (env[0].equals("\n}")) {
				env[0] = "{}";
			}
		};
		Command.cmd("cf", "env", app).in(getEnc).err(Cf::toErrLogger).start();
		return env[0];
	}

	private static void toOutLogger(InputStream is) {
		toLogger(is, Cf.outLogger);
	}

	private static void toErrLogger(InputStream is) {
		toLogger(is, Cf.errLogger);
	}

	private static void toLogger(InputStream is, Consumer<String> logger) {

		String line;
		try (var br = new BufferedReader(new InputStreamReader(is))) {
			while ((line = br.readLine()) != null) {
				if (!line.isEmpty()) {
					logger.accept(line.trim());
				}
			}
		} catch (IOException e) {
			Cf.errLogger.accept(String.format("Error: %s", e.getMessage()));
		}
	}
}
