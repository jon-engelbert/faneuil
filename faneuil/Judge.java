package faneuil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class Judge implements Runnable {
	private boolean isDoneConfirming = false;
//	private boolean isPresent = false;
	private int index;
    private CourtIF court;
    private int delayEnter = 0, delayConfirm = 0, delayLeave = 0;
	    
	Judge() {};
	Judge(int index) {
		this.index = index;
		synchronized (System.out) {
			System.out.println("JJJJJJJJJJJ--- Judge initialized");
		}
	}
	Judge(int index, int delayEnter, int delayConfirm, int delayLeave) {
		this.index = index;
		this.delayEnter = delayEnter;
		this.delayConfirm = delayConfirm;
		this.delayLeave = delayLeave;

		synchronized (System.out) {
			System.out.println("JJJJJJJJJJJ--- Judge initialized");
		}
	}
	
    boolean enter(final CountDownLatch notCourtInSessionLatch) throws InterruptedException {
//    	court.setJudge();
        // hold while judge is present.
        // proceed (end the barrier) before the judge arrives, or when the judge leaves
        // But What if two judges waiting?  can both enter at the same time?  
        // or will m_present/cv_present be locked as soon as one enters, until the end of the block, then it gets unlocked again

		// block in case there is another judge in the courtroom.
    	boolean isEntered = false;
    	boolean isCleared = true;
		long nanos = 1000_000_000;
		if (notCourtInSessionLatch != null)
			isCleared = notCourtInSessionLatch.await(5000, TimeUnit.MILLISECONDS);
		if (isCleared) {
			isEntered = true;
			synchronized (System.out) {
				System.out.println("JJJJJJJJJJJ--- Judge Entered: " + String.valueOf(index));
			}
		} else {
			synchronized (System.out) {
				System.out.println("JJJJJJJJJJJ--- TIMED OUT: Judge Entered: " + String.valueOf(index));
			}
		}
		return isEntered;
    };

    public void SitDown() {
		synchronized (System.out) {
			System.out.println( "JJJJJJJJJJJ--- Judge sits down:" + String.valueOf(index));
		}
    }

    public boolean confirm(CourtIF court, Judge courtJudge, final Lock lockImmigrantsAllPresent, final Condition cvImmigrantsAllPresent) throws InterruptedException {
//        std::unique_lock<std::mutex> lk(m_preconfirmMutex);
        // hold while judge is present.
        // proceed (end the barrier) before the judge arrives, or when the judge leaves
        // But What if two judges waiting?  can both enter at the same time?  
        // or will m_present/cv_present be locked as soon as one enters, until the end of the block, then it gets unlocked again
		boolean isConfirmed = false;
		lockImmigrantsAllPresent.lock();
		try {
			while ((courtJudge != null) && !court.isImmigrantsAllCheckedIn())
				cvImmigrantsAllPresent.await();
			isConfirmed = true;
		} finally {
			lockImmigrantsAllPresent.unlock();
		}
		synchronized (System.out) {
			System.out.println("JJJJJJJJJJJ--- Judge Confirmed: " + String.valueOf(index));
		}
		return isConfirmed;
    }


    public boolean leave(Judge courtJudge, CountDownLatch immigrantsAndJudgeClearedLatch) {
		boolean success = false;
		synchronized (System.out) {
			System.out.println("JJJJJJJJJJJ--- in judgeLeave ???");
		}
		if (courtJudge == this) {
			if (immigrantsAndJudgeClearedLatch != null) {
				success = true;
    			synchronized (System.out) {
    				System.out.println("JJJJJJJJJJJ--- judge about to countdown: " + String.valueOf(immigrantsAndJudgeClearedLatch.getCount()));
    			}
			}
			synchronized (System.out) {
				System.out.println("JJJJJJJJJJJ--- judge about to leave: " + String.valueOf(getIndex()));
			}
			//        cvJudgePresent.wait(lk, [this] {return !isJudgePresent;});
			// then set isJudgePresent, and then possibly notify?
		} else {
			synchronized (System.out) {
				System.out.println("JJJJJJJJJJJ--- wrong judge trying to leave ???");
			}
		}
        // m_isDoneConfirming = false;
		synchronized (System.out) {
			System.out.println( "JJJJJJJJJJJ--- Judge Left:" + String.valueOf(index));
		}
		return success;
    }
    
//    public boolean IsPresent()  {return isPresent;}
    
//	public void SetPresent(boolean isPresent) {this.isPresent = isPresent;}
	
    public boolean IsDoneConfirming() {return isDoneConfirming;}
   
	@Override
	public void run() {
	    try {
			Thread.sleep(delayEnter);
			if (enter(court.getImmigrantsAndJudgeClearedLatch())) {
				court.judgeEnter(this);
			}
			Thread.sleep(delayConfirm);
		    if (confirm(court, court.getJudge(), court.getLockConfirmed(), court.getCvConfirmed())) {
		    	court.judgeConfirm(this);
		    }
			Thread.sleep(delayLeave);
		    if (leave(court.getJudge(),  court.getImmigrantsAndJudgeClearedLatch())) {
		    	court.judgeLeave(this);
		    }
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			synchronized (System.out) {
				System.out.println( "Judge thread interruption exception:" + String.valueOf(index));
			}
		}
	    // reset the cv for the judge being present, so the next iteration of the whole process can start again.
	}
	

	public CourtIF getCourt() {
		return court;
	}
	public void setCourt(CourtIF court) {
		this.court = court;
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
}
