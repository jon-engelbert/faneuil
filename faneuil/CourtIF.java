package faneuil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

interface CourtIF {
	void judgeEnter(Judge judge);
	void judgeConfirm(Judge judge);
	void judgeLeave(Judge judge);
	void immigrantEnter(Immigrant immigrant);
	void immigrantCheckIn(Immigrant immigrant);
	void immigrantGetCertificate(Immigrant immigrant);
	void immigrantLeave(Immigrant immigrant);
	
	boolean isDoneConfirming();
	boolean isJudgeGoneAfterConfirmation();
	boolean isImmigrantsGoneAfterConfirmation();
	CountDownLatch getImmigrantsAndJudgeClearedLatch();
	Lock getLockConfirmed();
	Condition getCvConfirmed();
	Lock getLockNoJudge();
	Condition getCvNoJudge();
    boolean isImmigrantsAllCheckedIn();
	Judge getJudge();
}
