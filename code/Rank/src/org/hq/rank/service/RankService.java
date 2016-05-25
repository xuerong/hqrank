package org.hq.rank.service;

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.hq.rank.core.IRank;
import org.hq.rank.core.Rank;
import org.hq.rank.core.RankConfigure;
import org.hq.rank.core.RankData;
import org.hq.rank.core.RankException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RankService implements IRankService{
	private static Logger log = LoggerFactory
			.getLogger(RankService.class);
	private final ConcurrentHashMap<String, IRank> rankMap = new ConcurrentHashMap<String, IRank>();

	@Override
	public boolean createRank(String rankName) {
		IRank rank = new Rank();// 注意，这里rank里的线程已经启动，如果创建失败，要destroy这个rank
		IRank oldRank = rankMap.putIfAbsent(rankName,rank);
		if(oldRank != null){ // 没有放进去
			destroy(rank, rankName);
		}
		return oldRank == null;
	}

	@Override
	public boolean createRank(String rankName, int fieldCount) {
		RankConfigure rankConfigure = new RankConfigure();
		rankConfigure.setRankConditionCount(fieldCount);
		IRank rank = new Rank(rankConfigure);// 注意，这里rank里的线程已经启动，如果创建失败，要destroy这个rank
		IRank oldRank = rankMap.putIfAbsent(rankName,rank);
		if(oldRank != null){ // 没有放进去
			destroy(rank, rankName);
		}
		return oldRank == null;
	}

	@Override
	public void deleteRank(String rankName) {
		IRank rank = rankMap.remove(rankName);
		destroy(rank, rankName);
	}
	
	@Override
	public void deleteAllRank() {
		for (Entry<String, IRank> entry : rankMap.entrySet()) {
			destroy(entry.getValue(), entry.getKey());
		}
		rankMap.clear();
	}

	@Override
	public boolean hasRank(String rankName) {
		return rankMap.containsKey(rankName);
	}

	@Override
	public long put(String rankName, int id, long value) {
		IRank rank = getNotNullRankByName(rankName);
		return rank.set(id, value);
	}
	@Override
	public long[] put(String rankName, int id, long... value) {
		IRank rank = getNotNullRankByName(rankName);
		return rank.set(id, value);
	}
	
	@Override
	public long putIfAbsent(String rankName, int id, long value) {
		IRank rank = getNotNullRankByName(rankName);
		return rank.setIfAbsent(id, value);
	}

	@Override
	public long[] putIfAbsent(String rankName, int id, long... value) {
		IRank rank = getNotNullRankByName(rankName);
		return rank.setIfAbsent(id, value);
	}

	@Override
	public long[] delete(String rankName, int id) {
		IRank rank = getNotNullRankByName(rankName);
		return rank.delete(id);
	}

	@Override
	public boolean has(String rankName, int id) {
		IRank rank = getNotNullRankByName(rankName);
		return rank.has(id);
	}

	@Override
	public int getRankNum(String rankName, int id) {
		IRank rank = getNotNullRankByName(rankName);
		RankData rankData = rank.get(id);
		if(rankData == null){
			return -1;
		}
		return rankData.getRankNum();
	}

	@Override
	public RankData getRankDataById(String rankName, int id) {
		IRank rank = getNotNullRankByName(rankName);
		return rank.get(id);
	}

	@Override
	public int getRankId(String rankName, int rankNum) {
		RankData rankData = getRankDataByRankNum(rankName, rankNum);
		if(rankData == null){
			return -1;
		}
		return rankData.getId();
	}

	@Override
	public RankData getRankDataByRankNum(String rankName, int rankNum) {
		IRank rank = getNotNullRankByName(rankName);
		List<RankData> rankDataList = rank.getRankDatasByRankNum(rankNum, 1);
		if(rankDataList == null || rankDataList.size() == 0){
			return null;
		}
		return rankDataList.get(0);
	}

	@Override
	public List<RankData> getRankDatasByPage(String rankName, int page,int pageSize) {
		IRank rank = getNotNullRankByName(rankName);
		int begin = page * pageSize;
		return rank.getRankDatasByRankNum(begin, pageSize);
	}

	@Override
	public List<RankData> getRankDatasAroundId(String rankName, int id,int beforeNum, int afterNum) {
		IRank rank = getNotNullRankByName(rankName);
		int maxTryTimes = 3;
		int currentTryTimes = 0;
		List<RankData> rankDataList;
		while(true){
			RankData rankData = rank.get(id);
			if(rankData == null){
				return null;
			}
			int begin = rankData.getRankNum() - beforeNum;
			if(begin < 0){
				begin = 0;
			}
			int length = rankData.getRankNum() - begin + afterNum+1;
			rankDataList = rank.getRankDatasByRankNum(begin, length);
			if(rankDataList == null){
				return null;
			}
			// 验证一下：
			if(rankDataList.size() <= rankData.getRankNum() - begin){
				log.warn("has no get enough value : getLength = "+rankDataList.size()+",needLength="+length);
			}else{
				if(rankDataList.size() < length){
					log.info("has no get enough value : getLength = "+rankDataList.size()+",needLength="+length);
				}
				RankData rankData2 = rankDataList.get(rankData.getRankNum() - begin);
				if(rankData.getId() != rankData2.getId()){
					log.warn("rankData:"+rankData+",rankData2:"+rankData2+"\n newRankData:"+
							rank.get(id));
					// 说明在两次获取的间隔之间其排名被改变，在高并发情况下极易发生，三种解决方案：
					// 1,将其放回该位置（最不合理，但是最好实施）
					// 2，重新给rank设计一个函数，来专门解决该问题（最合理，但最难实施）
					// 3，重新获取，直到获取到，或获取次数到达某个最大值，采取方案1（较合理，较好实施）
					// 其实在极高并发情况下，3才会失败，并且此时其实时性(毫秒级)的就不再那么重要，所以暂时选择方案3
				}else{
					break;
				}
			}
			if(currentTryTimes++ > maxTryTimes){
				rankDataList.set(rankData.getRankNum() - begin, rankData); // 替换方案
				break;
			}
		}
		
		// end
		return rankDataList;
	}
	
	private void destroy(IRank rank,String rankName) {
		if(rank != null){
			try {
				rank.destory();
			} catch (InterruptedException e) {
				RankException rankException = new RankException("rank "+rankName+" destroy error");
				rankException.addSuppressed(e.getCause());
				throw rankException;
			}
		}
	}
	
	private IRank getNotNullRankByName(String rankName){
		IRank rank = rankMap.get(rankName);
		if(rank == null){
			throw new RankException("rank is not exist , rankName = "+rankName);
		}
		return rank;
	}
}
