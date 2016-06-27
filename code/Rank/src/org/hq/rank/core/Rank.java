package org.hq.rank.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.hq.rank.core.element.Element;
import org.hq.rank.core.node.AbNode;
import org.hq.rank.core.node.Node;
import org.hq.rank.core.node.NodeStepBase;
import org.hq.rank.core.node.SearchAbNodeResult;
import org.hq.rank.core.pool.ILockerPool;
import org.hq.rank.core.pool.LockerPool;
import org.hq.rank.core.pool.RankElementNodeMap;
import org.hq.rank.core.pool.RankPool;
import org.hq.rank.core.reoper.ReOper;
import org.hq.rank.core.reoper.ReOper.OperType;
import org.hq.rank.core.reoper.ReOperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Rank implements IRank {
	private static Logger log = LoggerFactory
			.getLogger(Rank.class);
	//
	private final RankElementNodeMap rankElementNodeMap;
	// lockerPool
	private final ILockerPool lockerPool;
	// rankpool
	private final RankPool rankPool;
	// 该rank的配置
	private final RankConfigure rankConfigure;
	// 该Rank的统计信息
	private RankStatistics rankStatistics = new RankStatistics(this);
	// 不支持负分
	private final Node head;
	// 存储所有的Element 如果.size效率低，就单独存储其数量
	private ConcurrentHashMap<Integer, Element> elementMap=new ConcurrentHashMap<Integer, Element>();
	// 这个是用来作为添加数据时的element的索引，在node数量比较多的时候可以大大提高效率
	private ConcurrentHashMap<Long, Node> nodeMap = new ConcurrentHashMap<Long, Node>();
	
	// reoper相关
	private final ReOperService reOperService;
//	// 配置
	private final int maxHitTimesNodeStep;
	private final int maxGetTimes;
	
	public Rank(){
		this(new RankConfigure());
	}
	public Rank(final RankConfigure rankConfigure){
		try {
			if(rankConfigure == null){
				this.rankConfigure = new RankConfigure();
			}else{
				this.rankConfigure = rankConfigure;
			}
			rankElementNodeMap = new RankElementNodeMap(this);
			lockerPool = new LockerPool(this);
			rankPool = new RankPool(this);
			if(!rankConfigure.check()){
				throw new RankException("rankConfigure is unavailable!"+rankConfigure);
			}
			reOperService = new ReOperService(this); // 这里面既有初始化，又有参数初始化，又有启动
			// 初始化
			maxHitTimesNodeStep = rankConfigure.getMaxHitTimesNodeStep();
			maxGetTimes = rankConfigure.getMaxGetRankDataTimes();
			// Node的构造方法中做了判断，起始的node不会创建rankElelment
			head = rankPool.getNode(Long.MAX_VALUE, 0,rankConfigure.getRankConditionCount()-1);
			nodeMap.put(head.getValue(), head);
		} catch (Exception e) {
			try {
				destory();
			} catch (InterruptedException e2) {
				throw new RankException("rank 创建错误 , 销毁错误");
			}
			throw new RankException("rank 创建错误");
		}
	}
	
	/**
	 * 当删除在进行的时候，其它操作都不能进行
	 * 1、 当删除一个node的时候，不能向其中添加点
	 * */
	@Override
	public long[] delete(int id) {
		rankStatistics.addDeleteCount();
//		Element element = elementMap.get(id);
		Element element = elementMap.remove(id);
		if(element == null){
			return null;
		}
		// 这个地方不一定成功，后面再修改
		boolean success = doDelete(element);
		long[] result = element.getValue();
		if(!success){
			if(reOperService.addQueue(element, OperType.Delete, 0, null)){
				return result;
			}
			throw new RankException("rank exception , addQueue fail");
		}
		return result;
	}
	@Override
	public boolean has(int id) {
		return elementMap.containsKey(id);
	}
	
	@Override
	public long set(int id, long value) {
		long[] result = set(id, new long[]{value});
		return result == null ? -1 : result[0];
	}
	/**
	 * 查找element，没有则添加，有则更新
	 * **/
	@Override
	public long[] set(int id, long... value) {
		return setWithAbsent(id, true, value);
	}
	@Override
	public long setIfAbsent(int id, long value) {
		long[] result = setIfAbsent(id,new long[]{value});
		return result == null ? -1 : result[0];
	}
	@Override
	public long[] setIfAbsent(int id, long... value) {
		return setWithAbsent(id, false, value);
	}
	@Override
	public long setByField(int id,int field,long value){
		if(id < 0){
			log.error("id requird >= 0");
			throw new RankException("id requird >= 0");
		}
		int fieldCount = rankConfigure.getRankConditionCount();
		if(field >= fieldCount){
			throw new RankException("field too large,field="+field+",but rank field is (0-"+(fieldCount-1)+")");
		}
		rankStatistics.addSetCount();
		
		long[] trueValue = new long[fieldCount]; // 默认是0
		trueValue[field] = value;
		Element element = rankPool.getElement(id, trueValue);
		while(!element.lock()){
			element = rankPool.getElement(id, trueValue);
//			System.err.println("error+++++++++++++++++++++++++++++++++++++");
		}
		Element oldElement = elementMap.put(id,element);
		if(oldElement == null){ //说明原来不存在
			throw new RankException("old data is not exist while try set by field,please set all value before");
		}
		long[] oldValues = oldElement.getValue();
		long[] newValues = element.getValue();
		for (int i=0;i<fieldCount;i++) { // 把其它值付给element
			if(i != field){
				newValues[i] = oldValues[i];
			}
		}
		if(!doUpdate(oldElement, element)){
			if(reOperService.addQueue(element, OperType.Update, 0, null, oldElement,null)){
				return oldValues[field];
			}
			elementMap.put(id, oldElement);
			throw new RankException("rank exception,addQueue fail");
		}
		return oldValues[field];
	}
	private void checkDataBeforeSet(int id,long...value ){
		if(id < 0){
			log.error("id requird >= 0");
			throw new RankException("id requird >= 0");
		}
		if(value == null || value.length != rankConfigure.getRankConditionCount()){
			throw new RankException("value is error!");
		}
		rankStatistics.addSetCount();
	}
	/**
	 * 如果存在是否还put
	 * @param id
	 * @param isAbsentPut true，存在与否都put，false，存在则不put
	 * @param value
	 * @return
	 */
	private long[] setWithAbsent(int id,boolean isAbsentPut, long... value){
		checkDataBeforeSet(id, value);
		Element element = rankPool.getElement(id, value);
		while(!element.lock()){
			element = rankPool.getElement(id, value);
//			System.err.println("error+++++++++++++++++++++++++++++++++++++");
		}
		Element oldElement;
		if(isAbsentPut){
			// 在进去之前就可以将其锁住，防止并发问题,putIfAbsent做不到
			oldElement = elementMap.put(id,element);
		}else{
			oldElement = elementMap.putIfAbsent(id,element);
			if(oldElement != null){ // 说明已经存在，不要put了
				rankPool.putElement(element); // 这个element的回收如果会产生问题，可以选择不回收了
				element.unLock();
				return oldElement.getValue();
			}
		}
//		Element oldElement = elementMap.put(id,element);
		if(oldElement != null){
			if(!doUpdate(oldElement, element)){
				if(reOperService.addQueue(element, OperType.Update, 0, null, oldElement,null)){
					return oldElement.getValue();
				}
				elementMap.put(id, oldElement);
				throw new RankException("rank exception,addQueue fail");
			}
			return oldElement.getValue();
		}else{
			if(doAdd(element/*, value*/)){
				element.unLock();
				return null;
			}else{
				if(reOperService.addQueue(element, OperType.Add, 0, null)){
					return null;
				}
				throw new RankException("rank exception,addQueue fail");
			}
		}
	}

	@Override
	public RankData get(int id) {
		rankStatistics.addGetCount();
		Element element = elementMap.get(id);
		if(element == null || element.getNode() == null){
			return null;
		}
		int times = 0;
		while(times++<maxGetTimes){
			countLocal.set(0);
			RankData rankData = doGet(element);
			if(rankData != null){
				return rankData;
			}
			// 如果上一次获取失败，放弃该时间片，因为很有可能正在和别的线程抢夺资源
			Thread.yield();
		}
//		System.err.println("get null");
		return null;
	}
	/**
	 * 根据排行的名词获取排行数据
	 */
	@Override
	public List<RankData> getRankDatasByRankNum(int begin, int length) {
		if(begin >= elementMap.size()){
			log.warn("has no enough player: begin:"+begin+",elementMapSize:"+elementMap.size());
			return null;
		}
		// 先找到begin的玩家element，后面的element或者后面node的element
		int rankNum = 0;
		NodeStepBase currentnodeStepStep = head.getParentNS().getParentNS();
		NodeStepBase currentNodeStep = head.getParentNS();
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
			return null;
		}
		Node currentNode = head;
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
			return null;
		}
		List<Element> elementList = new ArrayList<Element>(length);
//		System.err.println(currentNode.getValue());
		while(currentNode != null){
			int countN = currentNode.getCount();
			if(rankNum + countN >= begin){
				int beginE = begin - rankNum;
				int lengthE =length;
				Node currentNode2 = currentNode;
				while(elementList.size() < length && currentNode2 != null){
					currentNode2.getElementsByIndex(elementList,beginE,lengthE);
					beginE = 0;
					lengthE = length - elementList.size();
					currentNode2 = (Node)currentNode2.getNext();
				}
				break;
			}
			rankNum += countN;
			currentNode = (Node)currentNode.getNext();
		}
		if(currentNode == null){
			log.warn("has no enough player on currentNode");
			return null;
		}
		List<RankData> result = new ArrayList<RankData>(elementList.size());
		int count = 0;
		for (Element element : elementList) {
			RankData rankData = new RankData();
			rankData.setId(element.getId());
			rankData.setRankNum(begin+count++);
			rankData.setValue(element.getValue());
			result.add(rankData);
		}
		return result;
	}
	
	private RankData doGet(Element element){
		// 需要锁住自己，否则，如果此时被改变（修改或删除）的话，就会出现找不到的情况
		if(!element.lock()){
//			log.warn("!element.lock(),id:"+element.getId());
			return null;
		}
		long value = element.getValue()[0];
		Node node = nodeMap.get(value);
		if(node == null){
			log.warn("node == null");
			element.unLock();
			return null;
		}
		SearchAbNodeResult result = getStartNodeByNodeStep(value);
		Node currentNode = (Node)result.getNode();
		NodeStepBase nodeStep = currentNode.getParentNS();
		int rankNum = result.getRankNum();
		if (currentNode.getValue()<value) {
			log.warn("currentNode.getValue()<value，这说明上面命中之后被修改过");
			currentNode = head;
		}
		
		while(currentNode != null && currentNode.getParentNS() == nodeStep){
			if(currentNode == node){
				int localRankNum = currentNode.getRankValue(element);
				if(localRankNum == -1){
					element.unLock();
					log.warn("currentNode.getRankValue = -1:"+element);
					return null;
				}
				rankNum += localRankNum;
				break;
			}
			rankNum += currentNode.getCount();
			currentNode = (Node)currentNode.getNext();
		}
		if(currentNode == null || currentNode.getParentNS() != nodeStep){
			log.warn("id "+element.getId()+" not exist:currentNode:"+currentNode);
			element.unLock();
			return null;
		}
		
		RankData rankData = new RankData();
		rankData.setId(element.getId());
		rankData.setRankNum(rankNum);
		rankData.setValue(element.getValue());
		element.unLock();
		return rankData;
	}
	/**
	 * 获取最外层的nodeStep
	 * @return
	 */
	private NodeStepBase getHeadNodesStep(){
		NodeStepBase currentNodeStep = head.getParentNS();
		NodeStepBase result = currentNodeStep; 
		while(currentNodeStep != null){
			result = currentNodeStep;
			currentNodeStep = currentNodeStep.getParentNS();
		}
		return result;
	}
	private SearchAbNodeResult getStartNodeByValue(long value,NodeStepBase nodeStep){
		int rankNum = 0;
		int currentHitTimes = 0;
		// 通过nodestepstep寻找弄得step
		AbNode currentNode = nodeStep.getHead();
		NodeStepBase currentNodeStep = nodeStep;
		NodeStepBase previousNodeStep = null;
		int count = 0;
		while(currentNodeStep != null){
			count++;
			if(currentNodeStep.getValue() < value){
//				System.err.println("currentNodeStepStep.getValue():"+currentNodeStepStep.getValue()+",value:"+value);
				if(previousNodeStep != null && previousNodeStep.getValue() >= value){
					currentNode = previousNodeStep.getHead();
					if(currentNode.getValue() < value){
						log.warn("只要这个地方发生，就说明会产生找不到值得情况，即即使拿到相应的node，也是不对的node1");
						rankNum = -1;
						currentNode = null;
					}
					break;
				}else{
					rankNum = 0;
					currentNode = nodeStep.getHead();
					currentNodeStep = nodeStep;
					previousNodeStep = null;
					if(currentHitTimes++>maxHitTimesNodeStep){
						log.warn("nodestepstep:currentHitTimes++>NodeStep.maxHitTimes:"+
								currentHitTimes+","+previousNodeStep);
						rankStatistics.addFialHitByNodeStepStep();
						break;
					}
					Thread.yield();
					continue;
				}
			}else if(currentNodeStep.getNext() == null){ // 如果是最后一个
				if(previousNodeStep != null){
					rankNum += previousNodeStep.getElementCount();
				}
				currentNode = currentNodeStep.getHead();
				if(currentNode.getValue() < value){
					log.warn("只要这个地方发生，就说明会产生找不到值得情况，即即使拿到相应的node，也是不对的node1");
					rankNum = -1;
					currentNode = null;
				}
				break;
			}
			if(previousNodeStep != null){
				rankNum += previousNodeStep.getElementCount();
			}
			previousNodeStep = currentNodeStep;
			currentNodeStep = (NodeStepBase)currentNodeStep.getNext();
		}
		countLocal.set(countLocal.get()+count);
		return new SearchAbNodeResult(currentNode, rankNum);
	}
	// 定位复杂度统计参数
	private ThreadLocal<Integer> countLocal = new ThreadLocal<Integer>();
	private SearchAbNodeResult getStartNodeByNodeStep(long value){
		int rankNum = 0;
		NodeStepBase nodeStepBase = getHeadNodesStep();
		SearchAbNodeResult searchAbNodeResult = getStartNodeByValue(value, nodeStepBase);
		rankNum+=searchAbNodeResult.getRankNum();
		AbNode abNode = searchAbNodeResult.getNode();
		while (abNode instanceof NodeStepBase) {
			searchAbNodeResult = getStartNodeByValue(value, (NodeStepBase)abNode);
			rankNum+=searchAbNodeResult.getRankNum();
			abNode = searchAbNodeResult.getNode();
		}
		if(abNode instanceof Node){
			searchAbNodeResult.setRankNum(rankNum);
			return searchAbNodeResult;
		}
		throw new RankException("error");
	}
	/**
	 * 这里返回的Node一定是可以作为value对应node的前node，有可能是head
	 * {@code SearchNodeStepResult}
	 * */
	private SearchAbNodeResult getStartNodeByNodeStep1(long value){
		int rankNum = 0;
		int currentHitTimes = 0;
		// 通过nodestepstep寻找弄得step
		NodeStepBase currentNodeStep = head.getParentNS();
		NodeStepBase currentNodeStepStep = head.getParentNS().getParentNS();
		NodeStepBase previousNodeStepStep = null;
		while(currentNodeStepStep != null){
			if(currentNodeStepStep.getValue() < value){
//				System.err.println("currentNodeStepStep.getValue():"+currentNodeStepStep.getValue()+",value:"+value);
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
					if(currentHitTimes++>maxHitTimesNodeStep){
						log.warn("nodestepstep:currentHitTimes++>NodeStep.maxHitTimes:"+
								currentHitTimes+","+previousNodeStepStep);
						rankStatistics.addFialHitByNodeStepStep();
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
		Node currentNode=head;
		NodeStepBase previouNodeStep = /*(NodeStepBase)currentNodeStep.getPrevious(); //*/null;
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
					if(currentHitTimes++>maxHitTimesNodeStep){
						log.warn("nodestep:currentHitTimes++>NodeStep.maxHitTimes:"+currentHitTimes);
						rankStatistics.addFialHitByNodeStep();
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
			if(previouNodeStep.getHead().getValue() < value){
				log.warn("最后还是没有命中，命中失败，这说明上面命中之后被修改过");
				rankNum = 0;
				currentNode=head;
			}
		}
		SearchAbNodeResult result = new SearchAbNodeResult();
		result.setNode(currentNode);
		result.setRankNum(rankNum);
		return result;
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
	 * 
	 * */
	private boolean doAdd(Element element){
		countLocal.set(0);
		boolean result = doAdd_(element);
		rankStatistics.addSearchNodeCycCount(countLocal.get());
		return result;
	}
	private boolean doAdd_(Element element) {
		long[] _value = element.getValue();
		long value = _value[0];
		Node valueNode = nodeMap.get(value);
		if(valueNode != null){
			element = valueNode.add(element);
			return element != null;
		}
		SearchAbNodeResult result = getStartNodeByNodeStep(value);
		Node currentNode = (Node)result.getNode();
		Node previousNode = (Node)currentNode.getPrevious();
		Node prePreNode ;
		if (currentNode.getValue()<value) {
			log.warn("currentNode.getValue()<value，这说明上面命中之后被修改过");
			currentNode = head;
		} 
		int count=0;
		try{// 为了统计循环次数才加的这个，没有其它目的
			while(currentNode != null){
				count++;
				if(currentNode.getValue()>value){
					prePreNode = previousNode;
					previousNode = currentNode;
					currentNode = (Node)currentNode.getNext();
					if(currentNode == null){
						// 删除的时候需要校验该lock
						Node node = rankPool.getNode(element,value,rankConfigure.getRankConditionCount()-1);
						// 可能另外一个线程在这个地方执行的add，所以在锁住之后，还要进行一个校验
						boolean isLock = lockMultipleNode(previousNode,node);
						if(!isLock){
							return false;
						}
						// 再次校验，这个地方需要上一层的校验，否则有可能previous是错误的，
						if(previousNode.getNext()!=null || (previousNode.getPrevious() != prePreNode) || 
								(previousNode != head && prePreNode.getNext() != previousNode)){
							unLockMultipleNode(previousNode,node);
							return false;
						}
						addToNodeLinkedList(previousNode, node, currentNode);
						unLockMultipleNode(previousNode,node);
						return true;
					}
				}else if(currentNode.getValue() == value){
					element = currentNode.add(element);
					return element != null;
				}else{
					Node node = rankPool.getNode(element,value,rankConfigure.getRankConditionCount()-1);
					boolean isLock = lockMultipleNode(previousNode,node,currentNode);
					if(!isLock){
						return false;
					}
					// 再次校验前中后关系
					if(previousNode.getNext()!=currentNode 
							|| currentNode.getPrevious()!=previousNode){
						unLockMultipleNode(previousNode,node,currentNode);
						return false;
					}
					addToNodeLinkedList(previousNode, node, currentNode);
					unLockMultipleNode(previousNode,node,currentNode);
					return true;
				}
			}
		}finally{
			countLocal.set(countLocal.get()+count);
		}
		
		return false;
	}
	
	private void addToNodeLinkedList(Node previous,Node node,Node next){
		node.setNext(next); // 注意，这里的顺序不能随意，否者多线程查询会出错
		NodeStepBase nodeStep = previous.getParentNS(); // 需要时同一个nodestep，并且，要在加入链表之前设置，加入链表之后添加到nodeStep
		if(nodeStep == null){
			System.err.println(previous.getValue());
		}
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
		if(previous != null){ // 事实上这个一定有，因为头部是一个Long.MAX_VALUE
			node.setPrevious(previous);
			previous.setNext(node);
		}
		if(next != null){
			next.setPrevious(node);
		}
		nodeMap.put(node.getValue(), node);
//		nodeStep.putAbNode(node);
		nodeStep.putAbNodeWithElement(node);
		nodeStep.getReadLock().unlock();
	}
	
	/**
	 * 如果删除成功，或者node中有element，则返回成功，否者，放回失败
	 * */
	public boolean deleteNode(Node node){
		boolean success = doDeleteNode(node);
		if(!success){
			return reOperService.addQueue(null,OperType.DeleteNode, 0, node);
		}
		return true;
	}
	/**
	 * 如果删除成功，或者node中有element，则返回成功，否者，放回失败
	 * */
	private boolean doDeleteNode(Node node){
		if(node.getCount() <= 0){
			// 再删除之前，isexist已经是false了，并且是在delete中设置的，设置是与add不能并行的，所以用不着读写锁了
			Node pre = (Node)node.getPrevious();
			Node next = (Node)node.getNext();
			// 这样会导致很多reoper，最好在加锁完成第一个之后，就判断是否是要加锁的
			boolean isLock = lockMultipleNode(pre,node,next); 
			if(!isLock){
				return false;
			}
			// 再校验
			if((pre != node.getPrevious() || ( pre != null && pre.getNext() != node)) || 
				(next != node.getNext() || (next != null && next.getPrevious()!=node))){
				unLockMultipleNode(pre,node,next);
				return false;
			}
			
			
			NodeStepBase nodeStep = node.getParentNS();
			if(nodeStep.combineBeforeRemove()){ // 要先处理这里，再从链表中移除，因为这里面有根据计数来进行的相关处理，先移除导致数据减少
				nodeStep = node.getParentNS();
			}
			nodeStep.getReadLock().lock();
			while(nodeStep != node.getParentNS()){
				nodeStep.getReadLock().unlock();
				nodeStep = node.getParentNS();
				nodeStep.getReadLock().lock();
			}
			// 先从链中移除，防止在移除过程中，向其中添加元素
			if(pre != null){
				pre.setNext(next);
			}
			if(next != null){
				next.setPrevious(pre);
			}
			// 这里无论成功都已经删除掉了
			nodeMap.remove(node.getValue(),node);
			
			node.getParentNS().removeAbNode(node);
			rankPool.putNode(node); 
			nodeStep.getReadLock().unlock();
			
			unLockMultipleNode(pre,node,next);
			
			return true;
		}
		return true;
	}
	
	/**
	 * 当删除在进行的时候，其它操作都不能进行
	 * 1、 当删除一个node的时候，不能向其中添加点
	 * */
	private boolean doDelete(Element element) {
		if(!element.lock()){
			return false;
		}
		Node node = nodeMap.get(element.getValue()
				[0]);
		if(node == null){
			elementMap.remove(element.getId(), element); // 这个地方很关键，就是是他则删除
			rankPool.putElement(element); // 解锁之前加入pool
			element.unLock();
			
			return true;
		}
		if(node.getValue() == Long.MAX_VALUE){
			log.error("delete head , value is too big");
		}
		boolean success = node.delete(element);
		if(!success){
			element.unLock();
			return false;
		}else{
			// 有可能删除node
			if(node.getCount() <= 0){
				deleteNode(node);
			}
			// 必须是原来的element才能删除，并且必须是原子的
			elementMap.remove(element.getId(), element);
			rankPool.putElement(element); // 解锁之前加入pool
			element.unLock();
			return true;
		}
	}
	/**
	 * update 实行的是先删除再添加，非原子的，但可以确保数据最终正确
	 * 后面可以优化
	 * */
	private boolean doUpdate(Element oldElement,Element element){
		if(doDelete(oldElement)){
			if(doAdd(element/*, value*/)){
				element.unLock(); // 再上一个函数中加的锁，是否可以考虑在上一个函数中解锁
				return true;
			}else{
				if(reOperService.addQueue(element,OperType.Add, 0, null)){
					return true;
				}
			}
		}else{
			return false;
		}
		// 这个地方，return false，会引起重新update
		log.warn("这个地方，return false，会引起重新update，但一般不会发生");
		return false;
	}
	public boolean doReOper_(ReOper reOper,boolean isLastTime){
		boolean success = false;
		switch (reOper.getOperType()) {
		case Delete:
			success = doDelete(reOper.getElement());
			if(!success && isLastTime){
				// 简单放进去，这样并不托，这里发生的时候就是大问题了
				elementMap.put(reOper.getElement().getId(), reOper.getElement());
			}
			break;
		case RankElementDeleteNode:
			success = reOper.getRankElement().doDeleteNode(reOper.getNode());
			break;
		case DeleteNode:
			success = doDeleteNode(reOper.getNode());
			break;
		case Add:
			success = doAdd(reOper.getElement());
			if(success){
				reOper.getElement().unLock();
			}else if(isLastTime){
				reOper.getElement().unLock();
			}
			break;
		case Update:
			success = doUpdate(reOper.getOldElement(),reOper.getElement());
			if(!success && isLastTime){
				reOper.getElement().unLock();
				// 简单放进去，这样并不托，这里发生的时候就是大问题了
				elementMap.put(reOper.getOldElement().getId(), reOper.getOldElement());
			}
			break;
		default:
			break;
		}
		return success;
	}
	
	private boolean lockMultipleNode(Node... nodes){
		Node[] lockNodes = new Node[nodes.length];
		int i=0;
		for (Node node : nodes) {
			if(node == null){
				continue;
			}
			boolean isLock = lockerPool.tryLockNodeWLocker(node, rankConfigure.getRankConditionCount()-1);
			if(!isLock){
				for (int j=nodes.length-1;j>=0;j--) {
					if(lockNodes[j]!=null){
						lockerPool.unlockNodeWLocker(lockNodes[j], rankConfigure.getRankConditionCount()-1);
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
	/**
	 * 在此之前的都处理完结束
	 * 如果这个函数花费时间很长，说明访问数量过度，很可能是对较少的id作了较大的访问
	 * */
	@Override
	public void destory() throws InterruptedException{
		reOperService.destory();
	}
	
	private void unLockMultipleNode(Node... nodes){
		for (Node node : nodes) {
			if(node == null){
				continue;
			}
			lockerPool.unlockNodeWLocker(node, rankConfigure.getRankConditionCount()-1);
		}
	}
	
	public RankStatistics getRankStatistics() {
		return rankStatistics;
	}

	public Node getHead() {
		return head;
	}
	public int getNodeCount(){
		return nodeMap.size();
	}
	public int getElementCount(){
		return elementMap.size();
	}
	public int getReOperQueueSize(){
		return reOperService.getReOperQueueSize();
	}
	public RankConfigure getRankConfigure() {
		return rankConfigure;
	}
	public RankPool getRankPool() {
		return rankPool;
	}
	public ILockerPool getLockerPool() {
		return lockerPool;
	}
	public RankElementNodeMap getRankElementNodeMap() {
		return rankElementNodeMap;
	}
	public Element getFailElement() {
		return reOperService.getFailElement();
	}
	public ReOperService getReOperService() {
		return reOperService;
	}
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		Node currentNode = head;
		while(currentNode != null){
			sb.append(currentNode.toString()+"\n");
			currentNode = (Node)currentNode.getNext();
		}
		return sb.toString();
	}
	
	public String rankStatisticsInfo(){
		return rankStatistics.getNodeAndStepCount();
//		return rankStatistics.toString();
	}

	public static enum ReOperType{
		SingleThread, // 效率较高，易发生冲突
		MultiThread,// 理论上效率最高，但极易发生冲突，大数据并发不适用
		MultiSche // 小数据少效率相对较低，是上面两种的一半，数据大效率和上面差不多，不易发生冲突，建议使用这种
		
	}
}
