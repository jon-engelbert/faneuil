/**
 * 
 */
package faneuil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @author jonengelbert
 *
 */
public class Immigrant implements Runnable {

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	private final int index;
	Court court = null;
    private int delayEnter = 0, delayCheckIn = 0, delayGetCert = 0, delayLeave = 0;
	
	Immigrant() {
		index = 0;
	}

	Immigrant(int index)  {
		this.index = index;
	}
	
	Immigrant(int index, int delayEnter, int delayCheckIn, int delayGetCert, int delayLeave)  {
		this.index = index;
		this.delayEnter = delayEnter;
		this.delayCheckIn = delayCheckIn;
		this.delayGetCert = delayGetCert;
		this.delayLeave = delayLeave;
	}
	
	boolean enter(final CountDownLatch notCourtInSessionLatch) throws InterruptedException {
    	boolean isAdded = false;
    	boolean isCleared = true;
//			while (judge != null)
//				cvJudgePresent.awaitNanos(10_000_000);
			if (notCourtInSessionLatch != null) {
				isCleared = notCourtInSessionLatch.await(2000, TimeUnit.MILLISECONDS);
			}
			if (isCleared) {
				isAdded = true;
	    		synchronized (System.out) {
	    			System.out.println("Immigrant Enter (index): " + String.valueOf(getIndex()) );
	    		}
			}
		return isAdded;
	}

	@Override
	public void run() {
		try {
			Thread.sleep(delayEnter);
		    boolean is_entered = enter(court.getImmigrantsAndJudgeClearedLatch());
		    if (is_entered) {
				Thread.sleep(delayCheckIn);
				checkIn();
				court.immigrantCheckIn(this);
		        // cv_immigrant_present.notify_one();
		        sitDown();
				Thread.sleep(delayGetCert);
		        getCertificate(court.getLockConfirmed(), court.isDoneConfirming(),
		    			court.getCvConfirmed());
//		        court.immigrantGetCertificate(this); nothing to do here.
				Thread.sleep(delayLeave);
		        if (leave(court, court.getLockNoJudge(), court.getCvNoJudge()))
		        		court.immigrantLeave(this);
		    }
		} catch (InterruptedException e) {
			e.printStackTrace();
			synchronized (System.out) {
				System.out.println( "Immigrant thread interruption exception:" + String.valueOf(index));
			}
			return;
		}
	}

	public Court getCourt() {
		return court;
	}

	public void setCourt(Court court) {
		this.court = court;
	}

	private void sitDown() {
		synchronized (System.out) {
			System.out.println("Sit down, immigrant: " + String.valueOf(index));
		}
	};

	private void checkIn() throws InterruptedException {
		synchronized (System.out) {
			System.out.println("Check in, immigrant: " + String.valueOf(index));
		}
	};

	private void getCertificate(Lock lockConfirmed, boolean isDoneConfirming,
			Condition cvConfirmed) throws InterruptedException {
		long nanos = 500_000_000;
		lockConfirmed.lock();
		try {
			while (!isDoneConfirming) {
				if (nanos <= 0L)
					break;
				nanos = cvConfirmed.awaitNanos(nanos);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			lockConfirmed.unlock();
		}
		;
		synchronized (System.out) {
			System.out.println("getCertificate, immigrant: "
					+ String.valueOf(index));
		}
	};

	private boolean leave(final CourtIF court, Lock lockNoJudge, Condition cvNoJudge) throws InterruptedException {
		long nanos = 10_000_000;
		lockNoJudge.lock();
		try {
			while (court.getJudge() != null ) {
			    if (nanos <= 0L)
			    	break;
				nanos = court.getCvNoJudge().awaitNanos(nanos);
			}
		} finally {
			lockNoJudge.unlock();
		}
		synchronized (System.out) {
			System.out.println("Immigrant leaving: " + String.valueOf(index));
		}
		return true;
	}

	public int getIndex() {
		return index;
	}


//	void WaitRandom() {
//	    std::mt19937_64 eng{std::random_device{}()};  //  seed 
//	    std::uniform_int_distribution<> dist{1, 100};
//	    std::this_thread::sleep_for(std::chrono::milliseconds{dist(eng)});
//	}

}
