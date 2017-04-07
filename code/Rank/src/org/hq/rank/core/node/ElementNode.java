package org.hq.rank.core.node;

import java.util.List;

import org.hq.rank.core.Rank;
import org.hq.rank.core.UnsafeSupport;
import org.hq.rank.core.element.Element;
import org.hq.rank.core.element.ElementStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElementNode extends Node{

	private static Logger log = LoggerFactory
			.getLogger(ElementNode.class);
	/**
	 * Element类型的Node参数
	 */
	private Element head;
	//Node中ElementStep的引用
	private ElementStep headStep , tailStep;
	private volatile Element tail;
	/**
	 * 构造的时候有没有可能被访问？add方法
	 * */
	
	public ElementNode(Rank rank){
		super(rank);
	}
	@Override
	public void init(Element element,long value,final int conditionLevel){
		super.init(element, value, 0);
		
		if(value != Long.MAX_VALUE){ // 头里面什么也不要，否则可能出现内存溢出
			element.setNode(this);
			this.head = element;
			this.tail = element;
		}
	}

	public ElementStep getHeadStep() {
		return headStep;
	}

	public ElementStep getTailStep() {
		return tailStep;
	}

	public Element getHead() {
		return head;
	}

	@Override
	public Element add(Element element) {
		boolean isLock = rank.getLockerPool().tryLockNodeRLocker(this, 0);
		if(!isLock){
			return null;
		}
		boolean isNeedUnlock = true;
		try{
			if(getCount() <= 0){
				return null;
			}
			// 防止与其它线程共同添加或操作
			// 在tail lock的函数中个，在执行getAndIncrement之前，tail指向被修改，此时，lock的element就不是新的element
			Element _tail=tail;
			if(_tail == null){
				return null;
			}
			if(!_tail.lock()){
				return null;
			}
			if(_tail != tail){
				_tail.unLock();
				return null;
			}
			// 可能被修改或者删除，尤其是在池中重新取出之后
			if(!tail.equalsValue(element)){
				_tail.unLock();
				return null;
			}
			
			
			if(tailStep == null && getCount() > rank.getRankConfigure().getCutCountElementStep()){//ElementStep.fullCount){
				// 创建之，创建的时候加写锁，这样防止其它线程加数据进去
				rank.getLockerPool().unlockNodeRLocker(this, 0);
				isLock = rank.getLockerPool().tryLockNodeWLocker(this, 0);
				if(!isLock){
					isNeedUnlock = false;
					_tail.unLock();
					return null;
				}
				if(this.tailStep == null && getCount() > rank.getRankConfigure().getCutCountElementStep()){//ElementStep.fullCount){ // 再次校验
					this.headStep = rank.getRankPool().getElementStep(this);
					Element currentElement = head;
					while(currentElement != null){
						this.headStep.putElement(currentElement); 
						currentElement = currentElement.getNext();
					}
					this.tailStep = this.headStep;
				}
				rank.getLockerPool().unlockNodeWLocker(this, 0);
				isLock = rank.getLockerPool().tryLockNodeRLocker(this, 0);
				if(!isLock){
					isNeedUnlock = false;
					_tail.unLock();
					return null;
				}
			}
			if(tailStep != null){
				// 拆分
				if(tailStep.getCount() >= rank.getRankConfigure().getCutCountElementStep()){//ElementStep.fullCount){
					
					rank.getLockerPool().unlockNodeRLocker(this, 0);
					isLock = rank.getLockerPool().tryLockNodeWLocker(this, 0);
					if(!isLock){
						isNeedUnlock = false;
						_tail.unLock();
						return null;
					}
					if(tailStep.getCount() >= rank.getRankConfigure().getCutCountElementStep()){//ElementStep.fullCount){ // 再次校验
						
						ElementStep newStep = rank.getRankPool().getElementStep(this);
						tailStep.putElement(element);
						newStep.setPrevious(tailStep);
						tailStep.setNext(newStep);
						tailStep = newStep;
					}
					rank.getLockerPool().unlockNodeWLocker(this, 0);
					isLock = rank.getLockerPool().tryLockNodeRLocker(this, 0);
					if(!isLock){
						isNeedUnlock = false;
						_tail.unLock();
						return null;
					}
				}else{
					tailStep.putElement(element);
				}
			}
			
			element.setPrevious(_tail); //要先设置这个，再加入链表，否则查询相关操作可能会出错
			element.setNode(this);
			_tail.setNext(element);
			tail = element;
			
//			elementCount.getAndIncrement();
			getAndIncrement();
			
			if(parentNS != null){
				parentNS.putElement(); // 有没有可能空指针？不可能
			}
			_tail.unLock(); // 注意，前面的tail被赋值了
			
			return element;
		}finally{
			if(isNeedUnlock){
				rank.getLockerPool().unlockNodeRLocker(this, 0);
			}
		}
	}
	
	@Override
	public int getRankValue(Element element) {
		int rankNum = 0;
		Element currentElement = head;
		ElementStep step = element.getStep();
		if(step != null){
			ElementStep currentStep = headStep;
			while(currentStep != null && currentStep != step){
				rankNum += currentStep.getCount();
				currentStep = currentStep.getNext();
			}
			
			if(currentStep == null){
				log.warn("currentStep is null");
				currentElement = head;
				rankNum = 0;
			}else{
				currentElement = step.getHead();
			}
		}
		
		while(currentElement != null && currentElement != element){
			rankNum++;
			currentElement = currentElement.getNext();
		}
		if(currentElement == null){
			log.warn("currentElement is null");
			return -1;
		}
		return rankNum;
	}
	/**
	 * 这个可以通过传进来一个List<Element>，来减少new它的次数
	 * @param begin
	 * @param length
	 * @return
	 */
	@Override
	public void getElementsByIndex(List<Element> elementList , int begin ,int length) {
		if(getCount() <= begin || getCount()<1){
			return;
		}
		Element currentElement = head;
		int currentIndex = 0;
		while(currentElement != null && currentIndex < begin+length){
			if(currentIndex >= begin){
				elementList.add(currentElement);
			}
			currentElement = currentElement.getNext();
			currentIndex++;
		}
		return;
	}

	@Override
	public boolean delete(Element element) {
		boolean _isLock = rank.getLockerPool().tryLockNodeRLocker(this, 0);
		if(!_isLock){
			return false;
		}
		try {
			// 这个时候是不可能有人在删除node的，所以不用添加node的读锁
			Element pre = element.getPrevious();
			Element next = element.getNext();
			boolean isLock = lockMultipleElement(pre,next);
			if(!isLock){
				return false;
			}
			// 再校验
			if(element.getPrevious() != pre || element.getNext()!=next){
				unLockMultipleElement(pre,next);
				return false;
			}
			if((pre !=null && pre.getNext() !=element) || (next !=null && next.getPrevious() !=element)){
				unLockMultipleElement(pre,next);
				return false;
			}
			if(parentNS != null){
				parentNS.removeElement();
			}
			int c = decrementAndGet();
			if(c <= 0){
				// 当前node不能用就不解锁，这样其它的线程就不能用它，直到它被删除
				tail = null;
				unLockMultipleElement(pre,next);
				return true;
			}
			
			if(pre!=null){
				pre.setNext(next);
			}
			if(next != null){
				next.setPrevious(pre);
			}
			if(head == element){
				head = next;
			}
			if(tail == element){
				tail = pre;
			}
			
			ElementStep step = element.getStep();
			if(step != null){
				step.removeElement(element);
			}
			unLockMultipleElement(pre,next);
			return true;
		} finally {
			rank.getLockerPool().unlockNodeRLocker(this, 0);
		}
	}
	@Override
	public String toString(){
		if(value == Long.MAX_VALUE){
			return "head";
		}
		// step数量，总数量，每个step中的数量
		int stepNum = 0;
		int elementNum = 0;
		ElementStep currentStep = headStep ;
		StringBuilder sb = new StringBuilder("(");
		while(currentStep != null){
			stepNum++;
			elementNum += currentStep.getCount();
			sb.append(currentStep.getCount()+",");
			currentStep = currentStep.getNext();
		}
		StringBuilder rSb = new StringBuilder();
		rSb.append("node(value:"+value+"):")
		.append("stepNum:"+stepNum)
		.append(",elementNum:("+elementCount+"=="+elementNum+")")
		.append(",steps:"+sb.toString()+")");
		return rSb.toString();
	}
	
	@Override
	public void reset() {
		super.reset();
		rank.getRankPool().putElementStep(headStep);
		headStep =null ;
		tailStep = null;
		tail = null;
		head = null; // 后来加上的应该不会出现问题吧~
	}
	@Override
	public int getConditionLevel() {
		return 0;
	}

}
