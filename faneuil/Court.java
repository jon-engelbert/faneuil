package faneuil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author jonengelbert
 * @class Court
 * the Court class provides the synchronization elements and methods for the interactions of judges, immigrants, and spectators
 * who participate in court cases.  It uses condition variables with associated locks, detailed below, and a countdown latch.
 * ImmigrantsAllPresent: blocks the judge from confirming the immigrants / giving out certificates until the immigrants have all checked in yet, 
 * unblock when the immigrants have checked in.
 * NoJudge: blocks the immigrants and spectators from entering when the judge is present, unblock when there is no judge.
 * Confirmed:  Blocks the immigrants from getting certificates until the judge "confirms".
 * There is also a countdown latch.
 * ImmigrantsAndJudgeClearedSignal:  This enforces the rule that the judge and all immigrants must clear the room before a new set can enter.
 * This is instantiated when the immigrants have all checked in (or when the judge confirms), then 
 
 *I took another stab at the solution today, with the intent to maximize encapsulation, and eliminate side effects.  
 *I decided to add functions to the 'court' class that 
 *encapsulate the portions of each of individual's functionality that 
 *modify the court's condition variables and other concurrency-related variables.  
 *So, for example, when the judge enters the room, the portion of that function that determines 
 *if the judge enters the room is executed in a Judge class method, 
 *then the judge calls the court's method (through a callback) that modifies the ...
 *
 * changes to original problem description.
 * waiting area: when the judge arrives, he goes into the waiting area... 
 * all immigrants already in the waiting area must enter the court before the judge
 * the immigrant only enters the waiting area when the court is in session, or there is no judge in the waiting area.
 */
public class Court implements CourtIF{
//	private final Semaphore semImmigrantsCheckedIn = new Semaphore(0);
    private final Lock lockImmigrantsAllPresent = new ReentrantLock();
    private final Condition cvImmigrantsAllPresent  = lockImmigrantsAllPresent.newCondition(); 
    private final Lock lockImmigrantsAllCheckedIn = new ReentrantLock();
    private final Condition cvImmigrantsAllCheckedIn  = lockImmigrantsAllCheckedIn.newCondition(); 
    private final Lock lockJudgeWaiting = new ReentrantLock();
    private final Condition cvJudgeWaiting  = lockJudgeWaiting.newCondition(); 
    private final Lock lockJudgePresent = new ReentrantLock();
    private final Condition cvJudgePresent  = lockJudgePresent.newCondition(); 
    private final Lock lockConfirmed = new ReentrantLock();
    private final Condition cvConfirmed  = lockConfirmed.newCondition(); 
//    static private final Lock lockCourtCleared = new ReentrantLock();
//    private final Condition cvCourtCleared  = lockCourtCleared.newCondition(); 
	private CountDownLatch courtClearedLatch = null;
    private Judge judge = null;
//    private Map<Integer, Immigrant> immigrantsWaiting = new ConcurrentHashMap<Integer, Immigrant>();
//    private Map<Integer, Immigrant> immigrants = new ConcurrentHashMap<Integer, Immigrant>();
    private AtomicInteger nImmigrantsReadyToEnter = new AtomicInteger(0);
    private AtomicInteger nImmigrantsPresent = new AtomicInteger(0);
    private AtomicInteger nImmigrantsCheckedIn = new AtomicInteger(0);
    private Map<Integer, Spectator> spectators = new ConcurrentHashMap<Integer, Spectator>();
    private boolean isJudgeWaiting = false;
    private boolean isDoneConfirming = false;
    private boolean isJudgeGoneAfterConfirmation = true;
    private boolean isImmigrantsGoneAfterConfirmation = true;

	public void judgeWait(Judge judge) {
		isJudgeWaiting = true;
	}
	
	@Override
	public boolean isJudgeWaiting() {
		synchronized (System.out) {
			System.out.println("JJJJJJJJJJJ--- isJudgeWaiting: "
					+ String.valueOf(judge != null && judge.isWaiting()));
		}
		return (judge != null && judge.isWaiting());
	}
	
	@Override
	public boolean isJudgeInCourt() {
		return (judge != null && judge.isInCourt());
	}
	
	public void judgeEnter(Judge judge) {
		setImmigrantsAndJudgeClearedLatch(new CountDownLatch(getImmigrantsPresentCount().get()+1));
		setDoneConfirming(false);
		setJudge(judge);	
		lockJudgeWaiting.lock();
		cvJudgeWaiting.signalAll();
		lockJudgeWaiting.unlock();
	}
	public void judgeConfirm(Judge judge) {
		setDoneConfirming(true);
		setJudgeGoneAfterConfirmation(false);
		getLockConfirmed().lock();
		getCvConfirmed().signalAll();
		getLockConfirmed().unlock();
	}
	public void judgeLeave(Judge judge) {
		courtClearedLatch.countDown();
		setJudge(null);	
		lockJudgePresent.lock();
		cvJudgePresent.signalAll();
		lockJudgePresent.unlock();
	}
	
	public void immigrantWait() {
		nImmigrantsReadyToEnter.getAndIncrement();
	}
    public void immigrantEnter(Immigrant immigrant)  {
		setDoneConfirming(false);
//		immigrants.put(immigrant.getIndex(), immigrant);
		nImmigrantsPresent.getAndIncrement();
    }
    
    public void immigrantGetCertificate(Immigrant immigrant)  {
    // hold until  judge is done confirming.
    // proceed (end the barrier) when the judge is done confirming
    }

	public void immigrantLeave(Immigrant immigrant) {
		if (judge != null)
			System.out.print("illegal state for immigrant leave");

		nImmigrantsPresent.getAndDecrement();
		nImmigrantsCheckedIn.getAndDecrement();
		// immigrants.remove(immigrant.getIndex());
		if (courtClearedLatch != null) {
			synchronized (System.out) {
				System.out.println("Immigrant about to countdown: "
						+ String.valueOf(courtClearedLatch.getCount()));
			}
			courtClearedLatch.countDown();
		} else {
			synchronized (System.out) {
				System.out.println("IIIIIIIII::::::: Immigrant cannot countdown, immigrantsAndJudgeClearedSignal is null: ");
			}

		}
	}

	public void immigrantCheckIn(Immigrant immigrant)  {
		nImmigrantsCheckedIn.getAndIncrement();
		lockImmigrantsAllPresent.lock();
		try {
			cvImmigrantsAllPresent.signalAll();
		} finally {
			lockImmigrantsAllPresent.unlock();
		}
    }
    

    
    public boolean isImmigrantsAllCheckedIn() {
    	AtomicBoolean available = new AtomicBoolean(true);
		synchronized (System.out) {
			System.out.println("isImmigrantsAllCheckedIn, about to check: " + String.valueOf(nImmigrantsCheckedIn.get()));
		}
    	return available.compareAndSet(nImmigrantsCheckedIn.get() == nImmigrantsPresent.get(), true);
    }
    	
    public boolean addSpectatorBlocking(Spectator spec) throws InterruptedException {
    	boolean isAdded = false;
		lockJudgePresent.lock();
		long nanos = 10_000_000;
		try {
			while (judge != null) {
			    if (nanos <= 0L)
			    	break;
				cvJudgePresent.awaitNanos(nanos);
			}
//		if (immigrantsAndJudgeClearedSignal != null)
//			isCleared = immigrantsAndJudgeClearedSignal.await(1000, TimeUnit.MILLISECONDS);
			if (judge == null) {
				isAdded = true;
				spectators.put(spec.getIndex(), spec);
			}
		} finally {
			lockJudgePresent.unlock();
		}
		return isAdded;
    }
    

    
	public boolean isJudgePresent() {
		return (judge != null);
	}

	public boolean isJudgeDoneConfirming() {
		return (judge != null) && judge.IsDoneConfirming();
	}

	public Condition getCvImmigrantsAllPresent() {
		return cvImmigrantsAllPresent;
	}

	public Condition getCvJudgePresent() {
		return cvJudgePresent;
	}

	public Condition getCvConfirmed() {
		return cvConfirmed;
	}

	public Judge getJudge() {
		return judge;
	}

	// why does synchronized cause this function to never get called?
	//public synchronized void judgeLeave(final Judge judge) {
	/**
	 * judgeLeave causes the judge to leave the court.  
	 * The input parameter judge must be the court instance's judge.
	 * And the countdownlatch immigrantsAndJudgeClearedSignal must exist.
	 * Then immigrantsAndJudgeClearedSignal is counted down.
	 * Finally, the judge for the court is set to null (removed from the court), 
	 * and all Condition variables of the type NoJudge are signalled,
	 *  so that they wake up and stop blocking (because the judge is null).
	 * A lock of type NoJudge is used to block other threads from interrupting the steps of clearing the judge and notifying other threads.
	 * I'm not sure if this lock is necessary around both actions.  
	 * But the judge MUST be removed before signalling the other blocked threads.
	 * @param judge: the judge who is leaving the court
	 */
	
	public Lock getLockJudgePresent() {
		return lockJudgePresent;
	}


	public boolean isDoneConfirming() {
		return isDoneConfirming;
	}

	public void setDoneConfirming(boolean isDoneConfirming) {
		this.isDoneConfirming = isDoneConfirming;
	}

	public boolean isJudgeGoneAfterConfirmation() {
		return isJudgeGoneAfterConfirmation;
	}

	public void setJudgeGoneAfterConfirmation(boolean isJudgeGoneAfterConfirmation) {
		this.isJudgeGoneAfterConfirmation = isJudgeGoneAfterConfirmation;
	}

	public boolean isImmigrantsGoneAfterConfirmation() {
		return isImmigrantsGoneAfterConfirmation;
	}

	public void setImmigrantsGoneAfterConfirmation(
			boolean isImmigrantsGoneAfterConfirmation) {
		this.isImmigrantsGoneAfterConfirmation = isImmigrantsGoneAfterConfirmation;
	}


	public Lock getLockJudgeWaiting() {
		return lockJudgeWaiting;
	}

	public Condition getCvJudgeWaiting() {
		return cvJudgeWaiting;
	}

	public Lock getLockConfirmed() {
		return lockConfirmed;
	}

	public AtomicInteger getImmigrantsPresentCount() {
		return nImmigrantsPresent;
	}

	public void setnImmigrantsPresent(AtomicInteger nImmigrantsPresent) {
		this.nImmigrantsPresent = nImmigrantsPresent;
	}

	public AtomicInteger getImmigrantsCheckedInCount() {
		return nImmigrantsCheckedIn;
	}

	public AtomicInteger getImmigrantsReadyToEnterCount() {
		return nImmigrantsReadyToEnter;
	}

	public void setnImmigrantsCheckedIn(AtomicInteger nImmigrantsCheckedIn) {
		this.nImmigrantsCheckedIn = nImmigrantsCheckedIn;
	}

	public CountDownLatch getCourtClearedLatch() {
		return courtClearedLatch;
	}

//	public CountDownLatch getImmigrantsWaitingToEnterLatch() {
//		return immigrantsWaitingToEnterLatch;
//	}

	public void setImmigrantsAndJudgeClearedLatch(
			CountDownLatch immigrantsAndJudgeClearedLatch) {
		this.courtClearedLatch = immigrantsAndJudgeClearedLatch;
	}

	public void setJudge(Judge judge) {
		// TODO Auto-generated method stub
		this.judge = judge;
	}
	public AtomicInteger getnImmigrantsReadyToEnter() {
		return nImmigrantsReadyToEnter;
	}
	public void setnImmigrantsReadyToEnter(AtomicInteger nImmigrantsWaiting) {
		this.nImmigrantsReadyToEnter = nImmigrantsReadyToEnter;
	}

	@Override
	public Lock getLockImmigrantsAllPresent() {
		// TODO Auto-generated method stub
		return lockImmigrantsAllPresent;
	}

	@Override
	public Condition getCvImmigrantsPresent() {
		// TODO Auto-generated method stub
		return cvImmigrantsAllPresent;
	}

	@Override
	public Lock getLockImmigrantsCheckedIn() {
		// TODO Auto-generated method stub
		return lockImmigrantsAllPresent;
	}

	@Override
	public Condition getCvImmigrantsCheckedIn() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public boolean isInSession() {
		synchronized (System.out) {
			System.out.println("JJJJJJJJJJJ--- IsInSession: "
					+ String.valueOf((courtClearedLatch != null) && (courtClearedLatch.getCount() > 0)));
		}
		return ((courtClearedLatch != null) && (courtClearedLatch.getCount() > 0));
	}

}
