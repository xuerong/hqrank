package test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.hq.rank.core.RankData;
import org.hq.rank.service.IRankService;
import org.hq.rank.service.RankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 压力测试
 * 分为：
 * 1、压力测试下的正确性，用redis测试
 * 2、大数据执行效率，即执行时间
 * 测试方法分为两种：
 * 1、一定量的数据和线程，要多久执行完成
 * 2、每秒钟执行一定量的线程和数据，持续进行
 * @author a
 *
 */
public class BaseTest2 {
	private static Logger log = LoggerFactory.getLogger(BaseTest2.class);
	
	public static void main(String[] args) throws InterruptedException{
		IRankService rankService = new RankService();
		
		BaseTest2 test = new BaseTest2();
		test.test1(rankService);
		
		rankService.deleteAllRank();
	}
	/**
	 * 多线程
	 * @param rankService
	 * @throws InterruptedException
	 */
	private void test1(final IRankService rankService) throws InterruptedException{
		final int threadCount = 100;
		final int dataCountPerThread = 1000;
		final int maxId = 100000;
		final int maxValue = 1000000;
		Thread[] threads = new Thread[threadCount];
		final int[][] ids = new int[threadCount][];
		final long[][] values = new long[threadCount][];
		
		final CountDownLatch latch = new CountDownLatch(threadCount);
		
		rankService.createRank("rank_a");
		// 生成id和数据
		for(int i = 0;i<threadCount;i++){
			ids[i] = new int[dataCountPerThread];
			values[i] = new long[dataCountPerThread];
			for(int j = 0;j<dataCountPerThread;j++){
				ids[i][j] = randomId(maxId);
				values[i][j] = randomValue(maxValue);
			}
		}
		// 生成线程
		for(int threadI=0;threadI<threadCount ;threadI++){
			final int threadIndex = threadI;
			Thread thread = new Thread("threadIndex"+threadIndex){
				@Override
				public void run(){
					for(int i=0;i<dataCountPerThread;i++){
						rankService.put("rank_a", ids[threadIndex][i], values[threadIndex][i]);
					}
					latch.countDown();
				}
			};
			threads[threadI] = thread;
		}
		// 执行
		long t1 = System.nanoTime();
		for(int threadI=0;threadI<threadCount ;threadI++){
			threads[threadI].start();
		}
		latch.await();
		long t2 = System.nanoTime();
		log.info("useTime:"+(t2-t1)/1000000);
		// get
		int testId=30;
		for(int i=0;i<10;i++){
			RankData rankData = rankService.getRankDataById("rank_a", testId+i);
			log.info("rankData1:"+rankData);
		}
		rankService.put("rank_a", testId, 1);
		RankData rankData2 = rankService.getRankDataById("rank_a", testId);
		
		log.info("rankData2:"+rankData2);
	}
	
	
	private Random random = new Random();
	private int randomId(int maxId){
		maxId = Math.abs(maxId);
		return random.nextInt(maxId);
	}
	private long randomValue(long maxValue){
		maxValue = Math.abs(maxValue);
		return Math.abs(random.nextLong())%maxValue;
	}
}
