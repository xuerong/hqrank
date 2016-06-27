package org.hq.rank.core.node;

/**
 * 作为一个返回值用
 * @author a
 *
 */
public class SearchAbNodeResult {
	private AbNode node;
	private int rankNum;
	
	public SearchAbNodeResult(){}
	public SearchAbNodeResult(AbNode node,int rankNum){
		this.node = node;
		this.rankNum = rankNum;
	}
	
	public AbNode getNode() {
		return node;
	}
	public void setNode(AbNode node) {
		this.node = node;
	}
	public int getRankNum() {
		return rankNum;
	}
	public void setRankNum(int rankNum) {
		this.rankNum = rankNum;
	}
	
	
}
