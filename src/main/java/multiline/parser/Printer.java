package multiline.parser;

import java.util.List;
import java.util.Map;

public class Printer {
	public void printSamplesInfo(List<Line> sampleLines) {
		printSpliter();
		System.out.println("SampleLines total line num: " + sampleLines.size());

	}

	public void printResultInfo(Map<Integer, List<MultiPattern>> result) {
		printSpliter();
		System.out.println("Result info:");
		result.forEach((key, value) -> {
			System.out.println(key + " layer: " + value.size() + " results");
		});
	}

	public void printeSingleBase(Map<String, Integer> singleBase) {
		printSpliter();
		System.out.println("Single Base Info: " + singleBase.size() + " contents");

	}

	public void printSpliter() {
		System.out.println("=========================================");
	}

	public void printResultDetail(Map<Integer, List<MultiPattern>> result) {
		printSpliter();
		System.out.println("Result detail:");
		System.out.println("Layer\tSupport\tOccurrence\tContents");
		result.forEach((key, value) -> {
			value.forEach(mp -> {
				if (!mp.invalid)
					System.out.print(key + "\t" + mp.support + "\t" + mp.getOccurrence() + "\t"
							+ String.join(",", mp.singleTemplateSequences) + "\n");
			});
			System.out.print("###\t###\t###\t###\n");
		});

	}
}
