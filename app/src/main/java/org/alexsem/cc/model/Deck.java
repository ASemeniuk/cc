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
        limits.put(Card.Type.FEAR, 19);
        limits.put(Card.Type.HIT, 6);
        limits.put(Card.Type.BLOCK, 6);
        limits.put(Card.Type.DRINK, 9);
        limits.put(Card.Type.CASH, 9);
        limits.put(Card.Type.ZAP, 5);
        Set<Card.Ability> specials = new HashSet<>();
        for (int i = 0; i < 54; i++) {
            boolean ok = false;
            do {
                Card card = Card.random();
                Card.Type type = card.getType();
                if (limits.get(type) > 0) {
                    if (type == Card.Type.ZAP) {
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
        //Mobs & Potions & Coins
        for (int i = 2; i <= 10; i++) {
            cards.add(Card.getOther(Card.Type.FEAR, i));
            cards.add(Card.getOther(Card.Type.FEAR, i));
            cards.add(Card.getOther(Card.Type.DRINK, i));
            cards.add(Card.getOther(Card.Type.CASH, i));
        }
        cards.add(Card.getOther(Card.Type.FEAR, (int) (Math.random() * 9) + 2));
        //Swords & Shields
        for (int i = 2; i <= 7; i++) {
            cards.add(Card.getOther(Card.Type.HIT, i));
            cards.add(Card.getOther(Card.Type.BLOCK, i));
        }
        //Abilities
        Set<Card.Ability> specials = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            Card card;
            do {
                card = Card.getSpecial();
            } while (specials.contains(card.getAbility()));
            specials.add(card.getAbility());
            cards.add(card);
        }
        //Finalize
        Collections.shuffle(cards);
        Collections.shuffle(cards);
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
     * Return card to the bottom of the deck
     * @param card Card to add to the deck
     */
    public void receive(Card card) {
        if (cards != null) {
            cards.add(0, card);
        }
    }

}
