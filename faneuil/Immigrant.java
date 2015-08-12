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
 *  */
public class Immigrant implements Runnable {

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	private final int index;
	CourtIF court = null;
	private int delayEnter = 0, delayCheckIn = 0, delayGetCert = 0,
			delayLeave = 0;
	private boolean hasCertificate = false;
	private boolean isInCourt = false;

	Immigrant() {
		index = 0;
	}

	Immigrant(int index) {
		this.index = index;
	}

	Immigrant(int index, int delayEnter, int delayCheckIn, int delayGetCert,
			int delayLeave) {
		this.index = index;
		this.delayEnter = delayEnter;
		this.delayCheckIn = delayCheckIn;
		this.delayGetCert = delayGetCert;
		this.delayLeave = delayLeave;
	}

	/**
	 * @param court
	 * @param notCourtInSessionLatch
	 * @return
	 * @throws InterruptedException
	 * the immigrant only can enter if the court is available and there is no judge waiting, or the court is not available yet.
	 * 
	 */
	boolean isCanEnter(CourtIF court, final CountDownLatch notCourtInSessionLatch)
			throws InterruptedException {
		boolean isAdded = false;
		boolean isCleared = true;
		// while (judge != null)
		// cvJudgePresent.awaitNanos(10_000_000);
		while (court.isJudgeInCourt()) {
			court.getCvJudgePresent().await();
			if (notCourtInSessionLatch != null) {
				isCleared = notCourtInSessionLatch.await(2000,
						TimeUnit.MILLISECONDS);
			}
		}
		if (isCleared) {
			isAdded = true;
			synchronized (System.out) {
				System.out.println("Immigrant Enter (index): "
						+ String.valueOf(getIndex()));
			}
		}
		return isAdded;
	}
	
	boolean isCanGetReady(CourtIF court) throws InterruptedException {
		boolean passed = false;
		long nanos = 1000_000_000;
		court.getLockJudgeWaiting().lock();
		try {
			while (!court.isInSession() || !court.isJudgeWaiting()) 
				court.getCvJudgeWaiting().awaitNanos(nanos);
			passed = true;
		}
		finally {
			court.getLockJudgeWaiting().unlock();
		}
		return passed;
	}

	@Override
	public void run() {
		try {
			Thread.sleep(delayEnter);
			if (isCanGetReady(court)) {
				synchronized (System.out) {
					System.out.println("Immigrant is getting ready: "
							+ String.valueOf(getIndex()));
				}
				court.immigrantWait();
				synchronized (System.out) {
					System.out.println("Immigrant is about to isCanEnter: "
							+ String.valueOf(getIndex()));
				}
				if (isCanEnter(court, court.getCourtClearedLatch())) {
					isInCourt = true;
					Thread.sleep(delayCheckIn);
					court.immigrantCheckIn(this);
					// cv_immigrant_present.notify_one();
					sitDown();
					Thread.sleep(delayGetCert);
					if (isCanGetCertificate(court.getLockConfirmed(),
							court.isDoneConfirming(), court.getCvConfirmed())) {
						hasCertificate = true;
						// court.immigrantGetCertificate(this); nothing to do here.
					}
					Thread.sleep(delayLeave);
					if (isCanLeave(court, court.getLockJudgePresent(),
							court.getCvJudgePresent())) {
						isInCourt = false;
						court.immigrantLeave(this);
					}
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			synchronized (System.out) {
				System.out.println("Immigrant thread interruption exception:"
						+ String.valueOf(index));
			}
			return;
		}
	}

	public CourtIF getCourt() {
		return court;
	}

	public void setCourt(CourtIF court) {
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

	private boolean isCanGetCertificate(Lock lockConfirmed,
			boolean isDoneConfirming, Condition cvConfirmed)
			throws InterruptedException {
		boolean isProceed = false;
		long nanos = 500_000_000;
		lockConfirmed.lock();
		try {
			while (!isDoneConfirming) {
				if (nanos <= 0L)
					break;
				nanos = cvConfirmed.awaitNanos(nanos);
			}
			isProceed = true;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			lockConfirmed.unlock();
		}
		;
		synchronized (System.out) {
			System.out.println("can getCertificate, immigrant: "
					+ String.valueOf(index));
		}
		return isProceed;
	};

	
	/**
	 * @param court -- send the court interface for the getJudge method, so that when the judge leaves, then the immigrant can leave
	 * @param lockNoJudge 
	 * @param cvNoJudge
	 * @return
	 * @throws InterruptedException
	 */
	private boolean isCanLeave(final CourtIF court, Lock lockNoJudge,
			Condition cvNoJudge) throws InterruptedException {
		long nanos = 10_000_000;
		lockNoJudge.lock();
		try {
			while (court.getJudge() != null) {
				if (nanos <= 0L)
					break;
				nanos = cvNoJudge.awaitNanos(nanos);
			}
		} finally {
			lockNoJudge.unlock();
		}
		synchronized (System.out) {
			System.out.println("Immigrant can Leave: " + String.valueOf(index));
		}
		return true;
	}

	public int getIndex() {
		return index;
	}

	// void WaitRandom() {
	// std::mt19937_64 eng{std::random_device{}()}; // seed
	// std::uniform_int_distribution<> dist{1, 100};
	// std::this_thread::sleep_for(std::chrono::milliseconds{dist(eng)});
	// }

}
