package multiline.parser;

import java.time.ZoneOffset;

public class Parameters {
	final static public ZoneOffset ZONE_OFF_SET = ZoneOffset.of("+08:00");
//	#控制参数
	static public int MAX_WINDOW_SIZE = 50;// #应该是一个不小于2的整数
	static public int MAX_WINDOW_TS_SPAN = 3;// #seconds

	static public int MIN_OCCURRENCE = 50;// #his=50 os=15 win=50
	static public float MIN_SUPPORT = 0.93f;// #his=0.96 os=0.999 win=99

	static public float GROWTH_FACTOR = 0.01f;// #his=0.02 os=0.02 win=0.01
}
