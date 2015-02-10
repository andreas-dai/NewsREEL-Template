package eu.crowdrec.contest.evaluation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import eu.crowdrec.contest.evaluation.LinkedFileCacheDuplicateSupport.CacheEntry;


public class Evaluator {
	
	/**
	 * prevent invalid answers, recommending just everything
	 */
	private static final int MAX_NUMBER_OF_RECOMMENDATIONS = 3;
	
	/**
	 * the set of forbidden items
	 */
	private static final HashSet<Long> blackListedItems = new HashSet<Long>();
	
	/**
	 * aggregate the evaluation results for different domains
	 */
	private final static Map<Long, int[]> resultCount = new HashMap<Long, int[]>();
	
	
	/**
	 * The responseTime statistic.
	 */
	private final static SummaryStatistics responseTimeStatistic = new SummaryStatistics();

	/**
	 * create a histogram for debugging (a detailed analysis)
	 */
	private final static int[] histogram = new int [500];
	
	/**
	 * Run the evaluation process. Ensure that enough heap is available for caching.
	 * The amount of required memory is linear in the number of cached lines / the size of the time window
	 *   considered in the evaluation.
	 *     
	 * @param args the files used in the evaluation.
	 * @throws IOException indicates a problem while searching or opening the ground truth file(s)
	 */
	public static void main(String[] args) throws IOException {

		// define default settings for simplified testing
		String predictionFileName = "";
		String groundTruthFileName = "";

		// define the default window size
		long windowSizeInMillis = 5L * 60L * 1000L;
		
		// check the parameters
		if (args.length < 0 || args.length > 3) {
			System.out.println("usage: java Evaluator <predictionFileName> <groundTruthFileName> [<windowSizeInMillis>]");
			//System.exit(0);
			System.out.println(".. using the default values.");
		}
		
		// set the parameter values
		if (args.length > 0) {
			predictionFileName = args[0];
		}
		if (args.length > 1) {
			groundTruthFileName = args[1];
		}
		if (args.length > 2) {
			windowSizeInMillis = Long.parseLong(args[2]);
		}
		
		// inform the user that the evaluator has started
		System.out.println("Evaluation is running ...");
		System.out.println("predictionFileName= " + predictionFileName);
		System.out.println("groundTruthFileName= " + groundTruthFileName);
		System.out.println("windowSizeInMillis= " + windowSizeInMillis);
		
		// initialize the groundTruth linked list
		LinkedFileCacheDuplicateSupport lfc = new LinkedFileCacheDuplicateSupport();
		lfc.initialize(groundTruthFileName, windowSizeInMillis);

		// initialize the prediction list that should be evaluated
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(predictionFileName));
			for (String line = br.readLine(); line != null; line = br.readLine()) {

				try {
					// ignore comments and invalid lines
					if (line.length() < 2 || line.startsWith("#")) {
						continue;
					}
					
					// try to parse the prediction line
					String[] token = line.split("\t");
					
					long messageID = Long.parseLong(token[1]);
					long timeStamp = Long.parseLong(token[2]);
					long responseTime = Long.parseLong(token[3]);
					responseTimeStatistic.addValue(responseTime);
					int tmpResponseTime = (int) (responseTime/10);
					if (tmpResponseTime >= histogram.length) {
						tmpResponseTime = histogram.length-1;
					}
					histogram[tmpResponseTime]++;
					//long itemID = Long.parseLong(token[4]);
					
					long userID = -1;
					try {
						userID = Long.parseLong(token[5]);
					} catch (Exception ignored) {
					}
					
					long domainID = Long.parseLong(token[6]);
					String recommendations = token[7];
					final JSONObject jsonObj = (JSONObject) JSONValue.parse(recommendations);
					
					JSONObject recs = (JSONObject) jsonObj.get("recs");
					JSONObject recsInts = (JSONObject) recs.get("ints");
					JSONArray itemIds = (JSONArray) recsInts.get("3");
					if (itemIds != null) {
						for (int i = 0; i < itemIds.size() && i < MAX_NUMBER_OF_RECOMMENDATIONS; i++) {
							Long itemID = Long.parseLong(itemIds.get(i) + "");
							
							// check the IDs
							CacheEntry ce = new CacheEntry(userID, itemID, domainID, timeStamp);
							boolean valid = lfc.checkPrediction(ce, blackListedItems);
							
							//System.out.println("checking:\t" + timeStamp + "\t" + userID + "\t" + domainID + "\t" + itemID + "\t:" + valid) ;

							int[] countEntry = resultCount.get(domainID);
							if (countEntry == null) {
								resultCount.put(domainID, new int[2]);
								countEntry = resultCount.get(domainID);
							}
							if (valid) {
								countEntry[0]++;
							} else {
								countEntry[1]++;
							}
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("invalid line: " + line);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception ignored) {
				}
			}
		}

		// close and cleanup
		try {
			lfc.close();
		} catch (IOException ignored) {
		}
		
		// printout the results
		int[] overall = new int[2];
		final String DELIM = "\t"; 
		System.out.println("\nEvaluation results\n==================");
		for (Map.Entry<Long, int[]> entry: resultCount.entrySet()) {
			int[] values = entry.getValue();
			System.out.println(entry.getKey() + DELIM + Arrays.toString(values) + DELIM + NumberFormat.getInstance().format(1000*values[0] / values[1]) + " o/oo");
			for (int i = 0; i < values.length; i++) {
				overall[i] += values[i];
			}
		}
		System.out.println("all" + DELIM + Arrays.toString(overall) + DELIM + NumberFormat.getInstance().format(1000*overall[0] / overall[1]) + " o/oo");
		System.out.println(
				"mean/min/max/n/stdDev" + DELIM + 
				responseTimeStatistic.getMean() + DELIM + 
				responseTimeStatistic.getMin() + DELIM + 
				responseTimeStatistic.getMax() + DELIM + 
				responseTimeStatistic.getStandardDeviation() + DELIM + 
				responseTimeStatistic.getN());
		
		// print the histogram for the response time statistic
//		System.out.println("'==Histogram==");
//		for (int i = 0; i < histogram.length; i++) {
//			System.out.println((i*10) + DELIM + histogram[i]);
//		}
	}
}
