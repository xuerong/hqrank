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
# hqrank原理
#####1、排行数据结构
![image](https://github.com/xuerong/hqrank/blob/master/resource/hqrank-datastructure.jpg)
* node链表按照从大到小记录所有value,每个node节点记录一个分数，保存一个element节点链表
* 每个element链表按照时间从小到大记录该value的id，每个element节点记录一个id
* step链表(1)是对node链表的索引，提高对node的定位速度
* step链表(2)是对step链表(1)的索引，提高对step(1)的定位速度，从而提高对node的定位速度
* step链表(3)是对element链表的索引，提高对element的定位速度
* step和node中保存有其索引范围内的element数量，即排行所需数据

# More

* redis不是hqrank项目中的部分，是用来测试的，因为redis中有实时排序功能，在这里也提供一个redis的下载[Redis-x64-2.8.2400](http://pan.baidu.com/s/1o87v5s2)
* test包中有基本的例子。BaseTest1：基本的用法；BaseTest2：压力测试
* 测试过程中可以用jprofiler和jdk工具jconsole.exe来监控内存使用情况
* 工具还在开发过程中，还没有经过大量测试，如果有对此有兴趣的Coder，欢迎一起讨论和开发