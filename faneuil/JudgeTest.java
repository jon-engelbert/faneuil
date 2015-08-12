package faneuil;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

//Judge tests
//ENTER if no judge is in the court, AND if the court is cleared
//Don't enter if a judge is in the court
//Don't enter if the court is not cleared

//CONFIRM if isImmigrantsAllCheckedIn
// don't confirm if !isImmigrantsAllCheckedIn

// LEAVE results in decrement to immigrantsAndJudgeClearedSignal, which is a latch
public class JudgeTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testEnter() {
		Judge judge = new Judge();
		Court court = new Court();
		judge.setCourt(court);
		try {
			court.judgeEnter(judge);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assert(court.getJudge() == judge);
	}

	@Test
	public void testEnter2() {
		Judge judge1 = new Judge();
		Judge judge2 = new Judge();
		Court court = new Court();
		judge1.setCourt(court);
		judge2.setCourt(court);
		try {
			court.judgeEnter(judge1);
			court.judgeEnter(judge2);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assert(court.getJudge() == judge1);
	}

}
