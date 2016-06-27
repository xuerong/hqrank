package org.hq.rank.core;

public class RankData {
	private int id; // 
	private int rankNum; // 排行
	private long[] value;
	public RankData(){}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getRankNum() {
		return rankNum;
	}
	public void setRankNum(int rankNum) {
		this.rankNum = rankNum;
	}
	
	
	public long[] getValue() {
		return value;
	}
	public void setValue(long[] value) {
		this.value = value;
	}
	@Override
	public String toString(){
		return new StringBuilder("id:").append(id).append(",rankNum:").
				append(rankNum).append(",value:").append(valueToString()).toString();
	}
	private String valueToString(){
		StringBuilder sb = new StringBuilder("(");
		for (long l : value) {
			sb.append(l+",");
		}
		sb.append(")");
		return sb.toString();
	}
	
}
