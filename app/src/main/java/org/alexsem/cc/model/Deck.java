package org.alexsem.cc.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Deck {

    private List<Card> cards;

    public int size() {
        return cards.size();
    }

    /**
     * Generate completely new deck
     * @return New deck
     */
    @Deprecated
    public static Deck generateRandom() {
        Deck deck = new Deck();
        List<Card> cards = new ArrayList<>();
        Map<Card.Type, Integer> limits = new HashMap<>();
        limits.put(Card.Type.MONSTER, 19);
        limits.put(Card.Type.WEAPON, 6);
        limits.put(Card.Type.SHIELD, 6);
        limits.put(Card.Type.POTION, 9);
        limits.put(Card.Type.COIN, 9);
        limits.put(Card.Type.ABILITY, 5);
        Set<Card.Ability> specials = new HashSet<>();
        for (int i = 0; i < 54; i++) {
            boolean ok = false;
            do {
                Card card = Card.random();
                Card.Type type = card.getType();
                if (limits.get(type) > 0) {
                    if (type == Card.Type.ABILITY) {
                        if (!specials.contains(card.getAbility())) { //New special card
                            specials.add(card.getAbility());
                        } else { //Already added
                            continue;
                        }
                    }
                    cards.add(card);
                    limits.put(type, limits.get(type) - 1);
                    ok = true;
                }
            } while (!ok);
        }
        Collections.shuffle(cards);
        deck.cards = cards;
        return deck;
    }

    /**
     * Generate completely new deck
     * @return New deck
     */
    public static Deck generateFixed() {
        //Initialize
        Deck deck = new Deck();
        List<Card> cards = new ArrayList<>();
        //Generate Monsters & Potions & Coins
        for (int i = 2; i <= 10; i++) {
            cards.add(Card.getOther(Card.Type.MONSTER, i));
            cards.add(Card.getOther(Card.Type.MONSTER, i));
            cards.add(Card.getOther(Card.Type.POTION, i));
            cards.add(Card.getOther(Card.Type.COIN, i));
        }
        cards.add(Card.getOther(Card.Type.MONSTER, 10));
        //Generate Swords & Shields
        for (int i = 2; i <= 7; i++) {
            cards.add(Card.getOther(Card.Type.WEAPON, i));
            cards.add(Card.getOther(Card.Type.SHIELD, i));
        }
        //Generate Abilities
        Set<Card.Ability> specials = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            Card card;
            do {
                card = Card.getSpecial();
            } while (specials.contains(card.getAbility()));
            specials.add(card.getAbility());
            cards.add(card);
        }
        //Check for long sequences of similar cards
        boolean deckOk;
        do {
            Collections.shuffle(cards);
            deckOk = true;
            quick:
            for (int i = 0; i < cards.size(); i++) {
                switch (cards.get(i).getType()) {
                    case MONSTER:
                    case WEAPON:
                    case SHIELD:
                    case POTION:
                    case COIN:
                        if (i > 1 && cards.get(i - 1).getType() == cards.get(i).getType() && cards.get(i - 2).getType() == cards.get(i).getType()) {
                            deckOk = false;
                            break quick;
                        }
                        break;
                    case ABILITY:
                        if (i > 1 && (cards.get(i - 1).getType() == cards.get(i).getType() || cards.get(i - 2).getType() == cards.get(i).getType())) {
                            deckOk = false;
                            break quick;
                        }
                        break;
                }
                if (i > 3 && cards.get(i).getType() != Card.Type.MONSTER && cards.get(i - 1).getType() != Card.Type.MONSTER && cards.get(i - 2).getType() != Card.Type.MONSTER && cards.get(i - 3).getType() != Card.Type.MONSTER && cards.get(i - 4).getType() != Card.Type.MONSTER) {
                    deckOk = false;
                    break;
                }
            }
        } while (!deckOk);
        //Finalize
        deck.cards = cards;
        return deck;
    }

    /**
     * Remove card from the top of the deck (if possible)
     * @return Top card or null if no cards in the deck
     */
    public Card deal() {
        if (cards == null || cards.size() == 0) {
            return null;
        }
        Card top = cards.get(cards.size() - 1);
        cards.remove(top);
        return top;
    }

    /**
     * Remove card from the specified position in the deck (if possible)
     * @param position Position of card to deal
     * @return Requested card or null if no cards in the deck
     */
    public Card deal(int position) {
        if (cards == null || position < 0 || position > cards.size() - 1) {
            return null;
        }
        Card card = cards.get(position);
        cards.remove(card);
        return card;
    }

    /**
     * Return card to the bottom of the deck
     * @param card Card to add to the deck
     */
    public void receive(Card card) {
        if (cards != null) {
            cards.add(0, card);
        }
    }

    /**
     * Find card of specified type (from top down to bottom)
     * @param type Type of card to file
     * @return position of card or -1
     */
    public int find(Card.Type type) {
        for (int i = cards.size() - 1; i > -1; i--) {
            if (cards.get(i).getType() == type) {
                return i;
            }
        }
        return -1;
    }

}
