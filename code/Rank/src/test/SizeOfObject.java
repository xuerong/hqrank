package test;

import java.lang.instrument.Instrumentation;  
import java.lang.reflect.Array;  
import java.lang.reflect.Field;  
import java.lang.reflect.Modifier;  
import java.util.ArrayDeque;  
import java.util.Deque;  
import java.util.HashSet;  
import java.util.Set;  
  
/** 
 * 对象占用字节大小工具类 
 * 
 * @author tianmai.fh 
 * @date 2014-03-18 11:29 
 */  
public class SizeOfObject {  
    static Instrumentation inst;  
    public static void main(String[] args){
    	
    }
    public static void premain(String args, Instrumentation instP) {  
        inst = instP;  
    }  
  
    /** 
     * 直接计算当前对象占用空间大小，包括当前类及超类的基本类型实例字段大小、<br></br> 
     * 引用类型实例字段引用大小、实例基本类型数组总占用空间、实例引用类型数组引用本身占用空间大小;<br></br> 
     * 但是不包括超类继承下来的和当前类声明的实例引用字段的对象本身的大小、实例引用数组引用的对象本身的大小 <br></br> 
     * 
     * @param obj 
     * @return 
     */  
    public static long sizeOf(Object obj) {  
        return inst.getObjectSize(obj);  
    }  
  
    /** 
     * 递归计算当前对象占用空间总大小，包括当前类和超类的实例字段大小以及实例字段引用对象大小 
     * 
     * @param objP 
     * @return 
     * @throws IllegalAccessException 
     */  
    public static long fullSizeOf(Object objP) throws IllegalAccessException {  
        Set<Object> visited = new HashSet<Object>();  
        Deque<Object> toBeQueue = new ArrayDeque<Object>();  
        toBeQueue.add(objP);  
        long size = 0L;  
        while (toBeQueue.size() > 0) {  
            Object obj = toBeQueue.poll();  
            //sizeOf的时候已经计基本类型和引用的长度，包括数组  
            size += skipObject(visited, obj) ? 0L : sizeOf(obj);  
            Class<?> tmpObjClass = obj.getClass();  
            if (tmpObjClass.isArray()) {  
                //[I , [F 基本类型名字长度是2  
                if (tmpObjClass.getName().length() > 2) {  
                    for (int i = 0, len = Array.getLength(obj); i < len; i++) {  
                        Object tmp = Array.get(obj, i);  
                        if (tmp != null) {  
                            //非基本类型需要深度遍历其对象  
                            toBeQueue.add(Array.get(obj, i));  
                        }  
                    }  
                }  
            } else {  
                while (tmpObjClass != null) {  
                    Field[] fields = tmpObjClass.getDeclaredFields();  
                    for (Field field : fields) {  
                        if (Modifier.isStatic(field.getModifiers())   //静态不计  
                                || field.getType().isPrimitive()) {    //基本类型不重复计  
                            continue;  
                        }  
  
                        field.setAccessible(true);  
                        Object fieldValue = field.get(obj);  
                        if (fieldValue == null) {  
                            continue;  
                        }  
                        toBeQueue.add(fieldValue);  
                    }  
                    tmpObjClass = tmpObjClass.getSuperclass();  
                }  
            }  
        }  
        return size;  
    }  
  
    /** 
     * String.intern的对象不计；计算过的不计，也避免死循环 
     * 
     * @param visited 
     * @param obj 
     * @return 
     */  
    static boolean skipObject(Set<Object> visited, Object obj) {  
        if (obj instanceof String && obj == ((String) obj).intern()) {  
            return true;  
        }  
        return visited.contains(obj);  
    }  
}
