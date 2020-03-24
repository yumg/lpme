package multiline.parser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class Sample {

	private Reader reader;

	private List<Line> sampleLines;

	private Map<String, Integer> singleBase;

	private Map<Integer, List<MultiPattern>> result = new LinkedHashMap<>();

	public Map<Integer, List<MultiPattern>> getResult() {
		return result;
	}

	public Sample(Reader reader) {
		this.reader = reader;
	}

	public List<Line> getSampleLines() {
		return sampleLines;
	}

	public Map<String, Integer> getSingleBase() {
		return singleBase;
	}

	public void load(String path, Charset cs, String tsPattern) throws IOException {
		this.sampleLines = reader.read(path, cs, tsPattern);
	}

	public void load(String path, Charset cs, Pattern tsRegPattern) throws IOException {
		this.sampleLines = reader.read(path, cs, tsRegPattern);
	}

	public void prepare() {
		singleBase = new HashMap<>();
		for (Line l : sampleLines) {
			if (!singleBase.containsKey(l.singleTemplate))
				singleBase.put(l.singleTemplate, 1);
			else
				singleBase.put(l.singleTemplate, singleBase.get(l.singleTemplate) + 1);
		}
	}

//	public void prepare(int inputLimit) {
//		int count = 0;
//		singleBase = new HashMap<>();
//		for (Line l : sampleLines) {
//			if (count++ < inputLimit) {
//				if (!singleBase.containsKey(l.singleTemplate))
//					singleBase.put(l.singleTemplate, 1);
//				else
//					singleBase.put(l.singleTemplate, singleBase.get(l.singleTemplate) + 1);
//			} else
//				break;
//		}
//	}

	private List<MultiPattern> collectPairs() {
		Map<String, MultiPattern> idxCache = new HashMap<>();
		List<MultiPattern> rtv = new ArrayList<MultiPattern>();
		for (int i = 0; i < sampleLines.size() - 1; i++) {
			Line l1 = sampleLines.get(i);
			Line l2 = sampleLines.get(i + 1);
			long ts1 = l1.timestamp.toEpochSecond(Parameters.ZONE_OFF_SET);
			long ts2 = l2.timestamp.toEpochSecond(Parameters.ZONE_OFF_SET);
			if (ts2 - ts1 < Parameters.MAX_WINDOW_TS_SPAN) {
				String pairStr = l1.singleTemplate + "," + l2.singleTemplate;
				if (idxCache.containsKey(pairStr))
					idxCache.get(pairStr).count(new Line[] { l1, l2 });
				else {
					MultiPattern multiPattern = new MultiPattern(new Line[] { l1, l2 });
					idxCache.put(pairStr, multiPattern);
					rtv.add(multiPattern);
				}
			}
		}
		return rtv;
	}

	private List<MultiPattern> evaluatePairPattern() {
		List<MultiPattern> candidates = collectPairs();
		Map<String, Set<Long>> templateLines = new HashMap<>();
		Map<String, Integer> templateLinesCardinality = new HashMap<>();
		for (int i = candidates.size() - 1; i >= 0; i--) {
			MultiPattern candidate = candidates.get(i);
			if (candidate.getOccurrence() < Parameters.MIN_OCCURRENCE) {
				candidates.remove(i);
				continue;
			} else {
				candidate.lineNumIdx.forEach((lineTemplate, lineNums) -> {
					if (templateLines.containsKey(lineTemplate)) {
						Set<Long> lnSet = templateLines.get(lineTemplate);
						lnSet.addAll(lineNums);
						templateLinesCardinality.put(lineTemplate, lnSet.size());
					} else {
						templateLines.put(lineTemplate, lineNums);
						templateLinesCardinality.put(lineTemplate, lineNums.size());
					}
				});
			}
		}

		for (int i = candidates.size() - 1; i >= 0; i--) {
			MultiPattern candidate = candidates.get(i);
			float support = 0;

			for (String lineTemplate : candidate.singleTemplateSequences)
				support += new Float(candidate.getOccurrence()) / singleBase.get(lineTemplate).floatValue();
			support = support / 2;

			if (support < Parameters.MIN_SUPPORT)
				candidates.remove(i);
			else
				candidate.support = support;
		}
		return candidates;
	}

	/**
	 * 在这里就已经过滤了一次环相等
	 */
	public void stageTwoLines() {
		List<MultiPattern> secondLayer = this.evaluatePairPattern();
		filterLoops(secondLayer);
		result.put(new Integer(2), secondLayer);
	}

	/**
	 * <p>
	 * 1 过滤环相等
	 * </p>
	 * <p>
	 * 2 前后合并
	 * </p>
	 * <ul>
	 * <li>ab->abc</li>
	 * <li>xy<-xyz</li>
	 * <li>23->123</li>
	 * <li>vw<-uvw</li>
	 * </ul>
	 */
	public void growMore() {
		int support = 2;
		while (support < Parameters.MAX_WINDOW_SIZE && result.containsKey(new Integer(support))
				&& result.get(new Integer(support)).size() > 0) {
			int target = support + 1;
			List<MultiPattern> kLayerPattern = evaluateKLayerPattern(
					collectKLayer(target, result.get(new Integer(support))));
			filterLoops(kLayerPattern);
			result.put(new Integer(target), kLayerPattern);
			merge(result.get(support), result.get(support + 1));
			support++;
		}
	}

	private List<MultiPattern> collectKLayer(int k, List<MultiPattern> supportLayer) {
		Map<String, MultiPattern> idx = new HashMap<>();
		List<MultiPattern> rtv = new ArrayList<>();
		for (int i = 0; i < sampleLines.size() - k + 1; i++) {
			Line line = sampleLines.get(i);
			long ts0 = line.timestamp.toEpochSecond(Parameters.ZONE_OFF_SET);
			String multiStr = line.singleTemplate;
			int j = 1;
			while (j < k && (sampleLines.get(i + j).timestamp.toEpochSecond(Parameters.ZONE_OFF_SET)
					- ts0 < Parameters.MAX_WINDOW_TS_SPAN)) {
				multiStr += ("," + sampleLines.get(i + j++).singleTemplate);
			}

			if (j == k) {
				if (idx.containsKey(multiStr)) {
					idx.get(multiStr).count(sampleLines, i, j);
				} else {
					for (MultiPattern s : supportLayer) {
						if (!s.invalid && s.canSupportLines(sampleLines, i, j)) {
							MultiPattern multiPattern = new MultiPattern(sampleLines, i, j);
							multiPattern.supportBy = s;
							idx.put(multiStr, multiPattern);
							rtv.add(multiPattern);
							break;
						}
					}
				}
			}
		}
		return rtv;
	}

	private List<MultiPattern> evaluateKLayerPattern(List<MultiPattern> candidates) {
		Map<String, Set<Long>> templateLines = new HashMap<>();
		Map<String, Integer> templateLinesCardinality = new HashMap<>();
		for (int i = candidates.size() - 1; i >= 0; i--) {
			MultiPattern candidate = candidates.get(i);
			if (candidate.getOccurrence() < Parameters.MIN_OCCURRENCE) {
				candidates.remove(i);
				continue;
			} else {
				candidate.lineNumIdx.forEach((lineTemplate, lineNums) -> {
					if (templateLines.containsKey(lineTemplate)) {
						Set<Long> lnSet = templateLines.get(lineTemplate);
						lnSet.addAll(lineNums);
						templateLinesCardinality.put(lineTemplate, lnSet.size());
					} else {
						templateLines.put(lineTemplate, lineNums);
						templateLinesCardinality.put(lineTemplate, lineNums.size());
					}
				});
			}
		}

		for (int i = candidates.size() - 1; i >= 0; i--) {
			MultiPattern candidate = candidates.get(i);
			String ext = candidate.singleTemplateSequences.get(candidate.singleTemplateSequences.size() - 1);
			float extSupport = new Float(candidate.getOccurrence()) / singleBase.get(ext).floatValue();
			candidate.support = (candidate.supportBy.support + extSupport) / 2;
			if (candidate.support < Parameters.MIN_SUPPORT)
				candidates.remove(i);
			else
				candidate.supportBy.supports.add(candidate);

		}

		return candidates;
	}

	private void filterLoops(List<MultiPattern> candidates) {
		for (int i = 0; i < candidates.size(); i++) {
			MultiPattern mp1 = candidates.get(i);
			for (int j = i + 1; j < candidates.size(); j++) {
				MultiPattern mp2 = candidates.get(j);
				if (mp1.shiftEqual(mp2)) {
					if (mp1.support > mp2.support) {
						mp2.invalid = true;
						mp2.invalidCause = mp1;
					} else {
						mp1.invalid = true;
						mp1.invalidCause = mp2;
					}
				}
			}
		}
	}

	private void merge(List<MultiPattern> kLayer, List<MultiPattern> k1Layer) {

		// ab.count =10
		// abc.count =5
		// abd.count = 5
		// then delete ab
		//
		// ab.count =10
		// abc.count =2
		// abd.count = 3
		// then delete abc & abd
		//

		Map<MultiPattern, Integer> supportsCountM = new HashMap<MultiPattern, Integer>();
		for (MultiPattern mp1 : k1Layer) {
			MultiPattern mp = mp1.supportBy;
			if (supportsCountM.containsKey(mp)) {
				Integer c = supportsCountM.get(mp);
				supportsCountM.put(mp, new Integer(c + mp1.getOccurrence()));
			} else {
				supportsCountM.put(mp, new Integer(mp1.getOccurrence()));
			}
		}

		supportsCountM.forEach((key, value) -> {
			int diff = Math.abs(key.getOccurrence() - value);
			if (new Float(diff) / new Float(key.getOccurrence()) < Parameters.GROWTH_FACTOR)
//				if (key.getOccurrence() > (new Float(value) * 0.99)
//						&& key.getOccurrence() < (new Float(value) * (2 - 0.99)))
				key.invalid = true;
			else
				key.supports.forEach(m -> {
					m.invalid = true;
					m.invalidCause = key;
				});
		});

		// ab.count =10
		// 1ab.count =5
		// 2ab.count = 5
		// then delete ab
		//
		// ab.count =10
		// 1ab.count =2
		// 2ab.count = 3
		// then delete 1ab & 2ab
		//

		for (int i = kLayer.size() - 1; i >= 0; i--) {
			MultiPattern kc = kLayer.get(i);
			List<MultiPattern> k1Ref = new ArrayList<MultiPattern>();
			int count = 0;
			for (MultiPattern k1c : k1Layer) {
				int j = 0;
				for (; j < kc.singleTemplateSequences.size(); j++) {
					if (!kc.singleTemplateSequences.get(j).equals(k1c.singleTemplateSequences.get(j + 1)))
						break;
				}
				if (j == kc.singleTemplateSequences.size()) {
					count += k1c.getOccurrence();
					k1Ref.add(k1c);
				}
			}
//			if (count > (new Float(kc.getOccurrence()) * 0.99) && count < (new Float(kc.getOccurrence()) * (2 - 0.99)))
			int diff = Math.abs(kc.getOccurrence() - count);
			if (new Float(diff) / new Float(kc.getOccurrence()) < Parameters.GROWTH_FACTOR)
				kc.invalid = true;
			else
				k1Ref.forEach(k1p -> {
					k1p.invalid = true;
					k1p.invalidCause = kc;
				});

		}

	}

}
