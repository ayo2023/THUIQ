package THUIQ;


/**
 * This class represents an Element of a utility-list
 * 
 * @see THUIQ.AlgoTHUIQ
 * @see THUIQ.AttributeUtilityList
 * @author Jinbao Miao
 */

/** attribute value, a bool type only have 0 or 1. */

public class AttributeElement {
	// The five variables as described in the paper:
	/** transaction id */
	public final int tid;   
	
	/** itemset utility */
	public final int iutils;
	
	/** itemset attribute value */
	public final int iattris;
	
	/** remaining utility */
	public int rutils;
	
	/** remaining attribute value */
	public int rattris;
	
	/**
	 * Constructor.
	 * 
	 * @param tid  the transaction id
	 * @param iutils  the itemset utility
	 * @param rutils  the remaining utility
	 * @param iattris the itemset attribute value
	 * @param rattris the remaining attribute value
	 */
	public AttributeElement(int tid, int iutils, int rutils, int iattris, int rattris){
		this.tid = tid;
		this.iutils = iutils;
		this.rutils = rutils;
		this.iattris = iattris;
		this.rattris = rattris;
	}
}
