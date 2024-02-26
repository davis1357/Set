package bguspl.set.ex;

import bguspl.set.Env;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    ///Additions
    private final BlockingQueue<Integer> actionQueue ;
    private boolean penaltyActive;
    public int tokensPlaced = 0;
    private int[] slotsWithTokens = new int[3];

    public Dealer dealer;

    private  Object terminateLock = new Object();
   // private ScheduledExecutorService executor;

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.actionQueue = new ArrayBlockingQueue<>(3);
        this.penaltyActive = false;
        this.dealer = dealer;
        for(int i = 0 ; i < slotsWithTokens.length ; i++){
            slotsWithTokens[i] = -1;
        }
    }

    //Added functions:
    // Enqueue an action into the action queue. If the queue is full, it will wait until space becomes available.
    public void enqueueAction(int action) throws InterruptedException {
        actionQueue.put(action);
    }

    public boolean isHuman(){
        return human;
    }


    /** The main player thread of each player starts here (main loop for the player thread).**/
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        if (!human) createArtificialIntelligence();
        // TODO implement main player loop
        while (!terminate) {
            synchronized (this) {
                try {
                    if (dealer.getCanAct() && !penaltyActive) {
                        int action = actionQueue.take(); // Block until an action is available
                        doAction(action); // Execute the action
                    }
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    playerThread.interrupt();

                }
            }
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");

    }

    /** Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.*/

    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            while (true) {
                // TODO implement player key press simulator
                try {
                    synchronized (terminateLock) {
                        if (terminate) {
                            break;
                        }
                    }
                        if(dealer.getCanAct() && !penaltyActive){
                             keyPressed((int) (Math.random() * 12));
                             Integer newSlot=actionQueue.poll();
                             if(newSlot != null)
                                doAction((int)newSlot);
                        }
                        Thread.sleep(100);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();}

            }
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, "computer-" + id);
        aiThread.start();
    }

    /**Called when the game should be terminated.**/
    public void terminate() {
        // TODO implement
        terminate = true;
        try{
            actionQueue.add(1);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public int getSlotWithTokens(int i){
        return slotsWithTokens[i];
    }

    public int[] getSlotWithTokens(){
        return slotsWithTokens;
    }
    public int getSTokensPlaced(){
        return tokensPlaced;
    }

    public boolean emptyToken(int slot){
        if(slot >= 0){
            slotsWithTokens[slot] = -1;
            arrangeTokens(slotsWithTokens);
            tokensPlaced--;
            return true;
        }
        return false;
    }

    public int valToIndex(int val){
        for (int i = 0; i < slotsWithTokens.length ; i++){
            if(slotsWithTokens[i] == val){
                return i;
            }
        }
        return -1;
    }

    public  int[] arrangeTokens(int[] arr){
        int temp = 0;
        for( int i = 0 ; i < arr.length - 1 ; i++){
            if(arr[i] == -1 ){
                temp = arr[i];
                arr[i] = arr [i+ 1];
                arr[i+1] = temp;
            }
        }
        return arr;
    }

    public void setPenalty(boolean isPenalty)
    {
        penaltyActive= isPenalty;

    }
    /**This method is called when a key is pressed.
     * @param slot - the slot corresponding to the key pressed.**/

    public void keyPressed(int slot) {
        if(dealer.getCanAct() && !penaltyActive)
            actionQueue.offer(slot);
    }

    private void doAction(int slot){
        // TODO implement
        if(!penaltyActive && !terminate) {
            if(tokensPlaced < 3) {
                if (!table.tokenPlaced.get(id).contains(slot)  && ((table.slotToCard[slot] != null) && (table.slotToCard[slot] != -1))) {
                    table.placeToken(id, slot);
                    slotsWithTokens[tokensPlaced] = slot;
                    tokensPlaced++;
                    if(tokensPlaced == 3){
                        dealer.playersQueueAfterTokens.add(this);
                    }

                } else {
                    if(table.removeToken(id, slot)) {
                       emptyToken(valToIndex(slot));
                       //dealer.playersQueueAfterTokens.remove(this);
                    }
                }
            }
        }
    }


    /** Award a point to a player and perform other related actions.
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.**/

    public void point() {
        // TODO implement
        penaltyActive=true;
        score++;
        for(int i = 0 ; i < slotsWithTokens.length ; i++){
            table.removeToken(id, slotsWithTokens[i]);
            tokensPlaced--;
            slotsWithTokens[i]=-1;
        }
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, score);
        new Thread(() -> {
            long currentTime = System.currentTimeMillis();
            long pointEndTime = currentTime + env.config.pointFreezeMillis;
            env.ui.setFreeze(id, env.config.pointFreezeMillis);
            while (System.currentTimeMillis() < pointEndTime) {
                long remainingTime = pointEndTime - System.currentTimeMillis();
                env.ui.setFreeze(id, remainingTime+1000);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Penalty period has ended, clear the freeze
            env.ui.setFreeze(id, 0);
            penaltyActive = false;
        }).start();
    }

    /**Penalize a player and perform other related actions.**/
    public void penalty() {
        // TODO implement
      penaltyActive = true;
      for(int i = 0 ; i < slotsWithTokens.length ; i++){
          table.removeToken(id, slotsWithTokens[i]);
          tokensPlaced--;
          slotsWithTokens[i]=-1;
      }
      new Thread(() -> {
            long currentTime = System.currentTimeMillis();
            long penaltyEndTime = currentTime + env.config.penaltyFreezeMillis;
            env.ui.setFreeze(id, env.config.penaltyFreezeMillis);
            while (System.currentTimeMillis() < penaltyEndTime) {
                long remainingTime = penaltyEndTime - System.currentTimeMillis();
                env.ui.setFreeze(id, remainingTime+1000);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Penalty period has ended, clear the freeze
            env.ui.setFreeze(id, 0);
            penaltyActive = false;
        }).start();
    }

    public int score() {
        return score;
    }
}
