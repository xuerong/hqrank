package org.hq.rank.core;

public class _packet_ {
	/**
	 * 最后加try cache，尤其是获取的里面，如果出现空指针之类的
	 * 池要限制最大量，达到最大力量不再放入，或者清理多余
	 * hashCode是否有必要再hash一次，更离散一些
	 * 
	 * 锁和解锁尽量用try 和 finally结构
	 * 
	 * 
	 * 后面加功能的时候，如果有return，别忘了本函数中的锁解锁
	 * 
	 * 
	 * if(currentNodeStep.
					getHead(). // 这个地方有空指针可能
					getValue() < value){
	 */
	/**
	 * 后面要实现的功能：
	 * 1、配置文件：针对所有的rank，针对单个的rank（配置rank）
	 * 2、添加持久化：(1)同步/异步存储到文件/数据库，(2)启动加载，停止存储
	 * 3、失败的操作持久化
	 * 4、统一所有的异常，在service层捕获并处理
	 * 5、添加英文注释？
	 * 
	 * 
	 * 修改为maven项目？
	 */
}
