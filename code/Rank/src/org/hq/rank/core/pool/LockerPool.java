package org.hq.rank.core.pool;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hq.rank.core.Rank;
import org.hq.rank.core.RankException;
import org.hq.rank.core.node.Node;
import org.hq.rank.core.node.RankElement;

/**
 * 每级node有自己的锁库，通过某种规则获取，如hashcode，或对应的value
 * 同一个node一定是一样的hashcode，所以不会有问题，即使不同的node用了同样的hashcode
 * @author zhen
 *
 */
public class LockerPool implements ILockerPool{
	private final Rank rank;
	//
	private final int levelCount;
	private final LockerBox[] lockerBoxs;
	
	static class LockerBox{
		// node
		private int nodeLockerCount;
		private ReadWriteLock[] nodeLockers;
		private Lock[] nodeReadLock;
		private Lock[] nodeWriteLock;
		// rankElement
		private int rankElementLockerCount;
		private ReadWriteLock[] rankElementLockers;
		private Lock[] rankElementReadLock;
		private Lock[] rankElementWriteLock; 
	}
	
	public static void main(String[] args){
		Integer lockerBox = new Integer(0);
		System.err.println(lockerBox.hashCode());
		lockerBox = 4;
		System.err.println(lockerBox.hashCode());
	}
	
	public LockerPool(Rank rank){
		this.rank = rank;
		levelCount = rank.getRankConfigure().getRankConditionCount();
		lockerBoxs = new LockerBox[levelCount];
		
		for(int i = 0; i<levelCount ; i++){
			lockerBoxs[i] = new LockerBox();
			// node
			lockerBoxs[i].nodeLockerCount = 100000;
			lockerBoxs[i].nodeLockers = new ReentrantReadWriteLock[lockerBoxs[i].nodeLockerCount];
			lockerBoxs[i].nodeReadLock = new Lock[lockerBoxs[i].nodeLockerCount];
			lockerBoxs[i].nodeWriteLock = new Lock[lockerBoxs[i].nodeLockerCount];
			for(int k = 0; k<lockerBoxs[i].nodeLockerCount;k++){
				lockerBoxs[i].nodeLockers[k] = new ReentrantReadWriteLock();
				lockerBoxs[i].nodeReadLock[k] = lockerBoxs[i].nodeLockers[k].readLock();
				lockerBoxs[i].nodeWriteLock[k] = lockerBoxs[i].nodeLockers[k].writeLock();
			}
			// rankElement
			lockerBoxs[i].rankElementLockerCount = 10;
			lockerBoxs[i].rankElementLockers = new ReentrantReadWriteLock[lockerBoxs[i].rankElementLockerCount];
			lockerBoxs[i].rankElementReadLock = new Lock[lockerBoxs[i].rankElementLockerCount];
			lockerBoxs[i].rankElementWriteLock = new Lock[lockerBoxs[i].rankElementLockerCount];
			for(int k = 0; k<lockerBoxs[i].rankElementLockerCount;k++){
				lockerBoxs[i].rankElementLockers[k] = new ReentrantReadWriteLock();
				lockerBoxs[i].rankElementReadLock[k] = lockerBoxs[i].rankElementLockers[k].readLock();
				lockerBoxs[i].rankElementWriteLock[k] = lockerBoxs[i].rankElementLockers[k].writeLock();
			}
		}
	}
	@Override
	public boolean tryLockNodeWLocker(Node node , int conditionLevel) {
		if(conditionLevel < 0){
			return false;
		}
		LockerBox lockerBox = lockerBoxs[conditionLevel];
		boolean isLock = lockerBox.nodeWriteLock[node.hashCode()%lockerBox.nodeLockerCount].tryLock();
		// 如果这个是被回收了，且处于不同的level，这里可以返回false，而如果处于同样的level，也可以作为本次锁，不会发生冲突，后面要再仔细分析
		if(node.getConditionLevel() != conditionLevel){ // 锁错了
			if(node.getValue() == Long.MAX_VALUE){
				return isLock;
			}
			if(isLock){
				lockerBox.nodeWriteLock[node.hashCode()%lockerBox.nodeLockerCount].unlock();
			}
			return false;
		}
		return isLock;
	}

	@Override
	public void unlockNodeWLocker(Node node , int conditionLevel) {
		LockerBox lockerBox = lockerBoxs[conditionLevel];
		lockerBox.nodeWriteLock[node.hashCode()%lockerBox.nodeLockerCount].unlock();
	}

	@Override
	public boolean tryLockNodeRLocker(Node node , int conditionLevel) {
		if(conditionLevel < 0){
			return false;
		}
		LockerBox lockerBox = lockerBoxs[conditionLevel];
		boolean isLock = lockerBox.nodeReadLock[node.hashCode()%lockerBox.nodeLockerCount].tryLock();
		if(node.getConditionLevel() != conditionLevel){ // 锁错了
			if(node.getValue() == Long.MAX_VALUE){
				return isLock;
			}
			System.err.println("r,suocuole:"+node.getValue()+","+node.getClass()+","+node.getConditionLevel()+","+conditionLevel);
			if(isLock){
				lockerBox.nodeReadLock[node.hashCode()%lockerBox.nodeLockerCount].unlock();
			}
			return false;
		}
		return isLock;
	}

	@Override
	public void unlockNodeRLocker(Node node , int conditionLevel) {
		LockerBox lockerBox = lockerBoxs[conditionLevel];
		lockerBox.nodeReadLock[node.hashCode()%lockerBox.nodeLockerCount].unlock();
	}

	/**
	 * 说明：当创建nodestep的时候用到这个锁，此时rankElement是不可能被reset的，除非step的大小设置不合理，太小
	 */
	@Override
	public void lockRankElementWLocker(RankElement rankElement,
			int conditionLevel) {
		LockerBox lockerBox = lockerBoxs[conditionLevel];
		lockerBox.rankElementWriteLock[rankElement.hashCode()%lockerBox.rankElementLockerCount].lock();
		// 如果这个是被回收了，且处于不同的level，这里可以返回false，而如果处于同样的level，也可以作为本次锁，不会发生冲突，后面要再仔细分析
		if(rankElement.getConditionLevel() != conditionLevel){ // 锁错了，在此情况下，不可能做更改
			throw new RankException("lockRankElementWLocker rankElement.getConditionLevel() != conditionLevel");
		}
	}

	@Override
	public void unLockRankElementWLocker(RankElement rankElement,
			int conditionLevel) {
		LockerBox lockerBox = lockerBoxs[conditionLevel];
		lockerBox.rankElementWriteLock[rankElement.hashCode()%lockerBox.rankElementLockerCount].unlock();
	}

	@Override
	public void lockRankElementRLocker(RankElement rankElement,
			int conditionLevel) {
		LockerBox lockerBox = lockerBoxs[conditionLevel];
		lockerBox.rankElementReadLock[rankElement.hashCode()%lockerBox.rankElementLockerCount].lock();
		// 如果这个是被回收了，且处于不同的level，这里可以返回false，而如果处于同样的level，也可以作为本次锁，不会发生冲突，后面要再仔细分析
		if(rankElement.getConditionLevel() != conditionLevel){ // 锁错了，在此情况下，不可能做更改
			throw new RankException("lockRankElementRLocker rankElement.getConditionLevel() != conditionLevel");
		}
	}

	@Override
	public void unLockRankElementRLocker(RankElement rankElement,
			int conditionLevel) {
		LockerBox lockerBox = lockerBoxs[conditionLevel];
		lockerBox.rankElementReadLock[rankElement.hashCode()%lockerBox.rankElementLockerCount].unlock();
	}

}
