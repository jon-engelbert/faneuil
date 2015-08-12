package faneuil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class Judge implements Runnable {
	private boolean isWaiting = false;
	private boolean isInCourt = false;
	private boolean isDoneConfirming = false;
	// private boolean isPresent = false;
	private int index;
	private CourtIF court;
	private int delayEnter = 0, delayConfirm = 0, delayLeave = 0;

	Judge() {
	};

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

	/**
	 * hold while judge is present, and while immigrants are queued to get in
	 * 
	 * @param notCourtInSessionLatch
	 * @return
	 * @throws InterruptedException
	 */
	boolean isCanEnter(CourtIF court) throws InterruptedException {
		// court.setJudge();
		// hold while judge is present.
		// proceed (end the barrier) before the judge arrives, or when the judge
		// leaves
		// But What if two judges waiting? can both enter at the same time?
		// or will m_present/cv_present be locked as soon as one enters, until
		// the end of the block, then it gets unlocked again
		// block in case there is another judge in the courtroom.
		boolean isCleared = false;
		long nanos = 1000_000_000;
		if (court.getCourtClearedLatch() != null) {
			court.getCourtClearedLatch().await(5000, TimeUnit.MILLISECONDS);
		}
		court.getLockJudgePresent().lock();
		try {
			while (court.isJudgeInCourt())
				court.getCvJudgePresent().wait();
		} finally {
			court.getLockJudgePresent().unlock();
		}
		court.getLockImmigrantsAllPresent().lock();
		try {
			synchronized (System.out) {
				System.out.println("about to check immigrant counts " + String.valueOf(court.getImmigrantsReadyToEnterCount()) + ", " + String.valueOf(court.getImmigrantsPresentCount()));
			}
			while ((court.getImmigrantsReadyToEnterCount().get() != court.getImmigrantsPresentCount().get()) )
				court.getCvImmigrantsPresent().await(5000,
						TimeUnit.MILLISECONDS);
// SIDE EFFECT -- but necessary, I think, to prevent two judges from entering at once.
			court.judgeEnter(this);
			isCleared = true;
		} finally {
			court.getLockImmigrantsAllPresent().unlock();
		}
		if (isCleared) {
			synchronized (System.out) {
				System.out.println("JJJJJJJJJJJ--- Judge Can Enter: "
						+ String.valueOf(index));
			}
		} else {
			synchronized (System.out) {
				System.out
						.println("JJJJJJJJJJJ--- TIMED OUT: Judge Cannot Enter: "
								+ String.valueOf(index));
			}
		}
		return isCleared;
	};

	public void SitDown() {
		synchronized (System.out) {
			System.out.println("JJJJJJJJJJJ--- Judge sits down:"
					+ String.valueOf(index));
		}
	}

	/**
	 * @param court
	 *            -- need to access its getJudge and isImmigrantsAllCheckedIn
	 *            methods, which will change their return values due to external
	 *            events.
	 * @param lockImmigrantsAllPresent
	 * @param cvImmigrantsAllPresent
	 * @return
	 * @throws InterruptedException
	 */
	public boolean isCanConfirm(CourtIF court,
			final Lock lockImmigrantsAllCheckedIn,
			final Condition cvImmigrantsAllCheckedIn)
			throws InterruptedException {
		// std::unique_lock<std::mutex> lk(m_preconfirmMutex);
		// hold while immigrants haven't checked in
		boolean isCleared = false;
		lockImmigrantsAllCheckedIn.lock();
		try {
			while ((court.getJudge() != null)
					&& !court.isImmigrantsAllCheckedIn())
				cvImmigrantsAllCheckedIn.await();
			isCleared = true;
		} finally {
			lockImmigrantsAllCheckedIn.unlock();
		}
		synchronized (System.out) {
			System.out.println("JJJJJJJJJJJ--- Judge Can Confirm: "
					+ String.valueOf(index));
		}
		return isCleared;
	}

	// public boolean isCanLeave(Judge courtJudge, CountDownLatch
	// immigrantsAndJudgeClearedLatch) {
	// boolean success = false;
	// if (courtJudge == this) {
	// if (immigrantsAndJudgeClearedLatch != null) {
	// success = true;
	// }
	// synchronized (System.out) {
	// System.out.println("JJJJJJJJJJJ--- judge can leave: " +
	// String.valueOf(getIndex()));
	// }
	// // cvJudgePresent.wait(lk, [this] {return !isJudgePresent;});
	// // then set isJudgePresent, and then possibly notify?
	// } else {
	// synchronized (System.out) {
	// System.out.println("JJJJJJJJJJJ--- wrong judge trying to leave ???");
	// }
	// }
	// // m_isDoneConfirming = false;
	// return success;
	// }

	// public boolean IsPresent() {return isPresent;}

	// public void SetPresent(boolean isPresent) {this.isPresent = isPresent;}

	public boolean IsDoneConfirming() {
		return isDoneConfirming;
	}

	@Override
	public void run() {
		try {
			Thread.sleep(delayEnter);
			isWaiting = true;
			if (isCanEnter(court)) {
//				court.judgeEnter(this);
				isInCourt = true;
				isWaiting = false;
			}
			Thread.sleep(delayConfirm);
			if (isCanConfirm(court, court.getLockConfirmed(),
					court.getCvConfirmed())) {
				court.judgeConfirm(this);
				isDoneConfirming = true;
			}
			Thread.sleep(delayLeave);
			court.judgeLeave(this);
			isInCourt = false;
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			synchronized (System.out) {
				System.out.println("Judge thread interruption exception:"
						+ String.valueOf(index));
			}
		} finally {
			// what shoud happen?
			// should the judge leave the court, i.e. court.judgeLeave?
		}
		// reset the cv for the judge being present, so the next iteration of
		// the whole process can start again.
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

	public boolean isWaiting() {
		return isWaiting;
	}

	public boolean isInCourt() {
		return isInCourt;
	}

	public boolean isDoneConfirming() {
		return isDoneConfirming;
	}
}
