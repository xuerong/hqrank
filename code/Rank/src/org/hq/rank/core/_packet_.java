package org.hq.rank.core;

public class _packet_ {
	/**
	 * 最后加try cache，尤其是获取的里面，如果出现空指针之类的
	 * 池要限制最大量，达到最大力量不再放入，或者清理多余
	 * hashCode是否有必要再hash一次，更离散一些
	 * 
	 * 锁和解锁尽量用try 和 finally结构
	 * 
	 * 拆分node
	 * 对象继承一个父类，实现previous和next相关方法
	 * 
	 * 后面加功能的时候，如果有return，别忘了本函数中的锁解锁
	 * 
	 * 
	 * if(currentNodeStep.
					getHead(). // 这个地方有空指针可能
					getValue() < value){
	 */
}
