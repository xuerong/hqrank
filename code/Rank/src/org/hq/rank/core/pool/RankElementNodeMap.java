package org.hq.rank.core.pool;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.hq.rank.core.Rank;
import org.hq.rank.core.node.Node;
import org.hq.rank.core.node.RankElement;

/**
 * 如果是分段排行，rankElement比较多，每个里面单独一个ConcurrentHashMap占用太多内存
 * 统一用一组ConcurrentHashMap，数量可配置
 * 
 * hashcode可以再做一次离散处理，包括锁池里面的hashcode
 * get put remove clear
 * @author zhen
 *
 */
public class RankElementNodeMap {
	private final Rank rank;
	private final int mapCount;
	//存入rankElement.hascode%mapCount， key = rankElement.hascode+"_"+node.value
	private final ConcurrentHashMap<String, Node>[] maps; // id
	private final AtomicLong elementIdCreator = new AtomicLong(0);
	
	public RankElementNodeMap(Rank rank){
		this.rank = rank;
		this.mapCount = 1000;
		if(rank.getRankConfigure().getRankConditionCount() > 1){
			maps = new ConcurrentHashMap[this.mapCount];
			for(int i = 0;i< mapCount;i++){
				maps[i] = new ConcurrentHashMap<String, Node>();
			}
		}else{
			maps = null;
		}
	}
	// ElementId生成器，用于生成element的唯一id
	public String getNewId(){
		return ""+elementIdCreator.getAndIncrement();
	}
	public long getId(){
		return elementIdCreator.get();
	}
	// 由于rankElement.hashCode()是其内存地址，所以，不同的RankElement一定不同
	public Node get(RankElement rankElement,long value){
		int hashCode = rankElement.hashCode();
		ConcurrentHashMap<String, Node> map = maps[hashCode%mapCount];
		return map.get(rankElement.id+"_"+value);
	}
	public void put(RankElement rankElement,long value,Node node){
		int hashCode = rankElement.hashCode();
		ConcurrentHashMap<String, Node> map = maps[hashCode%mapCount];
		map.put(rankElement.id+"_"+value, node);
	}
	public void remove(RankElement rankElement,long value){
		int hashCode = rankElement.hashCode();
		ConcurrentHashMap<String, Node> map = maps[hashCode%mapCount];
		map.remove(rankElement.id+"_"+value);
	}
	
}
