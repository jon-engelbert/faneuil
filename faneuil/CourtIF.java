package faneuil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

interface CourtIF {
	void judgeWait(Judge judge);
	void judgeEnter(Judge judge);
	void judgeConfirm(Judge judge);
	void judgeLeave(Judge judge);
	void immigrantEnter(Immigrant immigrant);
	void immigrantCheckIn(Immigrant immigrant);
	void immigrantGetCertificate(Immigrant immigrant);
	void immigrantLeave(Immigrant immigrant);
	void immigrantWait();
	
	boolean isJudgeWaiting();
	boolean isJudgeInCourt();
	boolean isDoneConfirming();
	boolean isJudgeGoneAfterConfirmation();
	boolean isImmigrantsGoneAfterConfirmation();
	CountDownLatch getCourtClearedLatch();
//	CountDownLatch getImmigrantsWaitingToEnterLatch();
	Lock getLockImmigrantsAllPresent();
	Condition getCvImmigrantsPresent();
	Lock getLockImmigrantsCheckedIn();
	Condition getCvImmigrantsCheckedIn();
	Lock getLockConfirmed();
	Condition getCvConfirmed();
	Lock getLockJudgePresent();
	Condition getCvJudgePresent();
	Lock getLockJudgeWaiting();
	Condition getCvJudgeWaiting();
    boolean isImmigrantsAllCheckedIn();
	Judge getJudge();
	boolean isInSession();
	AtomicInteger getImmigrantsPresentCount();
	AtomicInteger getImmigrantsCheckedInCount();
	AtomicInteger getImmigrantsReadyToEnterCount();
}
