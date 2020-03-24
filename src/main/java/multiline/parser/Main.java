package multiline.parser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Pattern;

public class Main {

	static public final String LOG_PATH = "LOG.PATH";
	static public final String LOG_CHARSET = "LOG.CHARSET";
	static public final String LOG_TIMESTAMP_PATTERN = "LOG.TIMESTAMP.PATTERN";
	static public final String LOG_TIMESTAMP_REGEX_PATTERN = "LOG.TIMESTAMP.REGEX_PATTERN";
	static public final String ALGO_MAX_WINDOW_SIZE = "ALGO.MAX_WINDOW_SIZE";
	static public final String ALGO_MAX_WINDOW_TS_SPAN = "ALGO.MAX_WINDOW_TS_SPAN";
	static public final String ALGO_MIN_OCCURRENCE = "ALGO.MIN_OCCURRENCE";
	static public final String ALGO_MIN_SUPPORT = "ALGO.MIN_SUPPORT";
	static public final String ALGO_GROWTH_FACTOR = "ALGO.GROWTH_FACTOR";

	static private Integer inputLimit = null;

	private static Printer printer = new Printer();

	private static void exitWithError(String errMsg) {
		System.out.println(errMsg);
		System.exit(1);
	}

	private static void printProps(Properties prop) {
		System.out.println("");
		prop.forEach((k, v) -> {
			if (v != null && v.toString().length() != 0)
				System.out.println(k + ": \t" + v);
		});
		System.out.println("");
	}

	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			exitWithError("Error: No config.prop file provided!");
		} else {
			String configFile = args[0];

			String workDir = System.getProperty("user.dir");
			System.out.println("Current work dir is: " + workDir);

			Path path = Paths.get(configFile);
			if (!Files.exists(path)) {
				Path path2 = Paths.get(workDir + File.separator + configFile);
				if (Files.exists(path2))
					path = path2;
				else
					exitWithError("Error: Cannot find the config file \"" + configFile + "\"");
			}

			System.out.println("Use configs in " + path.toFile().getAbsolutePath());
			Properties properties = new Properties();
			properties.load(Files.newBufferedReader(path));
			printProps(properties);

			String logPath = properties.getProperty(LOG_PATH);
			String logCharset = properties.getProperty(LOG_CHARSET);
			String tsPattern = properties.getProperty(LOG_TIMESTAMP_PATTERN);
			String tsRegPattern = properties.getProperty(LOG_TIMESTAMP_REGEX_PATTERN);

			Parameters.MIN_OCCURRENCE = Integer.valueOf(properties.getProperty(ALGO_MIN_OCCURRENCE));
			Parameters.MIN_SUPPORT = Float.valueOf(properties.getProperty(ALGO_MIN_SUPPORT));
			Parameters.GROWTH_FACTOR = Float.valueOf(properties.getProperty(ALGO_GROWTH_FACTOR));
			Parameters.MAX_WINDOW_SIZE = Integer.valueOf(properties.getProperty(ALGO_MAX_WINDOW_SIZE));
			Parameters.MAX_WINDOW_TS_SPAN = Integer.valueOf(properties.getProperty(ALGO_MAX_WINDOW_TS_SPAN));

			resolveInputLimit(args);

			main0(logPath, logCharset, tsPattern.length() == 0 ? null : tsPattern,
					tsRegPattern.length() == 0 ? null : tsRegPattern, workDir);
		}
	}

	private static void resolveInputLimit(String[] args) {
		if (args.length >= 2) {
			String inputLimit = args[1];
			try {
				Main.inputLimit = Integer.valueOf(inputLimit);
			} catch (Exception e) {
				System.out.println("Invalid INPUT_LIMIT argument setting. Ignore it and set unlimited.");
			}
		}
	}

	private static void main0(String logPath, String logCharset, String tsPattern, String tsRegPattern, String workDir)
			throws IOException {

		Path path = Paths.get(logPath);
		if (!Files.exists(path)) {
			Path path2 = Paths.get(workDir + File.separator + logPath);
			if (Files.exists(path2))
				path = path2;
			else
				exitWithError("Error: " + logPath + " not exist!");
		}

		String cs = logCharset;

		Sample sample = new Sample(new Reader(Main.inputLimit == null ? Integer.MAX_VALUE : Main.inputLimit));
		if (tsRegPattern != null) {
			Pattern pattern = Pattern.compile(tsRegPattern);
			sample.load(path.toFile().getAbsolutePath(), Charset.forName(cs), pattern);
		} else
			sample.load(path.toFile().getAbsolutePath(), Charset.forName(cs), tsPattern);

		printer.printSamplesInfo(sample.getSampleLines());

		long start = System.nanoTime();
		sample.prepare();
		printer.printeSingleBase(sample.getSingleBase());
		sample.stageTwoLines();
		sample.growMore();
		long end = System.nanoTime();
		System.out.println("Run Time:" + (end - start) / 1000000);

		printer.printSpliter();
		System.out.println("After grow, get result below:");
		printer.printResultInfo(sample.getResult());
		printer.printResultDetail(sample.getResult());

	}
}
