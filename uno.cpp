#include <iostream>
#include <vector>
using namespace std;

// =================== CARD CLASS ===================
// Represents each UNO card (Encapsulation)
class Card {
    string color;
    string value;
public:
    Card(string c, string v) {
        color = c;
        value = v;
    }

    string getColor() { return color; }
    string getValue() { return value; }

    void displayCard() {
        cout << color << " " << value;
    }
};

// =================== PLAYER CLASS ===================
// Player HAS multiple cards (Composition)
class Player {
    string name;
    vector<Card> hand;
public:
    Player(string n) {
        name = n;
    }

    string getName() { return name; }

    void addCard(Card c) {
        hand.push_back(c);
    }

    bool hasWon() {
        return hand.empty();
    }

    void showHand() {
        cout << "\n🃏 " << name << "'s Deck:\n";
        for (int i = 0; i < hand.size(); i++) {
            cout << i + 1 << ". ";
            hand[i].displayCard();
            cout << endl;
        }
    }

    // Function for playing a card
    void playCard(int index, Card &topCard) {
        if (index >= 0 && index < hand.size()) {
            Card chosen = hand[index];
            if (chosen.getColor() == topCard.getColor() ||
                chosen.getValue() == topCard.getValue()) {
                cout << "\n🎯 " << name << " played ";
                chosen.displayCard();
                cout << "!\n";
                topCard = chosen;
                hand.erase(hand.begin() + index);
            } else {
                cout << "\n❌ Invalid move! Must match color or number.\n";
            }
        }
    }
};

// =================== GAME CLASS ===================
// Abstraction: Handles overall game flow
class Game {
    vector<Player> players;
    Card topCard;
public:
    Game() : topCard("Red", "0") {} // Default starting card

    // Setup players and their decks
    void setupGame() {
        int numPlayers, numCards;
        cout << "Enter number of players: ";
        cin >> numPlayers;
        cout << "Enter number of cards per player: ";
        cin >> numCards;

        // Create players
        for (int i = 0; i < numPlayers; i++) {
            string name;
            cout << "\nEnter name of Player " << i + 1 << ": ";
            cin >> name;
            Player p(name);

            // Input cards for each player
            for (int j = 0; j < numCards; j++) {
                string c, v;
                cout << "Enter color and value of card " << j + 1 << " (e.g., Red 5): ";
                cin >> c >> v;
                p.addCard(Card(c, v));
            }
            players.push_back(p);
        }

        cout << "\n🔥 Game Setup Complete! 🔥\n";
        cout << "Starting Top Card: ";
        topCard.displayCard();
        cout << "\n";
    }

    // Display top card
    void showTopCard() {
        cout << "\n🔝 Current Top Card: ";
        topCard.displayCard();
        cout << "\n";
    }

    // Start playing UNO
    void startGame() {
        int turn = 0;
        bool gameOver = false;

        while (!gameOver) {
            Player &current = players[turn % players.size()];
            cout << "\n===============================";
            cout << "\n🚨 " << current.getName() << "'s TURN 🚨";
            cout << "\n===============================\n";

            showTopCard();
            current.showHand();

            int choice;
            cout << "\n👉 Choose an option:\n";
            cout << "1. Play a card\n";
            cout << "2. Skip turn\n";
            cout << "Enter choice: ";
            cin >> choice;

            switch (choice) {
                case 1: {
                    int index;
                    cout << "Enter card number to play: ";
                    cin >> index;
                    current.playCard(index - 1, topCard);
                    break;
                }
                case 2:
                    cout << current.getName() << " skipped the turn! 😴\n";
                    break;
                default:
                    cout << "Invalid choice!\n";
            }

            if (current.hasWon()) {
                cout << "\n🏆🎉 " << current.getName() << " WINS THE GAME! 🎉🏆\n";
                gameOver = true;
                break;
            }

            // End of turn message
            cout << "\n✨ End of " << current.getName() << "'s turn! ✨\n";
            cout << "-------------------------------------\n";

            turn++; // Move to next player
        }
    }
};

// =================== MAIN FUNCTION ===================
// Menu-driven program (switch case)
int main() {
    cout << "=========================\n";
    cout << "🎮 UNO GAME USING OOP 🎮\n";
    cout << "=========================\n";

    Game g;
    int choice;

    do {
        cout << "\n====== MAIN MENU ======\n";
        cout << "1. Setup Game\n";
        cout << "2. Start Playing\n";
        cout << "3. Exit\n";
        cout << "Enter your choice: ";
        cin >> choice;
        switch (choice) {
            case 1:
                g.setupGame();
                break;
            case 2:
                g.startGame();
                break;
            case 3:
                cout << "\n👋 Thanks for playing UNO! See you again! 👋\n";
                break;
            default:
                cout << "Invalid choice! Try again.\n";
        }
    } while (choice != 3);
    return 0;
}
