package bguspl.set.ex;

import bguspl.set.Env;

import java.util.ArrayList;
import java.util.List;
//import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        while (!shouldFinish()) {
            placeCardsOnTable();
            timerLoop();
            updateTimerDisplay(true);
            removeAllCardsFromTable();
        }
        announceWinners();
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        //env.ui.setCountdown(30000,false);
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            removeCardsFromTable();
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        List<Integer> cardSets = new ArrayList<Integer>();
        for (int i = 0; i < 12; i++) {
            cardSets.add(table.slotToCard[i]);
        }
        if (env.util.findSets(cardSets, 1).size() == 0 && deck.size() == 0) {
            try {
                Thread.sleep(env.config.endGamePauseMillies);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            terminate = true;
            env.ui.dispose();
        }
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

   /* private void searchAndRemoveTokens(Player pla) {
        Vector<Integer> plVec = table.returnTokens().get(pla.id);
        if (plVec != null) {
            if (VectorToList(plVec, env.util.findSets(deck, 1000))) {
                for (int j = 0; j < plVec.size(); j++) {
                    table.removeCard(plVec.elementAt(j));
                }
            }
        }

    }*/

    // private void helpRemoveCardsFromTable(Player pl) {
    //     if(env.util.testSet(pl.slotsWithTokens) && pl.slotsWithTokens.length == 3){
    //         //Cards make valid set need to be removed
    //         table.removeCard(pl.slotsWithTokens[0]);
    //         table.removeCard(pl.slotsWithTokens[1]);
    //         table.removeCard(pl.slotsWithTokens[2]);
    //        // pl.point();

    //     } else{
    //        // pl.penalty();
    //     }
    //     table.removeToken(pl.id,pl.slotsWithTokens[0]);
    //     table.removeToken(pl.id,pl.slotsWithTokens[0]);
    //     table.removeToken(pl.id,pl.slotsWithTokens[0]);
    // }

    /** Checks cards should be removed from the table and removes them.**/
    private void removeCardsFromTable() {
        // List<Integer> cardSets = new ArrayList<Integer>();
        // for (int i = 0; i < 12; i++) {
        //     cardSets.add(table.slotToCard[i]);
        // }
        // //Check if the cards on the table include at least one set
        // if (env.util.findSets(cardSets, 1).size() == 0) {
        //     for (int i = 0; i < 12; i++)
        //         table.removeCard(i);

        // }/* else { //For each player check if the token he put is a valid set
        //     for (Player pl : players) {
        //         helpRemoveCardsFromTable(pl);
        //     }
        // }*/
        for(Player player : players)
        {
            if(player.tokensPlaced==3)
            {
                int[] cardsToCheck=new int[3];
                for(int i=0;i<3;i++)
                {
                    cardsToCheck[i]=table.slotToCard[player.slotsWithTokens[i]];
                }
                if(env.util.testSet(cardsToCheck))
                {
                    for(int i=0;i<3;i++)
                    {
                        //int card = (int) (Math.random() * deck.size());
                        table.removeCard(player.slotsWithTokens[i]);
                        //table.placeCard(deck.remove(card),player.slotsWithTokens[i]);
                    }
                    
                    player.point();
                    updateTimerDisplay(true);
                }
                else
                {
                    player.penalty();
                }
            }
        }
    }


    // private boolean VectorToList(Vector<Integer> vec, List<int[]> list) {
    //     for (int i = 0; i < list.size(); i++) {
    //         if (VectorToArray(vec, list.get(i))) {
    //             return true;
    //         }
    //     }
    //     return false;
    // }

    // private boolean VectorToArray(Vector<Integer> vec, int[] arr) {
    //     for (int i = 0; i < arr.length; i++) {
    //         if (arr[i] != vec.elementAt(i).intValue()) {
    //             return false;
    //         }
    //     }
    //     return true;
    // }

    /** Check if any cards can be removed from the deck and placed on the table.**/
    private void placeCardsOnTable() {
        // TODO implement
        int i = 0;
        while (i < table.slotToCard.length && !deck.isEmpty()) {
            if (table.slotToCard[i] == null || (table.slotToCard[i] == -1)) {
                int card = (int) (Math.random() * deck.size());
                if (card >= 0 && card < deck.size()) {
                    int randomCard = deck.remove(card);
                    table.placeCard(randomCard, i);

                }
            }
            i++;
        }
    }

    /**Sleep for a fixed amount of time or until the thread is awakened for some purpose.**/
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**Reset and/or update the countdown and the countdown display.**/
    private void updateTimerDisplay(boolean reset) {
        long currentTime = System.currentTimeMillis();

        if (reset || reshuffleTime == Long.MAX_VALUE) {
            env.ui.setCountdown(/*env.config.turnTimeoutMillis*/ 25 * 1000, false);
            reshuffleTime = currentTime + /*env.config.turnTimeoutMillis*/ 25 * 1000;

        } else {
            env.ui.setCountdown(reshuffleTime - currentTime, false);
        }

        if (reshuffleTime - currentTime <= env.config.turnTimeoutWarningMillis) {
            env.ui.setCountdown(reshuffleTime - currentTime, true);
        }
    }

    // public void resetTimerDisplay()
    // {
    //     long currentTime = System.currentTimeMillis();
    //     env.ui.setCountdown(/*env.config.turnTimeoutMillis*/ 25 * 1000, false);
    //     reshuffleTime = currentTime + /*env.config.turnTimeoutMillis*/ 25 * 1000;
    // }

    /** Returns all the cards from the table to the deck.**/
    private void removeAllCardsFromTable() {
        // TODO implement

        for (Player playerush : players) {
            for(int i=0;i<3;i++)
            {
                if(table.removeToken(playerush.id, playerush.slotsWithTokens[i]))
                {
                    playerush.tokensPlaced--;
                    playerush.slotsWithTokens[i]=-1;
                }
            }
        }

        // Remove all cards from the table and add them back to the deck
        for (int i = 0; i < table.slotToCard.length; i++) {
            if (table.slotToCard[i] != null) {
                deck.add(table.slotToCard[i]);
                table.removeCard(i);
            }
        }

        

        // Shuffle the deck
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // Reset the reshuffleTime to start from the configured turnTimeoutMillis
        reshuffleTime = System.currentTimeMillis() + /*env.config.turnTimeoutMillis*/ 25 * 1000 + 1000;
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
        List<Integer> winnerList = new ArrayList<Integer>();
        int winnerPoints = players[0].score();
        if (shouldFinish()) {
            for (int i = 1; i < players.length; i++) {
                if (players[i].score() >= winnerPoints) {
                    winnerPoints = players[i].score();
                }
            }
            for (int i = 1; i < players.length; i++) {
                if (players[i].score() == winnerPoints) {
                    winnerList.add(players[i].id);
                }
            }
            int[] winnerArr = new int[winnerList.size()];
            for (int j = 0; j < winnerList.size(); j++) {
                winnerArr[j] = winnerList.get(j);
            }
            env.ui.announceWinner(winnerArr);
        }
    }


}



