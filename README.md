# hqrank介绍
支持并发的实时排行、排行榜工具-java

目前hqrank的实现包括如下几点：

#####1、全缓存
所有的排行数据存储在内存中。所以，hqrank是作为一个排行工具，而不是数据存储工具而存在

#####2、基于id:value
对数据的存取可以通过id-value进行，可通过id获取对应排名，也可以通过排名获取对应id。（value也会在此给出）
分数不同的按照从大到小的顺序排列，分数相同的按照插入或修改时间顺序排列

#####3、支持多字段排行
比如先按等级排行，在此基础上再按经验排行，进而得出的排行列表。理论上支持任意多的字段数

#####4、支持并发
支持多个线程同时对排行进行操作

#####5、实时排行
排行操作是实时进行的，但需要说明一点：对于访问时间相差很少的两个访问，尤其是不同线程，可能不会严格满足先入先排，但这种相差时间是在毫秒级的（这和线程时间片以及系统提供加锁顺序相关）

#####5、目前实现功能
* 根据id获取排行数据及分数
* 根据排行获取对应id
* 根据id获取id前后若干个玩家的排行数据
* 分页查询，根据每页大小和页码获取排行数据
* 根据字段更新数据

#  如何使用

hqrank/code/Rank/是一个java项目，开发使用jdk版本为：1.7  
1、将该项目导入eclipse中  
2、在包org/hq/rank/service/中有IRankService.java，里面有详细的注释  
3、基本访问方法如下：
```Java
public static void main(String[] args) {
	IRankService rankService = new RankService();
	rankService.createRank("rankName");
	rankService.put("rankName", 10/*id*/, 100/*value*/); // put date to rank
	RankData rankData = rankService.getRankDataById("rankName", 10); // get date by id
	int rankNum = rankData.getRankNum(); // get rank num
	RankData rankData2 = rankService.getRankDataByRankNum("rankName", rankNum); // get date by rankNum
	List<RankData> rankDataList1 = rankService.getRankDatasAroundId("rankName", testId, 3, 6); // get date by id,and ranks around this id
	List<RankData> rankDataList2 = rankService.getRankDatasByPage("rankName", 7/*page*/, 9/*pageSize*/); // get date by page
}
```
![image](https://github.com/xuerong/hqrank/tree/master/resource/hqrank-datastructure.jpg)
# More

* redis不是hqrank项目中的部分，是用来测试的，因为redis中有实时排序功能，在这里也提供一个redis的下载[Redis-x64-2.8.2400](http://pan.baidu.com/s/1o87v5s2)
* test包中有基本的例子。BaseTest1：基本的用法；BaseTest2：压力测试
* 测试过程中可以用jprofiler和jdk工具jconsole.exe来监控内存使用情况
* 工具还在开发过程中，还没有经过大量测试，如果有对此有兴趣的Coder，欢迎一起讨论和开发


# About hqrank
Support for concurrent real-time ranking, ranking tool -java  
At present, the implementation of hqrank includes the following points:
#####1, full cache
All of the data is stored in memory. So, hqrank is a ranking tool, not a data storage tool.
#####2, based on id:value
Access to data can be carried out through the id-value, you can get the corresponding ranking by ID, you can also get the corresponding ID ranking. (value will also be given here)  
The scores differ according to the order from the large to small, the same score according to insert or modify the time sequence
#####3, support multi field ranking
For example, by ranking first, on this basis and then by experience, and then come out of the list. Theoretical support for any number of fields
#####4, support for concurrency
Supports multiple threads while operating on the list
#####5, real time ranking
Ranking operation is performed in real time, but need to illustrate a point: for the time to visit a small difference in the two access, especially different threads, may not be strictly satisfies the first in first row, but the difference of time is measured in milliseconds (the thread's time slice and the system provide locking sequence dependent)  

#####5, the current implementation of the function
* Get ranking data and scores based on ID
* According to the ranking to obtain the corresponding ID
* According to Id get ID before and after a number of players in the ranking data
* Paging query, according to the page size and page ranking data acquisition

# how to use
Hqrank/code/Rank/ is a java project, Developed using JDK version is: 1.7  
1, the project into the eclipse  
2, in the package IRankService.java in org/hq/rank/service/, which has detailed notes  
3, the basic access method is as follows:
```Java
public static void main(String[] args) {
	IRankService rankService = new RankService();
	rankService.createRank("rankName");
	rankService.put("rankName", 10/*id*/, 100/*value*/); // put date to rank
	RankData rankData = rankService.getRankDataById("rankName", 10); // get date from rank
	int rankNum = rankData.getRankNum(); // get rank num
	RankData rankData2 = rankService.getRankDataByRankNum("rankName", rankNum); // get date by rankNum
	List<RankData> rankDataList1 = rankService.getRankDatasAroundId("rankName", testId, 3, 6); // get date by id,and ranks around this id
	List<RankData> rankDataList2 = rankService.getRankDatasByPage("rankName", 7/*page*/, 9/*pageSize*/); // get date by page
}
```
# More
* redis is not part of the hqrank project, is used to test, because the redis has a real-time sorting function, here also provides a redis download [Redis-x64-2.8.2400](http://pan.baidu.com/s/1o87v5s2)
* there are basic examples in test package. BaseTest1: basic usage; BaseTest2: stress test
* test procedures can be used JProfiler and JDK tool jconsole.exe to monitor memory usage
* Tools are still in the process of development, has not been a lot of testing, if there is an interest in Coder, welcome to discuss and develop