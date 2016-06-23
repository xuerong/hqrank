package org.hq.rank.core.node;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.hq.rank.core.Rank;
import org.hq.rank.core.element.Element;
import org.hq.rank.core.pool.RankPoolElement;
import org.hq.rank.core.reoper.ReOper.OperType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RankElement implements RankPoolElement{
	private static Logger log = LoggerFactory
			.getLogger(RankElement.class);
	private final Rank rank;
	// 不支持负分
	private Node head;
	//
	private int conditionLevel; // >0
	// 当前rankElement中的node数量，不包括head
	private final AtomicInteger nodeCount = new AtomicInteger(0);
	
	public final String id ;
	public RankElement(final Rank rank){
		this.rank = rank;
		id = rank.getRankElementNodeMap().getNewId();
		// Node的构造方法中做了判断，起始的node不会创建rankElelment
		// 这里就创建head，而在重用它的时候不再创建
		head = rank.getRankPool().getNode(Long.MAX_VALUE, 0,conditionLevel);
		// head不放入其中
//		putTo(head.getValue(), head);
	}
	private void putTo(long value,Node node){
		rank.getRankElementNodeMap().put(this, value, node);
		nodeCount.getAndIncrement();
	}
	private void removeFrom(long value){
		rank.getRankElementNodeMap().remove(this, value);
		nodeCount.getAndDecrement();
	}
	private Node getFrom(long value){
		return rank.getRankElementNodeMap().get(this, value);
	}
	public boolean init(final Element element,final int conditionLevel){
		this.conditionLevel = conditionLevel;
		if(element.getId() >= 0){
			if(!add(element)){
				return false;
			}
		}else{
			System.err.println("buhuichuxian,id="+element.getId());
		}
		return true;
	}
	
	/**
	 * 当删除在进行的时候，其它操作都不能进行
	 * 1、 当删除一个node的时候，不能向其中添加点
	 * */
	public boolean delete(final Element element) {
		if(element == null){
			return true;
		}
		return doDelete(element);
	}
	/**
	 * 
	 * @param element
	 * @return 返回的是该Element在该rank中的排行 -1代表没有找到
	 */
	public int get(Element element){
		long value = element.getValue()[rank.getRankConfigure().getRankConditionCount() - conditionLevel];
		Node node = getFrom(value);//nodeMap.get(value);
		if(node == null){
			return -1;
		}
		SearchNodeStepResult result = getStartNodeByNodeStep(value);
		Node currentNode = result.node;
		NodeStepBase nodeStep = currentNode.getParentNS();
		int rankNum = result.rankNum;
		if (currentNode.getValue()<value) {
			log.warn("currentNode.getValue()<value，这说明上面命中之后被修改过");
			currentNode = head;
		}
		
		while(currentNode != null && currentNode.getParentNS() == nodeStep){
			if(currentNode == node){
				int localRankNum = currentNode.getRankValue(element);
				if(localRankNum == -1){
					log.warn("currentNode.getRankValue = -1");
					return -1;
				}
				rankNum += localRankNum;
				break;
			}
			rankNum += currentNode.getCount();
			currentNode = (Node)currentNode.getNext();
		}
		if(currentNode == null || currentNode.getParentNS()!= nodeStep){
			log.warn("id "+element.getId()+" not exist:currentNode:"+currentNode+","+(currentNode.getParentNS() != nodeStep));
			return -1;
		}
		return rankNum;
	}
	public void getElementsByRankNum(List<Element> elementList,int begin, int length) {
		// 先找到begin的玩家element，后面的element或者后面node的element
		int rankNum = 0;
		Node currentNode = head;
		NodeStepBase currentNodeStep = head.getParentNS();
		if(currentNodeStep != null){
			NodeStepBase currentnodeStepStep = currentNodeStep.getParentNS();
			while(currentnodeStepStep != null){
				int countNSS = currentnodeStepStep.getElementCount();
				if(rankNum+countNSS >= begin){
					currentNodeStep = (NodeStepBase)currentnodeStepStep.getHead();
					break;
				}
				rankNum += countNSS;
				currentnodeStepStep = (NodeStepBase)currentnodeStepStep.getNext();
			}
			if(currentnodeStepStep == null){
				log.warn("has no enough player on currentnodeStepStep");
				return;
			}
			
			while(currentNodeStep != null){
				int countNs = currentNodeStep.getElementCount();
				if(rankNum + countNs >= begin){
					currentNode = (Node)currentNodeStep.getHead();
					break;
				}
				rankNum += countNs;
				currentNodeStep = (NodeStepBase)currentNodeStep.getNext();
			}
			if(currentNodeStep == null){
				log.warn("has no enough player on currentNodeStep");
				return;
			}
		}
		
		while(currentNode != null){
			int countN = currentNode.getCount();
			if(rankNum + countN >= begin){
				int beginE = begin - rankNum;
				int lengthE =length;
				int baseSize = elementList.size();
				int currentGetNum = 0;
				Node currentNode2 = currentNode;
				while(currentGetNum < length && currentNode2 != null){
					currentNode2.getElementsByIndex(elementList,beginE,lengthE);
					beginE = 0;
					currentGetNum = elementList.size() - baseSize;
					lengthE = length - currentGetNum;
					currentNode2 = (Node)currentNode2.getNext();
				}
//				System.err.println(currentNode2+"rankElement");
				break;
			}
			rankNum += countN;
			currentNode = (Node)currentNode.getNext();
		}
		if(currentNode == null){
			log.warn("has no enough player on currentNode");
			return;
		}
		return ;
	}
	/**
	 * 这里返回的Node一定是可以作为value对应node的前node，有可能是head
	 * */
	private SearchNodeStepResult getStartNodeByNodeStep(long value){
		int rankNum = 0;
		int currentHitTimes = 0;
		Node currentNode=head;
		// 通过nodestepstep寻找弄得step
		NodeStepBase currentNodeStep = head.getParentNS();
		if(currentNodeStep != null){
			NodeStepBase currentNodeStepStep = head.getParentNS().getParentNS();
			NodeStepBase previousNodeStepStep = null;
			while(currentNodeStepStep != null){
				if(currentNodeStepStep.getValue() < value){
					if(previousNodeStepStep != null && previousNodeStepStep.getValue() >= value){
						currentNodeStep = (NodeStepBase)previousNodeStepStep.getHead();
						if(currentNodeStep.getHead().getValue() < value){
							log.warn("只要这个地方发生，就说明会产生找不到值得情况，即即使拿到相应的node，也是不对的node1");
							rankNum = 0;
							currentNodeStep = head.getParentNS();
						}
						break;
					}else{
						rankNum = 0;
						currentNodeStep = head.getParentNS();
						currentNodeStepStep = head.getParentNS().getParentNS();
						previousNodeStepStep = null;
						if(currentHitTimes++>rank.getRankConfigure().getMaxHitTimesNodeStep()){
							log.warn("nodestepstep:currentHitTimes++>NodeStep.maxHitTimes:"+
									currentHitTimes+","+previousNodeStepStep);
							break;
						}
						Thread.yield();
						continue;
					}
				}else if(currentNodeStepStep.getNext() == null){ // 如果是最后一个
					if(previousNodeStepStep != null){
						rankNum += previousNodeStepStep.getElementCount();
					}
					currentNodeStep = (NodeStepBase)currentNodeStepStep.getHead();
					if(currentNodeStep.getHead().getValue() < value){
						log.warn("只要这个地方发生，就说明会产生找不到值得情况，即即使拿到相应的node，也是不对的node1");
						rankNum = 0;
						currentNodeStep = head.getParentNS();
					}
					break;
				}
				if(previousNodeStepStep != null){
					rankNum += previousNodeStepStep.getElementCount();
				}
				previousNodeStepStep = currentNodeStepStep;
				currentNodeStepStep = (NodeStepBase)currentNodeStepStep.getNext();
			}
			// 通过nodestep寻找node
			currentHitTimes = 0;
			
			NodeStepBase previouNodeStep = null;
			while(currentNodeStep != null){
				if(currentNodeStep.getHead().getValue() < value){
					// 有可能不命中，当对应的nodestep正在拆分或者合并的时候，可能不命中
					if(previouNodeStep!=null && previouNodeStep.getHead().getValue() >= value){
						currentNode=(Node)previouNodeStep.getHead(); // 这个地方不可能是null
						if(currentNode.getValue()<value){
							log.warn("只要这个地方发生，就说明会产生找不到值得情况，即即使拿到相应的node，也是不对的node2");
							currentNode = head;
							rankNum = 0;
						}
						break;
					}else{ // 没有命中的话，采取传统的方法，重头到尾
						rankNum = 0;
						currentNode=head;
						currentNodeStep = head.getParentNS();
						previouNodeStep = null;
						if(currentHitTimes++>rank.getRankConfigure().getMaxHitTimesNodeStep()){
							log.warn("nodestep:currentHitTimes++>NodeStep.maxHitTimes:"+currentHitTimes);
							break;
						}
						Thread.yield();
						continue;
					}
				}else if(currentNodeStep.getNext() == null){ // 如果是最后一个
					if(previouNodeStep != null){
						rankNum += previouNodeStep.getElementCount();
					}
					currentNode=(Node)currentNodeStep.getHead(); 
					if(currentNode.getValue()<value){
						log.warn("只要这个地方发生，就说明会产生找不到值得情况，即即使拿到相应的node，也是不对的node2");
						currentNode = head;
						rankNum = 0;
					}
					break;
				}
				if(previouNodeStep != null){
					rankNum += previouNodeStep.getElementCount();
				}
				previouNodeStep = currentNodeStep;
				currentNodeStep = (NodeStepBase)currentNodeStep.getNext();
			}
			//
			if(currentNodeStep == null){
				currentNode=(Node)previouNodeStep.getHead();
				if(previouNodeStep.getHead() == null || previouNodeStep.getHead().getValue() < value){
					log.warn("最后还是没有命中，命中失败，这说明上面命中之后被修改过");
					rankNum = 0;
					currentNode=head;
				}
			}
		}
		
		SearchNodeStepResult result = new SearchNodeStepResult();
		result.node = currentNode;
		result.rankNum = rankNum;
		return result;
	}
	/**
	 * 作为一个返回值用
	 * @author a
	 *
	 */
	private static class SearchNodeStepResult{
		Node node;
		int rankNum;
	}
	/**
	 * 找到位置
	 * 1、没有对应的node
	 * 锁定上下node
	 * 重新校验
	 * 创建并添加
	 * 解锁
	 * 2、存在对应的node
	 * add就可以
	 * 
	 * 如何防止重复添加
	 * @param value :这个value是该层级的排行条件的value
	 * */
	public boolean add(Element element) {
		// 根据conditionLevel获取对应的排行值
		long value = element.getValue()[rank.getRankConfigure().getRankConditionCount() - conditionLevel];
		Node valueNode = getFrom(value);//nodeMap.get(value);
		if(valueNode != null){
			element = valueNode.add(element);
			return element != null;
		}
		SearchNodeStepResult result = getStartNodeByNodeStep(value);
		
		Node currentNode = result.node;
		Node previousNode = (Node)currentNode.getPrevious();
		Node prePreNode ;
		int level = conditionLevel-1; // 这里用到的node的锁都是这一层的
		if (currentNode.getValue()<value) {
			log.warn("currentNode.getValue()<value，这说明上面命中之后被修改过");
			currentNode = head;
		} 
		while(currentNode != null){
			if(currentNode.getValue()>value){
				prePreNode = previousNode;
				previousNode = currentNode;
				currentNode = (Node)currentNode.getNext();
				if(currentNode == null){
					// 删除的时候需要校验该lock
					Node node = rank.getRankPool().getNode(element,value,level);
					// 可能另外一个线程在这个地方执行的add，所以在锁住之后，还要进行一个校验
					boolean isLock = lockMultipleNode(level,previousNode,node);
					if(!isLock){	// 这里要不要把node放回池中?
						return false;
					}
					// 再次校验:如果满足这三个条件，就可以的
					if(previousNode.getNext()!=null || (previousNode.getPrevious() != prePreNode) || 
							(previousNode != head && prePreNode.getNext() != previousNode)){
						unLockMultipleNode(level,previousNode,node);
						return false;
					}
					
					addToNodeLinkedList(previousNode, node, currentNode);
					unLockMultipleNode(level,previousNode,node);
					return true;
				}
			}else if(currentNode.getValue() == value){ // 如果走到这里，说明该node在链表中，但是之前没有在map中
				element = currentNode.add(element);
				return element != null;
			}else{
				Node node = rank.getRankPool().getNode(element,value,level);
				boolean isLock = lockMultipleNode(level,previousNode,node,currentNode);
				if(!isLock){
					return false;
				}
				// 再次校验前中后关系
				if(previousNode.getNext()!=currentNode 
						|| currentNode.getPrevious()!=previousNode){
					unLockMultipleNode(level,previousNode,node,currentNode);
					return false;
				}
				addToNodeLinkedList(previousNode, node, currentNode);
				unLockMultipleNode(level,previousNode,node,currentNode);
				return true;
			}
		}
		return false;
	}
	
	private void addToNodeLinkedList(Node previous,Node node,Node next){
		node.setNext(next); // 注意，这里的顺序不能随意，否者多线程查询会出错
		NodeStepBase nodeStep = previous.getParentNS(); // 需要时同一个nodestep，并且，要在加入链表之前设置，加入链表之后添加到nodeStep
		// 判断并创建nodestep
		if(nodeStep == null && nodeCount.get()>rank.getRankConfigure().getCutCountNodeStep()){//nodeMap.size() > rank.getRankConfigure().getCutCountNodeStep()){
			rank.getLockerPool().lockRankElementWLocker(this, conditionLevel);
			// 加锁之后校验
			nodeStep = previous.getParentNS();
			if(nodeStep == null){
				// 这里传入null，确保这是最高层
				NodeStepBase nodeStepStep = rank.getRankPool().getNodeStepBase(null);
				
				NodeStepBase newNodeStep = rank.getRankPool().getNodeStepBase(nodeStepStep);
//				nodeStepStep.putNodeStep(newNodeStep);
				nodeStepStep.putAbNode(newNodeStep);
				
				newNodeStep.setHead(head);
				Node currentNode = head;
				
				while(currentNode != null){
//					newNodeStep.putAbNode(currentNode);
					newNodeStep.putAbNodeWithElement(currentNode);
					currentNode.setParentNS(newNodeStep);
					currentNode = (Node)currentNode.getNext();
				}
			}
			rank.getLockerPool().unLockRankElementWLocker(this, conditionLevel);
		}
		rank.getLockerPool().lockRankElementRLocker(this, conditionLevel);
		nodeStep = previous.getParentNS();
		if(nodeStep != null){
			if(nodeStep.cutBeforePut()){
				nodeStep = previous.getParentNS();
			}
			nodeStep.getReadLock().lock();
			while(nodeStep != previous.getParentNS()){ // 再校验
				nodeStep.getReadLock().unlock();
				nodeStep = previous.getParentNS();
				nodeStep.getReadLock().lock();
			}
			node.setParentNS(nodeStep);
		}
		
		if(previous != null){ // 事实上这个一定有，因为头部是一个Long.MAX_VALUE
			node.setPrevious(previous);
			previous.setNext(node);
		}
		if(next != null){
			next.setPrevious(node);
		}
		putTo(node.getValue(), node);
		if(nodeStep != null){
//			nodeStep.putAbNode(node);
			nodeStep.putAbNodeWithElement(node);
			nodeStep.getReadLock().unlock();
		}
		rank.getLockerPool().unLockRankElementRLocker(this, conditionLevel);
	}
	
	/**
	 * 如果删除成功，或者node中有element，则返回成功，否者，放回失败
	 * */
	public boolean deleteNode(Node node){
		boolean success = doDeleteNode(node);
		if(!success){
			return rank.getReOperService().addQueue(OperType.RankElementDeleteNode, 0, node,this);
		}
		return true;
	}
	/**
	 * 如果删除成功，或者node中有element，则返回成功，否者，放回失败
	 * */
	public boolean doDeleteNode(Node node){
		if(node.getCount() <= 0){
			int level = conditionLevel -1;
			
			Node pre = (Node)node.getPrevious();
			Node next = (Node)node.getNext();
			// 这样会导致很多reoper，最好在加锁完成第一个之后，就判断是否是要加锁的
			boolean isLock = lockMultipleNode(level,pre,node,next); 
			if(!isLock){
				return false;
			}
			if(node.getCount() > 0){ // 说明在删除过程中，有了新的添加进去，这样就不删除它了
				unLockMultipleNode(level,pre,node,next);
				return true;
			}
			
			// 再校验
			if(pre != node.getPrevious() || next != node.getNext()){ // 这个过中，是否有它上下的node被删除
				unLockMultipleNode(level,pre,node,next);
				return false;
			}
			
			if(pre.getNext() != node){ // 说明另外一个线程删除了它
				unLockMultipleNode(level,pre,node,next);
				return true;
			}
			if(next != null && next.getPrevious() != node){
				log.error("虽然node的前面是正确的pre，但是后面确实不正确的next，如果不是另外一个线程在删除它（加了锁，不大可能），就是其它未知错误");
				unLockMultipleNode(level,pre,node,next);
				return true;
			}
			// 因为是动态创建的，所以“读”操作要加读锁
			rank.getLockerPool().lockRankElementRLocker(this, level+1);
			NodeStepBase nodeStep = node.getParentNS();
			if(nodeStep != null){
				if(nodeStep.combineBeforeRemove()){ // 要先处理这里，再从链表中移除，因为这里面有根据计数来进行的相关处理，先移除导致数据减少
					nodeStep = node.getParentNS();
				}
				nodeStep.getReadLock().lock();
				while(nodeStep != node.getParentNS()){
					nodeStep.getReadLock().unlock();
					nodeStep = node.getParentNS();
					nodeStep.getReadLock().lock();
				}
			}
			
			// 先从链中移除，防止在移除过程中，向其中添加元素
			if(pre != null){
				pre.setNext(next);
			}
			if(next != null){
				next.setPrevious(pre);
			}
			// 这里无论成功都已经删除掉了
			removeFrom(node.getValue());
			
			
			if(nodeStep != null){
				nodeStep.removeAbNode(node);
				nodeStep.getReadLock().unlock();
			}
			rank.getRankPool().putNode(node); 
			rank.getLockerPool().unLockRankElementRLocker(this, level+1);
			unLockMultipleNode(level,pre,node,next);
			
			return true;
		}
		return true;
	}
	
	/**
	 * 当删除在进行的时候，其它操作都不能进行
	 * 1、 当删除一个node的时候，不能向其中添加点
	 * */
	private boolean doDelete(Element element) {
		Node node = getFrom(element.getValue()[rank.getRankConfigure().getRankConditionCount() - conditionLevel]);//nodeMap.get(element.getValue()[rank.getRankConfigure().getRankConditionCount() - conditionLevel]);
		if(node == null){
			return true;
		}
		boolean success = node.delete(element);
		if(!success){
			return false;
		}else{
			// 有可能删除node
			if(node.getCount() <= 0){
				deleteNode(node);
			}
			return true;
		}
	}
	
	private boolean lockMultipleNode(int level,Node... nodes){
		Node[] lockNodes = new Node[nodes.length];
		int i=0;
		for (Node node : nodes) {
			if(node == null){
				continue;
			}
			boolean isLock = rank.getLockerPool().tryLockNodeWLocker(node, level);
			if(!isLock){
				for (int j=nodes.length-1;j>=0;j--) {
					if(lockNodes[j]!=null){
						rank.getLockerPool().unlockNodeWLocker(lockNodes[j], level);
					}
					// 这里
				}
				// 可以放上面一点去
				return false;
			}
			lockNodes[i++] = node;
		}
		
		return true;
	}
	
	private void unLockMultipleNode(int level, Node... nodes){
		for (Node node : nodes) {
			if(node == null){
				continue;
			}
			rank.getLockerPool().unlockNodeWLocker(node, level);
		}
	}

	public Node getHead() {
		return head;
	}
	public int getConditionLevel() {
		return conditionLevel;
	}
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder("nodeCount:"+nodeCount.get()+"\n");
		Node currentNode = (Node)head.getNext();
		while(currentNode != null){
			sb.append(currentNode.toString()+"\n");
			currentNode = (Node)currentNode.getNext();
		}
		return sb.toString();
	}
	public int getNodeCount(){
		return nodeCount.get();
	}
	@Override
	public void reset() {
//		head = null;
		head.setNext(null); // head要重用
		head.setParentNS(null);
		//
//		这个地方发生，说明deletenode失败，但是，其上的node删除成功，reset时调用到了它
		if(nodeCount.get() > 0){
			System.err.println("its not possible :"+nodeCount.get()+","+conditionLevel);
		}
		conditionLevel = -1; // >0
		nodeCount.set(0);
	}
}
