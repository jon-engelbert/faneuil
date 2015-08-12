package faneuil;

import java.util.Random;


	/**
	 *
	*/

class FaneuilApp {
    public static void main(String args[]) {
    	int NUM_IMMIGRANTS_TO_CREATE = 10;
    	int NUM_SPECTATORS_TO_CREATE = 10;
    	int NUM_JUDGES_TO_CREATE = 2;
        //creating CyclicBarrier with 3 parties i.e. 3 Threads needs to call await()

        //starting each of thread
        Thread[] immigrantThreads = new Thread[NUM_IMMIGRANTS_TO_CREATE];
        Thread[] spectatorThreads = new Thread[NUM_SPECTATORS_TO_CREATE];
        Thread[] judgeThreads = new Thread[NUM_JUDGES_TO_CREATE];

        Court court = new Court();
//    	courtThread = new Thread(court);
//    	courtThread.start();
        
        Random rg = new Random();
    	
		for (int i = 0; i < NUM_JUDGES_TO_CREATE; i++) {
			Judge judge = new Judge(i, rg.nextInt(100), rg.nextInt(100),rg.nextInt(100));
			judgeThreads[i] = new Thread(judge);
			judge.setCourt(court);
		}
//    		judgeEnter(judge);
		for (int i = 0; i < NUM_IMMIGRANTS_TO_CREATE; i++) {
			Immigrant immigrant = new Immigrant(i, rg.nextInt(100), rg.nextInt(100), rg.nextInt(100), rg.nextInt(100));
			immigrantThreads[i] = new Thread(immigrant);
			immigrant.setCourt(court);
		}
		for (int i = 0; i < NUM_SPECTATORS_TO_CREATE; i++) {
			Spectator spec = new Spectator(i, rg.nextInt(100), rg.nextInt(100), rg.nextInt(100));
			spectatorThreads[i] = new Thread(spec);
			spec.setCourt(court);
		}
//    		judgeLeave(judge);
		for (int i = 0; i < NUM_JUDGES_TO_CREATE; i++) 
			judgeThreads[i].start();
		for (int i = 0; i < NUM_IMMIGRANTS_TO_CREATE; i++) 
			immigrantThreads[i].start();
		for (int i = 0; i < NUM_SPECTATORS_TO_CREATE; i++) 
			spectatorThreads[i].start();
    }
}



