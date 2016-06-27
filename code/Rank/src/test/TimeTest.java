package test;

import java.util.concurrent.atomic.AtomicInteger;

public class TimeTest {
	public static int time1 = 0;
	public static int time2 = 0;
	public static final int perCount = 8000;
	public static AtomicInteger count = new AtomicInteger(0);
	public static void println(int index){
		if(index == 0){
			count.getAndIncrement();
		}
		if(count.get()%perCount == 0){
			System.err.println(index+"dayin");
		}
	}
}
