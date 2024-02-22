package bguspl.set.ex;

import bguspl.set.Env;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.TimeUnit;

//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.Executors;

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
    private final BlockingQueue<Integer> actionQueue;
    private boolean penaltyActive;
    public int tokensPlaced = 0;
    public int[] slotsWithTokens = new int[3];

    //private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

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
        this.penaltyActive =false;
        for(int i=0;i<slotsWithTokens.length;i++)
        {
            slotsWithTokens[i]=-1;
        }
    }

    //Added functions:
    // Enqueue an action into the action queue. If the queue is full, it will wait until space becomes available.
    public void enqueueAction(int action) throws InterruptedException {
        actionQueue.put(action);
    }

    public synchronized boolean addAction(int action) {
        if (actionQueue.size() < 3) {
            try {
                actionQueue.put(action); // Add action to the queue
                return true; // Action added successfully
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return false; // Queue is full, action not added
    }

    /** The main player thread of each player starts here (main loop for the player thread).**/
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        if (!human) createArtificialIntelligence();
        //Added
        //RandomKeyPress();
        while (!terminate) {
            // TODO implement main player loop
            try{
                if(!penaltyActive)
                {
                    int slot=actionQueue.take();
                    doAction(slot);
                }
                Thread.sleep(10);
            } catch(InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
        ///
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                // TODO implement player key press simulator
                try {
                    keyPressed((int) (Math.random() * 12));
                    if(!penaltyActive)
                    {
                        int slot=actionQueue.take();
                        doAction(slot);
                    }
                    synchronized (this) { wait(10); }
                } catch (InterruptedException ignored) {}
            }
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    // private void RandomKeyPress()
    // {
    //     int[] keys=env.config.playerKeys(id);
    //     int ranKey = (int) (Math.random() * 12);
    //     //keyPressed(ranKey);
    //     for(int i=0;i<keys.length;i++)
    //     {
    //         System.out.println(keys[i]);
    //     }
    // }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        terminate = true;
    }

    public int getSlotWithTokens(int i){
        return slotsWithTokens[i];
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
        for (int i = 0; i < 3 ; i++){
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

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // TODO implement
        if(!penaltyActive)
            actionQueue.offer(slot);
    }

    private void doAction(int slot)
    {
            if(!penaltyActive) {
                if(tokensPlaced < 3) {
                    if (!table.tokenPlaced.get(id).contains(slot) &&  (table.slotToCard[slot] != null)) {
                        table.placeToken(id, slot);
                        slotsWithTokens[tokensPlaced] = slot;
                        tokensPlaced++;
                    } else {
                        if(table.removeToken(id, slot)) {
                           emptyToken(valToIndex(slot));
    //                        tokensPlaced--;
    //                        slotsWithTokens[tokensPlaced] = -1;
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
        for(int i=0;i<3;i++)
        {
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
                env.ui.setFreeze(id, remainingTime+500);
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
        //penaltyActive=false;
    }

    /**Penalize a player and perform other related actions.**/
    public void penalty() {
        // TODO implement
        penaltyActive = true;
        for(int i=0;i<3;i++)
        {
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
                env.ui.setFreeze(id, remainingTime+500);
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
