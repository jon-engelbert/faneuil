/**
 * 
 */
package faneuil;

/**
 * @author jonengelbert
 *
 */
public class Spectator implements Runnable {

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	private final int index;
	Court court = null;
    private int delayEnter = 0, delaySpectate = 0, delayLeave = 0;
	
	Spectator() {
		index = 0;
	}

	Spectator(int index)  {
		this.index = index;
	}
	Spectator(int index, int delayEnter, int delaySpectate, int delayLeave) {
		this.index = index;
		this.delayEnter = delayEnter;
		this.delaySpectate = delaySpectate;
		this.delayLeave = delayLeave;
	}
	
	boolean Enter() throws InterruptedException {
	    // std::unique_lock<std::mutex> lk(m);
	    // hold while judge is present.
	    // proceed (end the barrier) before the judge arrives, ** and when the previous set of immigrants has left? **
	    // Judge::cv_judgePresent.wait(lk, [this] {return !m_judge || !m_judge->IsPresent();});
	    boolean isEntered = false;
	    if (court != null) {
//			synchronized (System.out) {
//				System.out.println("Spectator Enter (soon): " + String.valueOf(index));
//			}
	        isEntered = court.addSpectatorBlocking(this);
	        if (isEntered) {
//				synchronized (System.out) {
//					System.out.println("Spectator Entered: " + String.valueOf(index));
//				}
	        }
	    }
        return isEntered;
	}

	@Override
	public void run() {
		try {
			Thread.sleep(delayEnter);
		    boolean isEntered = Enter();
		    if (isEntered) {
				Thread.sleep(delaySpectate);
		        Spectate();
				Thread.sleep(delayLeave);
		        Leave();
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

//	private void SitDown() {
//		synchronized (System.out) {
//			System.out.println("Spectator Sit Down: " + String.valueOf(index));
//		}
//	};

	private void Spectate() {
//		synchronized (System.out) {
//			System.out.println("Spectator Spectate: " + String.valueOf(index));
//		}
	}


	private void Leave() {
//		synchronized (System.out) {
//			System.out.println("Spectator Leave: " + String.valueOf(index));
//		}
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
