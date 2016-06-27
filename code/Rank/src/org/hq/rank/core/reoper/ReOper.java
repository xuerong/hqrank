package org.hq.rank.core.reoper;

import org.hq.rank.core.element.Element;
import org.hq.rank.core.node.Node;
import org.hq.rank.core.node.RankElement;

/**
 * 之前操作失败的数据
 * */
public class ReOper {
	private OperType operType;
	private int times;
	private Element element;
	private Element oldElement;
	
	private RankElement rankElement;
	
	public RankElement getRankElement() {
		return rankElement;
	}

	public void setRankElement(RankElement rankElement) {
		this.rankElement = rankElement;
	}

	public Element getOldElement() {
		return oldElement;
	}

	public void setOldElement(Element oldElement) {
		this.oldElement = oldElement;
	}

	private Node node;
	
	public OperType getOperType() {
		return operType;
	}

	public void setOperType(OperType operType) {
		this.operType = operType;
	}

	public int getTimes() {
		return times;
	}

	public void setTimes(int times) {
		this.times = times;
	}


	public Element getElement() {
		return element;
	}

	public void setElement(Element element) {
		this.element = element;
	}

	

	public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}
	
	public int timesIncrementAndGet(){
		return ++times;
	}
	
	@Override
	public String toString(){
		return new StringBuilder().append("element:"+element)
		.append(",oldElement:"+oldElement)
		.append(",element==oldElement:"+(element==oldElement))
		
		.append(",type:"+operType)
		.append(",times:"+times)
		.append(",node:"+node).toString();
	}

	public static enum OperType{
		Delete,Add,Update,DeleteNode,
		
		RankElementDeleteNode // 原来叫 RandomDeleteNode，但不记得为什么了
	}
}
