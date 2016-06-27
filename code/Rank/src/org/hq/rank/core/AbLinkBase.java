package org.hq.rank.core;
/**
 * 所有处于链中的类都集成自该类，包括：
 * {@code Node}
 * {@code Element}
 * {@code NodeStep}
 * {@code NodeStepStep}
 * {@code ElementStep}
 * @author zhen
 *
 */
public abstract class AbLinkBase  {
	protected AbLinkBase previous;
	protected AbLinkBase next;
	
	public AbLinkBase getPrevious() {
		return previous;
	}
	public void setPrevious(AbLinkBase previous) {
		this.previous = previous;
	}
	public AbLinkBase getNext() {
		return next;
	}
	public void setNext(AbLinkBase next) {
		this.next = next;
	}
	
	
}
