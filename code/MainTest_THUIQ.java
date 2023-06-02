package THUIQ;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

/**
 * Example of how to use the THUIQ algorithm from the source code.
 * 
 */

/**
 * tarHUISubsume = new int[j / 3];
 * 			int cnt = 0;
 * 			for(int i = 0; i < j; i++)
 * 				if(i % 3 == 0) tarHUISubsume[cnt ++] = houxuan[i];
 */

public class MainTest_THUIQ {
	public static void main(String[] arg) throws IOException {

		String input = "T40I10D100K.txt";
		String output = ".//THUIQ_output.txt";
		               //407  430 495  583  648  756  826  845
		                    // 357
		int[] tarHUISubsume = {21, 565};
		int min_utility = 30000;

		boolean select = false;
		// Applying the THUIQ algorithm

		AlgoTHUIQ THUIQ = new AlgoTHUIQ();
		THUIQ.runTHUIQ(input, output, min_utility, tarHUISubsume, select);
		THUIQ.printStats(input, min_utility, tarHUISubsume);

	}

	public static String fileToPath(String filename) throws UnsupportedEncodingException {
		URL url = MainTest_THUIQ.class.getResource(filename);
		return java.net.URLDecoder.decode(url.getPath(), "UTF-8");
	}
}
