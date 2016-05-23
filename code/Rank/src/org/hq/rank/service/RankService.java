package org.hq.rank.service;

import java.util.List;
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
		return rankMap.putIfAbsent(rankName, new Rank()) == null;
	}

	@Override
	public boolean createRank(String rankName, int fieldCount) {
		RankConfigure rankConfigure = new RankConfigure();
		rankConfigure.setRankConditionCount(fieldCount);
		return rankMap.putIfAbsent(rankName, new Rank(rankConfigure)) == null;
	}

	@Override
	public void deleteRank(String rankName) {
		rankMap.remove(rankName);
	}

	@Override
	public boolean hasRank(String rankName) {
		return rankMap.contains(rankName);
	}

	@Override
	public long put(String rankName, int id, long value) {
		IRank rank = rankMap.get(rankName);
		if(rank == null){
			throw new RankException("rank is not exist , rankName = "+rankName);
		}
		return rank.set(id, value);
	}
	@Override
	public long[] put(String rankName, int id, long... value) {
		IRank rank = rankMap.get(rankName);
		if(rank == null){
			throw new RankException("rank is not exist , rankName = "+rankName);
		}
		return rank.set(id, value);
	}

	@Override
	public long[] delete(String rankName, int id) {
		IRank rank = rankMap.get(rankName);
		if(rank == null){
			throw new RankException("rank is not exist , rankName = "+rankName);
		}
		return rank.delete(id);
	}

	@Override
	public boolean has(String rankName, int id) {
		IRank rank = rankMap.get(rankName);
		if(rank == null){
			throw new RankException("rank is not exist , rankName = "+rankName);
		}
		return rank.has(id);
	}

	@Override
	public int getRankNum(String rankName, int id) {
		IRank rank = rankMap.get(rankName);
		if(rank == null){
			throw new RankException("rank is not exist , rankName = "+rankName);
		}
		RankData rankData = rank.get(id);
		if(rankData == null){
			return -1;
		}
		return rankData.getRankNum();
	}

	@Override
	public RankData getRankDataById(String rankName, int id) {
		IRank rank = rankMap.get(rankName);
		if(rank == null){
			throw new RankException("rank is not exist , rankName = "+rankName);
		}
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
		IRank rank = rankMap.get(rankName);
		if(rank == null){
			throw new RankException("rank is not exist , rankName = "+rankName);
		}
		List<RankData> rankDataList = rank.getRankDatasByRankNum(rankNum, 1);
		if(rankDataList == null || rankDataList.size() == 0){
			return null;
		}
		return rankDataList.get(0);
	}

	@Override
	public List<RankData> getRankDatasByPage(String rankName, int page,int pageSize) {
		IRank rank = rankMap.get(rankName);
		if(rank == null){
			throw new RankException("rank is not exist , rankName = "+rankName);
		}
		int begin = page * pageSize;
		return rank.getRankDatasByRankNum(begin, pageSize);
	}

	@Override
	public List<RankData> getRankDatasAroundId(String rankName, int id,int beforeNum, int afterNum) {
		IRank rank = rankMap.get(rankName);
		if(rank == null){
			throw new RankException("rank is not exist , rankName = "+rankName);
		}
		RankData rankData = rank.get(id);
		if(rankData == null){
			return null;
		}
		int begin = rankData.getRankNum() - beforeNum;
		if(begin < 0){
			begin = 0;
		}
		int length = rankData.getRankNum() - begin + afterNum;
		List<RankData> rankDataList = rank.getRankDatasByRankNum(begin, length);
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
			}
		}
		// end
		return rankDataList;
	}

	@Override
	public void destroy(String rankName) {
		IRank rank = rankMap.get(rankName);
		if(rank == null){
			throw new RankException("rank is not exist , rankName = "+rankName);
		}
		try {
			rank.destory();
		} catch (InterruptedException e) {
			RankException rankException = new RankException("rank "+rankName+" destroy error");
			rankException.addSuppressed(e.getCause());
			throw rankException;
		}
	}
}
