package org.hq.rank.core;

import java.lang.reflect.Field;

import org.hq.rank.core.node.ElementNode;

import sun.misc.Unsafe;

public final class UnsafeSupport {
	public static final Unsafe unsafe = getUnsafe();
	
	private static final Unsafe getUnsafe(){
		try {
			Class<?> unsafeClass = Unsafe.class;
			for (Field f : unsafeClass.getDeclaredFields()) {
				if ("theUnsafe".equals(f.getName())) {
					f.setAccessible(true);
					return (Unsafe) f.get(null);
				}
			}
			throw new IllegalAccessException("no declared field: theUnsafe");
		} catch (Exception e) {
			throw new Error(e);
		}
	}
	
	public static final long getValueOffset(Class<?> cls,String valueName){
		try {
			return unsafe.objectFieldOffset
		            (cls.getDeclaredField(valueName));
		} catch (Exception e) {
			throw new Error(e);
		}
	}
	
	public static final int getAndIncrement(Object object,long valueOffset){
		for (;;) {
			int current = unsafe.getIntVolatile(object,valueOffset);
            int next = current + 1;
            if (compareAndSet(object,valueOffset,current, next))
                return current;
        }
	}
	
	public static final int getAndDecrement(Object object,long valueOffset) {
        for (;;) {
        	int current = unsafe.getIntVolatile(object,valueOffset);
            int next = current - 1;
            if (compareAndSet(object,valueOffset,current, next))
                return current;
        }
    }
	
	public static final int decrementAndGet(Object object,long valueOffset) {
        for (;;) {
        	int current = unsafe.getIntVolatile(object,valueOffset);
            int next = current - 1;
            if (compareAndSet(object,valueOffset,current, next))
                return next;
        }
    }
	
	public static final int getAndAdd(Object object,long valueOffset,int delta) {
        for (;;) {
        	int current = unsafe.getIntVolatile(object,valueOffset);
            int next = current + delta;
            if (compareAndSet(object,valueOffset,current, next))
                return current;
        }
    }
	
	public static final boolean compareAndSet(Object object,long valueOffset,int expect, int update) {
        return unsafe.compareAndSwapInt(object, valueOffset, expect, update);
    }
	
//	public static abstract class A{
//		long valueOffset = getValueOffset(A.class, "a");
//		int a=0;
//		int b=0;
//		public int getAndDecrement1(){
//			return getAndDecrement(this, valueOffset, b);
//		}
//	}
//	public static class B extends A{
//		int c=0;
//	}
//	public static void main(String[] args){
////		B b = new B();
////		b.getAndDecrement1();
////		
////		
//		ElementNode elementNode = new ElementNode(new Rank());
//		elementNode.decrementAndGet();
//		System.out.println(elementNode.getCount());
//	}
}
