package org.hq.rank.core;

import org.hq.rank.core.Rank.ReOperType;

public class RankConfigure {
	/**
	 * rank的配置
	 * ReOperType
	 * scheduleThreadCount
	 * maxScheduleTime
	 * multiThreadCount
	 * warnReOperTimes
	 * errorReoperTimes
	 * 
	 * NodeStep.fullCount
	 * NodeStep.deleteCount
	 * NodeStep.maxHitTimes
	 * NodeStepStep.fullCount
	 * NodeStepStep.deleteCount
	 * NodeStepStep.maxHitTimes
	 */
	// 默认值
	private static final ReOperType REOPERTYPE_DEFAULT = ReOperType.MultiSche;
	private static final int SCHEDULETHREADCOUNT_DEFAULT = 10;
	private static final int MAXSCHEDULETHREADCOUNT_DEFAULT = 100;
	private static final int MAXSCHEDULETIME_DEFAULT = 100000;// 纳秒
	private static final int MULTITHREADCOUNT_DEFAULT = 10;
	private static final int WARNREOPERTIMES_DEFAULT = 2000;
	private static final int ERRORREOPERTIMES_DEFAULT = 10000;
	private static final int CUTCOUNTNODESTEP_DEFAULT = 600;
	private static final int COMBINECOUNTNODESTEP_DEFAULT = 60;
	private static final int MAXHITTIMESNODESTEP_DEFAULT = 3;
	private static final int CUTCOUNTNODESTEPSTEP_DEFAULT = 600;
	private static final int COMBINECOUNTNODESTEPSTEP_DEFAULT = 60;
	private static final int MAXHITTIMESNODESTEPSTEP_DEFAULT = 3;
	
	//
	private String rankName;
	// 变量
	private ReOperType reOperType = REOPERTYPE_DEFAULT;
	private int scheduleThreadCount = SCHEDULETHREADCOUNT_DEFAULT;
	private int maxScheduleThreadCount = MAXSCHEDULETHREADCOUNT_DEFAULT;
	private int maxScheduleTime = MAXSCHEDULETIME_DEFAULT;
	private int multiThreadCount = MULTITHREADCOUNT_DEFAULT;
	private int warnReOperTimes = WARNREOPERTIMES_DEFAULT;
	private int errorReoperTimes = ERRORREOPERTIMES_DEFAULT;
	private int cutCountNodeStep = CUTCOUNTNODESTEP_DEFAULT;
	private int combineCountNodeStep = COMBINECOUNTNODESTEP_DEFAULT;
	private int maxHitTimesNodeStep = MAXHITTIMESNODESTEP_DEFAULT;
	private int cutCountNodeStepStep = CUTCOUNTNODESTEPSTEP_DEFAULT;
	private int combineCountNodeStepStep = COMBINECOUNTNODESTEPSTEP_DEFAULT;
	private int maxHitTimesNodeStepStep = MAXHITTIMESNODESTEPSTEP_DEFAULT;
	
	// 多条件排行，条件数量
	private int rankConditionCount = 1; // >0
	
	public RankConfigure() {
		rankName = "rank";
	}
	/**
	 * 返回当前配置是否合理
	 * @return
	 */
	public boolean check(){
		return true;
	}

	public ReOperType getReOperType() {
		return reOperType;
	}

	public void setReOperType(ReOperType reOperType) {
		this.reOperType = reOperType;
	}

	public int getScheduleThreadCount() {
		return scheduleThreadCount;
	}

	public void setScheduleThreadCount(int scheduleThreadCount) {
		this.scheduleThreadCount = scheduleThreadCount;
	}

	public int getMaxScheduleThreadCount() {
		return maxScheduleThreadCount;
	}
	public void setMaxScheduleThreadCount(int maxScheduleThreadCount) {
		this.maxScheduleThreadCount = maxScheduleThreadCount;
	}
	public int getMaxScheduleTime() {
		return maxScheduleTime;
	}

	public void setMaxScheduleTime(int maxScheduleTime) {
		this.maxScheduleTime = maxScheduleTime;
	}

	public int getMultiThreadCount() {
		return multiThreadCount;
	}

	public void setMultiThreadCount(int multiThreadCount) {
		this.multiThreadCount = multiThreadCount;
	}

	public int getWarnReOperTimes() {
		return warnReOperTimes;
	}

	public void setWarnReOperTimes(int warnReOperTimes) {
		this.warnReOperTimes = warnReOperTimes;
	}

	public int getErrorReoperTimes() {
		return errorReoperTimes;
	}

	public void setErrorReoperTimes(int errorReoperTimes) {
		this.errorReoperTimes = errorReoperTimes;
	}

	public int getCutCountNodeStep() {
		return cutCountNodeStep;
	}

	public void setCutCountNodeStep(int cutCountNodeStep) {
		this.cutCountNodeStep = cutCountNodeStep;
	}

	public int getCombineCountNodeStep() {
		return combineCountNodeStep;
	}

	public void setCombineCountNodeStep(int combineCountNodeStep) {
		this.combineCountNodeStep = combineCountNodeStep;
	}

	public int getMaxHitTimesNodeStep() {
		return maxHitTimesNodeStep;
	}

	public void setMaxHitTimesNodeStep(int maxHitTimesNodeStep) {
		this.maxHitTimesNodeStep = maxHitTimesNodeStep;
	}

	public int getCutCountNodeStepStep() {
		return cutCountNodeStepStep;
	}

	public void setCutCountNodeStepStep(int cutCountNodeStepStep) {
		this.cutCountNodeStepStep = cutCountNodeStepStep;
	}

	public int getCombineCountNodeStepStep() {
		return combineCountNodeStepStep;
	}

	public void setCombineCountNodeStepStep(int combineCountNodeStepStep) {
		this.combineCountNodeStepStep = combineCountNodeStepStep;
	}

	public int getMaxHitTimesNodeStepStep() {
		return maxHitTimesNodeStepStep;
	}

	public void setMaxHitTimesNodeStepStep(int maxHitTimesNodeStepStep) {
		this.maxHitTimesNodeStepStep = maxHitTimesNodeStepStep;
	}
	public int getRankConditionCount() {
		return rankConditionCount;
	}
	public void setRankConditionCount(int rankConditionCount) {
		this.rankConditionCount = rankConditionCount;
	}
	public String getRankName() {
		return rankName;
	}
	public void setRankName(String rankName) {
		this.rankName = rankName;
	}
	
	
}
