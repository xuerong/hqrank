package test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections.list.SetUniqueList;
import org.hq.rank.core.IRank;
import org.hq.rank.core.Rank;
import org.hq.rank.core.RankConfigure;
import org.hq.rank.core.RankData;
import org.hq.rank.core.node.RankElement;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class Test {
	private int testCount = 200;
	private CountDownLatch latch = new CountDownLatch(testCount);
	private CyclicBarrier barrier = new CyclicBarrier(testCount);
	private CountDownLatch latchGet = new CountDownLatch(testCount);
	private Random random = new Random();
	final JedisPool pool;
	final String testKey = "test4";
	final IRank rank;
	final List<Integer> nullRankDatas = new ArrayList<>();
	public static void main(String[] args) throws Exception{
		
//		System.err.println(Double.MAX_VALUE);//1.7976931348623157E308
//		System.err.println(Long.MAX_VALUE);//9223372036854775807
		new Test().test4();
//		new Test().test2(200,1000);
	}
	public void test7(){
		final ConcurrentHashMap<Integer, A> maps = new ConcurrentHashMap<>();
//		final Hashtable<Integer, A> maps = new Hashtable<>();
		for(int i=0;i<1;i++){
			new Thread(){
				@Override
				public void run(){
					while(true){
						A b;
						synchronized (maps) {
							A a = new A(); 
							b=a;
						}
						if(maps.containsKey(b.hashCode())){
							System.err.println("=========================================================");
						}
						maps.put(b.hashCode(), b);
					}
				}
			}.start();
		}
		
	}
	class A{
		int a = 5;
		int b = 6;
	}
	public void test6(){
		RankConfigure rankConfigure = new RankConfigure();
		rankConfigure.setRankConditionCount(3);
		IRank rank =  new Rank(rankConfigure);
		for(int i=0;i<2;i++){
			rank.set(1, 1321,636,5656);
		}
		System.err.println("finish");
	}
	// 测试根据名次获取id
	public void test5(){
		Rank rank = new Rank();
		rank.getRankConfigure().setRankConditionCount(2);
		rank.set(12, 123,123);
		rank.set(23, 223,123);
		List<RankData> rankDataList = rank.getRankDatasByRankNum(0, 5);
		System.err.println(rankDataList.size());
		for (RankData rankData : rankDataList) {
			System.err.println(rankData);
		}
	}
	
	public void test(){
		Jedis jedis = new Jedis("192.168.1.240", 6379);
		jedis.del("aaa");
//		jedis.auth("admin");
		jedis.zadd("aaa", 12, "xiaoaing");
		jedis.zadd("aaa", 13, "xiaogang");
		jedis.zadd("aaa", 12, "xiaofang");
		
		jedis.zadd("aaa", 2, "xiaoli");
//		jedis.zadd("aaa", 1, "xiaogang");
		
		System.err.println(jedis.zrank("aaa", "xiaoaing"));
		System.err.println(jedis.zrank("aaa", "xiaogang"));
		System.err.println(jedis.zrank("aaa", "xiaoli"));
		System.err.println(jedis.zrank("xxx", "xiaofang"));
	}
	long time1=0,time2=0,time3=0;
	
	public void test4() throws InterruptedException{
		RankConfigure rankConfigure = new RankConfigure();
		rankConfigure.setRankConditionCount(3);
		final Rank rank = new Rank(rankConfigure);
		final Rank rank2 = new Rank();
		
		final int threadCount = 200;
		final int initValueCount = 100000;
		final int intervalPerSet = 50; //  每50毫秒添加或修改一次，random.nextInt(100)
		final long maxValue = 1000000000000000000l;// 值的范围
		final int maxId = 2000000;// id范围
		final Random random = new Random();
		final String redisKey = "redisKey1";
		final boolean isUseRank = true;
		final boolean isUseRedis = false;
		final boolean isUseRank2 = true;
//		final HashSet<Integer> ids = new HashSet<>();
//		final List<Integer> ids = SetUniqueList.decorate(new ArrayList<Integer>());
		final AtomicInteger count = new AtomicInteger();
		final int allCount = 1000000;
		final CountDownLatch latch = new CountDownLatch(threadCount);
		long t1 = System.nanoTime();
		for(int i=0;i<threadCount;i++){
			Thread thread = new Thread(){
				@Override
				public void run(){
					Jedis jedis ;
					if(isUseRedis){
						jedis = pool.getResource();
					}
					while(true){
//						if(count.getAndIncrement() > allCount){
//							latch.countDown();
//							break;
//						}
//						long value = Math.abs(random.nextLong())%maxValue;
						long value = random.nextInt(100000000);
//						long value1 = value/10000000000000l,
//								value2 = value%10000000000000l/10000000l,
//								value3 = value%10000000l;
//								value4 = value%1000;
						long value1 = value/1000000l,
								value2 = value%1000000l/100,
								value3 = value%100l;
//								value4 = value%100l;
						int id = random.nextInt(maxId)+1;
						
						int interval = random.nextInt(intervalPerSet);
						long t1=System.nanoTime();
						if(isUseRedis){
							jedis.zadd(redisKey, value, redisKey+id);
						}
						long t2=System.nanoTime();
						
						if(isUseRank){
							rank.set(id, value1,value2,value3);
						}
						
						long t3=System.nanoTime();
						if(isUseRank2){
							rank2.set(id, value);
						}
						time1+=(t2-t1)/1000000;
						time2+=(t3-t2)/1000000;
//						ids.add(id);
						try {
							Thread.sleep(interval);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			};
			thread.start();
//			thread.
		}
//		latch.await();
//		System.err.println("over:"+(System.nanoTime() - t1)/1000000);
		// 每10秒打印一次 结果
		new Thread(){
			@Override
			public void run(){
				Jedis jedis ;
				if(isUseRedis){
					jedis = pool.getResource();
				}
				int printTime = 0;
				while (true) {
					System.err.println("-----------------------"+(printTime++)+"------------------------");
//					System.err.println("-----------------------"+time1+","+TimeTest.time1+","+TimeTest.time2+"------------------------");
					if(isUseRank2){
						System.out.println(rank2.rankStatisticsInfo());
					}else if(isUseRank){
						System.out.println(rank.rankStatisticsInfo());
					}
					int baseValue = maxId/2;
					for(int i=0;i<10;i++){
						int id = baseValue+i;
						StringBuilder sb = new StringBuilder("---------------------------------------"+(id)+":");
						if(isUseRank){
							sb.append(rank.get(id));
						}
						if(isUseRedis){
							sb.append(","+jedis.zrevrank(redisKey,redisKey+(id)));
						}
						if(isUseRank2){
							sb.append(","+rank2.get(id));
						}
						System.out.println(sb.toString());
					}
					System.out.println("ranElementId:"+rank.getRankElementNodeMap().getId()+"|"+rank.getRankPool());
//					if(ids.size()>0){
//						for(int i=0;i<10;i++){
//							int id = ids.get(random.nextInt(ids.size()));
//							System.out.println("---------------------------------------"+(id)+":"
////									+rank.get(id).getRankNum()
//									+","+jedis.zrevrank(redisKey,redisKey+(id))
//									+","+rank2.get(id).getRankNum()
//									);
//						}
//					}
					
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
		
	}
	
	public void test1(){ 
		boolean isRedis = false;
		try {
			
			int threadCount = 200;
			int countPerThread = 2000;
			long t1= System.nanoTime();
			if(isRedis){
				doRank1(threadCount, countPerThread,Type.Redis);
			}
			long t2 = System.nanoTime();
			if(isRedis){
				doGet1(threadCount, countPerThread,Type.Redis);
			}
			long t3 = System.nanoTime();
			doRank1(threadCount, countPerThread,Type.Rank);
			
			long t4 = System.nanoTime();
//			doGet1(threadCount, countPerThread,Type.Rank);
			long t5 = System.nanoTime();
			rank.destory();
			long t6 = System.nanoTime();
			RankData rankData2 = null;
			for(int i=0;i<10;i++){
				int testGetId = i*(threadCount*countPerThread/10)/*+54+(i*10)*/;
				if(isRedis){
					System.out.println(pool.getResource().zrevrank(testKey, testKey+testGetId)+
							",value:"+pool.getResource().zscore(testKey, testKey+testGetId));
				}
				RankData rankData = rank.get(testGetId);
				System.out.println(rankData);
				if(i == 120){
					rankData2 = rankData;
				}
			}
			// null rankdata
			System.out.println("----------------------null rankdata---------------------");
			for (Integer integer : nullRankDatas) {
				RankData rankData = rank.get(integer);
				long redisRank = -1;
				if(isRedis){
					redisRank = pool.getResource().zrevrank(testKey, testKey+integer);
				}
				System.err.println("id:"+integer+",rank:"+rankData+",redisRank:"+redisRank);
			}
			System.out.println("----------------------null rankdata end---------------------");
//			List<RankData> rankDatas = rank.getRankDatasByRankNum(rankData2.getRankNum() - 7, 11);
//			System.err.println(rankDatas.size());
//			for (RankData rankData : rankDatas) {
//				System.err.println(rankData);
//			}
			// 全部打印
//			for(int i=1;i<5136;i++){
//				RankData rankData = rank.get(i);
//				long redisRank = -1;
//				double redisValue = -1;
//				if(isRedis){
//					redisRank = pool.getResource().zrevrank(testKey, testKey+i);
//					redisValue = pool.getResource().zscore(testKey, testKey+i);
//				}
//				System.err.println("id:"+i+",rank:"+rankData+",redisRank:"+redisRank+",redisValue:"+redisValue);
//			}
			
			System.out.println();
//			System.out.println(rank.toString());
			System.out.println(rank.rankStatisticsInfo());
			if(isRedis){
				System.err.println("redis:set1:"+(t2-t1)/1000000+",get1:"+(t3-t2)/1000000);
			}
			System.err.println("rank:set1:"+(t4-t3)/1000000+
					",get1:"+(t5-t4)/1000000+",destroy:"+(t6-t5)/1000000);
			System.err.println("over");
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	// 测试多个redis io，以确定redis io所占用的时间
	public void test2(int threadCount,final int count){
		long t1 = System.nanoTime();
		
		final CyclicBarrier barrier = new CyclicBarrier(threadCount);
		final CountDownLatch latch = new CountDownLatch(threadCount);
		for(int i=0;i<threadCount;i++){
			final int index = i;
			new Thread(){
				@Override
				public void run(){
					// 一个线程一个jedis实例
					Jedis jedis = pool.getResource();
					barrierAwait(barrier); // 所有线程到齐之后再开始
					for(int i=0;i<count ;i++){
//						jedis.set("123", "");
//						jedis.clusterInfo();
					}
					if(jedis != null){
						jedis.close();
					}
					latch.countDown();
				}
			}.start();
		}
		latchAwait(latch); // 所有线程结束才算完成
		
		long t2 = System.nanoTime();
		System.err.println("iotime:"+(t2-t1)/1000000);
	}
	/**
	 * test3启动多个线程，不断的向rank中添加或修改排行，
	 * 初始添加十万个，然后每分钟添加十万个，修改十万个
	 */
	private void test3(){
		final int threadCount = 200;
		final int initValueCount = 100000;
		final int intervalPerSet = 50; //  每50毫秒添加或修改一次，random.nextInt(100)
		final int maxValue = 10000000;// 值的范围
		final int maxId = 5000000;// id范围
		final Random random = new Random();
		final String redisKey = "redisKey3";
		
		for(int i=0;i<threadCount;i++){
			new Thread(){
				@Override
				public void run(){
					final Jedis jedis = pool.getResource();
					while(true){
						int value = random.nextInt(maxValue);
						int id = random.nextInt(maxId);
						int interval = random.nextInt(intervalPerSet);
						rank.set(id, value);
						jedis.zadd(redisKey, value, redisKey+id);
						try {
							Thread.sleep(interval);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}.start();
		}
		// 每10秒打印一次 结果
		new Thread(){
			@Override
			public void run(){
				final Jedis jedis = pool.getResource();
				int printTime = 0;
				while (true) {
					System.out.println("-----------------------"+(printTime++)+"------------------------");
					System.out.println(rank.toString());
					for(int i=0;i<10;i++){
						System.out.println("---------------------------------------"+(1234321+i)+":"
								+rank.get(1234321+i)+","+jedis.zrevrank(redisKey,redisKey+(1234321+i))+
								",value:"+jedis.zscore(redisKey,redisKey+(1234321+i)));
						
					}
					
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
		
	}
	
	private void doRank1(int threadCount,final int count,final Type type){
		final CyclicBarrier barrier = new CyclicBarrier(threadCount);
		final CountDownLatch latch = new CountDownLatch(threadCount);
		for(int i=0;i<threadCount;i++){
			final int index = i;
			new Thread(){
				@Override
				public void run(){
					// 一个线程一个jedis实例
					Jedis jedis = null;
					if(type == type.Redis){
						jedis = pool.getResource();
					}
					barrierAwait(barrier); // 所有线程到齐之后再开始
					for(int i=0;i<count ;i++){
						setByType(type, createValue(index*count + i), createId(index*count + i), jedis);
					}
//					System.err.println("here");
					if(jedis != null){
						jedis.close();
					}
					latch.countDown();
					
//					System.err.println("wancheng:"+latch.getCount());
				}
			}.start();
		}
		latchAwait(latch); // 所有线程结束才算完成
		
	}
	private void doGet1(int threadCount,final int count,final Type type){
		final CyclicBarrier barrier = new CyclicBarrier(threadCount);
		final CountDownLatch latch = new CountDownLatch(threadCount);
		for(int i=0;i<threadCount;i++){
			final int index = i;
			new Thread(){
				@Override
				public void run(){
					// 一个线程一个jedis实例
					Jedis jedis = null;
					if(type == type.Redis){
						jedis = pool.getResource();
					}
					barrierAwait(barrier);
					for(int i=0;i<count ;i++){
						getByType(type, createId(index*count + i), jedis);
					}
					if(jedis != null){
						jedis.close();
					}
					latch.countDown();
				}
			}.start();
		}
		latchAwait(latch);
	}
	Test(){
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(401);
		config.setMinIdle(401);
		pool = new JedisPool(config, "192.168.1.240");
		RankConfigure rankConfigure = new RankConfigure();
		rankConfigure.setRankConditionCount(1);
		rank = new Rank(rankConfigure);
	}
	private int createId(int i){
//		return (i)%50136+1; // 这样会由于不同线程而导致不同的结果很正常，由于改变是顺序的，导致删除node的顺序及其不确定，对并发能力挑战很大
//		return random.nextInt(i+1)+1;
		return i+1;
//		return i%5000+1;
//		return i%2+1; // 两个id还是有问题
	}
	private long createValue(long value){
//		return random.nextInt(100);
//		int v= random.nextInt(1000)+1;
//		return value%(v+1);
		return 1000009 -(value+1);
//		return value;
//		return value%1000+1;
//		return value%254609*23+value%23;
//		return random.nextInt((int)value+1)+1;
	}
	private void setByType(Type type,long value,int id,Jedis jedis){
		if(type == Type.Redis){
			jedis.zadd(testKey, /*Integer.MAX_VALUE - */value, testKey+id);
		}else if(type == Type.Rank){
			rank.set(id, value);
//			rank.set(id, value/100,value%100);
//			rank.set(id, value/100000000,value%100000000/1000,value%1000);
		}
	}
	private void getByType(Type type,int id,Jedis jedis){
		if(type == Type.Redis){
//			jedis.zrank(testKey, testKey+id);
			jedis.zrevrank(testKey, testKey+id);
		}else if(type == Type.Rank){
			RankData rankData = rank.get(id);
			if(rankData == null){
//				System.err.println("id:"+id);
//				nullRankDatas.add(id);
			}
		}
	}
	enum Type{
		Redis,Rank
	}
	private void barrierAwait(CyclicBarrier barrier){
		try {
			barrier.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void latchAwait(CountDownLatch latch){
		try {
			latch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
