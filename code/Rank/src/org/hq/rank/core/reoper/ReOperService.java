package org.hq.rank.core.reoper;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.hq.rank.core.Rank;
import org.hq.rank.core.RankConfigure;
import org.hq.rank.core.RankException;
import org.hq.rank.core.element.Element;
import org.hq.rank.core.node.Node;
import org.hq.rank.core.node.RankElement;
import org.hq.rank.core.reoper.ReOper.OperType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReOperService {
	private static Logger log = LoggerFactory
			.getLogger(ReOperService.class);
	
	private final Rank rank;
	
	private LinkedBlockingQueue<ReOper> reOperQueue = new LinkedBlockingQueue<ReOper>();
	private Random random = new Random();
	private ScheduledExecutorService scheduleExecutorService = null;
	private ExecutorService singleExecutorService = Executors.newSingleThreadExecutor(); // 用来处理reOperQueue
	private ExecutorService multiExecutorService = null;
	// 配置
	private final int maxScheduleTime ;
	private final int warnReOperTimes;
	private final int errorReoperTimes;
	// distory 相关参数
	private volatile boolean isReOperRun = true;
	private volatile Thread distoryThread = null;
	private AtomicInteger reOperTaskCount = new AtomicInteger(0);
	// oper fail 通过这个，每次出现问题，都能通过栈追踪到问题发生的点，测试用的
	private Element failElement = null;
	
	
	public ReOperService(Rank rank){
		this.rank = rank;
		
		RankConfigure rankConfigure = rank.getRankConfigure();
		scheduleExecutorService = new RankScheduledThreadPool(rank);//Executors.newScheduledThreadPool(rankConfigure.getScheduleThreadCount());
		this.maxScheduleTime = rankConfigure.getMaxScheduleTime();
		multiExecutorService = Executors.newFixedThreadPool(rankConfigure.getMultiThreadCount());
		warnReOperTimes = rankConfigure.getWarnReOperTimes();
		errorReoperTimes = rankConfigure.getErrorReoperTimes();
		
		
		switch (rankConfigure.getReOperType()) {
		case SingleThread:
			doSingleThread();
			break;
		case MultiSche:
			doSingleToMultiSche();
			break;
		case MultiThread:
			doMultiThread();
			break;
		default:
			break;
		}
	}
	
	public void destory() throws InterruptedException{
		distoryThread = Thread.currentThread();
		synchronized (distoryThread) {
			isReOperRun = false;
			while(reOperTaskCount.get()>0){
				distoryThread.wait();
			}
		}
		if(reOperTaskCount.get() != 0 || reOperQueue.size() != 0){
			log.error("distory error:reOperTaskCount:"+reOperTaskCount.get() + ",reOperQueue:" +reOperQueue.size());
		}else{
			log.warn("rank distory success");
		}
		// 打断所有的reoper线程
		switch (rank.getRankConfigure().getReOperType()) {
		case SingleThread:
			singleExecutorService.shutdownNow();
			break;
		case MultiSche:
			singleExecutorService.shutdownNow();
			scheduleExecutorService.shutdownNow();
			break;
		case MultiThread:
			multiExecutorService.shutdownNow();
			break;
			
		default:
			break;
		}
	}
	
	public int getReOperQueueSize(){
		return reOperQueue.size();
	}
	
	private boolean addQueue(ReOper reOper){
		rank.getRankStatistics().addReOperCount();
		boolean result;
		try {
			result = reOperQueue.offer(reOper, 5, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		if(!result){
			log.error("addqueue false!");
			throw new RankException();
		}
		return result;
	}
	public boolean addQueue(Element element,OperType type,int times,Node node){
		return addQueue(element, type, times, node, null,null);
	}
	public boolean addQueue(OperType type,int times,Node node,RankElement rankElement){
		return addQueue(null, type, times, node, null,rankElement);
	}
	public boolean addQueue(Element element,OperType type,int times,Node node,Element oldElement,RankElement rankElement){
		ReOper reOper = new ReOper();
		reOper.setElement(element);
		reOper.setOperType(type);
		reOper.setTimes(0);
		reOper.setNode(node);
		reOper.setOldElement(oldElement);
		reOper.setRankElement(rankElement);
		
		boolean result = addQueue(reOper);
		if(result){
			reOperTaskCount.getAndIncrement();
		}
		
		return result;
	}
	
	private void doSingleThread(){
		singleExecutorService.execute(new Runnable() {
			@Override
			public void run() {
				try {
					while(true){
						ReOper reOper = reOperQueue.take();
						doReOper(reOper);
					}
				} catch (InterruptedException e) {
					if(!isReOperRun){
//						log.info("stop thread:"+Thread.currentThread().getName());
					}else{
						e.printStackTrace();
					}
				}
			}
		});
	}
	
	private void doSingleToMultiSche(){
		singleExecutorService.execute(new Runnable() {
			@Override
			public void run() {
				try {
					while(true){
						final ReOper reOper = reOperQueue.take();
						scheduleExecutorService.schedule(new Runnable() {
							@Override
							public void run() {
								doReOper(reOper);
							}
						}, random.nextInt(maxScheduleTime), TimeUnit.NANOSECONDS);
					}
				} catch (InterruptedException e) {
					if(!isReOperRun){
//						log.info("stop thread:"+Thread.currentThread().getName());
					}else{
						e.printStackTrace();
					}
				}
			}
		});
	}
	
	private void doMultiThread(){
		for(int i=0;i<rank.getRankConfigure().getMultiThreadCount();i++){
			multiExecutorService.execute(new Runnable() {
				@Override
				public void run() {
					try {
						while(true){
							ReOper reOper = reOperQueue.take();
							doReOper(reOper);
						}
					} catch (InterruptedException e) {
						if(!isReOperRun){
//							log.info("stop thread:"+Thread.currentThread().getName());
						}else{
							e.printStackTrace();
						}
					}
				}
			});
		}
	}
	private void doReOper(ReOper reOper){
		if(!rank.doReOper_(reOper,reOper.getTimes()>=errorReoperTimes)){
			int times = reOper.timesIncrementAndGet();
			if(times > warnReOperTimes && times < warnReOperTimes+3){
				failElement = reOper.getElement();
				log.warn("reopertimes is too many :"+reOper.toString());
			}
			if(times>errorReoperTimes){
				log.error("reopertimes is too many :"+reOper.toString());
				reOperTaskCount.getAndDecrement();
				throw new RankException("reopertimes is too many :"+reOper.toString());
			}else{
				addQueue(reOper);
			}
		}else{
			reOperTaskCount.getAndDecrement();
		}
		//
		if(!isReOperRun && reOperQueue.isEmpty()){
			synchronized (distoryThread) {
				distoryThread.notify();
			}
		}
	}
	
	public Element getFailElement() {
		return failElement;
	}
}
