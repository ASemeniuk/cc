package org.alexsem.cc.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.alexsem.cc.model.Animation;
import org.alexsem.cc.model.Card;
import org.alexsem.cc.model.Deck;

public class BoardView extends View {

    private static final int COLOR_BG_CARD = 0xffc0c0c0;
    private static final int COLOR_BG_TINT = 0xa0000000;
    private static final int COLOR_REGULAR = 0xff000000;
    private static final int COLOR_EMPH = 0xff922b22;
    private static final int COLOR_SPECIAL = 0xff2cc5c6;

    private final int LOC_LEFT_HAND = 10;
    private final int LOC_HERO = 11;
    private final int LOC_RIGHT_HAND = 12;
    private final int LOC_BACKPACK = 13;

    private float STROKE_WIDTH = 2;

    private int mFontSize;
    private int mFontPadding;
    private int mTopPadding;
    private int mCanvasWidth;
    private int mCanvasHeight;
    private int mEpsDrag;
    private int mDragReturnSpeed;

    private Paint mPaint;
    private TextPaint mTextPaint;

    private int mTouchedLocation = -1;
    private float mTouchedX;
    private float mTouchedY;
    private float mDragRelX = 0;
    private float mDragRelY = 0;
    private float mDragSpeedX = 0;
    private float mDragSpeedY = 0;
    private boolean isDragging = false;
    private int mDragReturnTicks = 0;
    private boolean isDiscarding = false;
    private Animation mDiscardAnimation;

    private Deck mDeck;
    private Box mDiscardBox;
    private Position[] mRowTop = new Position[4];
    private Position[] mRowBottom = new Position[4];
    private int mCoins;
    private int mHealthAddition;
    private boolean isFreshDeal;
    private boolean isNeedToReviveHero;
    private boolean isNeedToReflectDamage;
    private RectF mDeckPosition;

    private int mDealAnimationCount = 0;
    private Animation[] mDealAnimationTop = new Animation[4];
    private Animation[] mDealAnimationBottom = new Animation[4];
    private int mReceiveAnimationCount = 0;
    private Animation[] mReceiveAnimationTop = new Animation[4];
    private int mDropAnimationCount = 0;
    private Animation[] mDropAnimationBottom = new Animation[4];
    private boolean isHeroAnimated = false;
    private Animation mHeroAnimation = null;
    private int mCardAnimationCount = 0;
    private Animation[] mCardAnimationTop = new Animation[4];
    private Animation[] mCardAnimationBottom = new Animation[4];


    private boolean isBeginning = false;
    private boolean isMeasurementChanged = false;
    private boolean isGameOver = false;


    //----------------------------------------------------------------------------------------------

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void checkHardwareAcceleration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }
    }

    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        checkHardwareAcceleration();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mTextPaint = new TextPaint(new Paint());
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        int dpi = context.getResources().getDisplayMetrics().densityDpi;
        STROKE_WIDTH = STROKE_WIDTH * dpi / 160f;
        begin(); //Start
    }

    public void begin() {
        this.isBeginning = true;
        mDeck = Deck.generateFixed();
        for (int i = 0; i < 4; i++) {
            mRowTop[i] = new Position();
            mRowBottom[i] = new Position();
            mCardAnimationTop[i] = null;
            mCardAnimationBottom[i] = null;
            mDealAnimationTop[i] = null;
            mDealAnimationBottom[i] = null;
            mReceiveAnimationTop[i] = null;
            mDropAnimationBottom[i] = null;
        }
        mHeroAnimation = null;
        mDiscardAnimation = null;
        mDiscardBox = new Box();
        mCoins = 0;
        mHealthAddition = 0;
        mDealAnimationCount = 0;
        mReceiveAnimationCount = 0;
        mDropAnimationCount = 0;
        mCardAnimationCount = 0;
        isNeedToReflectDamage = false;
        isNeedToReviveHero = false;
        isHeroAnimated = false;
        isDragging = false;
        isDiscarding = false;
        isGameOver = false;
        isMeasurementChanged = true;
        invalidate();


//        Card card = Card.getSpecial(); //TODO
//        card.setAbility(Card.Ability.MIRROR);
//        mRowTop[0].setCard(card);
//        card = Card.getSpecial();
//        card.setAbility(Card.Ability.POTIONIZE);
//        mRowTop[1].setCard(card);
    }

    /**
     * Deal cards to missing positions of top row
     * All inactive cards from bottom row will be removed in process
     */
    private void dealTopRow() {
        for (int i = 0; i < 4; i++) {
            Card card = mRowBottom[i].getCard();
            if (card != null && !card.isActive()) {
                animateDropCard(card, i + 10);
                mRowBottom[i].setCard(null);
            }
            if (mRowTop[i].getCard() == null) {
                animateDealCard(mDeck.deal(), i);
            }
        }
        isFreshDeal = true;
    }

    //--------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (isGameOver || mDragReturnTicks > 0 || mDealAnimationCount > 0 || mReceiveAnimationCount > 0 || mDropAnimationCount > 0 || isHeroAnimated || mCardAnimationCount > 0 || isDiscarding) {
            return true;
        }
        float x = e.getX();
        float y = e.getY();
        Position pos;
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                for (int i = 0; i < 4; i++) {
                    pos = mRowTop[i];
                    if (pos != null && pos.contains(x, y) && canTouchThis(i)) {
                        mTouchedLocation = i;
                        mTouchedX = x;
                        mTouchedY = y;
                        invalidate();
                        break;
                    }
                    pos = mRowBottom[i];
                    if (pos != null && pos.contains(x, y) && canTouchThis(10 + i)) {
                        mTouchedLocation = 10 + i;
                        mTouchedX = x;
                        mTouchedY = y;
                        invalidate();
                        break;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (!isDragging && mTouchedLocation > -1) {
                    float dx = x - mTouchedX;
                    float dy = y - mTouchedY;
                    if (Math.sqrt(dx * dx + dy * dy) > mEpsDrag) {
                        isDragging = true;
                    }
                }
                if (isDragging) {
                    mDragRelX = x - mTouchedX;
                    mDragRelY = y - mTouchedY;
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
                if (isDragging) {
                    boolean received = false;
                    for (int i = 0; i < 4; i++) {
                        pos = mRowTop[i];
                        if (pos != null && pos.contains(x, y) && canReceiveThis(mTouchedLocation, i)) {
                            doReceive(mTouchedLocation, i);
                            received = true;
                            break;
                        }
                        pos = mRowBottom[i];
                        if (pos != null && pos.contains(x, y) && canReceiveThis(mTouchedLocation, i + 10)) {
                            doReceive(mTouchedLocation, i + 10);
                            received = true;
                            break;
                        }
                    }
                    if (mDiscardBox != null && mDiscardBox.contains(x, y) && canDiscardThis(mTouchedLocation)) {
                        doDiscard(mTouchedLocation);
                        received = true;
                    }
                    if (!received) {
                        float distance = (float) Math.sqrt(mDragRelX * mDragRelX + mDragRelY * mDragRelY);
                        mDragReturnTicks = (int) (distance / mDragReturnSpeed);
                        mDragSpeedX = mDragRelX / mDragReturnTicks;
                        mDragSpeedY = mDragRelY / mDragReturnTicks;
                    }
                }
                resetTouchFeedback();
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                resetTouchFeedback();
                invalidate();
                break;
        }
        return true;
    }

    /**
     * Removes any visual changes occurred after some item was touched
     */
    private void resetTouchFeedback() {
        isDragging = false;
        if (mDragReturnTicks <= 0 && mTouchedLocation > -1 && !isDiscarding) {
            mTouchedLocation = -1;
            invalidate();
        }
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Defines whether specific position can be touched
     * @param location Coordinate number of position
     * @return true if can be touched, false otherwise
     */
    private boolean canTouchThis(int location) {
        Card card = location >= 10 ? mRowBottom[location - 10].getCard() : mRowTop[location].getCard();
        if (card != null) {
            switch (card.getType()) {
                case FLEX:
                    return false;
                case FEAR:
                case HIT:
                case ZAP:
                    return true;
                case BLOCK:
                    return location != LOC_LEFT_HAND && location != LOC_RIGHT_HAND;
                case DRINK:
                case CASH:
                    return card.isActive();
            }
        }
        return false;
    }

    /**
     * Defines whether card can be received by specific position
     * @param source      Coordinate number of source card position
     * @param destination Coordinate number of dest card position
     * @return true or false
     */
    private boolean canReceiveThis(int source, int destination) {
        if (source == destination) {
            return false;
        }
        Card srcCard = source >= 10 ? mRowBottom[source - 10].getCard() : mRowTop[source].getCard();
        Card dstCard = destination >= 10 ? mRowBottom[destination - 10].getCard() : mRowTop[destination].getCard();
        switch (srcCard.getType()) {
            case FEAR:
                if (dstCard != null) {
                    return (dstCard.getType() == Card.Type.FLEX || (dstCard.getType() == Card.Type.BLOCK && (destination == LOC_LEFT_HAND || destination == LOC_RIGHT_HAND)));
                } else {
                    return false;
                }
            case HIT:
                if (dstCard != null) {
                    return (dstCard.getType() == Card.Type.FEAR && destination < 10 && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                } else {
                    return (source < 10 && destination == LOC_BACKPACK) || ((source < 10 || source == LOC_BACKPACK) && (destination == LOC_LEFT_HAND || destination == LOC_RIGHT_HAND));
                }
            case BLOCK:
                if (dstCard != null) {
                    return false;
                } else {
                    return (source < 10 && destination == LOC_BACKPACK) || ((source < 10 || source == LOC_BACKPACK) && (destination == LOC_LEFT_HAND || destination == LOC_RIGHT_HAND));
                }
            case DRINK:
                if (dstCard != null) {
                    return false;
                } else {
                    return (source < 10 && destination == LOC_BACKPACK) || ((source < 10 || source == LOC_BACKPACK) && (destination == LOC_LEFT_HAND || destination == LOC_RIGHT_HAND));
                }
            case CASH:
                if (dstCard != null) {
                    return false;
                } else {
                    return (destination == LOC_LEFT_HAND || destination == LOC_RIGHT_HAND || destination == LOC_BACKPACK);
                }
            case ZAP:
                if (dstCard != null) {
                    switch (srcCard.getAbility()) {
                        case SAP:
                            return (destination < 10 && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case LEECH:
                        case SACRIFICE:
                            return (dstCard.getType() == Card.Type.FEAR && destination < 10 && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case LASH:
                        case VANISH:
                            return (dstCard.getType() == Card.Type.FLEX && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case POTIONIZE:
                            return ((dstCard.getType() == Card.Type.CASH || dstCard.getType() == Card.Type.DRINK || dstCard.getType() == Card.Type.HIT || dstCard.getType() == Card.Type.BLOCK) && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case BASH:
                            return (destination < 10 && dstCard.getType() == Card.Type.FEAR &&
                                    ((source == LOC_LEFT_HAND && mRowBottom[2].getCard() != null && mRowBottom[2].getCard().getType() == Card.Type.BLOCK) ||
                                            (source == LOC_RIGHT_HAND && mRowBottom[0].getCard() != null && mRowBottom[0].getCard().getType() == Card.Type.BLOCK)));
                        case EXCHANGE:
                            return (mDeck.size() > 0 && destination < 10 && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case STEAL:
                            return (dstCard.getType() == Card.Type.FLEX && mDeck.size() > 0 && mRowBottom[3].getCard() == null && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case KILLER:
                            return (dstCard.getType() == Card.Type.FEAR && destination < 10 && dstCard.isWounded() && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case FORTIFY:
                            return (dstCard.getType() != Card.Type.FEAR && dstCard.getType() != Card.Type.ZAP && dstCard.isActive() && destination != LOC_HERO && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case REFLECT:
                        case REVIVE:
                        case LUCKY:
                        case LIFE:
                            return (dstCard.getType() == Card.Type.FLEX && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case BETRAYAL:
                            return (dstCard.getType() == Card.Type.FEAR && destination < 10 && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case FRENZY:
                            return (destination < 10 && dstCard.getType() == Card.Type.FEAR &&
                                    ((source == LOC_LEFT_HAND && mRowBottom[2].getCard() != null && mRowBottom[2].getCard().getType() == Card.Type.HIT) ||
                                            (source == LOC_RIGHT_HAND && mRowBottom[0].getCard() != null && mRowBottom[0].getCard().getType() == Card.Type.HIT)));
                        case TRADE:
                            return (dstCard.getType() != Card.Type.FEAR && dstCard.getType() != Card.Type.FLEX && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case MIDAS:
                            return (dstCard.getType() != Card.Type.FLEX && dstCard.getValue() > 0 && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case DEVOUR:
                            return (dstCard.getType() != Card.Type.FLEX && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case TRAP:
                        case MIRROR:
                            return (destination < 10 && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        //TODO other fun stuff here
                        default:
                            return false;
                    }
                } else {
                    return (source < 10 && destination == LOC_BACKPACK) || ((source < 10 || source == LOC_BACKPACK) && (destination == LOC_LEFT_HAND || destination == LOC_RIGHT_HAND));
                }
        }
        return false;
    }

    /**
     * Defines whether card can be discarded
     * @param location Coordinate number of card position
     * @return true or false
     */
    private boolean canDiscardThis(int location) {
        Card card = location >= 10 ? mRowBottom[location - 10].getCard() : mRowTop[location].getCard();
        switch (card.getType()) {
            case FEAR:
                return false;
            case HIT:
            case BLOCK:
            case DRINK:
            case ZAP:
                return (location < 10 || location == LOC_BACKPACK);
            case CASH:
                return (location < 10);
        }
        return false;
    }

    /**
     * Performs card movement
     * @param source      Coordinate number of source card position
     * @param destination Coordinate number of dest card position
     */
    private void doReceive(int source, int destination) {
        Position srcPosition = source >= 10 ? mRowBottom[source - 10] : mRowTop[source];
        Position dstPosition = destination >= 10 ? mRowBottom[destination - 10] : mRowTop[destination];
        Card srcCard = srcPosition.getCard();
        Card dstCard = dstPosition.getCard();
        Card hero = mRowBottom[1].getCard();

        switch (srcCard.getType()) {
            case FEAR:
                if (dstCard != null && dstCard.getType() == Card.Type.FLEX) { //Endure attack
                    if (isNeedToReflectDamage) { //Damage reflected
                        int randomTarget;
                        Card randomCard;
                        do {
                            randomTarget = (int) (Math.random() * 4);
                            randomCard = mRowTop[(randomTarget)].getCard();
                        } while (randomCard == null || randomCard.getType() == Card.Type.ZAP);
                        if (randomCard.getValue() > srcCard.getValue()) { //Target card can take damage
                            randomCard.setValue(randomCard.getValue() - srcCard.getValue());
                            animateCardSuffer(randomTarget);
                            randomCard.setWounded(true);
                        } else { //Card is too weak
                            animateCardCrack(randomCard, randomTarget);
                            mRowTop[randomTarget].setCard(null);
                        }
                        float distance = (float) Math.sqrt(mDragRelX * mDragRelX + mDragRelY * mDragRelY);
                        mDragReturnTicks = (int) (distance / mDragReturnSpeed);
                        mDragSpeedX = mDragRelX / mDragReturnTicks;
                        mDragSpeedY = mDragRelY / mDragReturnTicks;
                        isNeedToReflectDamage = false;
                    } else { //Damage taken
                        dstCard.setValue(Math.max(0, dstCard.getValue() - srcCard.getValue()));
                        animateCardSuffer(LOC_HERO);
                        srcPosition.setCard(null);
                    }
                } else if (dstCard != null && dstCard.getType() == Card.Type.BLOCK) { //Block attack
                    if (dstCard.getValue() > srcCard.getValue()) { //Shield can take damage (and more)
                        dstCard.setValue(dstCard.getValue() - srcCard.getValue());
                        animateCardSuffer(destination);
                        dstCard.setWounded(true);
                    } else if (dstCard.getValue() == srcCard.getValue()) { //Shield can take exact damage
                        animateCardCrack(dstCard, destination);
                        dstPosition.setCard(null);
                    } else { //Shield is too weak
                        hero.setValue(Math.max(0, hero.getValue() - (srcCard.getValue() - dstCard.getValue())));
                        animateCardSuffer(LOC_HERO);
                        animateCardCrack(dstCard, destination);
                        dstPosition.setCard(null);
                    }
                    srcPosition.setCard(null);
                }
                break;
            case HIT:
                if (dstCard != null && dstCard.getType() == Card.Type.FEAR) { //Attack
                    if (dstCard.getValue() > srcCard.getValue()) { //Mob can take damage (and more)
                        dstCard.setValue(dstCard.getValue() - srcCard.getValue());
                        animateCardSuffer(destination);
                        dstCard.setWounded(true);
                    } else { //Mob will be defeated
                        animateCardCrack(dstCard, destination);
                        dstPosition.setCard(null);
                    }
                    srcPosition.setCard(null);
                } else { //Pack/Equip
                    dstPosition.setCard(srcCard);
                    srcPosition.setCard(null);
                }
                break;
            case BLOCK:
                dstPosition.setCard(srcCard);
                srcPosition.setCard(null);
                break;
            case DRINK:
                dstPosition.setCard(srcCard);
                srcPosition.setCard(null);
                if (destination == LOC_LEFT_HAND || destination == LOC_RIGHT_HAND) { //Actually use
                    srcCard.setActive(false);
                    hero.setValue(Math.min(Card.HERO_MAX + mHealthAddition, hero.getValue() + srcCard.getValue()));
                    animateCardImprove(LOC_HERO);
                }
                break;
            case CASH:
                srcCard.setActive(false);
                dstPosition.setCard(srcCard);
                srcPosition.setCard(null);
                mCoins += srcCard.getValue();
                break;
            case ZAP:
                if ((destination == LOC_RIGHT_HAND || destination == LOC_LEFT_HAND || destination == LOC_BACKPACK) && source != LOC_LEFT_HAND && source != LOC_RIGHT_HAND) { //Pack/Equip
                    dstPosition.setCard(srcCard);
                    srcPosition.setCard(null);
                } else { //Actually use
                    switch (srcCard.getAbility()) {
                        case SAP:
                            animateReceiveCard(dstCard, destination);
                            dstPosition.setCard(null);
                            srcPosition.setCard(null);
                            break;
                        case LEECH:
                            int leeched = Math.min(srcCard.getValue(), dstCard.getValue());
                            if (dstCard.getValue() > leeched) { //Mob can take damage (and more)
                                dstCard.setValue(dstCard.getValue() - leeched);
                                animateCardSuffer(destination);
                                dstCard.setWounded(true);
                            } else { //Mob will be defeated
                                animateCardCrack(dstCard, destination);
                                dstPosition.setCard(null);
                            }
                            srcPosition.setCard(null);
                            hero.setValue(Math.min(Card.HERO_MAX + mHealthAddition, hero.getValue() + leeched));
                            animateCardImprove(LOC_HERO);
                            break;
                        case SACRIFICE:
                            int sacrificed = Card.HERO_MAX + mHealthAddition - hero.getValue();
                            if (dstCard.getValue() > sacrificed) { //Mob can take damage (and more)
                                dstCard.setValue(dstCard.getValue() - sacrificed);
                                animateCardSuffer(destination);
                                dstCard.setWounded(true);
                            } else { //Mob will be defeated
                                animateCardCrack(dstCard, destination);
                                dstPosition.setCard(null);
                            }
                            srcPosition.setCard(null);
                            break;
                        case POTIONIZE:
                            dstCard.setType(Card.Type.DRINK);
                            dstCard.setValue((int) (Math.random() * 9 + 2));
                            dstCard.setActive(true);
                            animateCardImprove(destination);
                            if (destination == LOC_LEFT_HAND || destination == LOC_RIGHT_HAND) { //Actually use
                                dstCard.setActive(false);
                                hero.setValue(Math.min(Card.HERO_MAX + mHealthAddition, hero.getValue() + dstCard.getValue()));
                                animateCardImprove(LOC_HERO);
                            }
                            srcPosition.setCard(null);
                            break;
                        case VANISH:
                            for (int i = 0; i < 4; i++) {
                                if (mRowTop[i].getCard() != null) {
                                    animateReceiveCard(mRowTop[i].getCard(), i);
                                    mRowTop[i].setCard(null);
                                }
                            }
                            srcPosition.setCard(null);
                            break;
                        case BASH:
                            Position shieldPosition = mRowBottom[source == LOC_LEFT_HAND ? 2 : 0];
                            if (dstCard.getValue() > shieldPosition.getCard().getValue()) { //Mob can take damage (and more)
                                dstCard.setValue(dstCard.getValue() - shieldPosition.getCard().getValue());
                                animateCardSuffer(destination);
                                dstCard.setWounded(true);
                            } else { //Mob will be defeated
                                animateCardCrack(dstCard, destination);
                                dstPosition.setCard(null);
                            }
                            if (shieldPosition.getCard().getValue() > 5) { //Shield can take bash damage
                                shieldPosition.getCard().setValue(shieldPosition.getCard().getValue() - 5);
                                animateCardSuffer(source == LOC_LEFT_HAND ? LOC_RIGHT_HAND : LOC_LEFT_HAND);
                            } else { //Shield is too weak
                                animateCardCrack(shieldPosition.getCard(), source == LOC_LEFT_HAND ? LOC_RIGHT_HAND : LOC_LEFT_HAND);
                                shieldPosition.setCard(null);
                            }
                            srcPosition.setCard(null);
                            break;
                        case LASH:
                            boolean leftToRight = (Math.random() < 0.5f);
                            int lashedCount = 0;
                            int lashIndex = leftToRight ? 0 : 3;
                            while (lashedCount < 3) {
                                Card card = mRowTop[lashIndex].getCard();
                                if (card != null && (card.getType() == Card.Type.FEAR)) {
                                    if (card.getValue() > srcCard.getValue()) { //Card can take damage (and more)
                                        card.setValue(card.getValue() - srcCard.getValue());
                                        animateCardSuffer(lashIndex);
                                        card.setWounded(true);
                                    } else { //Card will be defeated
                                        animateCardCrack(mRowTop[lashIndex].getCard(), lashIndex);
                                        mRowTop[lashIndex].setCard(null);
                                    }
                                    lashedCount++;
                                } else if (lashedCount > 0) {
                                    break;
                                }
                                lashIndex += (leftToRight ? 1 : -1);
                                if (lashIndex < 0 || lashIndex > 3) {
                                    break;
                                }
                            }
                            srcPosition.setCard(null);
                            break;
                        case EXCHANGE:
                            animateReceiveCard(dstCard, destination);
                            dstPosition.setCard(null);
                            Card exchangedCard = mDeck.deal(mDeck.find(Card.Type.ZAP));
                            if (exchangedCard != null) {
                                animateDealCard(exchangedCard, destination);
                            }
                            srcPosition.setCard(null);
                            break;
                        case STEAL:
                            Card stolenCard = mDeck.deal();
                            if (stolenCard.getType() == Card.Type.CASH) { //Coins
                                stolenCard.setActive(false);
                                mCoins += stolenCard.getValue();
                            }
                            animateDealCard(stolenCard, 13);
                            srcPosition.setCard(null);
                            break;
                        case KILLER:
                            animateCardCrack(dstCard, destination);
                            dstPosition.setCard(null);
                            srcPosition.setCard(null);
                            break;
                        case FORTIFY:
                            dstCard.setValue(dstCard.getValue() + 5);
                            animateCardImprove(destination);
                            srcPosition.setCard(null);
                            break;
                        case REFLECT:
                            isNeedToReflectDamage = true;
                            animateCardImprove(LOC_HERO);
                            srcPosition.setCard(null);
                            break;
                        case REVIVE:
                            isNeedToReviveHero = true;
                            animateCardImprove(LOC_HERO);
                            srcPosition.setCard(null);
                            break;
                        case FRENZY:
                            Position swordPosition = mRowBottom[source == LOC_LEFT_HAND ? 2 : 0];
                            if (dstCard.getValue() > swordPosition.getCard().getValue()) { //Mob can take damage (and more)
                                dstCard.setValue(dstCard.getValue() - swordPosition.getCard().getValue());
                                animateCardSuffer(destination);
                                dstCard.setWounded(true);
                            } else { //Mob will be defeated
                                animateCardCrack(dstCard, destination);
                                dstPosition.setCard(null);
                            }
                            animateCardImprove(source == LOC_LEFT_HAND ? LOC_RIGHT_HAND : LOC_LEFT_HAND);
                            srcPosition.setCard(null);
                            break;
                        case LUCKY:
                            int randomCount = 2 - (int)(Math.random() * 3) / 2;
                            for (int i = 0; i < randomCount; i++) {
                                int randomTarget;
                                Card randomCard;
                                do {
                                    randomTarget = (int) (Math.random() * 4);
                                    randomCard = mRowTop[(randomTarget)].getCard();
                                } while (randomCard == null);
                                animateCardCrack(randomCard, randomTarget);
                                mRowTop[randomTarget].setCard(null);
                                boolean cardsLeft = false;
                                for (Position p : mRowTop) {
                                    if (p.getCard() != null) {
                                        cardsLeft = true;
                                        break;
                                    }
                                }
                                if (!cardsLeft) { //No more cards left
                                    break;
                                }
                            }
                            srcPosition.setCard(null);
                            break;
                        case TRADE:
                            mCoins += 10;
                            mDragRelX = 0;
                            mDragRelY = 0;
                            mTouchedLocation = destination;
                            animateCardDiscard(destination);
                            srcPosition.setCard(null);
                            break;
                        case LIFE:
                            hero.setValue(hero.getValue() + srcCard.getValue());
                            mHealthAddition = Math.max(0, hero.getValue() - Card.HERO_MAX);
                            animateCardImprove(LOC_HERO);
                            srcPosition.setCard(null);
                            break;
                        case BETRAYAL:
                            for (int i = 0; i < 4; i++) {
                                Card card = mRowTop[i].getCard();
                                if (Math.abs(i - destination) == 1 && card != null && (card.getType() != Card.Type.ZAP)) {
                                    if (card.getValue() > dstCard.getValue()) { //Card can take damage (and more)
                                        card.setValue(card.getValue() - dstCard.getValue());
                                        animateCardSuffer(i);
                                        card.setWounded(true);
                                    } else { //Card will be defeated
                                        animateCardCrack(mRowTop[i].getCard(), i);
                                        mRowTop[i].setCard(null);
                                    }
                                }
                            }
                            srcPosition.setCard(null);
                            break;
                        case MIDAS:
                            switch (dstCard.getType()) {
                                case FEAR:
                                    dstCard.setValue(dstCard.getValue() / 2);
                                    break;
                                case ZAP:
                                    dstCard.setValue(dstCard.getValue() * 2);
                                    break;
                            }
                            dstCard.setType(Card.Type.CASH);
                            dstCard.setActive(true);
                            animateCardImprove(destination);
                            if (destination == LOC_LEFT_HAND || destination == LOC_RIGHT_HAND || destination == LOC_BACKPACK) { //Actually use
                                dstCard.setActive(false);
                                mCoins += dstCard.getValue();
                            }
                            srcPosition.setCard(null);
                            break;
                        case DEVOUR:
                            if (destination < 10) {
                                mRowTop[destination].setCard(Card.getSpecial());
                            } else {
                                mRowBottom[destination - 10].setCard(Card.getSpecial());
                            }
                            animateCardImprove(destination);
                            srcPosition.setCard(null);
                            break;
                        case TRAP:
                            dstCard.setActive(false);
                            srcPosition.setCard(null);
                            break;
                        case MIRROR:
                            animateReceiveCard(dstCard, destination);
                            srcPosition.setCard(null);
                            break;
                    }
                    //TODO other funny stuff here
                }
                break;
        }
        processMove();
    }

    /**
     * Discards card
     * @param location Card location
     */
    private void doDiscard(int location) {
        Position position = location >= 10 ? mRowBottom[location - 10] : mRowTop[location];
        Card card = position.getCard();

        switch (card.getType()) {
            case ZAP:
                animateCardDiscard(location);
                break;
            case HIT:
            case BLOCK:
            case DRINK:
                mCoins += card.getValue();
                animateCardDiscard(location);
                break;
            case CASH:
                animateCardDiscard(location);
                break;
        }
    }


    /**
     * Perform necessary calculations after each move
     */
    private void processMove() {
        if (mCardAnimationCount > 0 || mDealAnimationCount > 0 || mReceiveAnimationCount > 0) {
            return;
        }
        isFreshDeal = false;
        Card hero = mRowBottom[1].getCard();
        if (hero.getValue() <= 0) { //Check hero health
            if (isNeedToReviveHero) { //Can be revived
                hero.setValue(1);
                animateCardImprove(LOC_HERO);
                isNeedToReviveHero = false;
            } else { //Actual death
                animateHeroVanish();
                return;
            }
        }
        if (mDeck.size() == 0) { //Check deck size
            boolean win = true;
            for (int i = 0; i < 4; i++) {
                if (mRowTop[i].getCard() != null && mRowTop[i].getCard().isActive()) {
                    win = false;
                    break;
                }
            }
            if (win) {
                animateHeroWin();
                return;
            }
        }

        int emptyTop = 0;
        for (int i = 0; i < 4; i++) {
            if (mRowTop[i] != null && (mRowTop[i].getCard() == null || !mRowTop[i].getCard().isActive())) {
                emptyTop++;
                if (emptyTop >= 3) {
                    dealTopRow();
                    break;
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int newHeight = MeasureSpec.getSize(heightMeasureSpec);
        int newWidth = MeasureSpec.getSize(widthMeasureSpec);
        if (newWidth != mCanvasWidth || newHeight != mCanvasHeight) {
            mCanvasWidth = newWidth;
            mCanvasHeight = newHeight;
            isMeasurementChanged = true;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     *
     */
    private void calculatePositions() {
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        int cw = width / 5;
        int ch = cw * 4 / 3;

        mFontSize = ch / 7;
        mFontPadding = mFontSize / 3;

        int pHorz = (width - cw * 4) / 5;
        int pVert = pHorz;
        mTopPadding = pVert;

        mEpsDrag = cw / 5;
        mDragReturnSpeed = cw / 3;

        int topTop = height - ch * 2 - pVert * 3;
        int topBottom = height - ch - pVert;

        for (int i = 0; i < 4; i++) {
            Position pos = mRowTop[i];
            pos.setRect(
                    pHorz + (cw + pHorz) * i,
                    topTop,
                    pHorz + (cw + pHorz) * i + cw,
                    topTop + ch
            );
            pos = mRowBottom[i];
            pos.setRect(
                    pHorz + (cw + pHorz) * i,
                    topBottom,
                    pHorz + (cw + pHorz) * i + cw,
                    topBottom + ch
            );
        }

        int bw = cw * 4 / 5;
        int bh = cw * 3 / 5;
        mDiscardBox.setRect(width - pHorz - bw, topTop - pVert * 2 - bh, width - pHorz, topTop - pVert * 2);

        int dew = cw / 4;
        int deh = ch / 4;
        mDeckPosition = new RectF((width - dew) / 2, topTop - pVert * 2 - ch - deh, (width + dew) / 2, topTop - pVert * 2 - ch);

        if (isBeginning) { //Beginning of the game
            animateHeroAppear();
            dealTopRow();
            isBeginning = false;
        }
    }

    //----------------------------------------------------------------------------------------------

    private enum CardState {
        REGULAR, TOUCHED, RECEIVING, MOVED, INACTIVE
    }

    /**
     * Draw single card
     * @param canvas Canvas to draw to
     * @param rect   Rectangle to draw in
     * @param state  Current card state
     */
    private void drawCard(Canvas canvas, Card card, RectF rect, CardState state) {
        float radius = rect.width() / 7;
        float cx = (rect.right + rect.left) / 2;
        float cy = (rect.top + rect.bottom) / 2;

        if (card != null && state != CardState.MOVED) {
            mPaint.setStrokeWidth(0);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(COLOR_BG_CARD);
            canvas.drawRoundRect(rect, radius, radius, mPaint);
            mTextPaint.setTextSize(mFontSize);
            String text;
            switch (card.getType()) {
                case FLEX:
                    mPaint.setColor(COLOR_REGULAR);
                    mPaint.setStyle(Paint.Style.STROKE);
                    mPaint.setStrokeWidth(STROKE_WIDTH);
                    mPaint.setStrokeCap(Paint.Cap.ROUND);
                    canvas.drawCircle(cx, cy, radius * 2, mPaint);
                    canvas.drawLine(cx, cy - radius / 3, cx, cy + radius / 2, mPaint);
//                    canvas.drawLine(cx - radius * 3 / 6, cy + radius, cx + radius * 3 / 6, cy + radius, mPaint);
//                    canvas.drawLine(cx - radius * 3 / 6, cy + radius, cx - radius * 3 / 6 - radius / 3, cy + radius + radius / 3, mPaint);
//                    canvas.drawLine(cx + radius * 3 / 6, cy + radius, cx + radius * 3 / 6 + radius / 3, cy + radius + radius / 3, mPaint);
                    int value = card.getValue();
                    if (value >= 10) {
                        canvas.drawArc(new RectF(cx - radius * 4 / 6, cy + radius * 4 / 6 + radius * (13 - value) / 6, cx + radius * 4 / 6, cy + radius * 4 / 3), 0, 180, false, mPaint);
                    } else if (value >= 8) {
                        canvas.drawArc(new RectF(cx - radius * 4 / 6 + radius * (10 - value) / 6, cy + radius * 7 / 6, cx + radius * 4 / 6 - radius * (10 - value) / 6, cy + radius * 4 / 3), 0, 180, false, mPaint);
                    } else if (value >= 5) {
                        canvas.drawLine(cx - radius * 2 / 6 - radius * (7 - value) / 6, cy + radius * 5 / 4, cx + radius * 2 / 6 + radius * (7 - value) / 6, cy + radius * 5 / 4, mPaint);
                    } else if (value >= 3) {
                        canvas.drawArc(new RectF(cx - radius * 4 / 6 + radius * (value - 3) / 6, cy + radius * 7 / 6, cx + radius * 4 / 6 - radius * (value - 3) / 6, cy + radius * 4 / 3), 180, 180, false, mPaint);
                    } else if (value >= 0) {
                        canvas.drawArc(new RectF(cx - radius * 4 / 6, cy + radius * 7 / 6, cx + radius * 4 / 6, cy + radius * 4 / 3 + radius * (3 - value) / 6), 180, 180, false, mPaint);
                    }


                    mPaint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(cx - radius * 2 / 3, cy - radius / 2, radius / 4, mPaint);
                    canvas.drawCircle(cx + radius * 2 / 3, cy - radius / 2, radius / 4, mPaint);
                    mTextPaint.setColor(COLOR_EMPH);
                    text = String.format("%d/%d", value, Card.HERO_MAX + mHealthAddition);
                    canvas.drawText(text, rect.right - mFontPadding - mTextPaint.measureText(text), rect.top + mFontPadding - mTextPaint.ascent(), mTextPaint);
                    text = String.valueOf(mCoins);
                    canvas.drawText(text, rect.left + mFontPadding, rect.bottom - mFontSize - mFontPadding - mTextPaint.ascent(), mTextPaint);
                    mTextPaint.setColor(COLOR_SPECIAL);
                    text = String.format("%s%s", isNeedToReflectDamage ? "\u2600" : "", isNeedToReviveHero ? "\u2665" : "");
                    canvas.drawText(text, rect.right - mFontPadding - mTextPaint.measureText(text), rect.bottom - mFontSize - mFontPadding - mTextPaint.ascent(), mTextPaint);
                    break;
                case FEAR:
                    mPaint.setColor(COLOR_REGULAR);
                    mPaint.setStyle(Paint.Style.FILL);
                    mPaint.setStrokeWidth(STROKE_WIDTH);
                    canvas.drawCircle(cx, cy, radius * 2, mPaint);
                    mPaint.setColor(COLOR_BG_CARD);
                    canvas.drawCircle(cx - radius * 2 / 3, cy - radius / 2, radius / 3, mPaint);
                    canvas.drawCircle(cx + radius * 2 / 3, cy - radius / 2, radius / 3, mPaint);
                    mPaint.setStyle(Paint.Style.STROKE);
                    canvas.drawLine(cx - radius / 4, cy - radius * 3 / 4, cx - radius, cy - radius, mPaint);
                    canvas.drawLine(cx + radius / 4, cy - radius * 3 / 4, cx + radius, cy - radius, mPaint);
                    canvas.drawLine(cx - radius * 2 / 3, cy + radius, cx - radius / 3, cy + radius * 3 / 4, mPaint);
                    canvas.drawLine(cx - radius / 3, cy + radius * 3 / 4, cx, cy + radius, mPaint);
                    canvas.drawLine(cx, cy + radius, cx + radius / 3, cy + radius * 3 / 4, mPaint);
                    canvas.drawLine(cx + radius / 3, cy + radius * 3 / 4, cx + radius * 2 / 3, cy + radius, mPaint);
                    mTextPaint.setColor(COLOR_REGULAR);
                    text = String.valueOf(card.getValue());
                    canvas.drawText(text, rect.right - mFontPadding - mTextPaint.measureText(text), rect.top + mFontPadding - mTextPaint.ascent(), mTextPaint);
                    mTextPaint.setTextSize(mFontSize * 9 / 10);
                    text = String.valueOf(card.getName());
                    canvas.drawText(text, (rect.right + rect.left - mTextPaint.measureText(text)) / 2, rect.bottom - mFontSize - mFontPadding - mTextPaint.ascent(), mTextPaint);
                    break;
                case HIT: {
                    mPaint.setColor(COLOR_REGULAR);
                    mPaint.setStyle(Paint.Style.STROKE);
                    mPaint.setStrokeWidth(STROKE_WIDTH);
                    float x1 = rect.left + radius * 2;
                    float x3 = rect.right - radius * 2;
                    float y1 = rect.top + (rect.bottom - rect.top) / 4;
                    float y3 = rect.top + (rect.bottom - rect.top) * 3 / 4;
                    canvas.drawLine(x1 - radius / 2, y3, x3 - radius / 2, y1, mPaint);
                    canvas.drawLine(x3 - radius / 2, y1, x3, y1, mPaint);
                    canvas.drawLine(x3, y1, x3, y1 + radius * 2 / 3, mPaint);
                    canvas.drawLine(x3, y1 + radius * 2 / 3, x1, y3 + radius * 2 / 3, mPaint);
                    canvas.drawLine(x1, y3 + radius * 2 / 3, x1 - radius / 2, y3, mPaint);
                    canvas.drawLine(x1 - radius / 3, y3 - radius * 5 / 3, x1 + radius + radius / 3, y3 + radius / 3, mPaint);
                    mTextPaint.setColor(COLOR_REGULAR);
                    text = String.valueOf(card.getValue());
                    canvas.drawText(text, rect.left + mFontPadding, rect.top + mFontPadding - mTextPaint.ascent(), mTextPaint);
                }
                break;
                case BLOCK: {
                    mPaint.setColor(COLOR_REGULAR);
                    mPaint.setStyle(Paint.Style.STROKE);
                    mPaint.setStrokeWidth(STROKE_WIDTH);
                    float x1 = rect.left + radius * 2;
                    float x3 = rect.right - radius * 2;
                    float y1 = rect.top + (rect.bottom - rect.top) / 3;
                    float y2 = rect.top + (rect.bottom - rect.top) * 3 / 5;
                    float y3 = rect.top + (rect.bottom - rect.top) * 3 / 4;
                    canvas.drawLine(x1, y1, x3, y1, mPaint);
                    canvas.drawLine(x3, y1, x3, y2, mPaint);
                    canvas.drawLine(x3, y2, cx, y3, mPaint);
                    canvas.drawLine(cx, y3, x1, y2, mPaint);
                    canvas.drawLine(x1, y2, x1, y1, mPaint);
                    mTextPaint.setColor(COLOR_REGULAR);
                    text = String.valueOf(card.getValue());
                    canvas.drawText(text, rect.left + mFontPadding, rect.top + mFontPadding - mTextPaint.ascent(), mTextPaint);
                }
                break;
                case DRINK: {
                    mPaint.setColor(COLOR_EMPH);
                    mPaint.setStyle(Paint.Style.STROKE);
                    mPaint.setStrokeWidth(STROKE_WIDTH);
                    canvas.drawCircle(cx, cy + radius, radius * 3 / 2, mPaint);
                    float x1 = cx - radius / 2;
                    float x3 = cx + radius / 2;
                    float y1 = cy - radius * 2;
                    float y3 = cy;
                    canvas.drawLine(x1, y3, x1, y1, mPaint);
                    canvas.drawLine(x1, y1, x3, y1, mPaint);
                    canvas.drawLine(x3, y1, x3, y3, mPaint);
                    mTextPaint.setColor(COLOR_EMPH);
                    text = String.valueOf(card.getValue());
                    canvas.drawText(text, rect.left + mFontPadding, rect.top + mFontPadding - mTextPaint.ascent(), mTextPaint);
                }
                break;
                case CASH: {
                    mPaint.setColor(COLOR_REGULAR);
                    mPaint.setStyle(Paint.Style.STROKE);
                    mPaint.setStrokeWidth(STROKE_WIDTH);
                    canvas.drawCircle(cx, cy, radius * 3 / 2, mPaint);
                    float rad = radius * 3 / (2 * (float) Math.sqrt(2));
                    canvas.drawLine(cx + rad, cy - rad, cx - rad, cy + rad, mPaint);
                    mTextPaint.setColor(COLOR_EMPH);
                    text = String.valueOf(card.getValue());
                    canvas.drawText(text, rect.left + mFontPadding, rect.bottom - mFontSize - mFontPadding - mTextPaint.ascent(), mTextPaint);
                }
                break;
                case ZAP: {
                    mTextPaint.setColor(COLOR_SPECIAL);
                    if (card.getValue() > 0) {
                        text = String.valueOf(card.getValue());
                        canvas.drawText(text, rect.left + mFontPadding, rect.top + mFontPadding - mTextPaint.ascent(), mTextPaint);
                    }
                    mTextPaint.setTextSize(mFontSize * 9 / 10);
                    text = String.valueOf(card.getName());
                    canvas.drawText(text, (rect.right + rect.left - mTextPaint.measureText(text)) / 2, (rect.bottom + rect.top - mTextPaint.ascent()) / 2, mTextPaint);
                }
                break;
            }
            if (!card.isActive()) { //Draw tint
                mPaint.setColor(COLOR_BG_TINT);
                mPaint.setStyle(Paint.Style.FILL);
                canvas.drawRoundRect(rect, radius, radius, mPaint);
                mPaint.setAlpha(255);
            }
        }

        mPaint.setStrokeWidth(STROKE_WIDTH);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(state == CardState.TOUCHED ? COLOR_EMPH : state == CardState.RECEIVING ? COLOR_SPECIAL : COLOR_REGULAR);
        canvas.drawRoundRect(rect, radius, radius, mPaint);

    }

    /**
     * Draw position
     * @param canvas   Canvas to draw to
     * @param position Position to draw
     * @param state    Current card state
     */
    private void drawPosition(Canvas canvas, Position position, CardState state) {
        drawCard(canvas, position.getCard(), position.getRect(), state);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas.getWidth() == 0 || canvas.getHeight() == 0 || mDeck == null) { //Not inflated yet
            return;
        }

        if (isMeasurementChanged) { //New size or data needs to be applied
            isMeasurementChanged = false;
            calculatePositions();
        }

        for (int i = 0; i < 4; i++) { //Draw top row
            if (mRowTop[i] != null) {
                if (mCardAnimationCount > 0 && mCardAnimationTop[i] != null) {
                    continue;
                }
                drawPosition(canvas, mRowTop[i],
                        mTouchedLocation == i ? ((isDragging || mDragReturnTicks > 0 || isDiscarding) ? CardState.MOVED : CardState.TOUCHED) :
                                isDragging ? canReceiveThis(mTouchedLocation, i) ? CardState.RECEIVING : CardState.REGULAR
                                        : CardState.REGULAR);
            }
        }
        for (int i = 0; i < 4; i++) { //Draw bottom row
            if (mRowBottom[i] != null) {
                if (i == 1 && isHeroAnimated) {
                    drawPosition(canvas, mRowBottom[i], CardState.MOVED);
                    continue;
                }
                if (mCardAnimationCount > 0 && mCardAnimationBottom[i] != null) {
                    continue;
                }
                drawPosition(canvas, mRowBottom[i],
                        mTouchedLocation == 10 + i ? ((isDragging || mDragReturnTicks > 0 || isDiscarding) ? CardState.MOVED : CardState.TOUCHED) :
                                isDragging ? canReceiveThis(mTouchedLocation, 10 + i) ? CardState.RECEIVING : CardState.REGULAR
                                        : CardState.REGULAR);
            }
        }

//        if (mDeckPosition != null) { //Deck list
//            mPaint.setColor(COLOR_REGULAR);
//            mPaint.setStyle(Paint.Style.STROKE);
//            mPaint.setStrokeWidth(STROKE_WIDTH / 2);
//            canvas.drawRect(mDeckPosition, mPaint);
//        }

        if (mDiscardBox != null) { //Discard box
            mPaint.setColor(isDragging && canDiscardThis(mTouchedLocation) ? COLOR_SPECIAL : COLOR_REGULAR);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(STROKE_WIDTH);
            RectF rect = mDiscardBox.getRect();
            canvas.drawRect(rect, mPaint);
            mPaint.setStrokeWidth(STROKE_WIDTH / 2);
            canvas.drawLine(rect.left, rect.top, rect.right, rect.bottom, mPaint);
            canvas.drawLine(rect.right, rect.top, rect.left, rect.bottom, mPaint);
        }

        if ((isDragging || mDragReturnTicks > 0) && mTouchedLocation > -1) {
            canvas.save();
            canvas.translate(mDragRelX, mDragRelY);
            drawPosition(canvas, mTouchedLocation < 10 ? mRowTop[mTouchedLocation] : mRowBottom[mTouchedLocation - 10], isDragging ? CardState.TOUCHED : CardState.REGULAR);
            canvas.restore();
        }

        mTextPaint.setColor(COLOR_REGULAR);
        mTextPaint.setTextSize(mFontSize * 1.5f);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        int cardsLeft = mDeck.size() + mDealAnimationCount;
        canvas.drawText(String.valueOf(cardsLeft), canvas.getWidth() / 2, mTopPadding - mTextPaint.ascent(), mTextPaint);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        if (mReceiveAnimationCount > 0) { //Receive animations
            for (int i = 0; i < 4; i++) {
                if (mReceiveAnimationTop[i] != null) {
                    mReceiveAnimationTop[i].draw(canvas);
                }
            }
        }

        if (mDropAnimationCount > 0) { //Drop animations
            for (int i = 0; i < 4; i++) {
                if (mDropAnimationBottom[i] != null) {
                    mDropAnimationBottom[i].draw(canvas);
                }
            }
        }

        if (mDealAnimationCount > 0 && !isHeroAnimated && mCardAnimationCount == 0 && mReceiveAnimationCount == 0 && mDropAnimationCount == 0) { //Deal animations
            boolean check = false;
            for (int i = 0; i < 4; i++) {
                if (mDealAnimationBottom[i] != null) {
                    mDealAnimationBottom[i].draw(canvas);
                    check = true;
                    break;
                }
            }
            if (!check) {
                for (int i = 0; i < 4; i++) {
                    if (mDealAnimationTop[i] != null) {
                        mDealAnimationTop[i].draw(canvas);
                        break;
                    }
                }
            }
        }

        if (mCardAnimationCount > 0) { //Card animations
            for (int i = 0; i < 4; i++) {
                if (mCardAnimationBottom[i] != null) {
                    mCardAnimationBottom[i].draw(canvas);
                }
            }
            for (int i = 0; i < 4; i++) {
                if (mCardAnimationTop[i] != null) {
                    mCardAnimationTop[i].draw(canvas);
                }
            }
        }

        if (isHeroAnimated && mHeroAnimation != null) { //Hero animations
            mHeroAnimation.draw(canvas);
        }

        if (isDiscarding && mDiscardAnimation != null) {
            mDiscardAnimation.draw(canvas);
        }

        if (isGameOver) {
            canvas.drawColor(0xdd000000);
        }

        if (mDragReturnTicks > 0 || mDealAnimationCount > 0 || mReceiveAnimationCount > 0 || mDropAnimationCount > 0 || isHeroAnimated || mCardAnimationCount > 0 || isDiscarding) { //Need to animate
            this.postDelayed(mAnimationAction, 10);
        }

    }

    //----------------------------------------------------------------------------------------------

    /**
     * Runnable object used for animations
     */
    private Runnable mAnimationAction = new Runnable() {
        @Override
        public void run() {
            if (mDragReturnTicks > 0) { //Drag return animation
                mDragRelX -= mDragSpeedX;
                mDragRelY -= mDragSpeedY;
                mDragReturnTicks--;
                if (mDragReturnTicks <= 0) {
                    resetTouchFeedback();
                }
            }
            if (isHeroAnimated) {
                mHeroAnimation.tick();
                if (mHeroAnimation.isFinished()) {
                    mHeroAnimation.finish();
                    mHeroAnimation = null;
                    isHeroAnimated = false;
                }
            }
            if (isDiscarding) {
                mDiscardAnimation.tick();
                if (mDiscardAnimation.isFinished()) {
                    mDiscardAnimation.finish();
                    mDiscardAnimation = null;
                    isDiscarding = false;
                    resetTouchFeedback();
                    processMove();
                }
            }
            if (mCardAnimationCount > 0) { //Card animations
                for (int i = 0; i < 4; i++) {
                    Animation animation = mCardAnimationBottom[i];
                    if (animation != null) {
                        animation.tick();
                        if (animation.isFinished()) {
                            animation.finish();
                            mCardAnimationBottom[i] = null;
                            mCardAnimationCount--;
                        }
                    }
                }
                for (int i = 0; i < 4; i++) {
                    Animation animation = mCardAnimationTop[i];
                    if (animation != null) {
                        animation.tick();
                        if (animation.isFinished()) {
                            animation.finish();
                            mCardAnimationTop[i] = null;
                            mCardAnimationCount--;
                        }
                    }
                }
                if (mCardAnimationCount == 0) {
                    processMove();
                }
            }
            if (mReceiveAnimationCount > 0) { //Receive animations
                for (int i = 0; i < 4; i++) {
                    Animation animation = mReceiveAnimationTop[i];
                    if (animation != null) {
                        animation.tick();
                        if (animation.isFinished()) {
                            animation.finish();
                            mReceiveAnimationTop[i] = null;
                            mReceiveAnimationCount--;
                        }
                        break;
                    }
                }
                if (mReceiveAnimationCount == 0) {
                    processMove();
                }
            }
            if (mDropAnimationCount > 0) { //Drop animations
                for (int i = 0; i < 4; i++) {
                    Animation animation = mDropAnimationBottom[i];
                    if (animation != null) {
                        animation.tick();
                        if (animation.isFinished()) {
                            animation.finish();
                            mDropAnimationBottom[i] = null;
                            mDropAnimationCount--;
                        }
                        break;
                    }
                }
                if (mDropAnimationCount == 0) {
                    processMove();
                }
            }
            if (mDealAnimationCount > 0 && !isHeroAnimated && mCardAnimationCount == 0 && mReceiveAnimationCount == 0 && mDropAnimationCount == 0) { //Deal animations
                boolean check = false;
                for (int i = 0; i < 4; i++) {
                    Animation animation = mDealAnimationBottom[i];
                    if (animation != null) {
                        animation.tick();
                        if (animation.isFinished()) {
                            animation.finish();
                            mDealAnimationBottom[i] = null;
                            mDealAnimationCount--;
                        }
                        check = true;
                        break;
                    }
                }
                if (!check) {
                    for (int i = 0; i < 4; i++) {
                        Animation animation = mDealAnimationTop[i];
                        if (animation != null) {
                            animation.tick();
                            if (animation.isFinished()) {
                                animation.finish();
                                mDealAnimationTop[i] = null;
                                mDealAnimationCount--;
                            }
                            break;
                        }
                    }
                }
            }
            invalidate();
        }
    };

    /**
     * Start card dealing animation
     * @param card   Card to deal
     * @param target Target location
     */
    private void animateDealCard(Card card, int target) {
        Position position = (target < 10 ? mRowTop[target] : mRowBottom[target - 10]);
        if (card != null) { //Has card to deal
            mDealAnimationCount++;
            if (target < 10) {
                mDealAnimationTop[target] = new DeckDealAnimation(card, position);
            } else {
                mDealAnimationBottom[target - 10] = new DeckDealAnimation(card, position);
            }
        } else {
            position.setCard(null);
        }
        invalidate();
    }

    /**
     * Start card receiving animation
     * @param card   Card to receive
     * @param source Target location
     */
    private void animateReceiveCard(Card card, int source) {
        Position position = (source < 10 ? mRowTop[source] : mRowBottom[source - 10]);
        if (card != null) { //Has card to deal
            mReceiveAnimationCount++;
            if (source < 10) {
                mReceiveAnimationTop[source] = new DeckReceiveAnimation(card, position);
            } else {
//                mReceiveAnimationBottom[source - 10] = new DeckReceiveAnimation(card, position);
            }
        }
        invalidate();
    }

    /**
     * Start (disabled) card drop animation
     * @param card   Card to drop
     * @param source Target location
     */
    private void animateDropCard(Card card, int source) {
        Position position = (source < 10 ? mRowTop[source] : mRowBottom[source - 10]);
        if (card != null) { //Has card to deal
            mDropAnimationCount++;
            if (source < 10) {
//                mDropAnimationTop[source] = new DeckDropAnimation(card, position);
            } else {
                mDropAnimationBottom[source - 10] = new DeckDropAnimation(card, position);
            }
        }
        invalidate();
    }

    /**
     * Start mob disappearing animation
     * @param card   Card
     * @param target Target location
     */
    private void animateCardCrack(Card card, int target) {
        Position position = (target < 10 ? mRowTop[target] : mRowBottom[target - 10]);
        mCardAnimationCount++;
        if (target < 10) {
            mCardAnimationTop[target] = new CardCrackAnimation(card, position);
        } else {
            mCardAnimationBottom[target - 10] = new CardCrackAnimation(card, position);
        }
        invalidate();
    }

    /**
     * Start mob suffer animation
     * @param target Target location
     */
    private void animateCardSuffer(int target) {
        Position position = (target < 10 ? mRowTop[target] : mRowBottom[target - 10]);
        mCardAnimationCount++;
        if (target < 10) {
            mCardAnimationTop[target] = new CardSufferAnimation(position);
        } else {
            mCardAnimationBottom[target - 10] = new CardSufferAnimation(position);
        }
        invalidate();
    }

    /**
     * Start hero quaff animation
     * @param target Target location
     */
    private void animateCardImprove(int target) {
        Position position = (target < 10 ? mRowTop[target] : mRowBottom[target - 10]);
        mCardAnimationCount++;
        if (target < 10) {
            mCardAnimationTop[target] = new CardImproveAnimation(position);
        } else {
            mCardAnimationBottom[target - 10] = new CardImproveAnimation(position);
        }
        invalidate();
    }

    /**
     * Start hero appear animation
     */
    private void animateHeroAppear() {
        mRowBottom[1].setCard(Card.getHero());
        isHeroAnimated = true;
        mHeroAnimation = new HeroAppearAnimation();
        invalidate();
    }

    /**
     * Start hero disappear animation
     */
    private void animateHeroVanish() {
        isHeroAnimated = true;
        mHeroAnimation = new HeroVanishAnimation();
        invalidate();
    }

    /**
     * Start hero win animation
     */
    private void animateHeroWin() {
        isHeroAnimated = true;
        mHeroAnimation = new HeroWinAnimation();
        invalidate();
    }

    /**
     * Start card discard animation
     * @param target Target location
     */
    private void animateCardDiscard(int target) {
        Position position = (target < 10 ? mRowTop[target] : mRowBottom[target - 10]);
        isDiscarding = true;
        mDiscardAnimation = new CardDiscardAnimation(position);
        invalidate();
    }


    //----------------------------------------------------------------------------------------------

    /**
     * Class which describes one position on the deck
     */
    public static class Position {
        private Card card;
        private RectF rect;

        public Card getCard() {
            return card;
        }

        public void setCard(Card card) {
            this.card = card;
        }

        public void setRect(float left, float top, float right, float bottom) {
            this.rect = new RectF(left, top, right, bottom);
        }

        public boolean contains(float x, float y) {
            return (rect != null && rect.contains(x, y));
        }

        public RectF getRect() {
            return rect;
        }
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Class which describes position of the discard box
     */
    private static class Box {
        private RectF rect;

        public void setRect(float left, float top, float right, float bottom) {
            this.rect = new RectF(left, top, right, bottom);
        }

        public boolean contains(float x, float y) {
            return (rect != null && rect.contains(x, y));
        }

        public RectF getRect() {
            return rect;
        }

    }

    //----------------------------------------------------------------------------------------------

    /**
     * Animation class for card dealing
     */
    private class DeckDealAnimation implements Animation {

        private Card card;
        private Position position;

        private float curRelX;
        private float curRelY;
        private float curScale;

        private float dx;
        private float dy;
        private float ds;

        private int ticksLeft;

        public DeckDealAnimation(Card card, Position position) {
            this.card = card;
            this.position = position;
            RectF rect = position.getRect();
            curRelX = mDeckPosition.left - rect.left;
            curRelY = mDeckPosition.top - rect.top;
            curScale = mDeckPosition.width() / rect.width();
            ticksLeft = (int) (Math.sqrt(curRelX * curRelX + curRelY * curRelY) / mDragReturnSpeed);
            dx = -curRelX / ticksLeft;
            dy = -curRelY / ticksLeft;
            ds = (1 - curScale) / ticksLeft;
        }

        @Override
        public boolean isFinished() {
            return ticksLeft <= 0;
        }

        @Override
        public void tick() {
            curRelX += dx;
            curRelY += dy;
            curScale += ds;
            ticksLeft--;
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.save();
            canvas.translate(curRelX, curRelY);
            canvas.scale(curScale, curScale, position.getRect().left, position.getRect().top);
            drawCard(canvas, card, position.getRect(), CardState.REGULAR);
            canvas.restore();
        }

        @Override
        public void finish() {
            this.position.setCard(this.card);
        }

    }

    //----------------------------------------------------------------------------------------------

    /**
     * Animation class for card dealing
     */
    private class DeckReceiveAnimation implements Animation {

        private Card card;
        private Position position;

        private float curRelX;
        private float curRelY;
        private float curScale;

        private float dx;
        private float dy;
        private float ds;

        private int ticksLeft;

        public DeckReceiveAnimation(Card card, Position position) {
            this.card = card;
            this.position = position;
            RectF rect = position.getRect();
            curRelX = mDeckPosition.left - rect.left;
            curRelY = mDeckPosition.top - rect.top;
            ticksLeft = (int) (Math.sqrt(curRelX * curRelX + curRelY * curRelY) / mDragReturnSpeed);
            curRelX = 0;
            curRelY = 0;
            curScale = 1;
            dx = (rect.left - mDeckPosition.left) / ticksLeft;
            dy = (rect.top - mDeckPosition.bottom) / ticksLeft;
            ds = (1 - mDeckPosition.width() / rect.width()) / ticksLeft;
        }

        @Override
        public boolean isFinished() {
            return ticksLeft <= 0;
        }

        @Override
        public void tick() {
            curRelX -= dx;
            curRelY -= dy;
            curScale -= ds;
            ticksLeft--;
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.save();
            canvas.translate(curRelX, curRelY);
            canvas.scale(curScale, curScale, position.getRect().left, position.getRect().top);
            drawCard(canvas, card, position.getRect(), CardState.REGULAR);
            canvas.restore();
        }

        @Override
        public void finish() {
            mDeck.receive(this.card);
        }

    }

    //----------------------------------------------------------------------------------------------

    /**
     * Animation class for card dealing
     */
    private class DeckDropAnimation implements Animation {

        private Card card;
        private Position position;

        private float curRelY;
        private float dy;

        private int ticksLeft;

        public DeckDropAnimation(Card card, Position position) {
            this.card = card;
            this.position = position;
            RectF rect = position.getRect();
            curRelY = getMeasuredHeight() - rect.top;
            ticksLeft = (int) (curRelY / mDragReturnSpeed * 2);
            dy = curRelY / ticksLeft;
            curRelY = 0;
        }

        @Override
        public boolean isFinished() {
            return ticksLeft <= 0;
        }

        @Override
        public void tick() {
            curRelY += dy;
            ticksLeft--;
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.save();
            canvas.translate(0, curRelY);
            drawCard(canvas, card, position.getRect(), CardState.REGULAR);
            canvas.restore();
        }

        @Override
        public void finish() {
        }

    }

    //----------------------------------------------------------------------------------------------

    /**
     * Animation class for hero appearing
     */
    private class HeroAppearAnimation implements Animation {

        private float curRelHeight;
        private float dh;

        private int ticksLeft;

        public HeroAppearAnimation() {
            RectF rect = mRowBottom[1].getRect();
            curRelHeight = rect.bottom;
            ticksLeft = (int) ((rect.bottom - rect.top) / mDragReturnSpeed * 6);
            dh = -rect.height() / ticksLeft;
        }

        @Override
        public boolean isFinished() {
            return ticksLeft <= 0;
        }

        @Override
        public void tick() {
            curRelHeight += dh;
            ticksLeft--;
        }

        @Override
        public void draw(Canvas canvas) {
            RectF rect = mRowBottom[1].getRect();
            canvas.save();
            canvas.clipRect(rect.left, curRelHeight, rect.right, rect.bottom);
            drawPosition(canvas, mRowBottom[1], CardState.REGULAR);
            canvas.restore();
        }

        @Override
        public void finish() {
        }
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Animation class for hero disappearing
     */
    private class HeroVanishAnimation implements Animation {

        private float curRelHeight;
        private float curTint;
        private float dh;
        private float dt;

        private int ticksLeft;

        public HeroVanishAnimation() {
            RectF rect = mRowBottom[1].getRect();
            curRelHeight = rect.top;
            ticksLeft = (int) ((rect.bottom - rect.top) / mDragReturnSpeed * 8);
            curTint = 0;
            dt = 221f / ticksLeft;
            dh = rect.height() / ticksLeft;
        }

        @Override
        public boolean isFinished() {
            return ticksLeft <= 0;
        }

        @Override
        public void tick() {
            curRelHeight += dh;
            curTint += dt;
            ticksLeft--;
        }

        @Override
        public void draw(Canvas canvas) {
            RectF rect = mRowBottom[1].getRect();
            canvas.save();
            canvas.clipRect(rect.left, curRelHeight, rect.right, rect.bottom);
            drawPosition(canvas, mRowBottom[1], CardState.REGULAR);
            canvas.restore();
            canvas.drawColor(Color.argb((int) curTint, 0, 0, 0));
        }

        @Override
        public void finish() {
            mRowBottom[1].setCard(null);
            isGameOver = true;
        }
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Animation class for card improvement
     */
    private class CardImproveAnimation implements Animation {

        private final float[] CS = {1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.34f, 1.16f, 1f};

        private Position position;
        private RectF rect;
        private int mCurrentStep;

        public CardImproveAnimation(Position position) {
            this.position = position;
            rect = position.getRect();
            mCurrentStep = 0;
        }

        @Override
        public boolean isFinished() {
            return mCurrentStep >= CS.length;
        }

        @Override
        public void tick() {
            mCurrentStep++;
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.save();
            canvas.scale(CS[mCurrentStep], CS[mCurrentStep], rect.left + rect.width() / 2, rect.top + rect.height() / 2);
            drawPosition(canvas, position, CardState.REGULAR);
            canvas.restore();
        }

        @Override
        public void finish() {
        }
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Animation class for card breaking
     */
    private class CardCrackAnimation implements Animation {

        private Card card;
        private Position position;
        private float curDistance;
        private float dd;

        private Path[] mPaths = new Path[4];

        private int ticksLeft;

        public CardCrackAnimation(Card card, Position position) {
            this.card = card;
            this.position = position;
            RectF rect = position.getRect();
            curDistance = 0;
            ticksLeft = 9;
            dd = rect.width() / ticksLeft / 2;
            float cx = (rect.right + rect.left) / 2;
            float cy = (rect.bottom + rect.top) / 2;
            Path path = new Path();
            path.moveTo(rect.left, rect.top);
            path.lineTo(cx, cy);
            path.lineTo(rect.left, rect.bottom);
            path.close();
            mPaths[0] = path;
            path = new Path();
            path.moveTo(rect.right, rect.top);
            path.lineTo(cx, cy);
            path.lineTo(rect.left, rect.top);
            path.close();
            mPaths[1] = path;
            path = new Path();
            path.moveTo(rect.right, rect.bottom);
            path.lineTo(cx, cy);
            path.lineTo(rect.right, rect.top);
            path.close();
            mPaths[2] = path;
            path = new Path();
            path.moveTo(rect.left, rect.bottom);
            path.lineTo(cx, cy);
            path.lineTo(rect.right, rect.bottom);
            path.close();
            mPaths[3] = path;
        }

        @Override
        public boolean isFinished() {
            return ticksLeft <= 0;
        }

        @Override
        public void tick() {
            curDistance += dd;
            ticksLeft--;
        }

        @Override
        public void draw(Canvas canvas) {
            RectF rect = position.getRect();
            canvas.save();
            canvas.translate(-curDistance, 0);
            canvas.clipPath(mPaths[0]);
            drawCard(canvas, card, rect, CardState.REGULAR);
            canvas.restore();
            canvas.save();
            canvas.translate(0, -curDistance);
            canvas.clipPath(mPaths[1]);
            drawCard(canvas, card, rect, CardState.REGULAR);
            canvas.restore();
            canvas.save();
            canvas.translate(curDistance, 0);
            canvas.clipPath(mPaths[2]);
            drawCard(canvas, card, rect, CardState.REGULAR);
            canvas.restore();
            canvas.save();
            canvas.translate(0, curDistance);
            canvas.clipPath(mPaths[3]);
            drawCard(canvas, card, rect, CardState.REGULAR);
            canvas.restore();
        }

        @Override
        public void finish() {
        }
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Animation class for card suffering
     */
    private class CardSufferAnimation implements Animation {

        private Position position;

        private final float[] CX = {0f, -1f, 1f, -1f, 1f, -0.8f, 1f, 0f, 1f, 1f, -1f};
        private final float[] CY = {0f, -0.8f, 0f, 0.8f, 0f, 0.8f, -0.8f, 0f, 0.8f, 0f, 0.8f};

        private float mStepLength;
        private int mCurrentStep;

        public CardSufferAnimation(Position position) {
            this.position = position;
            RectF rect = position.getRect();
            mStepLength = rect.width() / 12;
            mCurrentStep = 0;
        }

        @Override
        public boolean isFinished() {
            return mCurrentStep >= CX.length;
        }

        @Override
        public void tick() {
            mCurrentStep++;
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.save();
            canvas.translate(mStepLength * CX[mCurrentStep], mStepLength * CY[mCurrentStep]);
            drawPosition(canvas, position, CardState.REGULAR);
            canvas.restore();
        }

        @Override
        public void finish() {
        }

    }

    //----------------------------------------------------------------------------------------------

    /**
     * Animation class for card discarding
     */
    private class CardDiscardAnimation implements Animation {

        private Position position;

        private float curRelX;
        private float curRelY;
        private float curScale;

        private float dx;
        private float dy;
        private float ds;

        private int ticksLeft;

        public CardDiscardAnimation(Position position) {
            this.position = position;
            RectF box = mDiscardBox.getRect();
            RectF rect = position.getRect();
            curRelX = mDragRelX;
            curRelY = mDragRelY;
            curScale = 1;
            ticksLeft = 8;
            dx = (box.left + box.width() / 2 - (rect.left + mDragRelX)) / ticksLeft;
            dy = (box.top + box.height() / 2 - (rect.top + mDragRelY)) / ticksLeft;
            ds = -1f / ticksLeft;
        }

        @Override
        public boolean isFinished() {
            return ticksLeft <= 0;
        }

        @Override
        public void tick() {
            curRelX += dx;
            curRelY += dy;
            curScale += ds;
            ticksLeft--;
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.save();
            canvas.translate(curRelX, curRelY);
            canvas.scale(curScale, curScale, position.getRect().left, position.getRect().top);
            drawPosition(canvas, position, CardState.REGULAR);
            canvas.restore();
        }

        @Override
        public void finish() {
            position.setCard(null);
        }

    }

    //----------------------------------------------------------------------------------------------

    /**
     * Animation class for card discarding
     */
    private class HeroWinAnimation implements Animation {

        private Position position;

        private float curRelX;
        private float curRelY;
        private float curScale;

        private float dx;
        private float dy;
        private float ds;

        private int ticksLeft;

        public HeroWinAnimation() {
            this.position = mRowBottom[1];
            RectF rect = position.getRect();
            curRelX = (getMeasuredWidth() / 2 - rect.left - rect.width() / 2);
            curRelY = (getMeasuredHeight() / 2 - rect.top - rect.height() / 2);
            curScale = 1;
            ticksLeft = (int) (Math.sqrt(curRelX * curRelX + curRelY + curRelY)) / mDragReturnSpeed * 20;
            dx = curRelX / ticksLeft;
            dy = curRelY / ticksLeft;
            curRelX = 0;
            curRelY = 0;
            ds = 1f / ticksLeft;
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public void tick() {
            if (ticksLeft > 0) {
                curRelX += dx;
                curRelY += dy;
                curScale += ds;
                ticksLeft--;
            }
        }

        @Override
        public void draw(Canvas canvas) {
            RectF rect = position.getRect();
            canvas.save();
            canvas.translate(curRelX, curRelY);
            canvas.scale(curScale, curScale, rect.left + rect.width() / 2, rect.top + rect.height() / 2);
            drawPosition(canvas, position, CardState.REGULAR);
            canvas.restore();
        }

        @Override
        public void finish() {
            position.setCard(null);
        }

    }


}
