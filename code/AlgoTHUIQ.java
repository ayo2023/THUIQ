package THUIQ;

import java.io.*;
import java.util.*;

/**
 * This is an implementation of the "THUIQ Algorithm" in paper:
 *
 *  Jinbao Miao et al. THUIQ: Target High Utility Itemset Querying. 2021.
 *ent
 * @author Jinbao Miao & Wensheng Gan, JNU, China
 */

public class AlgoTHUIQ {
	/** the time at which the algorithm started */
	public long startTimestamp = 0;
	/** the time at which the algorithm ended */
	public long endTimestamp = 0;

	/** the number of high-utility itemsets generated */
	public long tarCount = 0;

	public int a, b, compare;
	/**the length of tarHUISubsume */
	int tarHUISubCount = 0;

	/** select the sort order */

	public boolean select = false;

	/** map to remember the tarHUISubsume to mark*/
	Map<Integer, Integer> mapMark;

	/** Map to remember the TWU of each item */
	Map<Integer, Long> mapItemToTWU;

	/** Map to rember the TAU of each item */
	Map<Integer, Integer> mapItemToTAU;

	/** writer to write the output file  */
	BufferedWriter writer = null;

	/** the number of utility-list that was constructed */
	private long joinCount;

	/** buffer for storing the current itemset that is mined when performing mining
	 * the idea is to always reuse the same buffer to reduce memory usage. */
	final int BUFFERS_SIZE = 200;
	private int[] itemsetBuffer = null;

	private int minUtility = 0;
	/** this class represent an item and its utility in a transaction */
	class Pair{
		int item = 0;
		int utility = 0;
		int attribute = 0; // store the bool attribute
	}

	/**
	 * Default constructor
	 */
	public AlgoTHUIQ() {
	}

	/**
	 * Run the algorithm
	 * 
	 * @param input the input file path
	 * @param output the output file path
	 * @param minutility the minimum utility threshold
	 * @throws IOException exception if error while writing the file
	 */
	public void runTHUIQ(String input, String output, int minutility, int[] tarHUISubsume, boolean Select) throws IOException {
		// reset maximum
		MemoryLogger.getInstance().reset();

		// initialize the buffer for storing the current itemset
		itemsetBuffer = new int[BUFFERS_SIZE];

		startTimestamp = System.currentTimeMillis();

		writer = new BufferedWriter(new FileWriter(output));

		//  We create a  map to store the TWU of each item
		mapItemToTWU = new HashMap<Integer, Long>();

		// We create a map to store the TAU of each item
		mapItemToTAU = new HashMap<Integer, Integer>();

		// We create a map to the item of the tarHUISubsume
		mapMark = new HashMap<Integer, Integer>();

		select = Select;

		minUtility = minutility;
		tarHUISubCount = tarHUISubsume.length;
		for(int i=0; i < tarHUISubsume.length; i++)
			mapMark.put(tarHUISubsume[i], 1);

		// We scan the database a first time to calculate the TWU of each item.
		BufferedReader myInput = null;
		String thisLine;
		try {
			// prepare the object for reading the file
			myInput = new BufferedReader(new InputStreamReader( new FileInputStream(new File(input))));
			// for each line (transaction) until the end of file
			while ((thisLine = myInput.readLine()) != null) {
				// if the line is  a comment, is  empty or is a
				// kind of metadata
				if (thisLine.isEmpty() == true ||
						thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
						|| thisLine.charAt(0) == '@') {
					continue;
				}

				// split the transaction according to the : separator
				String split[] = thisLine.split(":");
				// the first part is the list of items
				String items[] = split[0].split(" ");
				// the second part is the transaction utility
				int transactionUtility = Integer.parseInt(split[1]);
				// for each item, we add the transaction utility to its TWU

				int tarTau = 0;

				for(int i=0; i <items.length; i++){
					// convert item to integer
					Integer item = Integer.parseInt(items[i]);
					// get the current TWU of that item
					Long twu = mapItemToTWU.get(item);
					// add the utility of the item in the current transaction to its twu
					twu = (twu == null)? transactionUtility : twu + (long)transactionUtility;
					//System.out.println(item + " : " + mapItemToTWU.get(item));
					mapItemToTWU.put(item, twu);
					if(mapMark.get(item) != null) tarTau ++;
				}
				if(tarTau >= tarHUISubCount){
					for(int i = 0; i < items.length; i ++){
						// convert item to integer
						Integer item = Integer.parseInt(items[i]);
						// get the current TAU of that item
						Integer TAU = mapItemToTAU.get(item);
						// add the tarTaU of the item in the current transaction to its twu
						TAU = (TAU == null) ? tarTau : Math.max(tarTau, TAU);

						mapItemToTAU.put(item, TAU);
					}
				}
			}
		} catch (Exception e) {
			// catches exception if error while reading the input file
			e.printStackTrace();
		}finally {
			if(myInput != null){
				myInput.close();
			}
		}

		// CREATE A LIST TO STORE THE UTILITY LIST OF ITEMS WITH TWU  >= MIN_UTILITY.
		List<AttributeUtilityList> listOfUtilityLists = new ArrayList<AttributeUtilityList>();
		// CREATE A MAP TO STORE THE UTILITY LIST FOR EACH ITEM.
		// Key : item    Value :  utility list associated to that item
		Map<Integer, AttributeUtilityList> mapItemToUtilityList = new HashMap<Integer, AttributeUtilityList>();

		// For each item
		for(Integer item: mapItemToTWU.keySet()){
			// if the item is promising  (TWU >= minUtility and TAU >= tarHUISubCount )
			//System.out.println(item + " : " + mapItemToTWU.get(item));
			if(mapItemToTWU.get(item) >= minUtility && mapItemToTAU.get(item) != null){
				// create an empty Utility List that we will fill later.
				AttributeUtilityList uList = new AttributeUtilityList(item);
				mapItemToUtilityList.put(item, uList);
				// add the item to the list of high TWU items
				listOfUtilityLists.add(uList);

			}
		}
		// SORT THE LIST OF HIGH TWU ITEMS IN ASCENDING ORDER
		Collections.sort(listOfUtilityLists, new Comparator<AttributeUtilityList>(){
			public int compare(AttributeUtilityList o1, AttributeUtilityList o2) {
				// compare the TWU of the items
				return compareItems(o1.item, o2.item);
			}
		} );
		/*for(AttributeUtilityList X : listOfUtilityLists)
			System.out.println(X.item + " TWU : " + mapItemToTWU.get(X.item) + " weighted : " + mapMark.get(X.item));*/
		// SECOND DATABASE PASS TO CONSTRUCT THE UTILITY LISTS 
		// OF 1-ITEMSETS  HAVING TWU  >= minutil (promising items)
		try {
			// prepare object for reading the file
			myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input))));
			// variable to count the number of transaction
			int tid =0;
			// for each line (transaction) until the end of file
			while ((thisLine = myInput.readLine()) != null) {
				// if the line is  a comment, is  empty or is a
				// kind of metadata
				if (thisLine.isEmpty() == true ||
						thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
						|| thisLine.charAt(0) == '@') {
					continue;
				}

				// split the line according to the separator
				String split[] = thisLine.split(":");
				// get the list of items
				String items[] = split[0].split(" ");
				// get the list of utility values corresponding to each item
				// for that transaction
				String utilityValues[] = split[2].split(" ");

				// Copy the transaction into lists but 
				// without items with TWU < minutility

				int remainingUtility = 0;
				int remainingAttribute = 0;
				// Create a list to store items
				List<Pair> revisedTransaction = new ArrayList<Pair>();
				// for each item
				for(int i=0; i <items.length; i++){
					// convert values to integers
					Pair pair = new Pair();
					pair.item = Integer.parseInt(items[i]);
					pair.utility = Integer.parseInt(utilityValues[i]);
					
					if(mapMark.get(pair.item) != null) 
						pair.attribute = Integer.valueOf(1);
					
					// if the item has enough utility
					if(mapItemToTWU.get(pair.item) >= minUtility && mapItemToTAU.get(pair.item) != null){
						// add it
						revisedTransaction.add(pair);
						remainingUtility += pair.utility;
						remainingAttribute += pair.attribute;
					}
				}
				
				//System.out.println("remainingAttribute = " + remainingAttribute);
				Collections.sort(revisedTransaction, new Comparator<Pair>(){
					public int compare(Pair o1, Pair o2) {
						return compareItems(o1.item, o2.item);
					}});

				// for each item left in the transaction
				for(Pair pair : revisedTransaction){
					// subtract the utility of this item from the remaining utility
					remainingUtility = remainingUtility - pair.utility;
					// subtract the attribute value of this item from the remaining attribute values
					remainingAttribute = remainingAttribute - pair.attribute;
					// get the utility list of this item
					AttributeUtilityList utilityListOfItem = mapItemToUtilityList.get(pair.item);

					// Add a new Element to the utility list of this item corresponding to this transaction
					AttributeElement element = new AttributeElement(tid, pair.utility, remainingUtility, pair.attribute, remainingAttribute);

					utilityListOfItem.addElement(element);
				}
				tid++; // increase tid number for next transaction

			}
		} catch (Exception e) {
			// to catch error while reading the input file
			e.printStackTrace();
		}finally {
			if(myInput != null){
				myInput.close();
			}
		}

		// check the memory usage
		MemoryLogger.getInstance().checkMemory();
		//System.out.println("X====================================");
		// Mine the database recursively
		THUIQSearch(0, null, listOfUtilityLists);
		// check the memory usage again and close the file.
		MemoryLogger.getInstance().checkMemory();
		// close output file
		writer.close();
		// record end time
		endTimestamp = System.currentTimeMillis();
	}
	private int compareItems(int item1, int item2) {
		long a = mapItemToTWU.get(item1);
		long b = mapItemToTWU.get(item2);
		//long compare = mapItemToTWU.get(item1) - mapItemToTWU.get(item2);
		int  compare = a == b ? 0 : (a > b ? 1 : -1);

		if(select == true) return (compare == 0)? item1 - item2 :  compare;

		int c = mapMark.get(item1) == null ? 0 : mapMark.get(item1);
		int d = mapMark.get(item2) == null ? 0 : mapMark.get(item2);

		// if the same, use the lexical order otherwise use the TWU
		return (c - d == 0) ? ((compare == 0)? item1 - item2 :  compare) : d - c;
	}

	/**
	 * This is the recursive method to find all high utility itemsets. It writes
	 * the itemsets to the output file.
	 *
	 * @param pAUL This is the Utility List of the prefix. Initially, it is empty.
	 * @param AULs The utility lists corresponding to each extension of the prefix.
	 * @param prefixLength The current prefix length
	 * @throws IOException
	 */
	private void THUIQSearch(int prefixLength, AttributeUtilityList pAUL, List<AttributeUtilityList> AULs) throws IOException {
		
		// For each extension X of prefix P
		for(int i=0; i< AULs.size(); i++){
			AttributeUtilityList X = AULs.get(i);


			// If X is a high utility itemset and it contains target pattern.
			// we save the itemset:  X
			if(X.sumIutils >= minUtility && X.sumIattris >= tarHUISubCount){
				// save to file
				writeOut(prefixLength, X.item, X.sumIutils, X.sumIattris);
			}

			// If the sum of the remaining attributes for X
			// is less than tarHUISubCount, we explore extensions of X.
			// (this is the pruning condition)

			if(X.sumIattris + X.sumRattris < tarHUISubCount) continue;
			// If the sum of the remaining utilities for X
			// is higher than minUtility, we explore extensions of X.
			// (this is the pruning condition)

			if(X.sumIutils + X.sumRutils >= minUtility){
				// This list will contain the attribute utility lists of X extensions.
				List<AttributeUtilityList> exAULs = new ArrayList<AttributeUtilityList>();
				// For each extension of p appearing
				// after X according to the total weighted order
				for(int j= i + 1; j < AULs.size(); j++){
					// This list will contain the attribute utility lists of X extensions.
					AttributeUtilityList Y = AULs.get(j);
					// we construct the extension XY
					// and add it to the list of extensions of X
					AttributeUtilityList temp = construct(pAUL, X, Y);

					if(temp != null){
						exAULs.add(temp);

						// construct table of xy include it tid, utility and attribute.
						joinCount++;
					}
				}
				// We create new prefix X
				itemsetBuffer[prefixLength] = X.item;

				// We make a recursive call to discover all itemsets with the prefix XY
				THUIQSearch(prefixLength+1, X, exAULs);
			}
		}
	}

	/**
	 * This method constructs the utility list of pXY
	 * @param AP :  the utility list of prefix P.
	 * @param Apx : the utility list of pX
	 * @param Apy : the utility list of pY
	 * @return the utility list of pXY
	 */
	private AttributeUtilityList construct(AttributeUtilityList AP, AttributeUtilityList Apx, AttributeUtilityList Apy) {
		// create an empy attribute utility list for pXY
		AttributeUtilityList pxyAUL = new AttributeUtilityList(Apy.item); // only have Y
		
		// for each element in the attribute utility list of pX
		for(AttributeElement ex : Apx.elements){
			// do a binary search to find element ey in py with tid = ex.tid
			AttributeElement ey = findElementWithTID(Apy, ex.tid);
			if(ey == null){
				continue;
			}
			// if the prefix p is null
			if(AP == null){
				// Create the new element
				AttributeElement eXY = new AttributeElement(ex.tid, ex.iutils + ey.iutils, ey.rutils, ex.iattris + ey.iattris, ey.rattris);
				// add the new element to the attribute utility list of pXY
				pxyAUL.addElement(eXY);

			}else{
				// find the element in the attribute utility list of p wih the same tid
				AttributeElement e = findElementWithTID(AP, ex.tid);
				if(e != null){
					// Create new element
					AttributeElement eXY = new AttributeElement(ex.tid, ex.iutils + ey.iutils - e.iutils,
							ey.rutils, ex.iattris + ey.iattris - e.iattris, ey.rattris);
					// add the new element to the attribute utility list of pXY
					pxyAUL.addElement(eXY);
				}
			}
		}
		
		// return the attribute utility list of pXY.
		return pxyAUL;
	}

	/**
	 * Do a binary search to find the element with a given tid in a utility list
	 * @param aulist the utility list
	 * @param tid  the tid
	 * @return  the element or null if none has the tid.
	 */
	private AttributeElement findElementWithTID(AttributeUtilityList aulist, int tid){
		List<AttributeElement> list = aulist.elements;

		// perform a binary search to check if  the subset appears in  level k-1.
		int first = 0;
		int last = list.size() - 1;

		// the binary search
		while( first <= last )
		{
			int middle = ( first + last ) >>> 1; // divide by 2

			if(list.get(middle).tid < tid){
				first = middle + 1;  
				//  the itemset compared is larger than the subset according to the lexical order
			}
			else if(list.get(middle).tid > tid){
				last = middle - 1; 
				//  the itemset compared is smaller than the subset  is smaller according to the lexical order
			}
			else{
				return list.get(middle);
			}
		}
		return null;
	}

	/**
	 * Method to write a high utility itemset to the output file.
	 *
	 * @param item an item to be appended to the prefix
	 * @param utility the utility of the prefix concatenated with the item
	 * @param prefixLength the prefix length
	 */
	private void writeOut(int prefixLength, int item, long utility, long attribute) throws IOException {
		tarCount++; // increase the number of high utility itemsets found

		//Create a string buffer
		StringBuilder buffer = new StringBuilder();
		// append the prefix
		for (int i = 0; i < prefixLength; i++) {
			buffer.append(itemsetBuffer[i]);
			buffer.append(' ');
		}
		
		// append the last item
		buffer.append(item);
		// append the utility value
		buffer.append(" #UTIL: ");
		buffer.append(utility);
		// append the attribute value
		buffer.append(" #ATTRI: ");
		buffer.append(attribute);
		
		// write to file
		writer.write(buffer.toString());
		writer.newLine();
	}

	/**
	 * Print statistics about the latest execution to System.out.
	 * Attribute value for Target Pattern Mining
	 * 
	 */
	public void printStats(String input, int minutil, int[] tararray) {
		System.out.println("==========  THUIQ ALGORITHM - STATS ==========");
		System.out.println(" Input file: " + input.toString());
		System.out.println(" minimum utility: " + minutil);
		System.out.println(" Target pattern: " + Arrays.toString(tararray));
		System.out.println(" Total time: " + (endTimestamp - startTimestamp)/1000.0 + " s");
		System.out.println(" Maximal memory: " + MemoryLogger.getInstance().getMaxMemory() + " MB");
		System.out.println(" Target HUIs count: " + tarCount);
		System.out.println(" Join count: " + joinCount);
		System.out.println("===================================================");
	}
}
