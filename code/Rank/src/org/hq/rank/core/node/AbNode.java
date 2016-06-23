package org.hq.rank.core.node;

import java.util.concurrent.atomic.AtomicInteger;

import org.hq.rank.core.AbLinkBase;

public abstract class AbNode extends AbLinkBase {
	protected NodeStepBase parentNS;
	protected volatile AtomicInteger elementCount = new AtomicInteger(0);

	public abstract long getValue();
	
	public NodeStepBase getParentNS() {
		return parentNS;
	}

	public void setParentNS(NodeStepBase parentNS) {
		this.parentNS = parentNS;
	}
	
	public int getCount(){ // node的继承重写这个方法，判断是否为maxLong，返回0
		return elementCount.get();
	}
	
}
