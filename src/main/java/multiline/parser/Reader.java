package multiline.parser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Reader {
	private int inputLimit = Integer.MAX_VALUE;

	public Reader() {

	}

	public Reader(int inputLimit) {
		this.inputLimit = inputLimit;
	}

	public List<Line> read(String path, Charset cs, String tsPattern) throws IOException {
		List<Line> rtv = new ArrayList<Line>();
		Path filePath = Paths.get(path);
		if (Files.exists(filePath)) {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern(tsPattern);
			long i = 0;
			Iterator<String> iterator = Files.lines(filePath, cs).iterator();
			int count = 0;
			while (iterator.hasNext()) {
				if (count++ < inputLimit) {
					String line = iterator.next();
					String[] split = line.split(",");
					rtv.add(new Line(LocalDateTime.parse(split[0].trim(), dtf), i++, split[1].trim()));
					i = i + 1;
				} else
					break;
			}
		}
		return rtv;
	}

	/**
	 * 
	 * @param path
	 * @param cs
	 * @param tsRegPattern ig.(?<year>\\d{4})(?<month>\\d{2})(?<day>\\d{2})-(?<hour>\\d{1,2}):(?<minute>\\d{1,2}):(?<seconds>\\d{1,2}):(?<millis>\\d{1,3})
	 * @return
	 * @throws IOException
	 */
	public List<Line> read(String path, Charset cs, Pattern tsRegPattern) throws IOException {
		List<Line> rtv = new ArrayList<Line>();
		Path filePath = Paths.get(path);
		if (Files.exists(filePath)) {
			long i = 0;
			Iterator<String> iterator = Files.lines(filePath, cs).iterator();
			int count = 0;
			while (iterator.hasNext()) {
				if (count++ < inputLimit) {
					String line = iterator.next();
					String[] split = line.split(",");
					Matcher matcher = tsRegPattern.matcher(split[0]);
					if (matcher.find()) {
						int year = Integer.parseInt(matcher.group("year"));
						int month = Integer.parseInt(matcher.group("month"));
						int day = Integer.parseInt(matcher.group("day"));
						int hour = Integer.parseInt(matcher.group("hour"));
						int minute = Integer.parseInt(matcher.group("minute"));
						int second = Integer.parseInt(matcher.group("seconds"));
						int millis = Integer.parseInt(matcher.group("millis"));
						LocalDateTime dt = LocalDateTime.of(year, month, day, hour, minute, second, millis * 1000000);
						rtv.add(new Line(dt, i++, split[1].trim()));
					}

					i = i + 1;
				} else
					break;
			}
		}
		return rtv;
	}

}
