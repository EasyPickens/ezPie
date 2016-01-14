package com.fanniemae.automation.common;

public class DataTypeRank {
	protected int _rank;
	protected String _name;
	
	public DataTypeRank(int rank, String typeName) {
		_rank = rank;
		_name = typeName;
	}
	
	public int getRank() {
		return _rank;
	}
	
	public String getTypeName() {
		return _name;
	}

}
