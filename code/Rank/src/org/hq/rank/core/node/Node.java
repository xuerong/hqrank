package org.hq.rank.core.node;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.hq.rank.core.Rank;
import org.hq.rank.core.element.Element;
import org.hq.rank.core.pool.RankPoolElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/***
 * Node分为两种，一种是存储数据的，即存储一个element链表，另一种是存储一个rank，即在同一个这个分数下，根据
 * 另一种分数进行排行
 * @author zhen
 *
 */
public abstract class Node extends AbNode implements INode,RankPoolElement{
	private static Logger log = LoggerFactory
			.getLogger(Node.class);
	protected final Rank rank; // 该node所处的rank
	// 该Node对应的值
	protected long value;
//	protected NodeStep nodeStep; // 所在的nodeStep
	// 当添加或删除node的时候，才用到这个锁,对node内部的控制不用该锁
//	private AtomicInteger locker = new AtomicInteger(0);
	// Node中的Element数量
	protected volatile AtomicInteger elementCount = new AtomicInteger(0);
	
//	private Node previous;
//	private Node next;
	/**
	 * 构造的时候有没有可能被访问？add方法
	 * */
	public Node(Rank rank){
		this.rank = rank;
	}
	
	public void init(Element element,long value,final int conditionLevel){
		this.value = value;
		// 第一个Node的值是MAX_VALUE，是个索引
		if(value != Long.MAX_VALUE){
			elementCount.getAndIncrement();
		}else{
			// 第一个字段默认就创建三层索引
			if(conditionLevel == rank.getRankConfigure().getRankConditionCount() - 1){
//				NodeStepStep nodeStepStep = rank.getRankPool().getNodeStepStep();
				// 这里设置了null，它上面就不会有更多层了，否则，会有更多层，不过还有待测试
				NodeStepBase nodeStepStep = rank.getRankPool().getNodeStepBase(null); 
//				this.nodeStep = rank.getRankPool().getNodeStep(nodeStepStep);

//				System.err.println("this.nodeStep:"+this.nodeStep + value);
//				this.nodeStep.putAbNode(this);
//				nodeStepStep.putNodeStep(this.nodeStep);
//				System.err.println(nodeStepStep);
				this.parentNS = rank.getRankPool().getNodeStepBase(nodeStepStep);
//				this.parentNS.putAbNode(this);
				this.parentNS.putAbNodeWithElement(this);
//				nodeStepStep.putNodeStep((NodeStep)this.parentNS);
				nodeStepStep.putAbNode(this.parentNS); // 上一步就会把所有上面step需要加到elementCount的加完了
			}
		}
	}
	@Override
	public int getCount() {
		if(/*value == -1 || */value == Long.MAX_VALUE){
			return 0;
		}
		return elementCount.get();
	}
	public long getValue() {
		return value;
	}

//	public Node getPrevious() {
//		return previous;
//	}
//	public void setPrevious(Node previous) {
//		this.previous = previous;
//	}
//	public Node getNext() {
//		return next;
//	}
//	public void setNext(Node next) {
//		this.next = next;
//	}
	
//	public NodeStep getNodeStep() {
////		return nodeStep;
//		return (NodeStep)parentNS;
//	}
	
//	public void setNodeStep(NodeStep nodeStep) {
////		this.nodeStep = nodeStep;
//		this.parentNS = nodeStep;
//	}

	@Override
	public abstract/* synchronized */Element add(Element element) ;
	
	@Override
	public abstract int getRankValue(Element element);
	/**
	 * 这个可以通过传进来一个List<Element>，来减少new它的次数
	 * @param begin
	 * @param length
	 * @return
	 */
	public abstract void getElementsByIndex(List<Element> elementList , int begin ,int length) ;
	@Override
	public abstract boolean delete(Element element) ;
	/**
	 * 锁住多个Element，锁失败，要解锁
	 * @param elements
	 * @return 是否成功锁住
	 */
	protected boolean lockMultipleElement(Element... elements){
		Element[] lockElements = new Element[elements.length];
		int i=0;
		for (Element element : elements) {
			if(element == null){
				i++;
				continue;
			}
			boolean isLock = element.lock();
			if(!isLock){
				for (int j = elements.length-1 ;j>=0 ;j-- ) {
					if(lockElements[j]!=null){
						lockElements[j].unLock();
					}
					// 这里
				}
				// 可以放上面一点去
				return false;
			}
			lockElements[i++] = element;
		}
		return true;
	}
	/**
	 * 解锁多个Element
	 * @param elements
	 */
	protected void unLockMultipleElement(Element... elements){
		for (Element element : elements) {
			if(element == null){
				continue;
			}
			element.unLock();
		}
	}
	
	@Override
	public void reset() {
		if(elementCount.get() > 0){
			log.error("its not possible , on reset, elementCount.get() > 0:"
					+elementCount.get()+",conditionLevel:"+getConditionLevel());
		}
		
		value = -1;
//		nodeStep = null; // 所在的nodeStep
		parentNS = null;
//		setNodeStep(nodeStep);
		// 这里不要重置锁，因为，这个时候锁被多少个人用着是不确定的
//		locker.set(0);
		// Node中的Element数量
		elementCount.set(0);
		previous = null;
		next = null;
	}

	public abstract int getConditionLevel();
}
