package org.hq.rank.core.node;

import java.util.List;

import org.hq.rank.core.Rank;
import org.hq.rank.core.RankException;
import org.hq.rank.core.element.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RankElementNode extends Node{

	private static Logger log = LoggerFactory.getLogger(RankElementNode.class);
	/**
	 * Node类型的Rank参数
	 */
	private int conditionLevel; //>=0
	private RankElement rankElement;
	
	public RankElementNode(Rank rank){
		super(rank);
	}
	@Override
	public void init(Element element,long value,final int conditionLevel){
		super.init(element, value, conditionLevel);
		this.conditionLevel = conditionLevel;
		if(this.conditionLevel <= 0){
			throw new RankException("conditionLevel <= 0 in RankElementNode");
		}
		if(value != Long.MAX_VALUE){
			rankElement = rank.getRankPool().getRankElement(element, conditionLevel);
		}
	}

	public RankElement getRankElement() {
		return rankElement;
	}

	@Override
	public Element add(Element element) {
		int level = conditionLevel;
		boolean isLock = rank.getLockerPool().tryLockNodeRLocker(this, level);
		if(!isLock){
			return null;
		}
		boolean isNeedUnlock = true;
		try{
			if(getCount() <= 0){
				return null;
			}
			if(rankElement.add(element)){
				getAndIncrement();
				
				if(parentNS != null){
					parentNS.putElement();
				}
				return element;
			}
			return null;
		}finally{
			if(isNeedUnlock){
				rank.getLockerPool().unlockNodeRLocker(this, level);
			}
		}
	}
	
	@Override
	public int getRankValue(Element element) {
		return rankElement.get(element);
	}
	/**
	 * 这个可以通过传进来一个List<Element>，来减少new它的次数
	 * @param begin
	 * @param length
	 * @return
	 */
	@Override
	public void getElementsByIndex(List<Element> elementList , int begin ,int length) {
		if(getCount() <= begin || getCount()<1){
			return;
		}
		rankElement.getElementsByRankNum(elementList, begin, length);
		return;
	}

	@Override
	public boolean delete(Element element) {
		int level = conditionLevel;
		boolean _isLock = rank.getLockerPool().tryLockNodeRLocker(this, level);
		if(!_isLock){
			return false;
		}
		try {
			if(rankElement.delete(element)){
				getAndDecrement();
				
				if(parentNS != null){
					parentNS.removeElement();
				}
				return true;
			}
			return false;
		} finally {
			rank.getLockerPool().unlockNodeRLocker(this, level);
		}
	}
	
	@Override
	public String toString(){
		if(value == Long.MAX_VALUE){
			return "head";
		}
		String nodeString = "["+value+","+conditionLevel+"]\n";
		return nodeString+rankElement.toString();
	}
	
	@Override
	public void reset() {
		super.reset();
		conditionLevel = -1; // 这里先不重置其conditionlevel，以为解锁的时候需要它来定位对应的锁
		
		if(rankElement.getNodeCount()>0){
			log.warn("rankElement的node还没有删除完成，不回收该rankElement");
		}else{
			rank.getRankPool().putRankElement(rankElement);
		}
		rankElement = null;
	}
	@Override
	public int getConditionLevel() {
		return conditionLevel;
	}

}
