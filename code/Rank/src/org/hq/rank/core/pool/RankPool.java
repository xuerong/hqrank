package org.hq.rank.core.pool;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.hq.rank.core.Rank;
import org.hq.rank.core.RankException;
import org.hq.rank.core.element.Element;
import org.hq.rank.core.element.ElementStep;
import org.hq.rank.core.node.ElementNode;
import org.hq.rank.core.node.Node;
import org.hq.rank.core.node.NodeStepBase;
import org.hq.rank.core.node.RankElement;
import org.hq.rank.core.node.RankElementNode;

/**
 * 这个池中可以存储一些Node、RankElement和Element，NodeStep,NodeStepStep,ElementStep同事他们提供reset方法
 * 
 * 可以通过put的时候，查看它的其它引用是否还在，来防止内存泄露
 * @author zhen
 *
 */
public class RankPool {
	// 池的最大值
	private final int eNodeMaxSize = 100;
	private final int rNodeMaxSize = 100;
	private final int rankElementMaxSize = 100;
	private final int elementMaxSize = 100;
	private final int nodeStepBaseMaxSize = 100;
	private final int elementStepMaxSize = 100;
	
	private final Rank rank; // 一个rank一个池
	public RankPool(Rank rank){
		this.rank = rank;
	}
	/**
	 * element
	 */
	private final Queue<Element> elements = new ConcurrentLinkedQueue<Element>();
	public void putElement(Element element){
		if(element == null){
			return;
		}
		if(elements.size() > elementMaxSize){
			return;
		}
		element.reset();
		elements.offer(element);
	}
	public Element getElement(int id,long... value){
		Element element = elements.poll();// new Element(rank);
		if(element == null){
			element = new Element(rank);
		}
		element.setId(id);
		element.setValue(value);
		return element;
	}
	public Element getNewElement(int id,long... value){
		Element element = new Element(rank);
		element.setId(id);
		element.setValue(value);
		return element;
	}
	/**
	 * node
	 */
	private final Queue<Node> eNodes = new ConcurrentLinkedQueue<Node>();
	private final Queue<Node> rNodes = new ConcurrentLinkedQueue<Node>();
	/**
	 * 放进去的时候，被锁的情况是不确定的
	 * @param node
	 */
	public void putNode(Node node){
		if(node == null){
			return;
		}
		node.reset();
		if(node instanceof ElementNode){
			if(eNodes.size() > eNodeMaxSize){
				return;
			}
			eNodes.offer(node);
		}else{
			if(rNodes.size() > rNodeMaxSize){
				return;
			}
			rNodes.offer(node);
		}
	}
	/**
	 * 得到一个node，此node被锁的情况是不确定的
	 * @param element
	 * @param value
	 * @param conditionLevel
	 * @return
	 */
	public Node getNode(Element element,long value,final int conditionLevel){
		if(conditionLevel < 0){ // 这个地方可以详细验证
			throw new RankException("error");
		}
		Node node = null;
		if(conditionLevel > 0){
			node = rNodes.poll();
			if(node == null){
				node = new RankElementNode(rank);
			}
		}else{
			node = eNodes.poll();
			if(node == null){
				node = new ElementNode(rank);
			}
		}
		node.init(element, value, conditionLevel);
		return node;
	}
	public Node getNode(long value,int id,final int conditionLevel){
		return getNode(getNewElement(id, value), value, conditionLevel);
	}
	private final Queue<NodeStepBase> nodeStepBases = new ConcurrentLinkedQueue<NodeStepBase>();
	public void putNodeStepBase(NodeStepBase nodeStepBase){
		if(nodeStepBase == null){
			return;
		}
		if(nodeStepBases.size() > nodeStepBaseMaxSize){
			return;
		}
		nodeStepBase.reset();
		nodeStepBases.offer(nodeStepBase);
	}
	public NodeStepBase getNodeStepBase(NodeStepBase parentNS){ // 如果parentNS是null，说明是最高一级
		NodeStepBase nodeStepBase = nodeStepBases.poll();
		if(nodeStepBase == null){
			nodeStepBase = new NodeStepBase(rank);
		}
		nodeStepBase.init(parentNS);
		return nodeStepBase;
	}
	/**
	 * RankElement
	 */
	private final Queue<RankElement> rankElements = new ConcurrentLinkedQueue<RankElement>();
	public void putRankElement(RankElement rankElement){
		if(rankElement == null){
			return;
		}
		if(rankElements.size() > rankElementMaxSize){
			return;
		}
		rankElement.reset();
		rankElements.offer(rankElement);
	}
	public RankElement getRankElement(final Element element,final int conditionLevel){
		RankElement rankElement = rankElements.poll();
		if(rankElement == null){
			rankElement = new RankElement(rank);
		}
		while(!rankElement.init(element, conditionLevel)){
			rankElement = rankElements.poll();
			if(rankElement == null){
				rankElement = new RankElement(rank);
			}
		}
		return rankElement;
	}
	/**
	 * ElementStep
	 */
	private final Queue<ElementStep> elementSteps = new ConcurrentLinkedQueue<ElementStep>();
	public void putElementStep(ElementStep elementStep){
		if(elementStep == null){
			return;
		}
		if(elementSteps.size() > elementStepMaxSize){
			return;
		}
		elementStep.reset();
		elementSteps.offer(elementStep);
	}
	public ElementStep getElementStep(Node node){
		ElementStep elementStep = elementSteps.poll();
		if(elementStep == null){
			elementStep = new ElementStep(rank);
		}
		elementStep.init(node);
		return elementStep;
	}
	@Override
	public String toString(){
		// Node、RankElement和Element，ElementStep
		StringBuilder sb = new StringBuilder();
		sb.append("RankPool:")
		.append("elementNode:"+eNodes.size()+",rankENode:"+rNodes.size())
		.append(",rankElement:"+rankElements.size())
		.append(",Element:"+elements.size())
		.append(",NodeStepBase:"+nodeStepBases.size())
		.append(",ElementStep:"+elementSteps.size());
		return sb.toString();
	}
}
