package nlp;

import java.util.Date;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.time.*;
import edu.stanford.nlp.time.SUTime.Temporal;
import edu.stanford.nlp.time.SUTime.Time;
import edu.stanford.nlp.util.CoreMap;

public class TextProcessor {
	List<AnnotationPipeline> pieplines;

	public TextProcessor() {
		pieplines = new ArrayList<AnnotationPipeline>();
		Properties props1 = new Properties();
		initializePipeline(props1);
		Properties props2 = new Properties();
		props2.setProperty("sutime.binders", "0");
		props2.setProperty("sutime.markTimeRanges", "true");
		props2.setProperty("sutime.includeRange", "true");
		initializePipeline(props2);		
	}

	private void initializePipeline(Properties props) {
		AnnotationPipeline pipeline = new AnnotationPipeline();
		pipeline.addAnnotator(new TokenizerAnnotator(false));
		pipeline.addAnnotator(new WordsToSentencesAnnotator(false));
		pipeline.addAnnotator(new POSTaggerAnnotator(false));
		pipeline.addAnnotator(new TimeAnnotator("sutime", props));
		pieplines.add(pipeline);
	}

	public String[] parse(String text) {
		TreeSet<Temporal> has_seen = new TreeSet<Temporal>(new TemporalComparator());
		TreeSet<Time> times = new TreeSet<Time>();
		for (AnnotationPipeline pipeline : pieplines) {
			Annotation annotation = new Annotation(text);
			annotation.set(CoreAnnotations.DocDateAnnotation.class,
					new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
			pipeline.annotate(annotation);
			List<CoreMap> timexAnnsAll = annotation.get(TimeAnnotations.TimexAnnotations.class);
			for (CoreMap cm : timexAnnsAll) {
				Temporal temporal = cm.get(TimeExpression.Annotation.class).getTemporal();
				temporal.getTime();
				if (has_seen.contains(temporal)) {
					continue;
				}
				has_seen.add(temporal);
				if (temporal.getTimexType().name().equals("TIME") || temporal.getTimexType().name().equals("DATE")) {
					if (temporal.getTime() != null) {
						try {
							times.add(temporal.getTime());
						} catch (NullPointerException e) {}
					}
				}
				//								System.out.println("@@@@@");
				//								System.out.println("Token text : " + cm);
				//								System.out.println("Temporal Value : " + temporal);
				//								System.out.println("Timex type : " + temporal.getTimexType().name());
				//								try { 
				//									System.out.println("Timex range : " + temporal.getRange());
				//								} catch (Exception e) {
				//									System.out.println("Timex range : N/A");
				//								}

			}
		}
		if (times.size() >= 2) {
			return new String[] {regexNormalize(Collections.min(times).toString(), 0),
					regexNormalize(Collections.max(times).toString(), 1)};
		} 
		for (Temporal temporal : has_seen) {
			if (isReadbleTime(temporal.getRange().toString())) {
				System.out.println("### Result " + temporal.getRange());
				List<String> string_list = Arrays.asList(temporal.getRange().toString().split(","));
				String s1 = regexNormalize(string_list.get(0), 0);
				String s2 = regexNormalize(string_list.get(1), 1);
				if (s1.equals(s2)) {
					if (text.contains("from") || text.contains("start") || text.contains("begin")) {
						s2 = null;
					} else if (text.contains("to") || text.contains("until") || text.contains("end")) {
						s1 = null;
					}
				}
				return new String[] {s1, s2};
			}
		}
		return new String[] {"upcoming 10 events", "upcoming 10 events"};
	}

	private boolean isReadbleTime(String s) {
		return !s.contains("UNKNOWN") && !s.contains("PXW") && !s.contains("PXD");
	}

	private static String regexNormalize(String s, int i) {
		String rtn = "";
		Pattern pattern_data = Pattern.compile("[0-9]{4,4}-[0-9]{2,2}-[0-9]{2,2}");
		Matcher matcher_date = pattern_data.matcher(s);
		if (matcher_date.find()) {
			rtn += matcher_date.group(0);
		}
		Pattern pattern_time = Pattern.compile("[0-9]{2,2}:[0-9]{2,2}");
		Matcher matcher_time = pattern_time.matcher(s);
		if (matcher_time.find()) {
			rtn += " ";
			rtn += matcher_time.group(0);
		} else if (s.contains("MO")) {
			rtn += " 05:00"; // needs to be changed!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		} else if (s.contains("AF")) {
			rtn += " 12:00";
		} else if (s.contains("NI")) {
			rtn += " 18:00";
		} else if (i == 0) {
			rtn += " 00:00";
		} else if (i == 1) {
			rtn += " 23:59";
		} else {
			System.out.println("Error");
		}

		return rtn;
	}

	/** Example usage:
	 *  java SUTimeDemo "Three interesting dates are 18 Feb 1997, the 20th of july and 4 days from today."
	 *
	 *  @param args Strings to interpret
	 */
	public static void main(String[] args) {
		System.out.println(args[0]);
		TextProcessor p = new TextProcessor();
		boolean debug = false;
		if (!debug) {
			Scanner s = null;
			try {
				s = new Scanner(new File("/Users/yba/Documents/U/Sirius/QueryClassifier/data/ASR_CA.txt"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return;
			}
			ArrayList<String> lines = new ArrayList<String>();
			while (s.hasNextLine()){
				lines.add(s.nextLine());
			}
			s.close();
			for (String line : lines) {
				System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ " + line);
				for (String result : p.parse(line)) {
					System.out.println(result);
				}
			}
		} else {
			p.parse(args[0]);
		}
	}

	private static class TemporalComparator implements Comparator<Temporal> {
		@Override
		public int compare(Temporal o1, Temporal o2) {
			int time_val_diff = o1.toString().compareTo(o2.toString());
			if (time_val_diff != 0) {
				return time_val_diff;
			}
			int type_diff = o1.getTimexType().name().compareTo(o2.getTimexType().name());
			return type_diff;
		}
	}
}

