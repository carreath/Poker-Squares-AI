import java.util.Collections;
import java.util.*;

/**
 * Reath_ProbabilityMCPlayer
 * Author: Caleb Reath
 * No MonteCarlo Average 45.133
 *
 * MC On all plays with utility 1+
 * MC depth 6 47.25
 * MC depth 8 47.436
 * MC depth 10 50
 * MC depth 12 52.5  === BEST ===
 * MC depth 14 48.5
 *
 * Use this player on depthLimit 12
 */
public class Reath_GreedyProbabilityMCPlayer implements PokerSquaresPlayer {
	private final int SIZE = 5; // number of rows/columns in square grid
	private final int NUM_POS = SIZE * SIZE; // number of positions in square grid
	private final int NUM_CARDS = Card.NUM_CARDS; // number of cards in deck
	private Random random = new Random(); // pseudorandom number generator for Monte Carlo simulation 
	private int[] plays = new int[NUM_POS]; // positions of plays so far (index 0 through numPlays - 1) recorded as integers using row-major indices.
	// row-major indices: play (r, c) is recorded as a single integer r * SIZE + c (See http://en.wikipedia.org/wiki/Row-major_order)
	// From plays index [numPlays] onward, we maintain a list of yet unplayed positions.
	private int numPlays = 0; // number of Cards played into the grid so far
	private PokerSquaresPointSystem system; // point system
	private int depthLimit = 2; // default depth limit for Greedy Monte Carlo (MC) play
	private Card[][] grid = new Card[SIZE][SIZE]; // grid with Card objects or null (for empty positions)
	private Card[] simDeck = Card.getAllCards(); // a list of all Cards. As we learn the index of cards in the play deck,
	                                             // we swap each dealt card to its correct index.  Thus, from index numPlays 
												 // onward, we maintain a list of undealt cards for MC simulation.
	private int[][] legalPlayLists = new int[NUM_POS][NUM_POS]; // stores legal play lists indexed by numPlays (depth)

	// Trained Rewards by iteration. iteration 0 is the british point system
	//4: AVG 45: {1.9403347077370845, 7.46640119462419, 50.47583994952415, 604.2120551924473, 64.03823178016725, 174.29193899782138, 1505.8823529411766, 257.78732545649837, 34285.71428571429};
	//3: AVG 45: {1.5037593984962405, 5.226480836236933, 55.52342394447657, 256.7901234567901, 49.629629629629626, 78.43137254901961, 640.0, 315.7894736842105, 6000.0};
	//2: AVG 43: {0.7518796992481203, 2.090592334494773, 36.09022556390977, 173.33333333333334, 33.5, 23.529411764705884, 160.0, 150.0, 600.0};
	//1: AVG 39: {0.7142857142857143, 2.142857142857143, 17.142857142857142, 104, 26.8, 10, 16, 30, 60};
	//0: AVG 30: {1, 3, 6, 12, 5, 10, 16, 30, 30};
	private static final double[] REWARDS = new double[] {1.9403347077370845, 7.46640119462419, 50.47583994952415, 604.2120551924473, 64.03823178016725, 174.29193899782138, 1505.8823529411766, 257.78732545649837, 34285.71428571429};

	private double[] rewards;

	private Reath_ProbabilityUtilities probUtil;
	private Reath_ModifiedGreedyMCPlayer greedyMCPlayer;

	private boolean mcEnabled = true;

	private int numPlay = 0;
	private HashSet<Integer> allPlays;

	/* (non-Javadoc)
	 * @see PokerSquaresPlayer#setPointSystem(PokerSquaresPointSystem, long)
	 */
	@Override
	public void setPointSystem(PokerSquaresPointSystem system, long millis) {
		this.system = system;
	}

	/**
	 * Create a Greedy Monte Carlo player that simulates greedy play to a given depth limit.
	 * @param depthLimit depth limit for random greedy simulated play
	 */
	public Reath_GreedyProbabilityMCPlayer(int depthLimit) {
		super();
		this.depthLimit = depthLimit;
		this.rewards = REWARDS;
	}

	public Reath_GreedyProbabilityMCPlayer(double[] rewards) {
		super();
		if (rewards != null) 
			this.rewards = rewards;
		else 
			this.rewards = REWARDS;
		this.mcEnabled = false;
	}
	
	/* (non-Javadoc)
	 * @see PokerSquaresPlayer#init()
	 */
	@Override
	public void init() {
		probUtil = new Reath_ProbabilityUtilities(rewards);
		greedyMCPlayer = new Reath_ModifiedGreedyMCPlayer(this.depthLimit, this.system);
		greedyMCPlayer.init();
		numPlay = 0;

		allPlays = new HashSet<>();
		for (int i=1; i<25; i++) {
			allPlays.add(i);
		}
	}

	/* (non-Javadoc)
	 * @see PokerSquaresPlayer#getPlay(Card, long)
	 */
	@Override
	public int[] getPlay(Card card, long millisRemaining) {
		int[] play;
		if (numPlay == 0) {
			numPlay++;

			play = new int[]{0, 0};
			probUtil.updateHands(card, play);
			greedyMCPlayer.makePlay(card, play[0], play[1]);
			return play;
		}
		if (numPlay == 24) {
			for (int i : allPlays) {
				return new int[]{i/5, i%5};
			}
		}

		long now = System.currentTimeMillis();

		// Get an updated utilityGrid with the new card included
		double[][] utilityGrid = probUtil.refreshProbabilities(card);

		// Get all high value critical plays to be simulated with MC
		int[] criticalPlays = getCriticalPlays(utilityGrid);

		// Get the maxGreedy play purely based on probabilities
		int[] maxPlay = getMaxGreedyPlay(utilityGrid);

		play = maxPlay;

		//System.out.println(Arrays.toString(criticalPlays));
		// If an error occurred select first available play
		if (criticalPlays.length == 0) { 
			//System.out.println("PLAY first");
			for (int i=0; i<25; i++) {
				if (greedyMCPlayer.getGrid()[i/5][i%5] == null) {
					play = new int[]{i/5,i%5};
				}
			}
			greedyMCPlayer.makePlay(card, play[0], play[1]);
		} else if (mcEnabled && criticalPlays.length < 18 && criticalPlays.length > 1) { // Iterate MC on a small set of high value plays
			//System.out.println("PLAY MC");
			millisRemaining -= (System.currentTimeMillis() - now);
			play = greedyMCPlayer.getMCPlay(card, millisRemaining, criticalPlays, utilityGrid);
		} else { // Greedy select the max probability utility
			//System.out.println("PLAY MAX");
			greedyMCPlayer.makePlay(card, play[0], play[1]);
		}

		// Update probability helper
		probUtil.updateHands(card, play);

		allPlays.remove(play[0]*5+play[1]);
		numPlay++;
		return play; // return it
	}

	// Get the critical plays with the highest utilities
	private int[] getCriticalPlays(double[][] utilityGrid) {
		// Use avg to select the top candidates
		double max = 0;
		int maxIndex = 0;
		for (int i=0; i<25; i++) {
			if (utilityGrid[i/5][i%5] > max) {
				max = utilityGrid[i/5][i%5];
				maxIndex = i;
			}	
		}

		// Count the number of plays better than the average
		int numCritical = 0;
		for (int i=0; i<25; i++) {
			if (utilityGrid[i/5][i%5] >= 1) {
				numCritical++;
			}
		}
		if (numCritical == 0) {
			int numPlays = 0;
			for (int i=0; i<25; i++) {
				if (utilityGrid[i/5][i%5] >= -999999) {
					numPlays++;
				}
			}
			// Select all plays above average
			int[] allPlays = new int[numPlays];
			int playCount = 0;
			for (int i=0; i<25; i++) {
				if (utilityGrid[i/5][i%5] >= -999999) {
					allPlays[playCount++] = i;
				}
			}
			return allPlays;
		}

		// Select all plays above average
		int[] criticalPlays = new int[numCritical];
		int criticalCount = 0;
		for (int i=0; i<25; i++) {
			if (utilityGrid[i/5][i%5] >= 1) {
				criticalPlays[criticalCount++] = i;
			}
		}

		if (max <= 0) {
			return new int[]{maxIndex};
		}

		return criticalPlays;
	}

	// Get the max utility play and return it
	private int[] getMaxGreedyPlay(double[][] utilityGrid) {
		double max = -999999;
		int[] maxPlay = new int[2];

		for (int i=0; i<25; i++) {
			if (utilityGrid[i/5][i%5] > max) {
				max = utilityGrid[i/5][i%5];
				maxPlay = new int[]{i/5,i%5};
			}
		}

		return maxPlay;
	}

	/* (non-Javadoc)
	 * @see PokerSquaresPlayer#getName()
	 */
	@Override
	public String getName() {
		if (this.mcEnabled)
			return "Reath_GreedyProbabilityMCPlayer_Depth" + this.depthLimit;
		else 
			return "Reath_GreedyProbabilityPlayer";
	}

	/**
	 * Demonstrate Reath_ProbabilityMCPlayer play with British point system.
	// Trained Rewards by iteration. iteration 0 is the british point system
	// 4: AVG 45: {1.9403347077370845, 7.46640119462419, 50.47583994952415, 604.2120551924473, 64.03823178016725, 174.29193899782138, 1505.8823529411766, 257.78732545649837, 34285.71428571429};
	// 3: AVG 43: {1.5037593984962405, 5.226480836236933, 55.52342394447657, 256.7901234567901, 49.629629629629626, 78.43137254901961, 640.0, 315.7894736842105, 6000.0};
	// 2: AVG 41: {0.7518796992481203, 2.090592334494773, 36.09022556390977, 173.33333333333334, 33.5, 23.529411764705884, 160.0, 150.0, 600.0};
	// 1: AVG 39: {0.7142857142857143, 2.142857142857143, 17.142857142857142, 104, 26.8, 10, 16, 30, 60};
	// 0: AVG 30: {1, 3, 6, 12, 5, 10, 16, 30, 30};
	*/
	public static void main(String[] args) {
		PokerSquaresPointSystem system = PokerSquaresPointSystem.getBritishPointSystem();
		System.out.println(system);
		double bestAvg = 0;
		double absoluteAvg = 0;
		int absoluteCount = 0;

		// Old Manual Data
		// {0.7142857142857143, 2.142857142857143, 17.142857142857142, 104, 26.8, 9.25315, 16, 30, 60};
		double[] rewards = Reath_GreedyProbabilityMCPlayer.REWARDS;

		// For each hand type edit their utilities
		for (int n=0; n<9; n++) {
			// Save the best reward to output
			double bestReward = rewards[n];
			double originalReward = rewards[n];
			bestAvg = 0;
			for (int i=0; i<80; i++) {
				rewards[n] = originalReward;
				// As time goes on discountFactor decreases allowing for the rewards to converge
				double discountFactor = 40 / (double)i;

				/* UNCOMMENT TO TRAIN
				if (i > 0)
					rewards[n] *= discountFactor;
				//*/

				long totalScore = 0;
				int min = 999999;
				int max = 0;
				// Run the games with the MonteCarlo simulations turned off for speed
				for (int j=0; j<500; j++) {
					Reath_GreedyProbabilityMCPlayer player = new Reath_GreedyProbabilityMCPlayer(12);
					int score = new PokerSquares(player, system).play(false, System.currentTimeMillis());
					if (score < min) {
						min = score;
					}
					if (score > max) {
						max = score;
					}
					totalScore += score;
					System.out.println(score + " " + ((double)totalScore/(double)(j+1)));
				}
				// Calculate the Average score for the 3000 runs and assign max's
				double averageScore = (double)totalScore / 500.0;
				if (averageScore > bestAvg) {
					bestAvg = averageScore;
					bestReward = rewards[n];
				}
				absoluteAvg += averageScore;
				absoluteCount++;

				// Output info for user
				System.out.println("Reward[" + n + "]: " + rewards[n] + ", Min: " + min + ", Avg: " + averageScore + ", bestReward: " + bestReward + ", Max: " + max + ", Best: " + bestAvg + ", AbsoluteAvg: " + (absoluteAvg/absoluteCount));

				// If the best average is much better than the current one break since we already converged
				if (bestAvg - averageScore > 1 && rewards[n] < bestReward) {
					break;
				}
			}
		}
		// Output results
		System.out.println(Arrays.toString(rewards));
	}

}