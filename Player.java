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
    public int[] slotsWithTokens = new int[3];

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
//        while (!terminate) {
            // TODO implement main player loop
//            try {
//                int slot = actionQueue.poll(1, TimeUnit.MILLISECONDS); // Wait for action with timeout
//                // Checks if an action was successfully retrieved from the queue
//                if (slot != -1) {
//                    keyPressed(slot);
//                    if (table.tokenInSlot[id] == 3) {
//                        // Notify dealer and wait for validation
//
//                    }
//                }
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }
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
                    synchronized (this) { wait(); }
                } catch (InterruptedException ignored) {}
            }
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        terminate = true;
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // TODO implement
        if(!penaltyActive) {
            if(tokensPlaced < 3) {
                if (!table.tokenPlaced.get(id).contains(slot)) {
                    table.placeToken(id, slot);
                    slotsWithTokens[tokensPlaced] = slot;
                    tokensPlaced++;
                }
                else
                {
                    table.removeToken(id, slot);
                    tokensPlaced--;
                    slotsWithTokens[tokensPlaced]=-1;
                }
            }

            // if (tokensPlaced == 3 && !env.util.testSet(slotsWithTokens)) {
            //     // Start penalty time
            //     //penalty();
            //     for(int i=0;i<3;i++)
            //     {
            //         table.removeToken(id, slotsWithTokens[i]);
            //     }
            //     tokensPlaced = 0;

            // } else if (tokensPlaced == 3 && env.util.testSet(slotsWithTokens)) {
            //     // Start score time
            //    // score();
            //    for(int i=0;i<3;i++)
            //    {
            //        table.removeToken(id, slotsWithTokens[i]);
            //    }
            //     tokensPlaced = 0;
            // }
        }
    }

    /** Award a point to a player and perform other related actions.
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.**/

    public void point() {
        // TODO implement
        score++;
        for(int i=0;i<3;i++)
        {
            table.removeToken(id, slotsWithTokens[i]);
            tokensPlaced--;
        }
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, score);
        try {
            Thread.sleep(env.config.pointFreezeMillis);

            env.ui.setFreeze(id, env.config.pointFreezeMillis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    /**Penalize a player and perform other related actions.**/
    public synchronized void penalty() {
        // TODO implement
        penaltyActive = true;
        for(int i=0;i<3;i++)
        {
            table.removeToken(id, slotsWithTokens[i]);
            tokensPlaced--;
        }
        try {
            long freezeTimer = env.config.penaltyFreezeMillis;
            env.ui.setFreeze(id,freezeTimer);
            Thread.sleep(freezeTimer);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        /*try{
            playerThread.wait(freezeTimer);
            env.ui.setFreeze(id, freezeTimer);
        }
        catch(InterruptedException ignored){
            playerThread.currentThread().interrupt();
        }*/
        penaltyActive = false;
    }

    public int score() {
        return score;
    }
}
