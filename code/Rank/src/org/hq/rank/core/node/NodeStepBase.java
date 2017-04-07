package org.hq.rank.core.node;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hq.rank.core.Rank;
import org.hq.rank.core.RankException;
import org.hq.rank.core.pool.RankPoolElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeStepBase extends AbNode implements RankPoolElement{
	private static Logger log = LoggerFactory.getLogger(NodeStepBase.class);
	
	protected final Rank rank;
	protected AbNode head =null;
	
	protected AtomicInteger nodeCount = new AtomicInteger(0);
	
	protected ReadWriteLock locker = new ReentrantReadWriteLock();
	protected Lock readLock = locker.readLock();
	protected Lock writeLock = locker.writeLock();
	
	public NodeStepBase(Rank rank){
		this.rank = rank;
	}
	public void init(NodeStepBase nodeStep){
		this.parentNS = nodeStep;
	}
	// 拆分相关
	public boolean cutBeforePut(){
		if(nodeCount.get() > rank.getRankConfigure().getCutCountNodeStep()){
			// 添加nodeStep
			NodeStepBase _previous = lockPrevious();
			this.writeLock.lock();
			if(_previous == previous && nodeCount.get() > rank.getRankConfigure().getCutCountNodeStep()){
				int newNodeCount = rank.getRankConfigure().getCutCountNodeStep() / 2;
				NodeStepBase nodeStepBase = rank.getRankPool().getNodeStepBase(parentNS);
				nodeStepBase.writeLock.lock();
				int currentCount = 0;
				AbNode currentNode = this.head;
				
				nodeStepBase.head = currentNode;
				
				nodeStepBase.next =this; // 先设置，有先设置的好处，提高查询的命中率
				nodeStepBase.previous = this.previous;
				
				int changeElementCount = 0;
				int changeNodeCount = 0;
				List<AbNode> changeNodeList = new ArrayList<AbNode>(newNodeCount);
				while(currentCount++ < newNodeCount && currentNode != null && currentNode.getParentNS() == this){
					changeNodeList.add(currentNode);
					// 在这个过程中不会有新的加入，因为这里加了写锁，而添加新的node要添加读锁
//					nodeStep.putNode(currentNode);
					nodeStepBase.nodeCount.getAndIncrement();
					changeNodeCount++; 
					
					currentNode.setParentNS(nodeStepBase);
					int eC=currentNode.getCount();
					changeElementCount += eC;
					nodeStepBase.getAndAdd(eC);
					currentNode = (AbNode)currentNode.getNext();
				}
				
				
				if(currentCount != newNodeCount+1){
					// 这个貌似不会出现，应该改成log.error，如果出现要仔细检查
					log.warn("在nodestep拆分过程中，被拆分的nodestep减少到fullCount / 2，停止拆分并回滚拆分设置的nodestep，此问题若经常出现，是大问题");
					log.warn("currentCount:"+currentCount+",nodeCount.get():"+nodeCount.get()+
							",currentNode != null:"+(currentNode != null)+",currentNode.getNodeStep() == this:"+(currentNode.getParentNS() == this)+
							",currentNode.getParentNS():"+currentNode.getParentNS());
					// 回滚，回滚之后，不拆分直接添加进去
					for (AbNode changeNode : changeNodeList) {
						changeNode.setParentNS(this);
					}
				}else{
					/// 添加进NodeStepStep
//					nodeStepStep.cutBeforePut();
					if(parentNS != null){
						parentNS.cutBeforePut(); //parentNS可能在这个地方改变了，改变成了它的新创建的上一个
						parentNS.getReadLock().lock();
						nodeStepBase.setParentNS(parentNS);
						parentNS.putAbNode(nodeStepBase);
					}
					
					//上面这一部分和下面这一部分的先后顺序不要修改，否则会降低搜索时的命中率
					// 每次到了这里都是1001，并没有减少，但在理论上讲，有可能减少到fullCount / 2以下
					this.head = currentNode;
					getAndAdd(changeElementCount*-1); 
					this.nodeCount.addAndGet(changeNodeCount*-1);// 如果在这个点进行查询操作，会显示错误的结果，虽然影响不大，但最好优化掉
					this.head = currentNode;
//					System.err.println("nodeCount after cut:"+this.nodeCount.get());
					//
					if(this.previous != null){
						this.previous.setNext(nodeStepBase);
					}
					this.previous = nodeStepBase;
					if(parentNS != null){
						parentNS.getReadLock().unlock();
					}
					
					nodeStepBase.writeLock.unlock();
					if(_previous != null){
						_previous.writeLock.unlock(); // 这里不会和delete冲突，因为都是要锁住两个
					}
					this.writeLock.unlock();
					rank.getRankStatistics().addCutNodeStepCount();
					return true;
				}
			}else{
//				log.info("加锁后校验失败，不拆分");
			}
			this.writeLock.unlock();
			if(_previous != null){
				_previous.writeLock.unlock(); // 这里不会和delete冲突，因为都是要锁住两个
			}
		}
		return false;
	}
	
	public void putAbNode(AbNode node){
		if(head == null || head == node.getNext()){
			head = node;
		}
		nodeCount.getAndIncrement();
	}
	public void putAbNodeWithElement(AbNode node){
		if(head == null || head == node.getNext()){
			head = node;
		}
		nodeCount.getAndIncrement();
		addElement(node.getCount());
	}
	// 对于像previous这样的全局变量，加锁之后要重新校验
	private NodeStepBase lockPrevious(){
		NodeStepBase _previous = (NodeStepBase)previous;
		if(_previous == null){
			return null;
		}
		_previous.writeLock.lock();
		while(_previous != previous){
			_previous.writeLock.unlock();
			_previous = (NodeStepBase)previous;
			if(_previous == null){
				return null;
			}
			_previous.writeLock.lock();
		}
		return _previous;
	}
	// 对于像previous这样的全局变量，加锁之后要重新校验
	private NodeStepBase lockNext(){
		NodeStepBase _next = (NodeStepBase)next;
		if(_next == null){
			return null;
		}
		_next.writeLock.lock();
		while(_next != next){
			_next.writeLock.unlock();
			_next = (NodeStepBase)next;
			if(_next == null){
				return null;
			}
			_next.writeLock.lock();
		}
		return _next;
	}
	
	public boolean combineBeforeRemove(){
		// 必须上面有，才能合并
		if(nodeCount.get() < rank.getRankConfigure().getCombineCountNodeStep()){
			// 第一个不能删除，最后一个可以
			if(previous != null){
				NodeStepBase previous = (NodeStepBase)this.previous;
				NodeStepBase _previous = lockPrevious();
				if(_previous==null){
					return false;
				}
				this.writeLock.lock();
				NodeStepBase _next = lockNext();
				
				// 再判断，如果已经有之前的线程将它合并了
				// 防止两个线程同时删除一个nodestep
				if(_previous.next != this || nodeCount.get() >= rank.getRankConfigure().getCombineCountNodeStep()){
					if(_next != null){
						_next.writeLock.unlock();
					}
					this.writeLock.unlock();
					_previous.writeLock.unlock();
					return false;
				}
				// 合并nodestepstep
				if(parentNS != null){
					if(parentNS.combineBeforeRemove()){
						parentNS = this.getParentNS();
					}
					parentNS.getReadLock().lock();
//					parentNS.removeAbNode(this);
					parentNS.removeNodeStepBase(this);
				}
				
//				// 先从链中移除，防止在移除过程中，向其中添加元素
				int _nodeCount = nodeCount.get();
				previous.nodeCount.getAndAdd(_nodeCount);
				previous.next = next;
				if(next != null){
					next.setPrevious(previous);
				}
				//
				if(_nodeCount > 0){
					AbNode currentNode = head;
					int count = 0;
					// 在这个过程中，没有可能有其它的加入，加入必须加读锁
					while(currentNode != null && currentNode.getParentNS() == this){
						currentNode.setParentNS(previous);
						previous.getAndAdd(currentNode.getCount());
						currentNode = (AbNode)currentNode.getNext();
						count++;
					}
					if(count != _nodeCount){
						StringBuilder sb = new StringBuilder("----count:"+count+",_nodeCount:"+_nodeCount);
						if(currentNode != null){
							sb.append(",currentNode.getNodeStep():"+currentNode.getParentNS());
						}else{
							sb.append("currentNode == null");
						}
						sb.append(",this:"+this);
						sb.append(",this.getNext():"+this.getNext());
						sb.append(",currentNode == this.getNext().getHead():"+(currentNode == ((NodeStepBase)this.getNext()).head));
						log.error(sb.toString());
					}else{
//						System.err.println("-------------------------------zhelishizhengquede:"+head.getConditionLevel());
					}
				}else{
					if(parentNS != null){
						parentNS.getReadLock().unlock();
					}
					
					log.warn("why nodeCount.get() = "+nodeCount.get());
					if(next != null){
						((NodeStepBase)next).writeLock.unlock();
					}
					this.writeLock.unlock();
					previous.writeLock.unlock();
					return false;
				}
				nodeCount.set(0);
				if(parentNS != null){
					parentNS.getReadLock().unlock();
				}
				rank.getRankPool().putNodeStepBase(this); // 在这里previous 和 next 都被修改成了null
				if(_next != null){
					_next.writeLock.unlock();
				}
				this.writeLock.unlock();
				_previous.writeLock.unlock();
				rank.getRankStatistics().addCombineNodeStepCount();
				return true;
			}
		}
		return false;
	}
	
	public void removeAbNode(AbNode node){
		int count = node.getCount();
		getAndAdd(count*-1);
		if(parentNS != null){
			parentNS.addElement(count*-1);
		}
		
		nodeCount.getAndDecrement();
		if(this.head == node){
			this.head = (AbNode)node.getNext();// 此时node.getNext()是被锁住的，而此时deleteCount要大于0
			if(head == null){
				log.error("在这之前进行过合并，如果这里出现，说明出现了问题");
			}
		}
	}
	public void removeNodeStepBase(NodeStepBase nodeStep){
		NodeStepBase previousNodeStepStep = ((NodeStepBase)nodeStep.getPrevious()).getParentNS();
		if(previousNodeStepStep != this){ // 卓明这个nodeStep是this的第一个nodeStep
			if(previousNodeStepStep == null || previousNodeStepStep != previous){
				log.error("相邻的nodestep的nodestepstep不是相邻关系");
				throw new RankException();
			}else{
				// 锁住上面的
				// 取出nodeStep的时候，是通过向上合并进行的，所以，原来的elementCount应该给上面的nodeStep，在这里也要给previous
				lockPrevious();
				getAndAdd(nodeStep.getElementCount() * -1);
				((NodeStepBase)previous).getAndAdd(nodeStep.getElementCount());
				((NodeStepBase)previous).writeLock.unlock();
			}
		}
		nodeCount.getAndDecrement();
		if(this.head == nodeStep){
			this.head = (NodeStepBase)nodeStep.getNext();
			if(this.head == null){
				log.error("在这之前进行过合并，如果这里出现，说明出现了问题");
			}
		}
	}

	
	public void addElement(int count){
		getAndAdd(count);
		if(parentNS != null)
			parentNS.addElement(count);
	}
	
	public void putElement(){
		getAndIncrement();
		if(parentNS != null)
			parentNS.putElement();
	}
	
	public void removeElement(){
		getAndDecrement();
		if(parentNS != null)
			parentNS.removeElement();
	}
	
	public int getElementCount() {
		return getCount();
	}

	public int getNodeCount() {
		return nodeCount.get();
	}

	public Lock getReadLock() {
		return readLock;
	}

	public Lock getWriteLock() {
		return writeLock;
	}

	public void setHead(AbNode head) {
		this.head = head;
	}
	public AbNode getHead() {
		return head;
	}
	public int getAbNodeCount(){
		return nodeCount.get();
	}
	@Override
	public String toString(){
		return ""+getValue();
	}
	
	@Override
	public long getValue() {
		return head.getValue();
	}
	@Override
	public void reset() {
		head = null;
		setCount(0);
		nodeCount.set(0);
		
		previous = null;
		next = null;
		
		parentNS = null;
	}
}
