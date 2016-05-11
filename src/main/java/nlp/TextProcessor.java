package nlp;

import java.util.Date;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.TreeSet;

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

	private void run(String text) {
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
			System.out.println("### Min " + Collections.min(times));
			System.out.println("### Max " + Collections.max(times));
			return;
		} 
		for (Temporal temporal : has_seen) {
			if (isReadbleTime(temporal.getRange().toString())) {
				System.out.println("### Result " + temporal.getRange());
				return;
			}
		}
		
		System.out.println("### Result: upcoming 10 events");
		return;
	}

	private boolean isReadbleTime(String s) {
		return !s.contains("UNKNOWN") && !s.contains("PXW") && !s.contains("PXD");
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
				p.run(line);
			}
		} else {
			p.run(args[0]);
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

