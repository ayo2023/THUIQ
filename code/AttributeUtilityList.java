package THUIQ;


import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a UtilityList as used by the HUI-Miner algorithm.
 *
 * @see THUIQ.AlgoTHUIQ
 * @author Jinbao Miao
 */
public class AttributeUtilityList {
	 Integer item;  // the item
	 long sumIutils = 0;  // the sum of item utilities
	 long sumRutils = 0;  // the sum of remaining utilities
	 long sumIattris = 0; // the sum of item attribute values
	 long sumRattris = 0; // the sum of remaining attribute values
	 List<AttributeElement> elements = new ArrayList<AttributeElement>();  // the elements
	 
	/**
	 * Constructor.
	 * @param item the item that is used for this utility list
	 */
	public AttributeUtilityList(Integer item){
		this.item = item;
	}
	
	/**
	 * Method to add an element to this utility list and update the sums at the same time.
	 */
	public void addElement(AttributeElement element){
		sumIutils += element.iutils;
		sumRutils += element.rutils;
		sumIattris = Math.max(sumIattris, element.iattris);
		sumRattris = Math.max(sumRattris, element.rattris);
		elements.add(element);
	}
	
	/**
	 * Get the support of the itemset represented by this utility-list
	 * @return the support as a number of trnsactions
	 */
	public int getSupport() {
		return elements.size();
	}
}
