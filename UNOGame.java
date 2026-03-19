package SY_OOPM;

import java.util.*;

/*
 Improved UNO-like game (single-file)
 - 2-4 players
 - Deck: 2 copies of each color+value (0-9, Skip, Reverse, Draw2) and 4 Wild + 4 Draw4
 - Random draw (card removed from deck)
 - Discard pile recycling (keep top card)
 - Options each turn: Play a card OR Draw a card. Additionally player may choose to "Say UNO" for that turn.
 - Say UNO rules:
     * Player may declare UNO in same turn as playing/drawing.
     * If player declares UNO and after their action they have exactly 1 card → success message.
     * If player declares UNO incorrectly (has >1 card after action) → penalty: draw 2 cards.
     * If player fails to declare UNO while they have exactly 1 card after action → penalty: draw 2 cards.
 - After drawing a card, the player's hand (their "deck") is displayed immediately.
 - NEW: Each turn displays the number of cards every player has.
*/

class Card {
    protected String color; // "Red", "Green", "Blue", "Yellow", or "Wild"
    protected String value; // "0"-"9", "Skip", "Reverse", "Draw2", "Wild", "Draw4"

    public Card(String color, String value) {
        this.color = color;
        this.value = value;
    }

    public String getColor() { return color; }
    public String getValue() { return value; }

    // Playable if same color or same value or if the card is a Wild
    public boolean isPlayableOn(Card top) {
        if (top == null) return true;
        return this.color.equalsIgnoreCase("Wild") ||
               this.color.equalsIgnoreCase(top.color) ||
               this.value.equalsIgnoreCase(top.value);
    }

    @Override
    public String toString() {
        return color + " " + value;
    }
}

class Deck {
    private ArrayList<Card> cards = new ArrayList<>();
    private Random rnd = new Random();

    public Deck() { build(); shuffle(); }

    // Build deck with required counts
    private void build() {
        String[] colors = {"Red", "Green", "Blue", "Yellow"};
        String[] numberAndActions = {
            "0","1","2","3","4","5","6","7","8","9",
            "Skip","Reverse","Draw2"
        };
        // For each color add two copies of each number/action
        for (String color : colors) {
            for (String val : numberAndActions) {
                cards.add(new Card(color, val));
                cards.add(new Card(color, val));
            }
        }
        // 4 Wild and 4 Draw4 (color "Wild")
        for (int i = 0; i < 4; i++) {
            cards.add(new Card("Wild", "Wild"));
            cards.add(new Card("Wild", "Draw4"));
        }
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    // Draw a random card and remove it from deck
    public Card drawRandom() {
        if (cards.isEmpty()) return null;
        return cards.remove(rnd.nextInt(cards.size()));
    }

    public boolean isEmpty() { return cards.isEmpty(); }

    public int size() { return cards.size(); }

    // Refill from discard pile (except top card). Discard list will be mutated:
    // all cards moved into deck, discardPile left with single top card.
    public void refillFromDiscard(ArrayList<Card> discardPile) {
        if (cards.isEmpty() && discardPile.size() > 1) {
            Card top = discardPile.remove(discardPile.size() - 1); // keep top
            cards.addAll(discardPile);
            discardPile.clear();
            discardPile.add(top);
            shuffle();
            System.out.println("Main deck was empty → refilled from discard pile (excluding top card).");
        }
    }
}

class Player {
    private String name;
    private ArrayList<Card> hand = new ArrayList<>();

    public Player(String name) { this.name = name; }

    public String getName() { return name; }
    public ArrayList<Card> getHand() { return hand; }

    public void addCard(Card c) { if (c != null) hand.add(c); }

    public Card playCard(int index) {
        if (index < 0 || index >= hand.size()) return null;
        return hand.remove(index);
    }

    public boolean hasPlayable(Card top) {
        for (Card c : hand) if (c.isPlayableOn(top)) return true;
        return false;
    }

    public void showHand() {
        System.out.println(name + "'s hand (" + hand.size() + "):");
        for (int i = 0; i < hand.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + hand.get(i));
        }
    }

    public boolean isEmpty() { return hand.isEmpty(); }
}

public class oopm_ia2 {
    private Deck deck;
    private ArrayList<Card> discard = new ArrayList<>();
    private ArrayList<Player> players = new ArrayList<>();
    private Scanner sc = new Scanner(System.in);
    private int currentIdx = 0;
    private int direction = 1; // 1 clockwise, -1 counterclockwise
    private Random rnd = new Random();

    public static void main(String[] args) {
        oopm_ia2 g = new oopm_ia2();
        g.run();
    }

    private void run() {
        setup();
        gameLoop();
    }

    private void setup() {
        deck = new Deck();

        System.out.println("Welcome to UNO (improved).");
        int n;
        while (true) {
            System.out.print("Enter number of players (2-4): ");
            n = safeInt();
            if (n >= 2 && n <= 4) break;
            System.out.println("Please enter a number between 2 and 4.");
        }

        System.out.print("Enter number of starting cards per player: ");
        int startCards = Math.max(1, safeInt());

        sc.nextLine(); // consume newline

        for (int i = 1; i <= n; i++) {
            System.out.print("Enter name of player " + i + ": ");
            String name = sc.nextLine().trim();
            if (name.isEmpty()) name = "Player" + i;
            players.add(new Player(name));
        }

        // Deal startCards randomly (removes from deck)
        for (int k = 0; k < startCards; k++) {
            for (Player p : players) {
                deck.refillFromDiscard(discard);
                Card c = deck.drawRandom();
                p.addCard(c);
            }
        }

        // Place initial top card: must not be Wild or Draw2/Draw4 (we'll treat Draw2/Draw4 as not allowed to start)
        Card first;
        do {
            deck.refillFromDiscard(discard);
            first = deck.drawRandom();
            if (first == null) break;
            if (first.getColor().equalsIgnoreCase("Wild")
                    || first.getValue().equalsIgnoreCase("Draw2")
                    || first.getValue().equalsIgnoreCase("Draw4")) {
                // put into discard and continue
                discard.add(first);
                first = null;
            }
        } while (first == null);

        if (first == null) {
            if (!discard.isEmpty()) first = discard.remove(discard.size() - 1);
            else first = new Card("Red", "0"); // fallback
        }
        discard.add(first);
        System.out.println("Starting top card: " + topCard());
    }

    private void gameLoop() {
        while (true) {
            deck.refillFromDiscard(discard);

            Player cur = players.get(currentIdx);
            System.out.println("\n-----------------------------------");
            System.out.println("Top card: " + topCard());
            System.out.println("Current player: " + cur.getName());

            // == NEW: Display number of cards every player has at start of turn ==
            System.out.println("\n--- Player Card Counts ---");
            for (Player p : players) {
                System.out.println(p.getName() + " :- " + p.getHand().size() + " cards");
            }
            System.out.println("---------------------------");

            cur.showHand();

            // Ask if player wants to declare UNO this turn (this can be used after they play a card)
            boolean declaredUnoThisTurn = askDeclareUno(cur);

            boolean turnDone = false;
            while (!turnDone) {
                System.out.println("\nOptions (you may only choose one action; saying UNO is separate):");
                System.out.println("  1. Play a card");
                System.out.println("  2. Draw a card");
                System.out.print("Choose option (1 or 2): ");
                int option = safeInt();
                if (option == 1) {
                    // Play a card by serial number
                    if (cur.getHand().isEmpty()) {
                        System.out.println("You have no cards to play.");
                        // turn ends
                        turnDone = true;
                        break;
                    }
                    System.out.print("Enter serial number of card to play: ");
                    int serial = safeInt() - 1; // user sees 1-based
                    Card attempted = null;
                    if (serial >= 0 && serial < cur.getHand().size()) {
                        attempted = cur.getHand().get(serial);
                    } else {
                        System.out.println("Invalid serial number.");
                        continue; // ask again
                    }

                    Card top = topCard();
                    if (!attempted.isPlayableOn(top)) {
                        System.out.println("You cannot play that card. It must match color or value, or be Wild.");
                        continue; // ask again
                    }

                    // Valid play: remove from player's hand and put into discard
                    Card played = cur.playCard(serial);
                    discard.add(played);
                    System.out.println(cur.getName() + " played: " + played);

                    // Apply effect if action
                    applyEffect(played);

                    // After play check UNO declarations / penalties
                    handleUnoAfterAction(cur, declaredUnoThisTurn);

                    // Check win
                    if (cur.isEmpty()) {
                        System.out.println("\n*** " + cur.getName() + " WINS! ***");
                        return;
                    }

                    // After a successful play, turn ends unless effect moved players already
                    turnDone = true;
                }
                else if (option == 2) {
                    // Draw a random card (remove from deck)
                    deck.refillFromDiscard(discard);
                    Card drawn = deck.drawRandom();
                    if (drawn == null) {
                        System.out.println("No cards available to draw.");
                        // turn ends
                        turnDone = true;
                    } else {
                        cur.addCard(drawn);
                        System.out.println("You drew: " + drawn);
                        // Display player's hand after draw (as requested)
                        cur.showHand();
                        System.out.println("Remaining cards in main deck: " + deck.size());

                        // After drawing, player may play a playable card during the same turn:
                        if (!cur.hasPlayable(topCard())) {
                            System.out.println("No playable card after draw. Turn ends.");
                            // After end of turn, handle UNO declarations
                            handleUnoAfterAction(cur, declaredUnoThisTurn);
                            turnDone = true;
                        } else {
                            // allow player to choose again (play or draw) in same turn
                            System.out.println("You have a playable card. You may play it now or end turn.");
                            // We let loop continue so user can choose to play; but they cannot draw again (we keep options same)
                            // If they choose to end the turn without playing, UNO penalties will be applied outside.
                            // Continue loop to allow play attempt.
                        }
                    }
                }
                else {
                    System.out.println("Invalid option. Choose 1 or 2.");
                }
            } // end per-turn loop

            System.out.print("Move to next player? (Press Enter) ");
            sc.nextLine();

            // Move to next player in current direction
            advanceToNext();
        }
    }

    // Ask player at start of turn whether they intend to declare UNO this turn (they can do it preemptively)
    private boolean askDeclareUno(Player p) {
        System.out.print("Do you want to declare UNO this turn? (y/n): ");
        String ans = sc.nextLine().trim().toLowerCase();
        while (!ans.equals("y") && !ans.equals("n") && !ans.equals("yes") && !ans.equals("no")) {
            System.out.print("Please enter y or n: ");
            ans = sc.nextLine().trim().toLowerCase();
        }
        return ans.startsWith("y");
    }

    // After a player's action (play or draw) handle UNO success/penalty logic
    private void handleUnoAfterAction(Player p, boolean declaredUnoThisTurn) {
        int handSize = p.getHand().size();
        if (declaredUnoThisTurn) {
            if (handSize == 1) {
                System.out.println(p.getName() + " declared UNO correctly! Good job!");
            } else if (handSize == 0) {
                // If they declared UNO but have 0 cards (they just won), no penalty; the win check happens elsewhere.
                System.out.println(p.getName() + " declared UNO but has no cards (they won).");
            } else {
                // Incorrect UNO declaration -> penalty
                System.out.println(p.getName() + " declared UNO incorrectly (has " + handSize + " cards). Penalty: draw 2 cards.");
                addPenaltyCards(p, 2);
            }
        } else {
            if (handSize == 1) {
                // Failed to declare UNO when required -> penalty
                System.out.println(p.getName() + " failed to declare UNO while having 1 card. Penalty: draw 2 cards.");
                addPenaltyCards(p, 2);
            }
            // otherwise nothing
        }
    }

    // Add n random cards from deck to player's hand (refilling if needed)
    private void addPenaltyCards(Player victim, int n) {
        deck.refillFromDiscard(discard);
        for (int i = 0; i < n; i++) {
            Card c = deck.drawRandom();
            if (c == null) {
                // try to refill once more
                deck.refillFromDiscard(discard);
                c = deck.drawRandom();
                if (c == null) {
                    System.out.println("No more cards to draw as penalty.");
                    return;
                }
            }
            victim.addCard(c);
        }
        System.out.println(victim.getName() + " now has " + victim.getHand().size() + " cards.");
    }

    // Apply action effects. NOTE: some actions move currentIdx (skip/draw effects),
    // so we carefully modify currentIdx or use advanceToNext().
    private void applyEffect(Card card) {
        String val = card.getValue();
        if (val.equalsIgnoreCase("Reverse")) {
            direction *= -1;
            System.out.println("Direction reversed.");
            // For 2 players, Reverse acts like Skip (switches turn)
            if (players.size() == 2) {
                System.out.println("Reverse acts as Skip with 2 players.");
                advanceToNext(); // effectively skip other player's immediate turn
            }
            else {
                // no immediate skip; just reverse direction. Next advance will follow new direction.
            }
        }
        else if (val.equalsIgnoreCase("Skip")) {
            System.out.println("Next player will be skipped.");
            advanceToNext(); // skip one player
            // after skip, next normal advance will go to the following player
        }
        else if (val.equalsIgnoreCase("Draw2")) {
            // Next player draws 2 and loses turn
            advanceToNext();
            Player victim = players.get(currentIdx);
            deck.refillFromDiscard(discard);
            for (int i = 0; i < 2; i++) {
                Card c = deck.drawRandom();
                if (c != null) victim.addCard(c);
            }
            System.out.println(victim.getName() + " drew 2 cards and their turn is skipped.");
            // after applying, move index to the player after the victim
            advanceToNext();
        }
        else if (val.equalsIgnoreCase("Draw4")) {
            // Next player draws 4 and loses turn. Also chooser should pick new color.
            advanceToNext();
            Player victim = players.get(currentIdx);
            deck.refillFromDiscard(discard);
            for (int i = 0; i < 4; i++) {
                Card c = deck.drawRandom();
                if (c != null) victim.addCard(c);
            }
            System.out.println(victim.getName() + " drew 4 cards and their turn is skipped.");
            // The player who played Draw4 gets to choose color. The Draw4 is top of discard so previous player chose.
            // We'll prompt for color now (the chooser is the previous player). For simplicity, prompt the current user:
            chooseColorForWild();
            // After effect, advance past the victim
            advanceToNext();
        }
        else if (val.equalsIgnoreCase("Wild")) {
            // Player must pick a color: we record this by replacing top of discard with a colored marker
            chooseColorForWild();
            // Next player's turn will proceed as usual (no extra skip)
            // advance is done by caller
        }
        else {
            // number card or normal card: just normal advance (done by caller)
        }
    }

    // Prompt player to choose a color; record it by replacing top of discard with same value but chosen color
    private void chooseColorForWild() {
        System.out.print("Choose color for Wild (Red/Green/Blue/Yellow): ");
        String color = sc.nextLine().trim();
        while (!isValidColor(color)) {
            System.out.print("Invalid color. Choose from Red/Green/Blue/Yellow: ");
            color = sc.nextLine().trim();
        }
        Card prevTop = discard.remove(discard.size() - 1);
        Card newTop = new Card(capitalize(color), prevTop.getValue()); // set chosen color
        discard.add(newTop);
        System.out.println("Color set to " + newTop.getColor() + ".");
    }

    private boolean isValidColor(String s) {
        if (s == null) return false;
        s = s.trim().toLowerCase();
        return s.equals("red") || s.equals("green") || s.equals("blue") || s.equals("yellow");
    }

    // Return top card (peek)
    private Card topCard() {
        if (discard.isEmpty()) return null;
        return discard.get(discard.size() - 1);
    }

    // Advance currentIdx by one in the active direction (wraps)
    private void advanceToNext() {
        currentIdx = (currentIdx + direction + players.size()) % players.size();
    }

    // Utility: safe int read
    private int safeInt() {
        while (true) {
            try {
                String line = sc.nextLine();
                if (line == null) return 0;
                line = line.trim();
                if (line.isEmpty()) continue;
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.print("Please enter a valid integer: ");
            }
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        s = s.toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
