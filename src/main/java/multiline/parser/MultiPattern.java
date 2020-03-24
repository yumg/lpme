package multiline.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MultiPattern {
	private int occurrence = 0;
	public List<String> singleTemplateSequences = new ArrayList<String>();
	public Map<String, Set<Long>> lineNumIdx = new HashMap<>();
	public float support = 0;
	public MultiPattern supportBy;
	public List<MultiPattern> supports = new ArrayList<MultiPattern>();

	public boolean invalid = false;
	public MultiPattern invalidCause = null;

	public MultiPattern(Line[] lines) {
		for (Line line : lines) {
			this.singleTemplateSequences.add(line.singleTemplate);
			if (this.lineNumIdx.containsKey(line.singleTemplate))
				lineNumIdx.get(line.singleTemplate).add(new Long(line.lineNum));
			else {
				HashSet<Long> hashSet = new HashSet<Long>();
				hashSet.add(new Long(line.lineNum));
				lineNumIdx.put(line.singleTemplate, hashSet);
			}
		}
		this.occurrence++;
	}

	public MultiPattern(List<Line> lines, int start, int offset) {
		for (int i = 0; i < offset; i++) {
			Line line = lines.get(start + i);
			this.singleTemplateSequences.add(line.singleTemplate);
			if (this.lineNumIdx.containsKey(line.singleTemplate))
				lineNumIdx.get(line.singleTemplate).add(new Long(line.lineNum));
			else {
				HashSet<Long> hashSet = new HashSet<Long>();
				hashSet.add(new Long(line.lineNum));
				lineNumIdx.put(line.singleTemplate, hashSet);
			}
		}
		this.occurrence++;
	}

	public void count(Line[] lines) {
		this.occurrence++;
		for (Line l : lines)
			this.lineNumIdx.get(l.singleTemplate).add(l.lineNum);

	}

	public void count(List<Line> lines, int start, int offset) {
		this.occurrence++;
		for (int i = 0; i < offset; i++) {
			Line l = lines.get(start + i);
			this.lineNumIdx.get(l.singleTemplate).add(l.lineNum);
		}

	}

	public boolean canSupportLines(Line[] lines) {
		for (int i = 0; i < lines.length - 1; i++) {
			String lineTemplate = lines[i].singleTemplate;
			if (lineTemplate != null && lineTemplate.equals(singleTemplateSequences.get(i)))
				continue;
			else
				return false;
		}
		return true;
	}

	public boolean canSupportLines(List<Line> lines, int start, int offset) {
		for (int i = 0; i < offset - 1; i++) {
			String lineTemplate = lines.get(start + i).singleTemplate;
			if (lineTemplate != null && lineTemplate.equals(singleTemplateSequences.get(i)))
				continue;
			else
				return false;
		}
		return true;
	}

	public String toString() {
		return String.join(",", this.singleTemplateSequences);
	}

	public int getOccurrence() {
		return this.occurrence;
	}

	public boolean shiftEqual(MultiPattern m) {
		if (this.singleTemplateSequences.containsAll(m.singleTemplateSequences)
				&& m.singleTemplateSequences.containsAll(this.singleTemplateSequences)) {
			String target = m.toString();
			int l = this.singleTemplateSequences.size();
			for (int i = 1; i < l; i++) {
				String part1 = String.join(",", this.singleTemplateSequences.subList(i, l));
				String part2 = String.join(",", this.singleTemplateSequences.subList(0, i));
				String t = part1 + "," + part2;
				if (t.equals(target))
					return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
}
