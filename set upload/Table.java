package bguspl.set.ex;

import bguspl.set.Env;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)

    ///

    protected final Integer[] tokenInSlot;

    private HashMap <Integer,Vector<Integer>>tokenPlaced= new HashMap<Integer,Vector<Integer>>();
    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */

    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        /////
        tokenInSlot= new Integer[slotToCard.length];
        for(int i=0;i<tokenInSlot.length;i++)
        {
            tokenInSlot[i]=-1;
        }

    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        cardToSlot[card] = slot;
        slotToCard[slot] = card;
        env.ui.placeCard(card,slot);
        // TODO implement
        System.out.println( "Card : " + card + " placed in slot : " + slot);
    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        // TODO implement

        int card = slotToCard[slot];
        slotToCard[slot] = -1 ;
        if(card == -1){
            return;
        }
        cardToSlot[card] = -1 ;
        env.ui.removeCard(slot);
        System.out.println( "Card removed from the slot : " + slot);
    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public void placeToken(int player, int slot) {
        // TODO implement
        // if(tokenPlaced.get(player) == null){
        //     tokenPlaced.put(player,new Vector<Integer>());
        // }

        // if(slotToCard[slot] != 0){
        //     System.out.println("This token already occupied");
        //     return;
        // }
        // env.ui.placeToken(player,slot);
        // //slotToCard[slot] = player;
        // System.out.println("The token changed to : " + player);

        // tokenPlaced.get(player).add(slot);
        tokenInSlot[slot]=player;
        env.ui.placeToken(player, slot);
    }

    public  HashMap <Integer,Vector<Integer>>  returnTokens(){
        return  tokenPlaced;
    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public boolean removeToken(int player, int slot) {
        // TODO implement
        // if(slotToCard[slot] != player){
        //      System.out.println("Slot " + slot + " does not contains player " + player + "'s token.");
        //      return false;
        // }
        // env.ui.removeToken(player,slot);
        // //slotToCard[slot] = 0;
        // System.out.println("Slot " + slot + " is now removed " + player + "'s token.");
        // helpRemoveToken(tokenPlaced.get(player), slot);
        // return true;
        if(tokenInSlot[slot]==player)
        {
            tokenInSlot[slot]=-1;
            env.ui.removeTokens(slot);
            return true;
        }
        return false;
    }

    public void helpRemoveToken(Vector<Integer> vec , int slot ){
        for(int i = 0 ; i < vec.size() ; i ++){
            if(vec.elementAt(i).intValue() == slot){
                vec.remove(i);
                break;
            }
        }
    }
}
