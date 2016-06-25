package org.hq.rank.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.hq.rank.core.node.Node;
import org.hq.rank.core.node.NodeStepBase;
import org.hq.rank.core.node.RankElement;
import org.hq.rank.core.node.RankElementNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RankStatistics {
	private static Logger log = LoggerFactory.getLogger(RankStatistics.class);
	/**
	 * 包括统计的数据和校验使用的统计数据
	 * 
	 * Element的总数
	 * Node的总数
	 * NodeStep的总数
	 * NodeStepStep的总数
	 * 
	 * Element加锁次数
	 * Node加锁次数
	 * Node中ElementStep合并次数(没有拆分)
	 * NodeStep拆分和合并次数
	 * NodeStepStep的拆分和合并次数
	 * 
	 * 
	 * ReOper总次数
	 * ReOper栈中的未执行任务reOperQueue
	 * 
	 * 执行的set次数
	 * 执行的get次数
	 * 执行的delete次数
	 * 以上三者的执行频率，效率等
	 * 定位难度（平均即每个访问需要花费的定位访问次数）
	 * 
	 * 
	 * 
	 * 校验相关：
	 * 当前还没有重做的任务数reOperTaskCount：验证
	 * Element的总数==Node中的Element和==NodeStep中的Element和==NodeStepStep中的Element总数和
	 * Node的总数==NodeStep中的Node总数
	 * NodeStep的总数==NodeStepStep中的NodeStep总数
	 * 
	 * 拆分失败
	 * 
	 * 大小关系
	 * 效率验证
	 * 配置状态
	 * 
	 */
	private final Rank rank;
	public RankStatistics(final Rank rank){
		this.rank = rank;
	}
	/**
	 * elementLockCount
	 */
	private AtomicInteger elementLockCount = new AtomicInteger(0);
	private AtomicInteger elementUnlockCount = new AtomicInteger(0);
	// 一些统计数据占用的cpu也挺高
	public int addElementLockCount(){
		return elementLockCount.getAndIncrement();
	}
	public int addElementUnlockCount(){
		return elementUnlockCount.getAndIncrement();
	}
	
	/**
	 * nodeLockCount
	 */
	private AtomicInteger nodeLockCount = new AtomicInteger(0);
	private AtomicInteger nodeUnlockCount = new AtomicInteger(0);
	
	public int addNodeLockCount(){
		return nodeLockCount.getAndIncrement();
	}
	public int addNodeUnlockCount(){
		return nodeUnlockCount.getAndIncrement();
	}
	
	/**
	 * ElementStep合并次数
	 */
	private AtomicInteger elementStepCombineTime = new AtomicInteger(0);
	
	public int addElementStepCombineTime(){
		return elementStepCombineTime.getAndIncrement();
	}
	/**
	 * NodeStep拆分和合并
	 */
	private AtomicInteger cutNodeStepCount = new AtomicInteger(0);
	private AtomicInteger combineNodeStepCount = new AtomicInteger(0);
	
	public int addCutNodeStepCount(){
		return cutNodeStepCount.getAndIncrement();
	}
	public int addCombineNodeStepCount(){
		return combineNodeStepCount.getAndIncrement();
	}
	/**
	 * NodeStepStep拆分和合并
	 */
	private AtomicInteger cutNodeStepStepCount = new AtomicInteger(0);
	private AtomicInteger combineNodeStepStepCount = new AtomicInteger(0);
	
	public int addCutNodeStepStepCount(){
		return cutNodeStepStepCount.getAndIncrement();
	}
	public int addCombineNodeStepStepCount(){
		return combineNodeStepStepCount.getAndIncrement();
	}
	/**
	 * set get delete 执行次数
	 */
	private AtomicInteger setCount = new AtomicInteger(0);
	private AtomicInteger getCount = new AtomicInteger(0);
	private AtomicInteger deleteCount = new AtomicInteger(0);
	
	public int addSetCount(){
		return setCount.getAndIncrement();
	}
	public int addGetCount(){
		return getCount.getAndIncrement();
	}
	public int addDeleteCount(){
		return deleteCount.getAndIncrement();
	}
	/**
	 * 一个执行所花费的循环次数，doAdd
	 */
	private AtomicInteger searchNodeCountPer = new AtomicInteger(0);
	private AtomicInteger searchNodeCycCountPer = new AtomicInteger(0);
	public void addSearchNodeCycCount(int count){
		searchNodeCycCountPer.addAndGet(count);
		searchNodeCountPer.incrementAndGet();
	}
	
	/**
	 * 重现执行次数
	 */
	private AtomicInteger reOperCount = new AtomicInteger(0);
	public int addReOperCount(){
		return reOperCount.getAndIncrement();
	}
	/**
	 * 通过NodeStep和NodeStepStep没有命中的次数
	 */
	private AtomicInteger fialHitByNodeStepStep = new AtomicInteger(0);
	private AtomicInteger fialHitByNodeStep = new AtomicInteger(0);
	
	public int addFialHitByNodeStepStep(){
		return fialHitByNodeStepStep.getAndIncrement();
	}
	public int addFialHitByNodeStep(){
		return fialHitByNodeStep.getAndIncrement();
	}
	public void showTest(){
		cycleTestNode();
		System.err.println("没有出现node循环");
	}
	// 递归遍历所有的node，确保没有出现node循环和node的大小错误，最好价格计数功能
	public int cycleTestNode(){
		int count = testCycleNode(rank,0);
		return count;
	}
	public int testCycleNode(Object object,int p_count/*从父node中计算的count*/){ // 这是一个比较好的校验函数
		Node head;
		if(object instanceof Rank){
			head = ((Rank)object).getHead();
		}else{
			head = ((RankElement)object).getHead();
		}
		Node cu = head;
		Node pre;
		int count = 0;
		int count2 = 0;
		while(cu != null){
			if(cu.getConditionLevel() > 0 && cu.getValue() < Long.MAX_VALUE){
				count+=testCycleNode(((RankElementNode)cu).getRankElement(),cu.getCount());
			}
			pre = cu;
			cu = (Node)cu.getNext();
			count++;
			count2++;
			if(cu != null){
				if(pre.getValue() <=  cu.getValue()){
					System.err.println("~这里有问题：在这个验证和set等并发情况下可能发生，但不能太多");
				}
				if(cu.getPrevious()!=pre){
					System.err.println("~这里有问题: 在这个验证和set等并发情况下可能发生，但不能太多");
				}
			}
		}
		if(object instanceof Rank){
			Rank r = (Rank)object;
			if(r.getNodeCount() != count2){
				System.err.println("rank:r.getNodeCount():"+r.getNodeCount()+",count2:"+count2);
			}
		}else{
			RankElement r = (RankElement)object;
			if(r.getNodeCount() != count2 - 1/* || r.getNodeCount() != p_count*/){ //r.getNodeCount()没有计算head
				System.err.println("rankelement:r.getNodeCount():"+r.getNodeCount()+
						",count2:"+count2+",p_count:"+p_count);
			}
		}
		return count;
	}
	public String getNodeAndStepCount(){
		int nodeStepStepCount = 0; // 自身链表
		int nodeStepCount = 0; // nodeStepStep中，自身链表
		// NodeStepStep
		List<NodeStepBase> nodeStepStepList = getNodeStepStepList(rank.getHead().getParentNS().getParentNS());
		nodeStepStepCount = nodeStepStepList.size();
		// NodeStep
		for (NodeStepBase nodeStepStep : nodeStepStepList) {
			nodeStepCount += nodeStepStep.getNodeCount();
		}
		// 定位复杂度
		int countSearchNodeCountPer=searchNodeCountPer.get();
		int countPerDoAdd = searchNodeCycCountPer.get()/countSearchNodeCountPer;
		searchNodeCountPer.set(0);
		searchNodeCycCountPer.set(0);
		StringBuilder sb = new StringBuilder();
		sb.append("nodeStepStepCount:"+nodeStepStepCount+"\n");
		sb.append("nodeStepCount:"+nodeStepCount+"\n");
		sb.append("SearchNodeCycCountPer(difficulty of location):"+countPerDoAdd+",searchNodeCountPer:"+countSearchNodeCountPer+"\n");
		return sb.toString();
	}
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		int nodeStepStepCount = 0; // 自身链表
		int nodeStepCount1 = 0,nodeStepCount2 = 0; // nodeStepStep中，自身链表
		int nodeCount1 = 0,nodeCount2 = 0,nodeCount3 = 0; // nodestep中，node自身链表，rank中
		int elementCount1 = 0,elementCount2 = 0,elementCount3 = 0,elementCount4 = 0; // nodeStepStep中记录的数量，nodeStep中，node中，rank中
		// NodeStepStep
		List<NodeStepBase> nodeStepStepList = getNodeStepStepList(rank.getHead().getParentNS().getParentNS());
		nodeStepStepCount = nodeStepStepList.size();
		// NodeStep
		for (NodeStepBase nodeStepStep : nodeStepStepList) {
			nodeStepCount1 += nodeStepStep.getNodeCount();
		}
		List<NodeStepBase> nodeStepList = getNodeStepList(rank.getHead().getParentNS());
		nodeStepCount2 = nodeStepList.size();
		// node
		for (NodeStepBase nodeStep : nodeStepList) {
			nodeCount1 += nodeStep.getNodeCount();
		}
		List<Node> nodeList = getNodeList(rank.getHead());
		nodeCount2 = nodeList.size();
		nodeCount3 = rank.getNodeCount();
		// element
		for (NodeStepBase nodeStepStep : nodeStepStepList) {
			elementCount1 += nodeStepStep.getElementCount();
			System.err.println("---------------------------------:"+nodeStepStep.getNodeCount()+","+nodeStepStep.getElementCount());
		}
		for (NodeStepBase nodeStep : nodeStepList) {
			elementCount2 += nodeStep.getElementCount();
		}
		for (Node node : nodeList) {
			elementCount3 += node.getCount();
		}
		elementCount4 = rank.getElementCount();
		// 定位复杂度
		int countPerDoAdd = searchNodeCycCountPer.get()/searchNodeCountPer.get();
		searchNodeCountPer.set(0);
		searchNodeCycCountPer.set(0);
		// 统计
		sb.append("nodeStepStepCount:"+nodeStepStepCount+"\n");
		sb.append("nodeStepCount:"+nodeStepCount1+","+nodeStepCount2+"\n");
		sb.append("nodeCount:"+nodeCount1+","+nodeCount2+","+nodeCount3+"\n");
		sb.append("elementCount:"+elementCount1+","+elementCount2+","+elementCount3+","+elementCount4+"\n");
		
		sb.append("setCount:"+setCount.get()+",getCount:"+getCount.get()+",deleteCount:"+deleteCount.get()+"\n");
		sb.append("reOperQueueSize:"+rank.getReOperQueueSize()+"\n");
		
		sb.append("nodeLockTimes:["+getNodeLockCount()+","+getNodeUnlockCount()+
				"],elementLockTimes:["+getElementLockCount()+","+getElementUnlockCount()+"]"+"\n");
		sb.append("elementStepCombineTime:"+elementStepCombineTime.get()+"\n");
		sb.append("combineNodeStepCount:"+combineNodeStepCount.get()+",cutNodeStepCount:"+cutNodeStepCount.get()+"\n");
		sb.append("combineNodeStepStepCount:"+combineNodeStepStepCount.get()+",cutNodeStepStepCount:"+cutNodeStepStepCount.get()+"\n");
		
		sb.append("reOperTimes:"+reOperCount.get()+"\n");
		sb.append("nodeCount by cycleTestNode:"+cycleTestNode()+"\n");
		sb.append("countPerDoAdd(difficulty of location):"+countPerDoAdd+"\n");
		
		// 校验
		// 详细
		int nodeStepCount = 0,nodeCount = 0;
		for (NodeStepBase nodeStepStep : nodeStepStepList) {
			List<NodeStepBase> nodeStepListByNodeStepStep = getNodeStepListByNodeStepStep(nodeStepStep);
			nodeStepCount += nodeStepListByNodeStepStep.size();
			for (NodeStepBase nodeStep : nodeStepListByNodeStepStep) {
				List<Node> nodeListByNodeStep = getNodeListByNodeStep(nodeStep);
				nodeCount += nodeListByNodeStep.size();
			}
		}
		sb.append("nodeStepCount:"+nodeStepCount+",nodeCount:"+nodeCount);
		return sb.toString();
	}
	private void showStackStr(final ConcurrentHashMap<Thread, StackTraceElement[]> elementLockStackTraceMap){
		for (Entry<Thread, StackTraceElement[]> entry : elementLockStackTraceMap.entrySet()) {
			System.err.println(entry.getKey()+":");
			int count = 0;
			for(StackTraceElement s: entry.getValue()){
	            System.err.println("-------"+s.getMethodName()+" : "+s);
	            if(count ++ >2){
	            	break;
	            }
	        }
		}
	}
	private List<NodeStepBase> getNodeStepStepList(NodeStepBase head){
		List<NodeStepBase> result = new ArrayList<NodeStepBase>();
		NodeStepBase currentNodeStepStep = head;
		while(currentNodeStepStep != null){
			result.add(currentNodeStepStep);
			currentNodeStepStep = (NodeStepBase)currentNodeStepStep.getNext();
		}
		return result;
	}
	private List<NodeStepBase> getNodeStepListByNodeStepStep(NodeStepBase nodeStepStep){
		List<NodeStepBase> result = new ArrayList<NodeStepBase>();
		int nodeStepCount = nodeStepStep.getNodeCount();
		NodeStepBase currentNodeStep = (NodeStepBase)nodeStepStep.getHead();
		int i = 0;
		for(;i<nodeStepCount && currentNodeStep != null; i++){
			result.add(currentNodeStep);
			
			// 校验
			if(currentNodeStep.getParentNS() != nodeStepStep){
				log.error("currentNodeStep.getNodeStepStep() != nodeStepStep");
			}
			
			currentNodeStep = (NodeStepBase)currentNodeStep.getNext();
		}
		if(currentNodeStep == null && i !=nodeStepCount){
			log.error("currentNodeStep == null,i:"+i+",nodeStepCount:"+nodeStepCount);
		}
		return result;
	}
	private List<NodeStepBase> getNodeStepList(NodeStepBase head){
		List<NodeStepBase> result = new ArrayList<NodeStepBase>();
		NodeStepBase currentNodeStep = head;
		while(currentNodeStep != null){
			result.add(currentNodeStep);
			currentNodeStep = (NodeStepBase)currentNodeStep.getNext();
		}
		return result;
	}
	private List<Node> getNodeListByNodeStep(NodeStepBase nodeStep){
		List<Node> result = new ArrayList<Node>();
		int nodeCount = nodeStep.getNodeCount();
		Node currentNode = (Node)nodeStep.getHead();
		int i = 0;
		for(;i<nodeCount && currentNode != null;i++){
			result.add(currentNode);
			
			// 校验
			if(currentNode.getParentNS() != nodeStep){
				// 并发状态下这个是可能出现的，因为在这里没有加锁，如果在非并发情况下出现就有问题了
				log.error("currentNode.getNodeStep() != nodeStep:"+currentNode.getParentNS()+"!="+nodeStep); 
			}
			
			currentNode = (Node)currentNode.getNext();
		}
		if(currentNode == null && i != nodeCount){
			log.error("currentNode == null,i:"+i+",nodeCount:"+nodeCount);
		}
		return result;
	}
	private List<Node> getNodeList(Node head){
		List<Node> result = new ArrayList<Node>();
		Node currentNode = head;
		
		while(currentNode != null){
			result.add(currentNode);
			currentNode = (Node)currentNode.getNext();
		}
		return result;
	}
	//
	public int getElementLockCount(){
		return elementLockCount.get();
	}
	public int getElementUnlockCount() {
		return elementUnlockCount.get();
	}
	public  int getNodeLockCount(){
		return nodeLockCount.get();
	}
	public int getNodeUnlockCount() {
		return nodeUnlockCount.get();
	}
	public int getElementStepCombineTime() {
		return elementStepCombineTime.get();
	}
	public int getCutNodeStepCount() {
		return cutNodeStepCount.get();
	}
	public int getCombineNodeStepCount() {
		return combineNodeStepCount.get();
	}
	public int getCutNodeStepStepCount() {
		return cutNodeStepStepCount.get();
	}
	public int getCombineNodeStepStepCount() {
		return combineNodeStepStepCount.get();
	}
	public int getSetCount() {
		return setCount.get();
	}
	public int getGetCount() {
		return getCount.get();
	}
	public int getDeleteCount() {
		return deleteCount.get();
	}
	public int getReOperCount() {
		return reOperCount.get();
	}
	public int getFialHitByNodeStepStep() {
		return fialHitByNodeStepStep.get();
	}
	public int getFialHitByNodeStep() {
		return fialHitByNodeStep.get();
	}
	
	
}






