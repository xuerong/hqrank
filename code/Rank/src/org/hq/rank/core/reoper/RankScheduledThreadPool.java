package org.hq.rank.core.reoper;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.hq.rank.core.Rank;

public class RankScheduledThreadPool extends ScheduledThreadPoolExecutor{
	private final Rank rank;
	public RankScheduledThreadPool(Rank rank){
		super(rank.getRankConfigure().getScheduleThreadCount(),
				new RankThreadFactory(rank.getRankConfigure().getRankName()),
				new RankRejectedExecutionHandler(rank.getRankConfigure().getRankName()));
		this.rank = rank;
		// 最大值要合适，不能影响其它业务
		setMaximumPoolSize(rank.getRankConfigure().getMaxScheduleThreadCount());
	}
	
	/**
	 * copy from 
	 * {@code java.util.concurrent.Executors.DefaultThreadFactory}
	 * @author zhen
	 *
	 */
	static class RankThreadFactory implements ThreadFactory{
		@Override
	    public Thread newThread(Runnable runnable){
	        Thread thread = new Thread(group, runnable, (new StringBuilder()).append(namePrefix).append(threadNumber.getAndIncrement()).toString(), 0L);
	        if(thread.isDaemon())
	            thread.setDaemon(false);
	        if(thread.getPriority() != 5)
	            thread.setPriority(5);
	        return thread;
	    }
	
	    private static final AtomicInteger poolNumber = new AtomicInteger(1);
	    private final ThreadGroup group;
	    private final AtomicInteger threadNumber = new AtomicInteger(1);
	    private final String namePrefix;
	
	
	    RankThreadFactory(final String name){
	        SecurityManager securitymanager = System.getSecurityManager();
	        group = securitymanager == null ? Thread.currentThread().getThreadGroup() : securitymanager.getThreadGroup();
	        namePrefix = (new StringBuilder()).append(name).append("-Pool-").append(poolNumber.getAndIncrement()).append("-thread-").toString();
	    }
	}
	/**
	 * copy from 
	 * {@code java.util.concurrent.ThreadPoolExecutor.AbortPolicy}
	 * 
	 * 这里要对排行处理失败的数据持久化，如果这里出现问题，说明出现了大问题，如排行数据访问量过大
	 * 
	 * @author zhen
	 *
	 */
	public static class RankRejectedExecutionHandler implements RejectedExecutionHandler{
		private final String name;
		@Override
	    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadpoolexecutor){
	        throw new RejectedExecutionException((new StringBuilder()).append(name).append("-Task ").append(runnable.toString()).append(" rejected from ").append(threadpoolexecutor.toString()).toString());
	    }
	
	    public RankRejectedExecutionHandler(final String name){
	    	this.name = name;
	    }
	}
}
