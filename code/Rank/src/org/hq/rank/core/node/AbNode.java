package org.hq.rank.core.node;

import org.hq.rank.core.AbLinkBase;
import org.hq.rank.core.UnsafeSupport;

public abstract class AbNode extends AbLinkBase {
	public static final long elementCountOffset = UnsafeSupport.getValueOffset(AbNode.class, "elementCount");
	protected NodeStepBase parentNS;
	protected volatile int elementCount = 0;

	public abstract long getValue();
	
	public NodeStepBase getParentNS() {
		return parentNS;
	}

	public void setParentNS(NodeStepBase parentNS) {
		this.parentNS = parentNS;
	}
	
	public int getCount(){ // node的继承重写这个方法，判断是否为maxLong，返回0
		return elementCount;
	}
	public void setCount(int newValue){
		elementCount = newValue;
	}
	
	public int getAndAdd(int delta){
		return UnsafeSupport.getAndAdd(this, elementCountOffset, delta);
	}
	public int getAndIncrement(){
		return UnsafeSupport.getAndIncrement(this, elementCountOffset);
	}
	public int getAndDecrement(){
		return UnsafeSupport.getAndDecrement(this, elementCountOffset);
	}
	public int decrementAndGet(){
		return UnsafeSupport.decrementAndGet(this, elementCountOffset);
	}
	
}
