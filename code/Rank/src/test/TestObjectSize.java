package test;
/**
 * 6 40b
 * 5 32b
 * 4 32b
 * 3 24b
 * 2 24b
 * 1 16b
 * 0 16b
 * 
 * 
 * 现下如下结论：
 * 1、8b作为基本的分配单元
 * 2、空对象应该在大于8b，小于等于12b
 * 3、一个int值4b
 * 4、占用字节数公式：16+n/2*8
 * 
 * @author a
 *
 */
public class TestObjectSize {
	private int value1=1;
	private int value2=1;
	private int value3=1;
	private int value4=1;
	private int value5=1;
	private int value6=1;
	public static void main(String[] args) throws InterruptedException{
		TestObjectSize testObjectSize = new TestObjectSize();
		synchronized (testObjectSize) {
			testObjectSize.wait();
		}
		
	}
}
