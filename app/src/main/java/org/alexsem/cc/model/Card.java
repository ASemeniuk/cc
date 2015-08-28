package org.alexsem.cc.model;

public class Card {

    public static final int HERO_MAX = 13;

    public enum Type {
        FLEX, FEAR, HIT, BLOCK, DRINK, CASH, ZAP
    }

    public enum Ability {
        SAP, VANISH, LEECH, SACRIFICE, POTIONIZE, KILLER, EXCHANGE, STEAL, LASH, BASH, REFLECT,/* BETRAYAL,*/ REVIVE, FRENZY, LUCKY,
        TRADE, /*SWAP, MORPH,*/ FORTIFY,/*MIDAS, DEVOUR, TRAP,*/ LIFE,/* BLEED, SUICIDE, BLOODPACT, BOUNTY, EQUALIZE, DIGGER, MIRROR*/
    }

    private static final String[] mobNames = {"", "", "PLAGUE", "CROW", "FIRELAMB", "SLIME", "INCUBUS", "GOBLIN", "SPIDER", "TROLL", "SOULEATER"};
    private static final Type[] types = Type.values();
    private static final Ability[] abilities = Ability.values();

    private Type type;
    private int value;
    private boolean active;
    private boolean wounded;
    private String name;
    private Ability ability = null;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isWounded() {
        return wounded;
    }

    public void setWounded(boolean wounded) {
        this.wounded = wounded;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Ability getAbility() {
        return ability;
    }

    public void setAbility(Ability ability) {
        this.ability = ability;
        this.setName(this.getAbility().name().toUpperCase());
        switch (this.getAbility()) {
            case LEECH:
            case LASH:
                this.setValue(3);
                break;
            case FORTIFY:
            case LIFE:
                this.setValue(5);
                break;
            default:
                this.setValue(0);
                break;
        }
    }

    public static Card getHero() {
        Card card = new Card();
        card.setType(Type.FLEX);
        card.setValue(HERO_MAX);
        card.setActive(true);
        card.setWounded(false);
        card.setName("");
        return card;
    }

    public static Card getSpecial() {
        Card card = new Card();
        card.setType(Type.ZAP);
        card.setActive(true);
        card.setWounded(false);
        card.setAbility(abilities[(int) (Math.random() * abilities.length)]);
//        card.setAbility(abilities[(int) (Math.random() * 5) + 5]);
        return card;
    }

    public static Card getOther(Type type, int value) {
        Card card = new Card();
        card.setType(type);
        card.setValue(value);
        card.setActive(true);
        card.setWounded(false);
        if (type == Type.FEAR) {
            card.setName(mobNames[value]);
        } else {
            card.setName("");
        }
        return card;
    }

    @Deprecated
    public static Card random() {
        Card card = new Card();
        card.setType(types[(int) (Math.random() * (types.length - 1)) + 1]);
        switch (card.getType()) {
            case FEAR:
            case DRINK:
            case CASH:
                card.setValue((int) (Math.random() * 9) + 2);
                break;
            case HIT:
            case BLOCK:
                card.setValue((int) (Math.random() * 6) + 2);
                break;
            case ZAP:
                card = getSpecial();
                break;
        }
        card.setActive(true);
        card.setWounded(false);
        return card;
    }


}
