package test;

import java.util.concurrent.ThreadPoolExecutor;

import org.hq.rank.service.IRankService;
import org.hq.rank.service.RankService;


/***
 * 这个创建一种测试，这个测试运行一下就能将很多种可能测试一遍
 * 需要测试的情况如下：
 * id：固定，随机，较少（多重复）
 * 
 * 单字段：
 * 固定value，
 * 随机value，
 * 有限value，
 * 两个字段：
 * 随机较少
 * 第一个多，第二个少
 * 第一个少，第二个多
 * 都多
 * 三个字段：
 * 
 * 
 * 删除相关
 * @author zhen
 *
 */
public class TestAll {
	
	
	static IRankService rankService = new RankService();
	
	public static void main(String[] args){
		
		rankService.createRank("first");
		rankService.createRank("second");
		for(int i=0;i<100;i++){
			rankService.put("first", 123*i, 321*i);
//			rankService.put("first", 123*i, 654);
			rankService.put("second", 123*i, 321*i);
			rankService.put("first", 123*i, 123*i);
			rankService.put("second", 123*i, 123*i);
		}
		rankService.delete("first", 123*6);
		System.out.println(rankService.getRankDataById("first", 123*7));
		rankService.deleteRank("first");
		rankService.deleteRank("second");
		
	}
}
