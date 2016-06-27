package org.hq.rank.core;

import java.util.List;

public interface IRank {
	/**
	 * zheerne
	 * @param id
	 * @return
	 */
	public long[] delete(int id);
	public long set(int id , long value);
	public long[] set(int id , long... value);
	public long setIfAbsent(int id , long value);
	public long[] setIfAbsent(int id , long... value);
	public long setByField(int id,int field,long value);
	public RankData get(int id);
	public boolean has(int id);
	public List<RankData> getRankDatasByRankNum(int begin,int length);
	public void destory() throws InterruptedException;
	
	public String rankStatisticsInfo();
}
