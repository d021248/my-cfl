package d021248.cfl.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class Cf {

	private static final Pattern TARGET_PATTERN = Pattern.compile("^\\S+:\\s+(\\S+)$");
	private static final Pattern APPS_PATTERN = Pattern
			.compile("^(\\S+)\\s+(\\S+)\\s+(\\d+/\\d+)\\s+(\\d+\\S+)\\s+(\\d+\\S+)\\s?(.*)$");
	private static final String CF = System.getProperty("CFL", "cf");
	private static Consumer<String> outLogger = System.out::println;
	private static Consumer<String> errLogger = System.err::println;

	private Cf() {

	}

	public static Command cmd(String... cmd) {
		return Command.cmd(cmd).sync().in(Cf::toOutLogger).err(Cf::toErrLogger);
	}

	public static void setOutLogger(Consumer<String> logger) {
		Cf.outLogger = logger;
	}

	public Consumer<String> getOutLogger() {
		return Cf.outLogger;
	}

	public static void setErrLogger(Consumer<String> logger) {
		Cf.errLogger = logger;
	}

	public Consumer<String> getErrLogger() {
		return Cf.errLogger;
	}

	public static Target target() {
		var attrList = new ArrayList<String>();

		// @formatter:off
		Consumer<InputStream> getTarget = is -> {
			try (var bufferedReader = new BufferedReader(new InputStreamReader(is))) {
				bufferedReader
					.lines()
					.map(TARGET_PATTERN::matcher)
					.filter(Matcher::matches)
					.map(matcher -> matcher.group(1))
					.forEach(attrList::add);
			} catch (IOException e) {
				Cf.errLogger.accept(String.format("Error: %s", e.getMessage()));
			}
		};
		// @formatter:on

		Command.cmd("cf", "target").in(getTarget).err(Cf::toErrLogger).start();
		return new Target(attrList.get(0), attrList.get(2), attrList.get(2), attrList.get(3), attrList.get(4));
	}

	public static List<App> apps() {
		var appList = new ArrayList<App>();

		// @formatter:off
		Consumer<InputStream> getAppList = is -> {
			try (var bufferedReader = new BufferedReader(new InputStreamReader(is))) {
				bufferedReader
					.lines()
					.map(APPS_PATTERN::matcher)
					.filter(Matcher::matches)
					.map(matcher -> new App(matcher.group(1), 
											matcher.group(2), 
											matcher.group(3), 
											matcher.group(4),
											matcher.group(5), 
											matcher.group(6)))
					.forEach(appList::add);
			} catch (IOException e) {
				Cf.errLogger.accept(String.format("Error: %s", e.getMessage()));
			}
			Collections.sort(appList, (l, r) -> l.name.compareTo(r.name));
		};
		// @formatter:on

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

		// @formatter:off
		Consumer<InputStream> getEnc = is -> {
			String result = null;
			try (var bufferedReader = new BufferedReader(new InputStreamReader(is))) {
				result = bufferedReader
						.lines()
						.dropWhile(line -> !line.equals("{"))
						.takeWhile(line -> !line.equals("}"))
						.collect(Collectors.joining("\n", "", "\n}"));
			} catch (IOException e) {
				Cf.errLogger.accept(String.format("Error: %s", e.getMessage()));
			}

			if (result == null || result.equals("\n}")) {
				result = "{}";
			}

			env[0] = result;
		};
		// @formatter:on

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
		// @formatter:off
		try (var bufferedReader = new BufferedReader(new InputStreamReader(is))) {
			bufferedReader
				.lines()
				.filter(Predicate.not(String::isEmpty))
				.map(String::trim)
				.forEach(logger::accept);
		} catch (IOException e) {
			Cf.errLogger.accept(String.format("Error: %s", e.getMessage()));
		}
		// @formatter:on
	}
}
