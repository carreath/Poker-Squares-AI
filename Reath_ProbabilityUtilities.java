import java.util.*;

/*
 * Reath_ProbabilityUtilities.java
 * 
 * This class is used to calculate utility values to inform 
 * A monteCarlo search algorithm on how to successfully play Poker Squares
 *
 * Author: Caleb Reath
*/
public class Reath_ProbabilityUtilities {
    private Card[][] hands; // 2D representation of all 10 hands in a poker squares game (0-4 rows, 5-9 cols)
    private int[] handsSizes; // Keep track of the size of each hand for quick reference 
    private int[] tempCardIndex; // Keep track of where the new Card was inserted for later removal

    private HashSet<Card> cards; // Use a hashSet for instant lookup of any card to check if it exists in the deck

    private double[] handUtilities; // The utility values for each individual hand (all added together)
    private double[][] handProbs; // The probability of each type of hand occurring with the given hand
    private double[][] prevHandProbs; // Probabilities from previous turn for reference
    private double[] handRewards; // Rewards for each hand type

    private int[] ranks; // Total number of cards of any rank remaining in the deck
    private int[] suits; // Total number of cards of any suit remaining in the deck

    private int cardsPlayed = 0; // Turn counter

    private boolean[][] possibleHands; // Holds wether a hand type is possible given the selected hand (with replacments)

    public Reath_ProbabilityUtilities(double[] adj) {
        handRewards = adj;

        hands = new Card[10][5];
        handsSizes = new int[10];
        tempCardIndex = new int[10];

        cards = new HashSet<Card>(Arrays.asList(Card.getAllCards()));

        handUtilities = new double[10];
        handProbs = new double[10][9];
        prevHandProbs = new double[10][9];
        //handRewards = new double[] {1,3,6,12,5,10,16,30,30};

        ranks = new int[13];
        suits = new int[4];

        // Populate ranks and suits with new deck
        for (int i=0; i<13; i++) {
            ranks[i] = 4;
        }
        for (int i=0; i<4; i++) {
            suits[i] = 13;
        }

        possibleHands = new boolean[10][9];
    }

    // Update all variables for hands with the placment info for the new card
    public void updateHands(Card drawnCard, int[] location) {
        hands[location[0]][location[1]] = drawnCard;
        hands[location[1] + 5][location[0]] = drawnCard;
        handsSizes[location[0]]++;
        handsSizes[location[1] + 5]++;
        ranks[drawnCard.getRank()]--;
        suits[drawnCard.getSuit()]--;
        cards.remove(drawnCard);
        cardsPlayed++;
        getPossibleHands(null);
        calculateProbabilities(null);
    }

    // Refreshes the probabiliy tables and returns the utilityGrid for the new hands
    public double[][] refreshProbabilities(Card drawnCard) {
        // Get the hands possible with the new card added to each eligible hand
        getPossibleHands(drawnCard);

        // Calculate the new probabilities with the new card added to each hand
        calculateProbabilities(drawnCard);

        /**
        for (int i=0; i<10; i++) {
            System.out.println(Arrays.toString(prevHandProbs[i]));
        }
        System.out.println("\n");
        for (int i=0; i<10; i++) {
            System.out.println(Arrays.toString(handProbs[i]));
        }
        //**/

        // Calculate the utilities
        return getUtilityGrid();
    }

    // returns the probabilities
    public double[][] getProbabilities() {
        return handProbs;
    }

    // Calculates the utilities for use in the Poker Squares game
    private double[][] getUtilityGrid() {
        // Utility grid will be setup like the game grid in rows and cols
        double[][] utilityGrid = new double[5][5];

        // Add together all hand utilities with respect to the probabilities prior to the new card
        double[] utility = new double[10];
        for (int k=0; k<10; k++) {
            for (int i=0; i<9; i++) {
                utility[k] += (handProbs[k][i] - prevHandProbs[k][i]);
            }
        }

        // If there is no card in the spot i j then combine both utilities from the correct row and col in utilities
        for (int i=0; i<5; i++) {
            for (int j=0; j<5; j++) {
                if (hands[i][j] == null) {
                    utilityGrid[i][j] = (utility[i] + utility[j + 5]);
                } else {
                    utilityGrid[i][j] = -(1.0/0.0);
                }
            }
        }

        return utilityGrid;
    }

    // Given unlimited numbers of any card get all possible hands for each row and column
    // given the new drawn card
    private void getPossibleHands(Card drawnCard) {
        for (int hand=0; hand<10; hand++) {
            // Reset possibility of current hand to avoid forgetting
            possibleHands[hand] = new boolean[9];
            tempCardIndex[hand] = -1;

            // If hand isnt complete yet
            if (handsSizes[hand] < 5) {
                // Insert the new card into this hand temporarily
                if (drawnCard != null) {
                    for (int i=0; i<5; i++) {
                        if (hands[hand][i] == null) {
                            tempCardIndex[hand] = i;
                            hands[hand][i] = drawnCard;
                            handsSizes[hand]++;
                            break;
                        }
                    }
                }

                // Get the current handId after insertion of new card
                int newHand = PokerHand.getPokerHandId(hands[hand]);

                // Sets the possibility of the current hand if not High Card
                if (newHand != 0) {
                    //possibleHands[hand][newHand - 1] = true;
                }

                // If hand is not one of the 5 card hands
                if (newHand < 4) {
                    int firstSuit = -1;

                    int lowestCard = 13;
                    int highestCard = 0;

                    boolean flush = true;
                    boolean royal = true;
                    boolean straight = true;

                    for (int i=0; i<5; i++) {
                        Card card = hands[hand][i];
                        if (card != null) {
                            // Check Flush possibility
                            if (flush) {
                                if (firstSuit == -1) {
                                    firstSuit = card.getSuit();
                                } else if (firstSuit != card.getSuit()) {
                                    flush = false;
                                }
                            }

                            // Check Straight possibility
                            if (straight) {
                                if (card.getRank() > highestCard) {
                                    highestCard = card.getRank();
                                }
                                if (card.getRank() < lowestCard) {
                                    lowestCard = card.getRank();
                                }

                                // Check range of cards. If the range exceeds 4 then it cannot be a straight
                                if (highestCard - lowestCard > 4) {
                                    straight = false;
                                }
                            }

                            // Check if all cards in hand are royal
                            if (royal && card.getRank() > 0 && card.getRank() < 9) {
                                royal = false;
                            }

                            // Break early if hand cannot be royal, straight, or a flush
                            if (!royal && !flush && !straight) {
                                break;
                            }
                        }
                    }

                    // Set possible hands using simple logic
                    if (newHand == 0) { // HighCard
                        if (handsSizes[hand] == 0) {
                            possibleHands[hand][0] = true; // One pair
                            possibleHands[hand][1] = true; // Two pair
                            possibleHands[hand][2] = true; // Three of a kind
                            possibleHands[hand][3] = true; // Straight
                            possibleHands[hand][4] = true; // Flush
                            possibleHands[hand][5] = true; // Full house
                            possibleHands[hand][6] = true; // Four of a kind
                            possibleHands[hand][7] = true; // Straight Flush
                            possibleHands[hand][8] = royal; // Royal Flush
                        } else if (handsSizes[hand] == 1) {
                            possibleHands[hand][0] = true; // One pair
                            possibleHands[hand][1] = true; // Two pair
                            possibleHands[hand][2] = true; // Three of a kind
                            possibleHands[hand][3] = true; // Straight
                            possibleHands[hand][4] = true; // Flush
                            possibleHands[hand][5] = true; // Full house
                            possibleHands[hand][6] = true; // Four of a kind
                            possibleHands[hand][7] = true; // Straight Flush
                            possibleHands[hand][8] = royal; // Royal Flush
                        } else if (handsSizes[hand] == 2) {
                            possibleHands[hand][0] = true; // One pair
                            possibleHands[hand][1] = true; // Two pair
                            possibleHands[hand][2] = true; // Three of a kind
                            possibleHands[hand][3] = straight; // Straight
                            possibleHands[hand][4] = flush; // Flush
                            possibleHands[hand][5] = true; // Full house
                            possibleHands[hand][6] = true; // Four of a kind
                            possibleHands[hand][7] = straight && flush; // Straight Flush
                            possibleHands[hand][8] = royal && flush; // Royal Flush
                        } else if (handsSizes[hand] == 3) {
                            possibleHands[hand][0] = true; // One pair
                            possibleHands[hand][1] = true; // Two pair
                            possibleHands[hand][2] = true; // Three of a kind
                            possibleHands[hand][3] = straight; // Straight
                            possibleHands[hand][4] = flush; // Flush
                            possibleHands[hand][7] = straight && flush; // Straight Flush
                            possibleHands[hand][8] = royal && flush; // Royal Flush
                        } else if (handsSizes[hand] == 4) {
                            possibleHands[hand][0] = true; // One pair
                            possibleHands[hand][3] = straight; // Straight
                            possibleHands[hand][4] = flush; // Flush
                            possibleHands[hand][7] = straight && flush; // Straight Flush
                            possibleHands[hand][8] = royal && flush; // Royal Flush
                        }
                    } else if (newHand == 1) { // One pair
                        if (handsSizes[hand] == 2) {
                            possibleHands[hand][1] = true; // Two pair
                            possibleHands[hand][2] = true; // Three of a kind
                            possibleHands[hand][4] = flush; // Flush
                            possibleHands[hand][5] = true; // Full house
                            possibleHands[hand][6] = true; // Four of a kind
                        } else if (handsSizes[hand] == 3) {
                            possibleHands[hand][1] = true; // Two pair
                            possibleHands[hand][2] = true; // Three of a kind
                            possibleHands[hand][4] = flush; // Flush
                            possibleHands[hand][5] = true; // Full house
                            possibleHands[hand][6] = true; // Four of a kind
                        } else if (handsSizes[hand] == 4) {
                            possibleHands[hand][1] = true; // Two pair
                            possibleHands[hand][2] = true; // Three of a kind
                            possibleHands[hand][4] = flush; // Flush
                        }
                    } else if (newHand == 2) { // Two pair
                        if (handsSizes[hand] == 4) {
                            possibleHands[hand][2] = true; // Three of a kind
                            possibleHands[hand][4] = flush; // Flush
                            possibleHands[hand][5] = true; // Full house
                        }
                    } else if (newHand == 3) { // Three of a kind
                        if (handsSizes[hand] < 5) {
                            possibleHands[hand][4] = flush; // Flush
                            possibleHands[hand][5] = true; // Full house
                            possibleHands[hand][6] = true; // Four of a kind
                        }
                    }
                }
            }
        }
    }

    // Calculate the probabilities
    private void calculateProbabilities(Card drawnCard) {
        boolean noCard = (drawnCard == null);
        // For each hand
        for (int hand=0; hand<10; hand++) {
            // save the prvious probabilities for future use
            prevHandProbs[hand] = handProbs[hand];
            handProbs[hand] = new double[9];

            // if the haand already has an Id set its probability to 100%
            int handId = PokerHand.getPokerHandId(hands[hand]);
            if (!noCard && handId > 0)
                handProbs[hand][handId - 1] = 1.0;

            // Get the probabilities for each hand type
            if (!noCard || handId != 1) handProbs[hand][0] = onePairProb(hand, handId);
            if (!noCard || handId != 2) handProbs[hand][1] = twoPairProb(hand, handId);
            if (!noCard || handId != 3) handProbs[hand][2] = threeOfKindProb(hand, handId);
            if (!noCard || handId != 4) handProbs[hand][3] = straightProb(hand, handId);
            if (!noCard || handId != 5) handProbs[hand][4] = flushProb(hand, handId);
            if (!noCard || handId != 6) handProbs[hand][5] = fullHouseProb(hand, handId);
            if (!noCard || handId != 7) handProbs[hand][6] = fourOfKindProb(hand, handId);
            if (!noCard || handId != 8) handProbs[hand][7] = straightFlushProb(hand, handId);
            if (!noCard || handId != 9) handProbs[hand][8] = royalFlushProb(hand, handId);

            // If we added the drawn card to the hands, we can remove it now
            if (!noCard && tempCardIndex[hand] != -1) {
                hands[hand][tempCardIndex[hand]] = null;
                handsSizes[hand]--;
            }

            // Use the probabilities with the rewards to create the handUtilities
            handProbs[hand][0] *= handRewards[0];
            handProbs[hand][1] *= handRewards[1];
            handProbs[hand][2] *= handRewards[2];
            handProbs[hand][3] *= handRewards[3];
            handProbs[hand][4] *= handRewards[4];
            handProbs[hand][5] *= handRewards[5];
            handProbs[hand][6] *= handRewards[6];
            handProbs[hand][7] *= handRewards[7];
            handProbs[hand][8] *= handRewards[8];
        }
    }

    // Probability of One pair
    // Adds together prob(getting a pair with only new cards from deck)
    // and prob(completing an existing pair in the hand)
    private double onePairProb(int hand, int handId) {
        if (handId == 1) {
            return 1.0;
        } else if (handId > 1) {
            return 0.0;
        }

        double prob = 0.0;
        if (possibleHands[hand][1]) {
            int[] tempRanks = new int[13];
            int max = 0;

            // Setup
            for (int i=0; i<5; i++) {
                Card card = hands[hand][i];
                if (card != null) {
                    tempRanks[card.getRank()]++;
                    if (tempRanks[card.getRank()] > max) {
                        max = tempRanks[card.getRank()];
                    }
                }
            }

            // Begin Calculations
            if (2 - max + handsSizes[hand] > 5) {
                prob = 0;
            } else {
                // New pair from deck
                int tempTotal = 0;
                if (handsSizes[hand] <= 3) {
                    for (int i=0; i<13; i++) {
                        if (tempRanks[i] == 0 && ranks[i] >= 2) {
                            tempTotal += choose(ranks[i], 2);
                        }
                    }
                }
                double totalCombinations = choose(52 - cardsPlayed, 2);
                prob += tempTotal / totalCombinations;

                // Existing cards from hand
                for (int i=0; i<13; i++) {
                    if (2 - tempRanks[i] + handsSizes[hand] <= 5) {
                        prob += choose(ranks[i], 2 - tempRanks[i]) / choose(52 - cardsPlayed, 2 - tempRanks[i]);
                    }
                }
            }
        }
        return prob;
    }

    // Probability of Two pair (2nd hardest one)
    // Adds together prob(getting one or two pairs with only new cards from deck)
    // and prob(completing an existing set of pairs in the hand)
    private double twoPairProb(int hand, int handId) {
        if (handId == 2) {
            return 1.0;
        } else if (handId > 2) {
            return 0.0;
        }
        
        double prob = 0.0;
        if (possibleHands[hand][1]) {
            int[] tempRanks = new int[13];
            int max = 0;
            // Setup
            for (int i=0; i<5; i++) {
                Card card = hands[hand][i];
                if (card != null) {
                    tempRanks[card.getRank()]++;
                    if (tempRanks[card.getRank()] > max) {
                        max = tempRanks[card.getRank()];
                    }
                }
            }

            // Begin Calculations
            double totalCombinations = 1;
            double tempTotal = 0;
            // New cards from deck
            if (handsSizes[hand] <= 1) {
                for (int i=0; i<13; i++) {
                    if (tempRanks[i] == 0 && ranks[i] >= 2) {
                        double temp = choose(ranks[i], 2);
                        for (int j=i+1; j<13; j++) {
                            if (tempRanks[j] == 0 && ranks[j] >= 2) {
                                tempTotal += temp * choose(ranks[i], 2);
                            }
                        }
                    }
                }
                totalCombinations = choose(52 - cardsPlayed, 4);
                prob += tempTotal / totalCombinations;
            }

            // Existing cards from hand
            for (int i=0; i<13; i++) {
                if (2 - tempRanks[i] + handsSizes[hand] <= 5) {
                    for (int j=i + 1; j<13; j++) {
                        if ((2 - tempRanks[i]) + (2 - tempRanks[j]) + handsSizes[hand] <= 5) {
                            prob += choose(ranks[i], 2 - tempRanks[i]) * choose(ranks[j], 2 - tempRanks[j]) / choose(52 - cardsPlayed, (2 - tempRanks[i]) + (2 - tempRanks[j]));
                        }
                    }
                }
            }
        }
        return prob;
    }

    // Probability of TOAK
    // Adds together prob(getting a TOAK with only new cards from deck)
    // and prob(completing an existing set of cards in the hand)
    private double threeOfKindProb(int hand, int handId) {
        if (handId == 3) {
            return 1.0;
        } else if (handId > 3) {
            return 0.0;
        }
        
        double prob = 0.0;
        if (possibleHands[hand][2]) {
            int[] tempRanks = new int[13];
            int max = 0;
            // Setup
            for (int i=0; i<5; i++) {
                Card card = hands[hand][i];
                if (card != null) {
                    tempRanks[card.getRank()]++;
                    if (tempRanks[card.getRank()] > max) {
                        max = tempRanks[card.getRank()];
                    }
                }
            }

            // Begin Calculations
            if (3 - max + handsSizes[hand] > 5) {
                prob = 0;
            } else {
                int tempTotal = 0;
                // New cards from deck
                if (handsSizes[hand] <= 2) {
                    for (int i=0; i<13; i++) {
                        if (tempRanks[i] == 0 && ranks[i] >= 3) {
                            tempTotal += choose(ranks[i], 3);
                        }
                    }
                }
                double totalCombinations = choose(52 - cardsPlayed, 3);
                prob += tempTotal / totalCombinations;

                // Existing cards from hand
                for (int i=0; i<13; i++) {
                    if (3 - tempRanks[i] + handsSizes[hand] <= 5) {
                        prob += choose(ranks[i], 3 - tempRanks[i]) / choose(52 - cardsPlayed, 3 - tempRanks[i]);
                    }
                }
            }
        }
        return prob;
    }

    // Probability of Straight
    // Adds together prob(getting the remaining cards from a shifting range)
    // Simply get the lowest and highest cards. get the range between them
    // then shift from the left to the right. Add together each frame position
    private double straightProb(int hand, int handId) {
        if (handId == 4) {
            return 1.0;
        } else if (handId > 4) {
            return 0.0;
        }

        double prob = 0.0;
        if (possibleHands[hand][3]) {
            int[] tempRanks = new int[13];
            int lowCard = 13;
            int highCard = 0;
            // Setup
            for (int i=0; i<5; i++) {
                Card card = hands[hand][i];
                if (card != null) {
                    tempRanks[card.getRank()]++;
                    if (card.getRank() < lowCard) {
                        lowCard = card.getRank();
                    }
                    if (card.getRank() > highCard) {
                        highCard = card.getRank();
                    }
                }
            }

            // Begin Calculations
            int start = highCard - 4;
            if (start < 0) start = 0;

            for (int i=start; i<=lowCard; i++) {
                int count = 1;
                // New and existing cards from deck
                for (int j=i; j<i+5 && j<13; j++) {
                    if (tempRanks[j] == 0) {
                        if (ranks[j] == 0) {
                            i = j;
                            count = 0;
                            break;
                        }
                        count*=ranks[j];
                    }
                }
                double totalCombinations = choose(52 - cardsPlayed, 5 - handsSizes[hand]);
                prob += (double)(count) / (double)(totalCombinations);
            }
        }
        return prob;
    }

    // Probability of Flush
    // Simply check if there is only one suit in hand.
    // Then get the probability of choosing the remaining cards as that suit
    private double flushProb(int hand, int handId) {
        if (handId == 5) {
            return 1.0;
        } else if (handId > 5) {
            return 0.0;
        }

        double prob = 0.0;
        if (possibleHands[hand][4]) {
            int suit = -1;
            // Setup
            for (int i=0; i<5; i++) {
                Card card = hands[hand][i];
                if (card != null) {
                    suit = card.getSuit();
                }
            }

            // Begin Calculations
            if (suit == -1) {
                // New cards from deck
                prob = choose(suits[0], 5);
                prob += choose(suits[1], 5);
                prob += choose(suits[2], 5);
                prob += choose(suits[3], 5);
            } else {
                // Existing cards from hand
                prob = choose(suits[suit], 5 - handsSizes[hand]);
            }

            double totalCombinations = choose(52 - cardsPlayed, 5 - handsSizes[hand]);
            prob /= totalCombinations;
        }
        return prob;
    }

    // Probability of Full House (hardest one)
    // Either flat probability of getting a full house OR
    // Add together joint probabilities of getting any TOAK and 2OAK times the other way around
    // and prob(completing any permutation of incomplete sets of TOAK or 2OAK)
    private double fullHouseProb(int hand, int handId) {
        if (handId == 6) {
            return 1.0;
        } else if (handId > 6) {
            return 0.0;
        }

        double prob = 0.0;
        if (possibleHands[hand][5]) {
            int[] tempRanks = new int[13];
            int rank1 = 0;
            int rank2 = 0;
            // Setup
            for (int i=0; i<5; i++) {
                Card card = hands[hand][i];
                if (card != null) {
                    tempRanks[card.getRank()]++;
                    if (rank1 == -1) {
                        rank1 = card.getRank();
                    } else {
                        rank2 = card.getRank();
                    }
                }
            }

            // Begin Calculations
            double totalCombinationsForRanks = 0;
            if (rank1 == -1) {
                // New cards from deck
                prob = 3744.0 / choose(52 - cardsPlayed, 5); 
            } else if (rank2 == -1) {
                // New and Existing cards from hand
                double rank13OAKCombos = choose(ranks[rank1], 3 - tempRanks[rank1]);
                double rank12OAKCombos = choose(ranks[rank1], 2 - tempRanks[rank1]);
                double rank23OAKCombos = 0;
                double rank22OAKCombos = 0;

                for (int i=0; i<13; i++) {
                    if (i != rank1) {
                        if (ranks[i] >= 2) {
                            rank22OAKCombos += choose(ranks[i], 2);
                        }
                        if (ranks[i] >= 3) {
                            rank23OAKCombos += choose(ranks[i], 3);
                        }
                    }
                }

                totalCombinationsForRanks = rank13OAKCombos * rank22OAKCombos + rank23OAKCombos * rank12OAKCombos;
            } else {
                // Existing cards from hand
                double rank13OAKCombos = choose(ranks[rank1], 3 - tempRanks[rank1]);
                double rank12OAKCombos = choose(ranks[rank1], 2 - tempRanks[rank1]);
                double rank23OAKCombos = choose(ranks[rank1], 3 - tempRanks[rank2]);
                double rank22OAKCombos = choose(ranks[rank1], 2 - tempRanks[rank2]);

                totalCombinationsForRanks = rank13OAKCombos * rank22OAKCombos + rank23OAKCombos * rank12OAKCombos;
            }

            double totalCombinations = choose(52 - cardsPlayed, 5 - handsSizes[hand]);
            prob = totalCombinationsForRanks / totalCombinations;
        }
        return prob;
    }

    // Probability of FOAK
    // Adds together prob(getting a FOAK with only new cards from deck)
    // and prob(completing an existing set of cards in the hand)
    private double fourOfKindProb(int hand, int handId) {
        if (handId == 7) {
            return 1.0;
        } else if (handId > 7) {
            return 0.0;
        }
        
        double prob = 0.0;
        if (possibleHands[hand][6]) {
            int[] tempRanks = new int[13];
            int max = 0;
            // Setup
            for (int i=0; i<5; i++) {
                Card card = hands[hand][i];
                if (card != null) {
                    tempRanks[card.getRank()]++;
                    if (tempRanks[card.getRank()] > max) {
                        max = tempRanks[card.getRank()];
                    }
                }
            }
            // Begin Calculations
            if (4 - max + handsSizes[hand] > 5) {
                prob = 0;
            } else {
                // New cards from deck
                int tempTotal = 0;
                if (handsSizes[hand] <= 1) {
                    for (int i=0; i<13; i++) {
                        if (tempRanks[i] == 0 && ranks[i] == 4) {
                            tempTotal++;
                        }
                    }
                }
                double totalCombinations = choose(52 - cardsPlayed, 4);
                prob += tempTotal / totalCombinations;

                // Existing cards from hand
                for (int i=0; i<13; i++) {
                    if (4 - tempRanks[i] + handsSizes[hand] <= 5) {
                        prob += choose(ranks[i], 4 - tempRanks[i]) / choose(52 - cardsPlayed, 4 - tempRanks[i]);
                    }
                }
            }
        }
        return prob;
    }

    // Probability of Straight Flush
    // Simple, Do the same as flush and straight
    private double straightFlushProb(int hand, int handId) {
        if (handId == 8) {
            return 1.0;
        } else if (handId > 8) {
            return 0.0;
        }

        double prob = 0.0;
        if (possibleHands[hand][7]) {
            int[] tempRanks = new int[13];
            int lowCard = 13;
            int highCard = 0;
            int suit = -1;
            // Setup
            for (int i=0; i<5; i++) {
                Card card = hands[hand][i];
                if (card != null) {
                    tempRanks[card.getRank()]++;
                    suit = card.getSuit();
                    if (card.getRank() < lowCard) {
                        lowCard = card.getRank();
                    }
                    if (card.getRank() > highCard) {
                        highCard = card.getRank();
                    }
                }
            }

            // Begin Calculations
            int start = highCard - 4;
            if (start < 0) start = 0;

            for (int i=start; i<=lowCard; i++) {
                int count = 1;
                // New and Existing cards from deck
                for (int j=i; j<i+5 && j<13; j++) {
                    if (tempRanks[j] == 0) {
                        if (suit != -1 && !cards.contains(Card.getCard(suit*13 + j))) {
                            i = j;
                            count = 0;
                            break;
                        }
                        count++;
                    }
                }
                
                double totalCombinations = choose(52 - cardsPlayed, 5 - handsSizes[hand]);
                prob = count / totalCombinations;
            }
        }
        return prob;
    }

    // Probability of Royal Flush
    // Also Simple, To check royalty just check if all cards from T-A are in hand or deck
    // Then get the probabilities of drawing them.
    // Also probability of a flush
    private double royalFlushProb(int hand, int handId) {
        if (handId == 9) {
            return 1.0;
        }

        double prob = 0.0;
        if (possibleHands[hand][8]) {
            int suit = -1;
            // Setup
            for (int i=0; i<5; i++) {
                Card card = hands[hand][i];
                if (card != null) {
                    suit = card.getSuit();
                    break;
                }
            }

            // Begin Calculations
            double count = 0;
            if (suit == -1) {
                // New cards from deck
                for (int j=0; j<4; j++) {
                    count++;
                    for (int i=0; i<13; i++) {
                        if (i==1) {
                            i = 8;
                        }
                        if (suit != -1 && !cards.contains(Card.getCard(suit*13 + i))) {
                            count--;
                            break;
                        }
                    }
                }
            } else {
                // Existing cards from hand
                count = 1;
                for (int i=0; i<13; i++) {
                    if (i==1) {
                        i = 8;
                    }
                    if (!cards.contains(Card.getCard(suit*13 + i))) {
                        count--;
                        break;
                    }
                }
            }
            double totalCombinations = choose(52 - cardsPlayed, 5 - handsSizes[hand]);
            prob = count / totalCombinations;
        }
        return prob;
    }  

    // n Choose k Combinatorics algorithm
    private double choose(int n, int k) {
        if (n == k) {
            return 1;
        } else if (n < k) {
            return 0;
        }

        double combinations = 1;

        for (int i=n; i > n-k; i--) {
            combinations *= n;
        }
        combinations /= factorial(k);

        return combinations;
    }

    // Factorial Algorithm
    private double factorial(int n) {
        double value = 1;

        for (int i=2; i<=n; i++) {
            value *= i;
        }

        return value;
    }
}

















































