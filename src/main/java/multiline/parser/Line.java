package multiline.parser;

import java.time.LocalDateTime;

public class Line {
	public LocalDateTime timestamp;
	public String singleTemplate;
	public long lineNum;

	public Line(LocalDateTime ts, long lineNum, String singleTemplate) {
		this.timestamp = ts;
		this.lineNum = lineNum;
		this.singleTemplate = singleTemplate;
	}

	

}
