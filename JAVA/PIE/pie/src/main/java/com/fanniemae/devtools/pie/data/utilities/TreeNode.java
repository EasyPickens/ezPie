/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.devtools.pie.data.utilities;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * @author Tara Tritt
 * @since 2016-06-30
 * 
 */

public class TreeNode<T> implements Iterable<TreeNode<T>>{
	private T _data;
    private List<TreeNode<T>> _children;

    public TreeNode(T data) {
        _data = data;
        _children = new LinkedList<TreeNode<T>>();
    }
    
    public T getData(){
    	return _data;
    }
    
    public List<TreeNode<T>> getChildren(){
    	return _children;
    }

    public TreeNode<T> addChild(T child) {
        TreeNode<T> childNode = new TreeNode<T>(child);
        _children.add(childNode);
        return childNode;
    }
    
    public TreeNode<T> addChild(TreeNode<T> childNode) {
        _children.add(childNode);
        return childNode;
    }

	@Override
	public Iterator<TreeNode<T>> iterator() {
		return _children.iterator();
	}
	
	public void print(){
		_print("root");
	}
	
	private void _print(String path){
		System.out.println(path + "." + _data.toString());
		Iterator<TreeNode<T>> iter = _children.iterator();
		while(iter.hasNext()){
			TreeNode<T> next = iter.next();
			next._print(path+"."+ _data.toString());
		}
	}
}
