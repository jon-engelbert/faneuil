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
 *then the judge calls the court's method (through a callback) that modifies the 
 */
public class Court implements CourtIF{
//	private final Semaphore semImmigrantsCheckedIn = new Semaphore(0);
    private final Lock lockImmigrantsAllPresent = new ReentrantLock();
    private final Condition cvImmigrantsAllPresent  = lockImmigrantsAllPresent.newCondition(); 
    private final Lock lockNoJudge = new ReentrantLock();
    private final Condition cvNoJudge  = lockNoJudge.newCondition(); 
    private final Lock lockConfirmed = new ReentrantLock();
    private final Condition cvConfirmed  = lockConfirmed.newCondition(); 
//    static private final Lock lockCourtCleared = new ReentrantLock();
//    private final Condition cvCourtCleared  = lockCourtCleared.newCondition(); 
	private CountDownLatch immigrantsAndJudgeClearedLatch = null;
    private Judge judge = null;
    private Map<Integer, Immigrant> immigrants = new ConcurrentHashMap<Integer, Immigrant>();
    private AtomicInteger nImmigrantsPresent = new AtomicInteger(0);
    private AtomicInteger nImmigrantsCheckedIn = new AtomicInteger(0);
    private Map<Integer, Spectator> spectators = new ConcurrentHashMap<Integer, Spectator>();
    private boolean isDoneConfirming = false;
    private boolean isJudgeGoneAfterConfirmation = true;
    private boolean isImmigrantsGoneAfterConfirmation = true;

	public void judgeEnter(Judge judge) {
		setImmigrantsAndJudgeClearedLatch(new CountDownLatch(getnImmigrantsPresent().get()+1));
		setDoneConfirming(false);
		setJudge(judge);				
	}
	public void judgeConfirm(Judge judge) {
		setDoneConfirming(true);
		setJudgeGoneAfterConfirmation(false);
		getLockConfirmed().lock();
		getCvConfirmed().signalAll();
		getLockConfirmed().unlock();
	}
	public void judgeLeave(Judge judge) {
		getImmigrantsAndJudgeClearedLatch().countDown();
		setJudge(null);	
		lockNoJudge.lock();
		cvNoJudge.signalAll();
		lockNoJudge.unlock();
	}
	
    public void immigrantEnter(Immigrant immigrant)  {
		setDoneConfirming(false);
		immigrants.put(immigrant.getIndex(), immigrant);
		nImmigrantsPresent.getAndIncrement();
    }
    
    public void immigrantGetCertificate(Immigrant immigrant)  {
    // hold until  judge is done confirming.
    // proceed (end the barrier) when the judge is done confirming
    }

    public void immigrantLeave(Immigrant immigrant)  {
			if (judge == null) {
				nImmigrantsPresent.getAndDecrement();
				nImmigrantsCheckedIn.getAndDecrement();
    			immigrants.remove(immigrant.getIndex());
				if (immigrantsAndJudgeClearedLatch != null) {
	    			synchronized (System.out) {
	    				System.out.println("Immigrant about to countdown: " + String.valueOf(immigrantsAndJudgeClearedLatch.getCount()));
	    			}
	    			immigrantsAndJudgeClearedLatch.countDown();
				} else {
	    			synchronized (System.out) {
	    				System.out.println("IIIIIIIII::::::: Immigrant cannot countdown, immigrantsAndJudgeClearedSignal is null: ");
	    			}

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
		lockNoJudge.lock();
		long nanos = 10_000_000;
		try {
			while (judge != null) {
			    if (nanos <= 0L)
			    	break;
				cvNoJudge.awaitNanos(nanos);
			}
//		if (immigrantsAndJudgeClearedSignal != null)
//			isCleared = immigrantsAndJudgeClearedSignal.await(1000, TimeUnit.MILLISECONDS);
			if (judge == null) {
				isAdded = true;
				spectators.put(spec.getIndex(), spec);
			}
		} finally {
			lockNoJudge.unlock();
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
		return cvNoJudge;
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
	
	public Lock getLockImmigrantsAllPresent() {
		return lockImmigrantsAllPresent;
	}

	public Lock getLockJudgePresent() {
		return lockNoJudge;
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


	public Lock getLockNoJudge() {
		return lockNoJudge;
	}

	public Condition getCvNoJudge() {
		return cvNoJudge;
	}

	public Lock getLockConfirmed() {
		return lockConfirmed;
	}

	public AtomicInteger getnImmigrantsPresent() {
		return nImmigrantsPresent;
	}

	public void setnImmigrantsPresent(AtomicInteger nImmigrantsPresent) {
		this.nImmigrantsPresent = nImmigrantsPresent;
	}

	public AtomicInteger getnImmigrantsCheckedIn() {
		return nImmigrantsCheckedIn;
	}

	public void setnImmigrantsCheckedIn(AtomicInteger nImmigrantsCheckedIn) {
		this.nImmigrantsCheckedIn = nImmigrantsCheckedIn;
	}

	public CountDownLatch getImmigrantsAndJudgeClearedLatch() {
		return immigrantsAndJudgeClearedLatch;
	}

	public void setImmigrantsAndJudgeClearedLatch(
			CountDownLatch immigrantsAndJudgeClearedLatch) {
		this.immigrantsAndJudgeClearedLatch = immigrantsAndJudgeClearedLatch;
	}

	public void setJudge(Judge judge) {
		// TODO Auto-generated method stub
		this.judge = judge;
	}

}
