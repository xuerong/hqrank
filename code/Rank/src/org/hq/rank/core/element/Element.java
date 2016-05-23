package org.hq.rank.core.element;

import java.util.concurrent.atomic.AtomicInteger;

import org.hq.rank.core.Rank;
import org.hq.rank.core.RankException;
import org.hq.rank.core.node.Node;
import org.hq.rank.core.pool.RankPoolElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Element implements RankPoolElement{
	private static Logger log = LoggerFactory
			.getLogger(Element.class);
	private final Rank rank; //  该Element所处的Rank
	
	private int id;
	private AtomicInteger locker = new AtomicInteger(0);
	
	private Node node;
	private ElementStep step;
	private Element next;
	private Element previous;
	
	private long[] value;
	
	public Element(Rank rank){
		this.rank = rank;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public long[] getValue() {
		return value;
	}

	public void setValue(long[] value) {
		this.value = value;
	}
	public boolean lock(){
		rank.getRankStatistics().addElementLockCount();
		int isLock = locker.getAndIncrement();
		if(isLock>0){
			rank.getRankStatistics().addElementUnlockCount();
			locker.getAndDecrement();
			return false;
		}
		return true;
	}
	/**
	 * 只有lock成功，才能unlock
	 * */
	public void unLock(){
		rank.getRankStatistics().addElementUnlockCount();
		locker.getAndDecrement();
		if(locker.get()<0){
			log.error("<0:"+locker.get()+","+id+","+getNode().getValue());
			throw new RankException();
		}
	}
	
	public Node getNode() {
		return node;
	}
	public void setNode(Node node) {
		this.node = node;
	}
	public Element getNext() {
		return next;
	}
	public void setNext(Element next) {
		this.next = next;
	}
	public Element getPrevious() {
		return previous;
	}
	public void setPrevious(Element previous) {
		this.previous = previous;
	}
	public ElementStep getStep() {
		return step;
	}
	public void setStep(ElementStep step) {
		this.step = step;
	}
	/**
	 * 两个Element的值是否相等
	 * @param element
	 * @return
	 */
	public boolean equalsValue(Element element){
		if(value == null && element.value ==null){
			return true;
		}
		if(value == null || value.length == 0 || element.value == null || element.value.length == 0){
			return false;
		}
		if(value.length != element.value.length){
			return false;
		}
		int i=0;
		for (long l : value) {
			if(l != element.value[i++]){
				return false;
			}
		}
		return true;
	}
	
	@Override
	public String toString(){
		StringBuilder valueStr = new StringBuilder(",values:");
		for (long l : value) {
			valueStr.append(","+l);
		}
		return "id:"+id+"locker:"+locker.get()+valueStr.toString();
	}

	@Override
	public void reset() {
//		locker.set(0);
		node = null;
		step = null;
		next = null;
		previous = null;
		id = 0;
		value = null;
	}
}
