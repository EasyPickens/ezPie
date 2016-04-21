package com.fanniemae.devtools.pie.common;

/**
 * 
 * @author Richard Monson
 * @since 2016-01-05
 * 
 */
public class ArrayUtilities {
	 public static int indexOf(String[][] aItems, String sTarget) {
	        return indexOf(aItems, sTarget, 0, false);
	    }

	    public static int indexOf(String[][] aItems, String sTarget, int nDimension) {
	        return indexOf(aItems, sTarget, nDimension, false);
	    }

	    public static int indexOf(String[][] aItems, String sTarget, boolean bIgnoreCase) {
	        return indexOf(aItems, sTarget, 0, bIgnoreCase);
	    }

	    public static int indexOf(String[][] aItems, String sTarget, int nDimension, boolean bIgnoreCase) {
	        if (aItems == null) {
	            return -1;
	        }
	        if (nDimension > aItems.length) {
	            return -1;
	        }

	        //int nLength = aItems[nDimension].length;
	        int nLength = aItems.length;
	        if (nLength == 0) {
	            return -1;
	        }

	        int nIndex = -1;
	        for (int i = 0; i < nLength; i++) {
	            if (aItems[i][nDimension].equalsIgnoreCase(sTarget) || (bIgnoreCase && aItems[i][nDimension].equalsIgnoreCase(sTarget))) {
	                nIndex = i;
	                break;
	            }
	        }

	        return nIndex;
	    }
}
