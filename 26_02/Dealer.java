package bguspl.set.ex;

import bguspl.set.Env;

import java.util.*;
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
    //private ScheduledExecutorService executor;

    //Additions
    private  Thread playerThreads[];
    private boolean canAct=true;
    //private Queue<Player> setQueue;
    public Vector<Player> playersQueueAfterTokens ;
    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        terminate = false;
       // executor =  Executors.newScheduledThreadPool(1);
        playerThreads= new Thread[players.length];
        playersQueueAfterTokens= new Vector<Player>();
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        //***********************************************
        for(int i = 0 ; i < playerThreads.length ; i++){
            playerThreads[i]=new Thread(players[i]);
            playerThreads[i].start();
            players[i].setPenalty(true);
        }
        //***********************************************
        while (!shouldFinish()) {
            placeCardsOnTable();

            for (Player playerush : players) {
                playerush.setPenalty(false);
            }
           // table.hints();
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
        for(int i = players.length-1; i >= 0 ; i--){
            players[i].terminate();
        }
        terminate = true;
     }

    public boolean getCanAct(){
        return canAct;
    }

    /**Check if the game should be terminated or the game end conditions are met.
     *@return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }


    /** Checks cards should be removed from the table and removes them.**/
    private void removeCardsFromTable() {
        if(!playersQueueAfterTokens.isEmpty()) {
            synchronized (playersQueueAfterTokens) {
                Iterator<Player> iterator = playersQueueAfterTokens.iterator();
                while (iterator.hasNext()) {
                    Player player = iterator.next();
                    if (player.tokensPlaced == 3) {
                        int[] cardsToCheck = new int[3];
                        int[] cardsSlots = new int[3];
                        for (int i = 0; i < cardsToCheck.length; i++) {
                            //if (player.getSlotWithTokens(i) > -1)
                                cardsToCheck[i] = table.slotToCard[player.getSlotWithTokens(i)];
                            cardsSlots[i] = player.getSlotWithTokens(i);
                        }
                        if (env.util.testSet(cardsToCheck)) {
                            for (int i = 0; i < cardsToCheck.length; i++) {
                                //int card = (int) (Math.random() * deck.size());
                                if (player.getSlotWithTokens(i) > -1) {
                                    table.removeCard(player.getSlotWithTokens(i));
                                    //table.placeCard(deck.remove(card), player.getSlotWithTokens(i));
                                }
                            }

                            player.point();
                            updateTimerDisplay(true);
                            for (Player pla_yer: players) {
                                for(int j = pla_yer.getSlotWithTokens().length -1 ; j >=0 ; j--) {
                                    if(pla_yer.getSlotWithTokens(j)==cardsSlots[0] || pla_yer.getSlotWithTokens(j)==cardsSlots[1] ||  pla_yer.getSlotWithTokens(j)==cardsSlots[2]){
                                        table.removeToken(pla_yer.id, pla_yer.getSlotWithTokens(j));
                                        pla_yer.emptyToken(j);
                                    }
                                }
                            }
                        } else {
                            player.penalty();
                        }

                    }
                }
            }
        }
    }

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

        canAct = true;
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

    /** Returns all the cards from the table to the deck.**/
    private void removeAllCardsFromTable() {
        // TODO implement
        canAct = false;

        // Remove all cards from the table and add them back to the deck
        for (int i = 0; i < table.slotToCard.length; i++) {
            if (table.slotToCard[i] != null && table.slotToCard[i] != -1) {
                deck.add(table.slotToCard[i]);
                table.removeCard(i);
            }
        }

        for (Player playerush : players) {
            for (int i = 0 ; i < playerush.getSlotWithTokens().length ; i++) {
                if(playerush.tokensPlaced > 0 && table.removeToken(playerush.id, playerush.getSlotWithTokens(0))){
                    playerush.emptyToken(i);
                }
                /*table.removeToken(playerush.id, playerush.slotsWithTokens[0]);
                table.removeToken(playerush.id, playerush.slotsWithTokens[1]);
                table.removeToken(playerush.id, playerush.slotsWithTokens[2]);
                */
            }

        }

        // Shuffle the deck
        //Collections.shuffle(deck);
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
        if(players.length > 0) {
            int winnerPoints = players[0].score();
            if (shouldFinish()) {
                for (int i = 1; i < players.length; i++) {
                    if (players[i].score() >= winnerPoints) {
                        winnerPoints = players[i].score();
                    }
                }
                for (int i = 0; i < players.length; i++) {
                    if (players[i].score() == winnerPoints) {
                        winnerList.add(players[i].id);
                    }
                }
                int[] winnerArr = new int[winnerList.size()];
                for (int j = 0; j < winnerList.size(); j++) {
                    winnerArr[j] = winnerList.get(j);
                }
                env.ui.announceWinner(winnerArr);
                try{
                    Thread.sleep(env.config.endGamePauseMillies);
                }
                catch(InterruptedException e) {throw new RuntimeException(e);}
                terminate();
            }
        }
        else{
            System.out.println("There are no players");
        }
  }

    public void test()
    {
        placeCardsOnTable();
        assert table.slotToCard.length == 4 : "error";

        removeAllCardsFromTable();
        assert table.countCards() == 0 : "second";

        announceWinners();

    }
}



