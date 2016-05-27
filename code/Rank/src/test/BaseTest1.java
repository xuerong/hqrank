package test;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.hq.rank.core.RankData;
import org.hq.rank.service.IRankService;
import org.hq.rank.service.RankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 测试一些基本的操作
 * 包括：service中的接口的简单访问，主要说明工具的使用方法和对其正确性的测试
 * @author a
 *
 */
public class BaseTest1 {
	private static Logger log = LoggerFactory.getLogger(BaseTest1.class);
	
	public static void main(String[] args) throws InterruptedException{
		IRankService rankService = new RankService();
		
		BaseTest1 test = new BaseTest1();
//		test.test1(rankService);
//		test.test2(rankService);
//		test.test3(rankService);
//		test.test4(rankService);
//		test.test5(rankService);
		
		rankService.deleteAllRank();
	}
	/**
	 * 对rank的创建，删除
	 */
	private void test1(IRankService rankService){
		boolean hasRankA,hasRankB;
		
		rankService.createRank("rank_a");
		hasRankA = rankService.hasRank("rank_a");
		hasRankB = rankService.hasRank("rank_b");
		log.info("hasRankA:"+hasRankA+",hasRankB:"+hasRankB);
		
		rankService.createRank("rank_b");
		hasRankA = rankService.hasRank("rank_a");
		hasRankB = rankService.hasRank("rank_b");
		log.info("hasRankA:"+hasRankA+",hasRankB:"+hasRankB);
		
		rankService.deleteRank("rank_a");
		hasRankA = rankService.hasRank("rank_a");
		hasRankB = rankService.hasRank("rank_b");
		log.info("hasRankA:"+hasRankA+",hasRankB:"+hasRankB);
		
		rankService.deleteAllRank();
		hasRankA = rankService.hasRank("rank_a");
		hasRankB = rankService.hasRank("rank_b");
		log.info("hasRankA:"+hasRankA+",hasRankB:"+hasRankB);
	}
	/**
	 * 基本的增删该查
	 * @param rankService
	 */
	private void test2(IRankService rankService){
		rankService.createRank("rank_a");
		for (int i=0;i<100;i++) {
			rankService.put("rank_a", i, i);
		}
		int testId=30;
		RankData rankData1 = rankService.getRankDataById("rank_a", testId);
		rankService.put("rank_a", testId, 20);
		RankData rankData2 = rankService.getRankDataById("rank_a", testId);
		rankService.putIfAbsent("rank_a", testId, 60);
		RankData rankData3 = rankService.getRankDataById("rank_a", testId);
		rankService.delete("rank_a", testId);
		RankData rankData4 = rankService.getRankDataById("rank_a", testId);
		
		log.info("rankData1:"+rankData1);
		log.info("rankData2:"+rankData2);
		log.info("rankData3:"+rankData3);
		log.info("rankData4:"+rankData4);
	}
	public static void main(String[] args) {
		IRankService rankService = new RankService();
		rankService.createRank("rankName");
		rankService.put("rankName", 10/*id*/, 100/*value*/); // put date to rank
		RankData rankData = rankService.getRankDataById("rankName", 10); // get date from rank
		int rankNum = rankData.getRankNum(); // get rank num
	}
	/**
	 * 一些get操作
	 * @param rankService
	 */
	public void test3(IRankService rankService){
		rankService.createRank("rank_a");
		for (int i=0;i<100;i++) {
			rankService.put("rank_a", i, i);
		}
		
		int testId=30;
		RankData rankData1 = rankService.getRankDataById("rank_a", testId);
		RankData rankData2 = rankService.getRankDataByRankNum("rank_a", rankData1.getRankNum());
		List<RankData> rankDataList1 = rankService.getRankDatasAroundId("rank_a", testId, 3, 6);
		List<RankData> rankDataList2 = rankService.getRankDatasByPage("rank_a", 7, 9);
		int rankId = rankService.getRankId("rank_a", rankData1.getRankNum());
		int rankNum = rankService.getRankNum("rank_a", testId);
		
		log.info("rankData1:"+rankData1);
		log.info("rankData2:"+rankData2);
		log.info("rankDataList1:"+rankDataList1);
		log.info("rankDataList2:"+rankDataList2);
		log.info("rankId:"+rankId);
		log.info("rankNum:"+rankNum);
	}
	/**
	 * 多字段的操作
	 * @param rankService
	 */
	public void test4(IRankService rankService){
		rankService.createRank("rank_a", 3);
		for (int i=0;i<1000;i++) {
			int value1 = i/100;
			int value2 = i%100/10;
			int value3 = i%10;
			rankService.put("rank_a", i, value1,value2,value3);
		}
		int testId=30;
		RankData rankData1 = rankService.getRankDataById("rank_a", testId);
		rankService.put("rank_a", testId, 6,6,6);
		RankData rankData2 = rankService.getRankDataById("rank_a", testId);
		
		log.info("rankData1:"+rankData1);
		log.info("rankData2:"+rankData2);
	}
	/**
	 * 多线程操作
	 * @param rankService
	 * @throws InterruptedException 
	 */
	public void test5(final IRankService rankService) throws InterruptedException{
		int threadCount = 10;
		final CountDownLatch latch = new CountDownLatch(threadCount);
		
		rankService.createRank("rank_a");
		
		for(int threadI=0;threadI<threadCount ;threadI++){
			final int threadIndex = threadI;
			Thread thread = new Thread("threadIndex"+threadIndex){
				@Override
				public void run(){
					for(int i=0;i<100;i++){
						int base = threadIndex*100+i;
						rankService.put("rank_a", base, base);
					}
					latch.countDown();
				}
			};
			thread.start();
		}
		latch.await();
		
		int testId=30;
		RankData rankData1 = rankService.getRankDataById("rank_a", testId);
		rankService.put("rank_a", testId, 666);
		RankData rankData2 = rankService.getRankDataById("rank_a", testId);
		
		log.info("rankData1:"+rankData1);
		log.info("rankData2:"+rankData2);
	}
}
