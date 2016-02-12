package org.alexsem.cc.model;

public class Card {

    public static final int HERO_MAX = 13;

    public enum Type {
        HERO, MONSTER, MONSTER_TAMED, WEAPON, SHIELD, POTION, COIN, ABILITY
    }

    public enum Ability {
        SAP("Push back a dungeon card into the deck"),
        VANISH("Redraw all dungeon cards"),
        LEECH("Gain life equal to the value of ther selected dungeon card"),
        SACRIFICE("Attack a monster for the amount of player life missing"),
        POTIONIZE("Transform and item card into a random potion card"),
        KILLER("Remove a damaged dungeon card"),
        EXCHANGE("Move a dungeon card back into the deck and replace it with one of your ability cards"),
        STEAL("If your backpack is empty add the next card from the deck into it"),
        LASH("Attack between 1 and 3 monster cards for 3"),
        BASH("Use an equipped shield as a weapon. Each attack the shield looses 1 durability"),
        REFLECT("Reflect the damage taken to a random dungeon card"),
        BETRAYAL("Force a monster to attack its neighbour cards"),
        REVIVE("After taking fatal damage, revive the player with 1 health"),
        FRENZY("Attack with your equipped weapon twice"),
        LUCKY("Remove up to 2 randomly selected dungeon cards"),
        TRADE("Sell any non monster card for the double of its value"),
        SWAP("Swap the value of the selected dungeon card with a random adjacent card"),
        MORPH("Transform a card into a random new card"),
        FORTIFY("Increase a cards value by 5"),
        MIDAS("Transform a card into a coin card. Monster cards halve their value, ability cards double it"),
        DEVOUR("Transform a card into a random ability card"),
        TRAP("Trap a dungeon card. This card does not need to be played"),
        LIFE("Raise the player life by 5. If it exceeds 13 raise the maximum life"),
        BLEED("Collect 1 gold for each point of damage taken in one turn"),
        //TODO WORSHIP("Redraw 4 random monster cards and gain their values in gold"),
        BLOODPACT("Swap the player health with a monster card"),
        BOUNTY("Collect 3 gold for each monster slain bigger or equal to 10"),
        EQUALIZE("Give the adjacent cards the value of the selected dungeon card"),
        DIGGER("Shuffle 3 randomly selected removed cards back into the deck"),
        MIRROR("Duplicate a dungeon card and shuffle it back into the deck"),
        POISON("Use a potion as a weapon, adjacent cards receive half the damage"),
        DOOM("Remove all cards and reduce the players life to 1"),
        BRIBE("Remove a dungeon card and pay its value in gold"),
        STAB("Trigger redraw. It will hit a random dungeon card"),
        //TODO        HEIST("Shuffle the last 3 cards sold to shop into the deck"),
        TAME("Equip a monster as a sword or shield"),
        FEAST("Raise a dungeon cards value by the values of its adjacent cards"),
        CHAOS("All cards including the player randomly swap their values"), //TODO test
        FAITH("Double the value of the next 3 cards form the top of the deck"),
        CHAMPION("Exit the dungeon victorious");

        Ability(String description) {
            this.description = description;
        }

        private String description;

        public String getDescription() {
            return description;
        }
    }

    private static final String[] mobNames = {"", "", "PLAGUE", "CROW", "FIRELAMB", "SLIME", "INCUBUS", "GOBLIN", "SPIDER", "TROLL", "SOULEATER"};
    private static final Type[] types = Type.values();
    private static final Ability[] abilities = Ability.values();

    private Type type;
    private int value;
    private int initialValue;
    private boolean active;
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
        if (type == Type.MONSTER && value >= 10) {
            setAbility(Ability.BOUNTY);
        }
    }

    public int getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(int initialValue) {
        this.initialValue = initialValue;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
        if (type == Type.ABILITY && ability != null) {
            this.setName(this.getAbility().name().toUpperCase());
            switch (this.getAbility()) {
                case LEECH:
                case LASH:
                case BOUNTY:
                case FAITH:
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
        this.setInitialValue(this.getValue());
    }

    public static Card getHero() {
        Card card = new Card();
        card.setType(Type.HERO);
        card.setValue(HERO_MAX);
        card.setInitialValue(HERO_MAX);
        card.setActive(true);
        card.setName("");
        return card;
    }

    public static Card getSpecial() {
        Card card = new Card();
        card.setType(Type.ABILITY);
        card.setActive(true);
        card.setAbility(abilities[(int) (Math.random() * abilities.length)]);
        return card;
    }

    public static Card getOther(Type type, int value) {
        Card card = new Card();
        card.setType(type);
        card.setValue(value);
        card.setInitialValue(value);
        card.setActive(true);
        if (type == Type.MONSTER) {
            card.setName(mobNames[card.getValue()]);
        } else {
            card.setName("");
        }
        return card;
    }

    public static Card random() {
        Type type = types[(int) (Math.random() * (types.length - 1)) + 1];
        switch (type) {
            case HERO:
                return getHero();
            case MONSTER:
            case POTION:
            case COIN:
                return getOther(type, (int) (Math.random() * 9) + 2);
            case WEAPON:
            case SHIELD:
                return getOther(type, (int) (Math.random() * 6) + 2);
            case ABILITY:
                return getSpecial();
            default:
                return null;
        }
    }

    /**
     * Create new card by cloning the existing one
     * @param source Card to clone
     * @return Cloned card
     */
    public static Card clone(Card source) {
        Card card = new Card();
        card.setType(source.getType());
        card.setAbility(source.getAbility());
        card.setName(source.getName());
        card.setValue(source.getValue());
        card.setInitialValue(source.getInitialValue());
        card.setActive(source.isActive());
        return card;
    }


    /**
     * Restore initial card state
     */
    public void restoreState() {
        this.setValue(this.getInitialValue());
        this.setActive(true);
        if (this.getType() != Type.ABILITY) {
            this.setAbility(null);
        }
    }


}
