package org.alexsem.cc.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import org.alexsem.cc.model.Animation;
import org.alexsem.cc.model.Card;
import org.alexsem.cc.model.Deck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BoardView extends View {

    private final int COLOR_BG_MAIN = 0xffededed;
    private final int COLOR_BG_CARD = 0xffc0c0c0;
    private final int COLOR_BG_TINT = 0xa0000000;
    private final int COLOR_REGULAR = 0xff000000;
    private final int COLOR_EMPH = 0xff922b22;
    private final int COLOR_SPECIAL = 0xff2cc5c6;

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
    private int mLongTouchedLocation = -1;
    private Runnable mLongTouchRunnable;
    private boolean isRestartTouched = false;

    private Deck mDeck;
    private Box mDiscardBox;
    private Position[] mRowTop = new Position[4];
    private Position[] mRowBottom = new Position[4];
    private List<Card> mGraveyard = new ArrayList<>();
    private int mCoins;
    private int mHealthAddition;
    private int mDamageTakenDuringTurn;
    private int mBountyTargetsDelivered;
    private boolean isFreshDeal;
    private boolean isDamageTakenDuringTurn;
    private boolean isNeedToReviveHero;
    private boolean isNeedToReflectDamage;
    private RectF mDeckPosition;
    private RectF mRestartButton;

    private int mDealAnimationCount = 0;
    private Animation[] mDealAnimationTop = new Animation[4];
    private Animation[] mDealAnimationBottom = new Animation[4];
    private int mReceiveAnimationCount = 0;
    private Animation[] mReceiveAnimationTop = new Animation[4];
    private Animation[] mReceiveAnimationBottom = new Animation[4];
    private int mDropAnimationCount = 0;
    private Animation[] mDropAnimationBottom = new Animation[4];
    private boolean isHeroAnimated = false;
    private Animation mHeroAnimation = null;
    private int mCardAnimationCount = 0;
    private Animation[] mCardAnimationTop = new Animation[4];
    private Animation[] mCardAnimationBottom = new Animation[4];
    private Animation[] mDisableAnimationTop = new Animation[4];
    private Animation[] mDisableAnimationBottom = new Animation[4];
    private boolean isCoinAnimated = false;
    private Animation mCoinAnimation = null;

    private boolean isBeginning = false;
    private boolean isRestarting = false;
    private boolean isMeasurementChanged = false;
    private boolean isGameOver = false;
    private boolean isHeroWon = false;


    //----------------------------------------------------------------------------------------------

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void checkHardwareAcceleration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }
    }

    public BoardView(Context context) {
        super(context);
        checkHardwareAcceleration();
        setBackgroundColor(COLOR_BG_MAIN);
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
        this.isRestarting = false;
        mDeck = Deck.generateFixed();
        for (int i = 0; i < 4; i++) {
            mRowTop[i] = new Position();
            mRowBottom[i] = new Position();
            mCardAnimationTop[i] = null;
            mCardAnimationBottom[i] = null;
            mDealAnimationTop[i] = null;
            mDealAnimationBottom[i] = null;
            mReceiveAnimationTop[i] = null;
            mReceiveAnimationBottom[i] = null;
            mDropAnimationBottom[i] = null;
            mDisableAnimationTop[i] = null;
            mDisableAnimationBottom[i] = null;
        }
        mGraveyard.clear();
        mHeroAnimation = null;
        mDiscardAnimation = null;
        mDiscardBox = new Box();
        mCoins = 0;
        mHealthAddition = 0;
        mDamageTakenDuringTurn = 0;
        mBountyTargetsDelivered = 0;
        mDealAnimationCount = 0;
        mReceiveAnimationCount = 0;
        mDropAnimationCount = 0;
        mCardAnimationCount = 0;
        isCoinAnimated = false;
        isNeedToReflectDamage = false;
        isNeedToReviveHero = false;
        isHeroAnimated = false;
        isDragging = false;
        isRestartTouched = false;
        isDiscarding = false;
        isGameOver = false;
        isHeroWon = false;
        isMeasurementChanged = true;
        invalidate();

//        Card card = Card.getSpecial(); //TODO
//        card.setAbility(Card.Ability.FEAST);
//        mRowTop[0].setCard(card);
//        mRowTop[1].setCard(Card.getOther(Card.Type.MONSTER, 7));
//        mRowTop[2].setCard(Card.getOther(Card.Type.MONSTER, 7));
//        while (mDeck.size() > 0) {
//            mDeck.deal();
//        }
//        isNeedToReviveHero = true;
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
                destroyCard(mRowBottom[i]);
            }
            if (mRowTop[i].getCard() == null) {
                animateDealCard(mDeck.deal(), i);
            }
        }
        isFreshDeal = true;
        if (!isDamageTakenDuringTurn) {
            mDamageTakenDuringTurn = 0;
        }
        isDamageTakenDuringTurn = false;
    }

    //--------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (mDragReturnTicks > 0 || mDealAnimationCount > 0 || mReceiveAnimationCount > 0 || mDropAnimationCount > 0 || isHeroAnimated || mCardAnimationCount > 0 || isDiscarding) {
            return true;
        }
        float x = e.getX();
        float y = e.getY();
        Position pos;
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!isGameOver && !isHeroWon) {
                    boolean longTouch = false;
                    for (int i = 0; i < 4; i++) {
                        pos = mRowTop[i];
                        if (pos != null && pos.contains(x, y) && canTouchThis(i)) {
                            if (pos.getCard() != null && pos.getCard().getType() == Card.Type.ABILITY) {
                                longTouch = true;
                            }
                            mTouchedLocation = i;
                            mTouchedX = x;
                            mTouchedY = y;
                            invalidate();
                            break;
                        }
                        pos = mRowBottom[i];
                        if (pos != null && pos.contains(x, y) && canTouchThis(10 + i)) {
                            if (pos.getCard() != null && pos.getCard().getType() == Card.Type.ABILITY) {
                                longTouch = true;
                            }
                            mTouchedLocation = 10 + i;
                            mTouchedX = x;
                            mTouchedY = y;
                            invalidate();
                            break;
                        }
                    }
                    if (longTouch) { //Need to perform long touch routine
                        mLongTouchedLocation = mTouchedLocation;
                        mLongTouchRunnable = new Runnable() {
                            @Override
                            public void run() {
                                Card longTouchedCard = null;
                                if (mLongTouchedLocation >= 10) {
                                    longTouchedCard = mRowBottom[mLongTouchedLocation - 10].getCard();
                                } else if (mLongTouchedLocation >= 0) {
                                    longTouchedCard = mRowTop[mLongTouchedLocation].getCard();
                                }
                                if (longTouchedCard != null && longTouchedCard.getType() == Card.Type.ABILITY) {
                                    Toast.makeText(getContext(), longTouchedCard.getAbility().getDescription(), Toast.LENGTH_LONG).show();
                                }
                                mLongTouchedLocation = -1;
                            }
                        };
                        BoardView.this.postDelayed(mLongTouchRunnable, 1500);
                    }
                }
                if (mRestartButton != null && mRestartButton.contains(x, y)) { //Restart touched
                    isRestartTouched = true;
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (!isGameOver && !isHeroWon) {
                    if (!isDragging && mTouchedLocation > -1) {
                        float dx = x - mTouchedX;
                        float dy = y - mTouchedY;
                        if (Math.sqrt(dx * dx + dy * dy) > mEpsDrag) {
                            isDragging = true;
                            mLongTouchedLocation = -1;
                            BoardView.this.removeCallbacks(mLongTouchRunnable);
                        }
                    }
                    if (isDragging) {
                        mDragRelX = x - mTouchedX;
                        mDragRelY = y - mTouchedY;
                        invalidate();
                    }
                }
                if (isRestartTouched && !mRestartButton.contains(x, y)) {
                    isRestartTouched = false;
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
                if (!isGameOver && !isHeroWon) {
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
                }
                if (isRestartTouched && mRestartButton.contains(x, y)) {
                    if (isGameOver || isHeroWon) {
                        begin();
                    } else {
                        for (int i = 0; i < 4; i++) {
                            while (mDeck.size() > 0) {
                                mDeck.deal();
                            }
                            animateReceiveCard(mRowTop[i].getCard(), i, false);
                            destroyCard(mRowTop[i]);
                            if (i != 1) {
                                animateDropCard(mRowBottom[i].getCard(), i + 10);
                                destroyCard(mRowBottom[i]);
                            }
                        }
                        animateHeroVanish();
                        isRestartTouched = false;
                        isRestarting = true;
                    }
                }
                resetTouchFeedback();
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                BoardView.this.removeCallbacks(mLongTouchRunnable);
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
        isRestartTouched = false;
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
                case HERO:
                    return false;
                case MONSTER:
                case WEAPON:
                case ABILITY:
                    return true;
                case SHIELD:
                    return (location != LOC_LEFT_HAND && location != LOC_RIGHT_HAND) || card.getAbility() == Card.Ability.BASH;
                case POTION:
                case COIN:
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
            case MONSTER:
                if (dstCard != null) {
                    return (dstCard.getType() == Card.Type.HERO || (dstCard.getType() == Card.Type.SHIELD && (destination == LOC_LEFT_HAND || destination == LOC_RIGHT_HAND)));
                } else {
                    return false;
                }
            case WEAPON:
                if (dstCard != null) {
                    return (dstCard.getType() == Card.Type.MONSTER && destination < 10 && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                } else {
                    return (source < 10 && destination == LOC_BACKPACK) || ((source < 10 || source == LOC_BACKPACK) && (destination == LOC_LEFT_HAND || destination == LOC_RIGHT_HAND));
                }
            case SHIELD:
                if (dstCard != null) {
                    return (srcCard.getAbility() == Card.Ability.BASH && dstCard.getType() == Card.Type.MONSTER && destination < 10 && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                } else {
                    return (source < 10 && destination == LOC_BACKPACK) || ((source < 10 || source == LOC_BACKPACK) && (destination == LOC_LEFT_HAND || destination == LOC_RIGHT_HAND));
                }
            case POTION:
                if (dstCard != null) {
                    return (srcCard.getAbility() == Card.Ability.POISON && dstCard.getType() == Card.Type.MONSTER && destination < 10 && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                } else {
                    return (source < 10 && destination == LOC_BACKPACK) || ((source < 10 || source == LOC_BACKPACK) && (destination == LOC_LEFT_HAND || destination == LOC_RIGHT_HAND));
                }
            case COIN:
                if (dstCard != null) {
                    return false;
                } else {
                    return (destination == LOC_LEFT_HAND || destination == LOC_RIGHT_HAND || destination == LOC_BACKPACK);
                }
            case ABILITY:
                if (dstCard != null) {
                    switch (srcCard.getAbility()) {
                        case SAP:
                        case EXCHANGE:
                        case TRAP:
                            return (destination < 10 && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case VANISH:
                        case LASH:
                        case BASH:
                        case REFLECT:
                        case REVIVE:
                        case FRENZY:
                        case LUCKY:
                        case LIFE:
                        case BLEED:
                        case SUICIDE:
                        case BOUNTY:
                        case DIGGER:
                        case DOOM:
                        case STAB:
                        case CHAOS:
                        case CHAMPION:
                            return (dstCard.getType() == Card.Type.HERO && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case LEECH:
                        case SACRIFICE:
                        case KILLER:
                            return (dstCard.getType() == Card.Type.MONSTER && destination < 10 && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case POTIONIZE:
                            return ((dstCard.getType() == Card.Type.COIN || dstCard.getType() == Card.Type.POTION || dstCard.getType() == Card.Type.WEAPON || dstCard.getType() == Card.Type.SHIELD) && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case STEAL:
                            return (dstCard.getType() == Card.Type.HERO && mDeck.size() > 0 && mRowBottom[3].getCard() == null && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case BETRAYAL:
                            return (dstCard.getType() == Card.Type.MONSTER && destination < 10 && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case TRADE:
                            return (dstCard.getType() != Card.Type.MONSTER && dstCard.getType() != Card.Type.HERO && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case SWAP:
                        case EQUALIZE:
                        case FEAST:
                            return (destination < 10 && dstCard.getValue() > 0 && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case MORPH:
                        case DEVOUR:
                        case MIRROR:
                            return (dstCard.getType() != Card.Type.HERO && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case FORTIFY:
                        case MIDAS:
                            return (dstCard.getType() != Card.Type.HERO && dstCard.getValue() > 0 && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case BLOODPACT:
                            return (dstCard.getType() == Card.Type.MONSTER && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case POISON:
                            return (dstCard.getType() == Card.Type.POTION && dstCard.getAbility() == null && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
                        case BRIBE:
                            return (destination < 10 && dstCard.getValue() > 0 && mCoins >= dstCard.getValue() && (source == LOC_LEFT_HAND || source == LOC_RIGHT_HAND));
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
            case MONSTER:
                return false;
            case WEAPON:
            case SHIELD:
            case POTION:
            case ABILITY:
                return (location < 10 || location == LOC_BACKPACK);
            case COIN:
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
            case MONSTER:
                if (dstCard != null && dstCard.getType() == Card.Type.HERO) { //Endure attack
                    if (isNeedToReflectDamage) { //Damage reflected
                        int randomTarget;
                        Card randomCard;
                        do {
                            randomTarget = (int) (Math.random() * 4);
                            randomCard = mRowTop[(randomTarget)].getCard();
                        } while (randomCard == null || randomCard.getType() == Card.Type.ABILITY);
                        if (randomCard.getValue() > srcCard.getValue()) { //Target card can take damage
                            randomCard.setValue(randomCard.getValue() - srcCard.getValue());
                            animateCardSuffer(randomTarget);
                        } else { //Card is too weak
                            animateCardCrack(randomCard, randomTarget);
                        }
                        float distance = (float) Math.sqrt(mDragRelX * mDragRelX + mDragRelY * mDragRelY);
                        mDragReturnTicks = (int) (distance / mDragReturnSpeed);
                        mDragSpeedX = mDragRelX / mDragReturnTicks;
                        mDragSpeedY = mDragRelY / mDragReturnTicks;
                        isNeedToReflectDamage = false;
                    } else { //Damage taken
                        takeDamage(srcCard.getValue());
                        destroyCard(srcPosition);
                    }
                } else if (dstCard != null && dstCard.getType() == Card.Type.SHIELD) { //Block attack
                    if (dstCard.getValue() > srcCard.getValue()) { //Shield can take damage (and more)
                        dstCard.setValue(dstCard.getValue() - srcCard.getValue());
                        animateCardSuffer(destination);
                    } else if (dstCard.getValue() == srcCard.getValue()) { //Shield can take exact damage
                        animateCardCrack(dstCard, destination);
                    } else { //Shield is too weak
                        takeDamage((srcCard.getValue() - dstCard.getValue()));
                        animateCardCrack(dstCard, destination);
                    }
                    destroyCard(srcPosition);
                }
                break;
            case WEAPON:
                if (dstCard != null && dstCard.getType() == Card.Type.MONSTER) { //Attack
                    if (dstCard.getValue() > srcCard.getValue()) { //Mob can take damage (and more)
                        dstCard.setValue(dstCard.getValue() - srcCard.getValue());
                        animateCardSuffer(destination);
                    } else { //Mob will be defeated
                        animateCardCrack(dstCard, destination);
                    }
                    if (srcCard.getAbility() == Card.Ability.FRENZY) {
                        srcCard.setAbility(null);
                        animateCardSuffer(source);
                        float distance = (float) Math.sqrt(mDragRelX * mDragRelX + mDragRelY * mDragRelY);
                        mDragReturnTicks = (int) (distance / mDragReturnSpeed);
                        mDragSpeedX = mDragRelX / mDragReturnTicks;
                        mDragSpeedY = mDragRelY / mDragReturnTicks;
                    } else {
                        destroyCard(srcPosition);
                    }
                } else { //Pack/Equip
                    dstPosition.setCard(srcCard);
                    destroyCard(srcPosition);
                }
                break;
            case SHIELD:
                if (dstCard != null) {
                    if (srcCard.getAbility() == Card.Ability.BASH && dstCard.getType() == Card.Type.MONSTER) { //Bash
                        if (dstCard.getValue() > srcCard.getValue()) { //Mob can take damage (and more)
                            dstCard.setValue(dstCard.getValue() - srcCard.getValue());
                            animateCardSuffer(destination);
                        } else { //Mob will be defeated
                            animateCardCrack(dstCard, destination);
                        }
                        if (srcCard.getValue() > 5) {
                            srcCard.setValue(srcCard.getValue() - 5);
                            animateCardSuffer(source);
                            float distance = (float) Math.sqrt(mDragRelX * mDragRelX + mDragRelY * mDragRelY);
                            mDragReturnTicks = (int) (distance / mDragReturnSpeed);
                            mDragSpeedX = mDragRelX / mDragReturnTicks;
                            mDragSpeedY = mDragRelY / mDragReturnTicks;
                        } else {
                            destroyCard(srcPosition);
                        }
                    }
                } else { //Pack/Equip
                    dstPosition.setCard(srcCard);
                    destroyCard(srcPosition);
                }
                break;
            case POTION:
                if (srcCard.getAbility() == Card.Ability.POISON) { //Poison
                    if (dstCard != null && dstCard.getType() == Card.Type.MONSTER) { //Attack
                        if (srcCard.getValue() > 0) {
                            if (dstCard.getValue() > srcCard.getValue()) { //Mob can take damage (and more)
                                dstCard.setValue(dstCard.getValue() - srcCard.getValue());
                                animateCardSuffer(destination);
                            } else { //Mob will be defeated
                                animateCardCrack(dstCard, destination);
                            }
                            for (int i = 0; i < 4; i++) {
                                Card card = mRowTop[i].getCard();
                                if (Math.abs(i - destination) == 1 && card != null && card.getValue() > 0) {
                                    if (card.getValue() > srcCard.getValue() / 2) { //Card can take damage (and more)
                                        card.setValue(card.getValue() - srcCard.getValue() / 2);
                                        animateCardSuffer(i);
                                    } else { //Card will be defeated
                                        animateCardCrack(card, i);
                                    }
                                }
                            }
                        } //TODO miss
                        destroyCard(srcPosition);
                    } else { //Pack/Equip
                        dstPosition.setCard(srcCard);
                        destroyCard(srcPosition);
                    }
                } else { //Regular potion
                    dstPosition.setCard(srcCard);
                    if (destination == LOC_LEFT_HAND || destination == LOC_RIGHT_HAND) { //Actually use
                        animateCardDisable(destination);
                        hero.setValue(Math.min(Card.HERO_MAX + mHealthAddition, hero.getValue() + srcCard.getValue()));
                        animateCardImprove(LOC_HERO);
                        srcCard.setValue(0);
                    }
                    destroyCard(srcPosition);
                }
                break;
            case COIN:
                animateCardDisable(destination);
                dstPosition.setCard(srcCard);
                addCoins(srcCard.getValue());
                srcCard.setValue(0);
                destroyCard(srcPosition);
                break;
            case ABILITY:
                if ((destination == LOC_RIGHT_HAND || destination == LOC_LEFT_HAND || destination == LOC_BACKPACK) && source != LOC_LEFT_HAND && source != LOC_RIGHT_HAND) { //Pack/Equip
                    dstPosition.setCard(srcCard);
                    destroyCard(srcPosition);
                } else { //Actually use
                    switch (srcCard.getAbility()) {
                        case SAP:
                            animateReceiveCard(dstCard, destination, true);
                            destroyCard(dstPosition);
                            destroyCard(srcPosition);
                            break;
                        case VANISH:
                            for (int i = 0; i < 4; i++) {
                                if (mRowTop[i].getCard() != null) {
                                    animateReceiveCard(mRowTop[i].getCard(), i, true);
                                    destroyCard(mRowTop[i]);
                                }
                            }
                            destroyCard(srcPosition);
                            break;
                        case LEECH:
                            int leeched = Math.min(srcCard.getValue(), dstCard.getValue());
                            if (dstCard.getValue() > leeched) { //Mob can take damage (and more)
                                dstCard.setValue(dstCard.getValue() - leeched);
                                animateCardSuffer(destination);
                            } else { //Mob will be defeated
                                animateCardCrack(dstCard, destination);
                            }
                            destroyCard(srcPosition);
                            hero.setValue(Math.min(Card.HERO_MAX + mHealthAddition, hero.getValue() + leeched));
                            animateCardImprove(LOC_HERO);
                            break;
                        case SACRIFICE:
                            int sacrificed = Card.HERO_MAX + mHealthAddition - hero.getValue();
                            if (sacrificed > 0) {
                                if (dstCard.getValue() > sacrificed) { //Mob can take damage (and more)
                                    dstCard.setValue(dstCard.getValue() - sacrificed);
                                    animateCardSuffer(destination);
                                } else { //Mob will be defeated
                                    animateCardCrack(dstCard, destination);
                                }
                            } //TODO miss
                            destroyCard(srcPosition);
                            break;
                        case POTIONIZE:
                            Card potionCard = Card.clone(dstCard);
                            potionCard.setType(Card.Type.POTION);
                            potionCard.setValue((int) (Math.random() * 9 + 2));
                            potionCard.setInitialValue(potionCard.getValue());
                            potionCard.setAbility(null);
                            if (destination >= 10) { //Bottom row
                                potionCard.setActive(true);
                            }
                            animateCardTransform(destination, potionCard);
                            destroyCard(srcPosition);
                            break;
                        case KILLER:
                            if (dstCard.getValue() < dstCard.getInitialValue()) {
                                animateCardCrack(dstCard, destination);
                            } //TODO miss
                            destroyCard(srcPosition);
                            break;
                        case EXCHANGE:
                            animateReceiveCard(dstCard, destination, true);
                            destroyCard(dstPosition);
                            Card exchangedCard = mDeck.deal(mDeck.find(Card.Type.ABILITY));
                            if (exchangedCard != null) {
                                animateDealCard(exchangedCard, destination);
                            }
                            destroyCard(srcPosition);
                            break;
                        case STEAL:
                            Card stolenCard = mDeck.deal();
                            if (stolenCard.getType() == Card.Type.COIN) { //Coins
                                animateCardDisable(LOC_BACKPACK);
                                addCoins(stolenCard.getValue());
                                stolenCard.setValue(0);
                            }
                            animateDealCard(stolenCard, LOC_BACKPACK);
                            destroyCard(srcPosition);
                            break;
                        case LASH:
                            boolean leftToRight = (Math.random() < 0.5f);
                            int maxLash = (int) (Math.random() * 3) + 1;
                            for (int lashedCount = 0, lashIndex = leftToRight ? 0 : 3; lashIndex >= 0 && lashIndex <= 3 && lashedCount < maxLash; lashIndex += (leftToRight ? 1 : -1)) {
                                Card card = mRowTop[lashIndex].getCard();
                                if (card != null && (card.getType() == Card.Type.MONSTER)) {
                                    if (card.getValue() > srcCard.getValue()) { //Card can take damage (and more)
                                        card.setValue(card.getValue() - srcCard.getValue());
                                        animateCardSuffer(lashIndex);
                                    } else { //Card will be defeated
                                        animateCardCrack(mRowTop[lashIndex].getCard(), lashIndex);
                                    }
                                    lashedCount++;
                                }
                            }
                            destroyCard(srcPosition);
                            break;
                        case BASH:
                            int shieldLocation = source == LOC_LEFT_HAND ? LOC_RIGHT_HAND : LOC_LEFT_HAND;
                            Card shieldCard = mRowBottom[shieldLocation - 10].getCard();
                            if (shieldCard != null && shieldCard.getType() == Card.Type.SHIELD) {
                                shieldCard.setAbility(Card.Ability.BASH);
                                animateCardImprove(shieldLocation);
                            }
                            destroyCard(srcPosition);
                            break;
                        case REFLECT:
                            isNeedToReflectDamage = true;
                            animateCardImprove(LOC_HERO);
                            destroyCard(srcPosition);
                            break;
                        case BETRAYAL:
                            for (int i = 0; i < 4; i++) {
                                Card card = mRowTop[i].getCard();
                                if (Math.abs(i - destination) == 1 && card != null && card.getType() != Card.Type.ABILITY) {
                                    if (card.getValue() > dstCard.getValue()) { //Card can take damage (and more)
                                        card.setValue(card.getValue() - dstCard.getValue());
                                        animateCardSuffer(i);
                                    } else { //Card will be defeated
                                        animateCardCrack(mRowTop[i].getCard(), i);
                                    }
                                }
                            }
                            destroyCard(srcPosition);
                            break;
                        case REVIVE:
                            isNeedToReviveHero = true;
                            animateCardImprove(LOC_HERO);
                            destroyCard(srcPosition);
                            break;
                        case FRENZY:
                            int swordLocation = source == LOC_LEFT_HAND ? LOC_RIGHT_HAND : LOC_LEFT_HAND;
                            Card swordCard = mRowBottom[swordLocation - 10].getCard();
                            if (swordCard != null && swordCard.getType() == Card.Type.WEAPON) {
                                swordCard.setAbility(Card.Ability.FRENZY);
                                animateCardImprove(swordLocation);
                            }
                            destroyCard(srcPosition);
                            break;
                        case LUCKY:
                            int randomCount = 2 - (int) (Math.random() * 3) / 2;
                            int previousTarget;
                            for (int i = 0; i < randomCount; i++) {
                                int randomTarget;
                                Card randomCard;
                                do {
                                    randomTarget = (int) (Math.random() * 4);
                                    randomCard = mRowTop[(randomTarget)].getCard();
                                } while (randomCard == null);
                                previousTarget = randomTarget;
                                animateCardCrack(randomCard, randomTarget);
                                boolean cardsLeft = false;
                                for (int j = 0; j < mRowTop.length; j++) {
                                    if (mRowTop[j].getCard() != null && j != previousTarget) {
                                        cardsLeft = true;
                                        break;
                                    }
                                }
                                if (!cardsLeft) { //No more cards left
                                    break;
                                }
                            }
                            destroyCard(srcPosition);
                            break;
                        case TRADE:
                            addCoins(10);
                            mDragRelX = 0;
                            mDragRelY = 0;
                            mTouchedLocation = destination;
                            animateCardDiscard(destination);
                            destroyCard(srcPosition);
                            break;
                        case SWAP:
                            boolean left2Right = (Math.random() < 0.5f);
                            for (int swapIndex = left2Right ? 0 : 3; swapIndex >= 0 && swapIndex <= 3; swapIndex += (left2Right ? 1 : -1)) {
                                Card card = mRowTop[swapIndex].getCard();
                                if (Math.abs(swapIndex - destination) != 1 || card == null) {
                                    continue;
                                }
                                if (card.getValue() > 0) {
                                    int tempValue = dstCard.getValue();
                                    dstCard.setValue(card.getValue());
                                    card.setValue(tempValue);
                                    if (tempValue > dstCard.getValue()) { //Adjacent card improved
                                        animateCardImprove(swapIndex);
                                        animateCardSuffer(destination);
                                    } else if (tempValue < dstCard.getValue()) { //Adjacent card value decreased
                                        animateCardSuffer(swapIndex);
                                        animateCardImprove(destination);
                                    } else { //Equality
                                        animateCardImprove(swapIndex);
                                        animateCardImprove(destination);
                                    }
                                    break;
                                }
                            } //TODO miss
                            destroyCard(srcPosition);
                            break;
                        case MORPH:
                            Card randomCard = Card.random();
                            if (destination < 10 && randomCard.getType() == Card.Type.MONSTER) {
                                randomCard.setActive(mRowTop[destination].getCard().isActive());
                            }
                            animateCardTransform(destination, randomCard);
                            destroyCard(srcPosition);
                            break;
                        case FORTIFY:
                            dstCard.setValue(dstCard.getValue() + srcCard.getValue());
                            animateCardImprove(destination);
                            destroyCard(srcPosition);
                            break;
                        case MIDAS:
                            Card coinCard = Card.clone(dstCard);
                            switch (dstCard.getType()) {
                                case MONSTER:
                                    coinCard.setValue(dstCard.getValue() / 2);
                                    break;
                                case ABILITY:
                                    coinCard.setValue(dstCard.getValue() * 2);
                                    break;
                            }
                            coinCard.setInitialValue(coinCard.getValue());
                            coinCard.setType(Card.Type.COIN);
                            coinCard.setActive(true);
                            animateCardTransform(destination, coinCard);
                            destroyCard(srcPosition);
                            break;
                        case DEVOUR:
                            Card specialCard = Card.getSpecial();
                            animateCardTransform(destination, specialCard);
                            destroyCard(srcPosition);
                            break;
                        case TRAP:
                            animateCardDisable(destination);
                            destroyCard(srcPosition);
                            break;
                        case LIFE:
                            hero.setValue(hero.getValue() + srcCard.getValue());
                            if (hero.getValue() > Card.HERO_MAX + mHealthAddition) {
                                mHealthAddition = hero.getValue() - Card.HERO_MAX;
                            }
                            animateCardImprove(LOC_HERO);
                            destroyCard(srcPosition);
                            break;
                        case BLEED:
                            if (mDamageTakenDuringTurn > 0) {
                                addCoins(mDamageTakenDuringTurn);
                            } //TODO miss
                            destroyCard(srcPosition);
                            break;
                        case SUICIDE:
                            for (int i = 0; i < mRowTop.length; i++) {
                                if (mRowTop[i].getCard() != null) {
                                    animateReceiveCard(mRowTop[i].getCard(), i, false);
                                    destroyCard(mRowTop[i]);
                                }
                                animateDealCard(Card.getOther(Card.Type.MONSTER, (int) (Math.random() * 9 + 2)), i);
                            }
                            destroyCard(srcPosition);
                            break;
                        case BLOODPACT:
                            Card heroCard = mRowBottom[1].getCard();
                            int tempHp = dstCard.getValue();
                            dstCard.setValue(heroCard.getValue());
                            if (tempHp > dstCard.getValue()) { //Hero health increased
                                heroCard.setValue(tempHp);
                                animateCardImprove(LOC_HERO);
                                animateCardSuffer(destination);
                            } else { //Mob health increased\
                                takeDamage(dstCard.getValue() - tempHp);
                                animateCardImprove(destination);
                            } //TODO miss
                            destroyCard(srcPosition);
                            break;
                        case BOUNTY:
                            if (mBountyTargetsDelivered > 0) {
                                addCoins(mBountyTargetsDelivered * srcCard.getValue());
                            } //TODO miss
                            destroyCard(srcPosition);
                            break;
                        case EQUALIZE:
                            for (int i = 0; i < 4; i++) {
                                Card card = mRowTop[i].getCard();
                                if (Math.abs(i - destination) == 1 && card != null && card.getValue() > 0) {
                                    if (card.getValue() <= dstCard.getValue()) { //Card increased value
                                        animateCardImprove(i);
                                    } else { //Value decreased
                                        animateCardSuffer(i);
                                    }
                                    card.setValue(dstCard.getValue());
                                }
                            }
                            destroyCard(srcPosition);
                            break;
                        case DIGGER:
                            destroyCard(srcPosition);
                            for (int count = 0; count < 3 && mGraveyard.size() > 0; count++) {
                                int random = (int) (Math.random() * mGraveyard.size());
                                Card resedCard = Card.clone(mGraveyard.get(random));
                                resedCard.restoreState();
                                animateReceiveCard(resedCard, count + 10, true);
                                mGraveyard.remove(random);
                            }
                            break;
                        case MIRROR:
                            Card mirroredCard = Card.clone(dstCard);
                            mirroredCard.restoreState();
                            animateReceiveCard(mirroredCard, destination, true);
                            destroyCard(srcPosition);
                            break;
                        case POISON:
                            dstCard.setAbility(Card.Ability.POISON);
                            dstCard.setActive(true);
                            animateCardImprove(destination);
                            destroyCard(srcPosition);
                            break;
                        case DOOM:
                            destroyCard(srcPosition);
                            for (int i = 0; i < 4; i++) {
                                if (mRowTop[i].getCard() != null) {
                                    animateCardCrack(mRowTop[i].getCard(), i);
                                }
                                if (i == 1) { //Hero card
                                    takeDamage(mRowBottom[i].getCard().getValue() - 1);
                                    continue;
                                }
                                if (mRowBottom[i].getCard() != null) {
                                    animateCardCrack(mRowBottom[i].getCard(), i + 10);
                                }
                            }
                            break;
                        case BRIBE:
                            addCoins(-dstCard.getValue());
                            animateReceiveCard(dstCard, destination, false);
                            destroyCard(dstPosition);
                            destroyCard(srcPosition);
                            break;
                        case STAB:
                            int stabTarget;
                            do {
                                stabTarget = (int) (Math.random() * 4);
                            } while (mRowTop[stabTarget].getCard() == null);
                            for (int i = 0; i < 4; i++) {
                                Card card = mRowTop[i].getCard();
                                if (i == stabTarget) {
                                    animateCardCrack(card, stabTarget);
                                } else {
                                    if (card != null) {
                                        animateReceiveCard(card, i, true);
                                        destroyCard(mRowTop[i]);
                                    }
                                }
                            }
                            destroyCard(srcPosition);
                            break;
                        case FEAST:
                            int feastedValue = 0;
                            for (int i = 0; i < 4; i++) {
                                Card card = mRowTop[i].getCard();
                                if (Math.abs(i - destination) == 1 && card != null && card.getValue() > 0) {
                                    feastedValue += card.getValue();
                                    animateCardCrack(card, i);
                                }
                            }
                            if (feastedValue > 0) {
                                if (dstCard.getValue() > feastedValue) {
                                    animateCardSuffer(destination);
                                } else {
                                    animateCardImprove(destination);
                                }
                                dstCard.setValue(feastedValue);
                            } //TODO miss
                            destroyCard(srcPosition);
                            break;
                        case CHAOS:
                            ArrayList<Integer> valuedPositions = new ArrayList<>();
                            ArrayList<Integer> valuedValues = new ArrayList<>();
                            for (int i = 0; i < 4; i++) {
                                Card card = mRowTop[i].getCard();
                                if (card != null && card.getValue() > 0) {
                                    valuedPositions.add(i);
                                    valuedValues.add(card.getValue());
                                }
                                card = mRowBottom[i].getCard();
                                if (card != null && card.getValue() > 0) {
                                    valuedPositions.add(i + 10);
                                    valuedValues.add(card.getValue());
                                }
                            }
                            Collections.shuffle(valuedValues);
                            for (int i = 0; i < valuedPositions.size(); i++) {
                                int position = valuedPositions.get(i);
                                int value = valuedValues.get(i);
                                Card card = position < 10 ? mRowTop[position].getCard() : mRowBottom[position - 10].getCard();
                                if (position == LOC_HERO && card.getValue() > value) { //Damage to Hero card
                                    takeDamage(card.getValue() - value);
                                    continue;
                                }
                                if (card.getValue() > value) {
                                    animateCardSuffer(position);
                                } else {
                                    animateCardImprove(position);
                                }
                                card.setValue(value);
                            }
                            destroyCard(srcPosition);
                            break;
                        case CHAMPION:
                            destroyCard(srcPosition);
                            while (mDeck.size() > 0) {
                                mDeck.deal();
                            }
                            for (int i = 0; i < 4; i++) {
                                if (mRowTop[i].getCard() != null) {
                                    animateReceiveCard(mRowTop[i].getCard(), i, false);
                                    destroyCard(mRowTop[i]);
                                }
                                if (i != 1 && mRowBottom[i].getCard() != null) {
                                    animateDropCard(mRowBottom[i].getCard(), i + 10);
                                    destroyCard(mRowBottom[i]);
                                }
                            }
                            break;
                    }
                }
                break;
        }
        processMove();
    }

    /**
     * Apply damage to Hero card
     * @param amount Damage to inflict
     */
    private void takeDamage(int amount) {
        if (!isDamageTakenDuringTurn) {
            mDamageTakenDuringTurn = 0;
            isDamageTakenDuringTurn = true;
        }
        mDamageTakenDuringTurn += amount;
        mRowBottom[1].getCard().setValue(Math.max(0, mRowBottom[1].getCard().getValue() - amount));
        animateCardSuffer(LOC_HERO);
    }

    /**
     * Destroy card at specified position, moving it to the graveyard
     * @param position Card position
     */
    private void destroyCard(Position position) {
        Card card = position.getCard();
        if (card != null) {
            mGraveyard.add(card);
            if (card.getType() == Card.Type.MONSTER && card.getAbility() == Card.Ability.BOUNTY) {
                mBountyTargetsDelivered++;
            }
            position.setCard(null);
        }
    }

    /**
     * Adds coins to hero bank
     * @param amount Number of coins to add
     */
    private void addCoins(int amount) {
        if (isCoinAnimated && mCoinAnimation != null) {
            ((CoinAddAnimation) mCoinAnimation).addAmount(amount);
        } else {
            isCoinAnimated = true;
            mCoinAnimation = new CoinAddAnimation(amount);
        }
        invalidate();
    }

    /**
     * Discards card
     * @param location Card location
     */
    private void doDiscard(int location) {
        Position position = location >= 10 ? mRowBottom[location - 10] : mRowTop[location];
        Card card = position.getCard();

        switch (card.getType()) {
            case ABILITY:
                animateCardDiscard(location);
                break;
            case WEAPON:
            case SHIELD:
            case POTION:
                addCoins(card.getValue());
                animateCardDiscard(location);
                break;
            case COIN:
                animateCardDiscard(location);
                break;
        }
    }

    /**
     * Perform necessary calculations after each move
     */
    private void processMove() {
        if (mCardAnimationCount > 0 || mDealAnimationCount > 0 || mReceiveAnimationCount > 0 || isHeroAnimated) { //Animations in progress
            return;
        }
        if (isRestarting) { //Need to restart
            return;
        }
        isFreshDeal = false;
        Card hero = mRowBottom[1].getCard();
        if (hero.getValue() <= 0) { //Check hero health
            if (isNeedToReviveHero) { //Can be revived
                hero.setValue(1);
                animateCardImprove(LOC_HERO);
                isNeedToReviveHero = false;
                return;
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
        int pVert = (width - cw * 4) / 5;
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

        int res = cw * 4 / 15;
        mRestartButton = new RectF(width - pHorz - res, pVert, width - pHorz, pVert + res);

        if (isBeginning) { //Beginning of the game
            animateHeroAppear();
            dealTopRow();
            isBeginning = false;
        }
    }

    //----------------------------------------------------------------------------------------------

    private enum CardState {
        REGULAR, TOUCHED, RECEIVING, MOVED, SHARP
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
            if (state == CardState.SHARP) {
                canvas.drawRect(rect, mPaint);
            } else {
                canvas.drawRoundRect(rect, radius, radius, mPaint);
            }
            mTextPaint.setTextSize(mFontSize);
            String text;
            switch (card.getType()) {
                case HERO:
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
                    if (value >= 24) {
                        canvas.drawArc(new RectF(cx - radius * 7 / 6, cy + radius * 4 / 6 - radius * 2 / 3, cx + radius * 7 / 6, cy + radius * 4 / 3), 0, 180, false, mPaint);
                    } else if (value >= 18) {
                        canvas.drawArc(new RectF(cx - radius * 4 / 6 - radius * (value - 17) / 14, cy + radius * 4 / 6 - radius * 2 / 3, cx + radius * 4 / 6 + radius * (value - 17) / 14, cy + radius * 4 / 3), 0, 180, false, mPaint);
                    } else if (value >= 10) {
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
                    text = String.format("\u2666%d", mCoins);
                    canvas.drawText(text, rect.left + mFontPadding, rect.bottom - mFontSize - mFontPadding - mTextPaint.ascent(), mTextPaint);
                    mTextPaint.setColor(COLOR_SPECIAL);
                    text = String.format("%s%s", isNeedToReflectDamage ? "\u2746" : "", isNeedToReviveHero ? "\u2665" : "");
                    canvas.drawText(text, rect.right - mFontPadding - mTextPaint.measureText(text), rect.bottom - mFontSize - mFontPadding - mTextPaint.ascent(), mTextPaint);
                    break;
                case MONSTER:
                    mPaint.setColor(COLOR_REGULAR);
                    mPaint.setStyle(Paint.Style.FILL);
                    mPaint.setStrokeWidth(STROKE_WIDTH);
                    canvas.drawCircle(cx, cy, radius * 2, mPaint);
                    mPaint.setColor(COLOR_BG_CARD);
                    if (card.getValue() < card.getInitialValue()) {
                        canvas.drawCircle(cx - radius * 2 / 3, cy - radius / 3, radius / 3, mPaint);
                        canvas.drawCircle(cx + radius * 2 / 3, cy - radius / 3, radius / 3, mPaint);
                        mPaint.setStyle(Paint.Style.STROKE);
                        canvas.drawLine(cx - radius / 4, cy - radius * 3 / 4, cx - radius, cy - radius * 2 / 3, mPaint);
                        canvas.drawLine(cx + radius / 4, cy - radius * 3 / 4, cx + radius, cy - radius * 2 / 3, mPaint);
                        canvas.drawLine(cx, cy - radius * 2, cx - radius / 2, cy - radius * 3 / 2, mPaint);
                        canvas.drawLine(cx - radius / 2, cy - radius * 3 / 2, cx + radius * 4 / 7, cy - radius * 13 / 8, mPaint);
                        canvas.drawLine(cx + radius * 4 / 7, cy - radius * 13 / 8, cx, cy - radius * 7 / 6, mPaint);
                    } else {
                        canvas.drawCircle(cx - radius * 2 / 3, cy - radius / 2, radius / 3, mPaint);
                        canvas.drawCircle(cx + radius * 2 / 3, cy - radius / 2, radius / 3, mPaint);
                        mPaint.setStyle(Paint.Style.STROKE);
                        canvas.drawLine(cx - radius / 4, cy - radius * 3 / 4, cx - radius, cy - radius, mPaint);
                        canvas.drawLine(cx + radius / 4, cy - radius * 3 / 4, cx + radius, cy - radius, mPaint);
                    }
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
                case WEAPON: {
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
                    if (card.getAbility() == Card.Ability.FRENZY) {
                        mTextPaint.setColor(COLOR_SPECIAL);
                        text = "\u2605";
                        canvas.drawText(text, rect.right - mFontPadding - mTextPaint.measureText(text), rect.bottom - mFontSize - mFontPadding - mTextPaint.ascent(), mTextPaint);
                    }
                }
                break;
                case SHIELD: {
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
                    if (card.getAbility() == Card.Ability.BASH) {
                        mTextPaint.setColor(COLOR_SPECIAL);
                        text = "\u2725";
                        canvas.drawText(text, rect.right - mFontPadding - mTextPaint.measureText(text), rect.bottom - mFontSize - mFontPadding - mTextPaint.ascent(), mTextPaint);
                    }
                }
                break;
                case POTION: {
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
                    if (card.getAbility() == Card.Ability.POISON) {
                        mTextPaint.setColor(COLOR_SPECIAL);
                        text = "\u2668";
                        canvas.drawText(text, rect.right - mFontPadding - mTextPaint.measureText(text), rect.bottom - mFontSize - mFontPadding - mTextPaint.ascent(), mTextPaint);
                    }
                }
                break;
                case COIN: {
                    mPaint.setColor(COLOR_REGULAR);
                    mPaint.setStyle(Paint.Style.STROKE);
                    mPaint.setStrokeWidth(STROKE_WIDTH);
                    canvas.drawCircle(cx, cy, radius * 3 / 2, mPaint);
                    float rad = radius * 3 / (2 * (float) Math.sqrt(2));
                    canvas.drawLine(cx + rad, cy - rad, cx - rad, cy + rad, mPaint);
                    mTextPaint.setColor(COLOR_EMPH);
                    text = String.format("%d", card.getValue());
                    canvas.drawText(text, rect.left + mFontPadding, rect.bottom - mFontSize - mFontPadding - mTextPaint.ascent(), mTextPaint);
                }
                break;
                case ABILITY: {
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

        if (state != CardState.SHARP) {
            mPaint.setStrokeWidth(STROKE_WIDTH);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(state == CardState.TOUCHED ? COLOR_EMPH : state == CardState.RECEIVING ? COLOR_SPECIAL : COLOR_REGULAR);
            canvas.drawRoundRect(rect, radius, radius, mPaint);
        }
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
                if (i == 1 && (isHeroAnimated || isHeroWon) && !isCoinAnimated) {
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
            mPaint.setStrokeWidth(STROKE_WIDTH);
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
            for (int i = 0; i < 4; i++) {
                if (mReceiveAnimationBottom[i] != null) {
                    mReceiveAnimationBottom[i].draw(canvas);
                    break;
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

        if (mDealAnimationCount > 0 && !isHeroAnimated /*&& mCardAnimationCount == 0*/ && mReceiveAnimationCount == 0 && mDropAnimationCount == 0) { //Deal animations
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
                if (mDragReturnTicks > 0 && mTouchedLocation == i) {
                    drawPosition(canvas, mRowTop[i], CardState.MOVED);
                } else {
                    if (mCardAnimationTop[i] != null && !(mDragReturnTicks > 0 && mTouchedLocation == i)) {
                        mCardAnimationTop[i].draw(canvas);
                    } else if (mDisableAnimationTop[i] != null && mDealAnimationCount == 0 && !(mDragReturnTicks > 0 && mTouchedLocation == i)) {
                        mDisableAnimationTop[i].draw(canvas);
                    }
                }
                if (mDragReturnTicks > 0 && mTouchedLocation == 10 + i) {
                    drawPosition(canvas, mRowBottom[i], CardState.MOVED);
                } else {
                    if (mCardAnimationBottom[i] != null && !(mDragReturnTicks > 0 && mTouchedLocation == 10 + i)) {
                        mCardAnimationBottom[i].draw(canvas);
                    } else if (mDisableAnimationBottom[i] != null && mDealAnimationCount == 0 && !(mDragReturnTicks > 0 && mTouchedLocation == 10 + i)) {
                        mDisableAnimationBottom[i].draw(canvas);
                    }
                }
            }
        }

        if (isHeroAnimated && mHeroAnimation != null && !isCoinAnimated) { //Hero animations
            mHeroAnimation.draw(canvas);
        }

        if (isDiscarding && mDiscardAnimation != null) {
            mDiscardAnimation.draw(canvas);
        }

        if (isCoinAnimated && mCoinAnimation != null) {
            mCoinAnimation.draw(canvas);
        }

        if (isHeroWon) {
            Position position = mRowBottom[1];
            RectF rect = position.getRect();
            float curRelX = (getMeasuredWidth() / 2 - rect.left - rect.width() / 2);
            float curRelY = (getMeasuredHeight() / 2 - rect.top - rect.height() / 2);
            canvas.save();
            canvas.translate(curRelX, curRelY);
            canvas.scale(2f, 2f, rect.left + rect.width() / 2, rect.top + rect.height() / 2);
            drawPosition(canvas, position, CardState.REGULAR);
            canvas.restore();
        }

        if (isGameOver) {
            canvas.drawColor(0xdd000000);
        }

        if (mRestartButton != null) { //Restart button
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(isRestartTouched ? COLOR_REGULAR : COLOR_BG_CARD);
            mPaint.setStrokeWidth(STROKE_WIDTH * 3 / 2);
            canvas.save();
            canvas.rotate(isRestartTouched ? 130f : 0f, mRestartButton.left + mRestartButton.width() / 2, mRestartButton.top + mRestartButton.height() / 2);
            canvas.drawArc(mRestartButton, -20f, 270f, false, mPaint);
            canvas.drawLine(mRestartButton.left + mRestartButton.width() / 2, mRestartButton.top, mRestartButton.left + mRestartButton.width() / 8, mRestartButton.top - mRestartButton.height() / 8, mPaint);
            canvas.drawLine(mRestartButton.left + mRestartButton.width() / 2, mRestartButton.top, mRestartButton.left + mRestartButton.width() * 3 / 8, mRestartButton.top + mRestartButton.height() * 3 / 8, mPaint);
            canvas.restore();
            mPaint.setStrokeWidth(STROKE_WIDTH);
        }

        if (mDragReturnTicks > 0 || mDealAnimationCount > 0 || mReceiveAnimationCount > 0 || mDropAnimationCount > 0 || isHeroAnimated || mCardAnimationCount > 0 || isDiscarding || isCoinAnimated) { //Need to animate
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
            if (isHeroAnimated && !isCoinAnimated) {
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
                    isDiscarding = false;
                    resetTouchFeedback();
                    processMove();
                }
            }
            if (mCardAnimationCount > 0) { //Card animations (includes disabled)
                for (int i = 0; i < 4; i++) {
                    Animation animation = mCardAnimationBottom[i];
                    if (animation != null) {
                        if (!(mDragReturnTicks > 0 && mTouchedLocation == 10 + i)) {
                            animation.tick();
                            if (animation.isFinished()) {
                                animation.finish();
                                mCardAnimationBottom[i] = null;
                                mCardAnimationCount--;
                            }
                        }
                    } else {
                        animation = mDisableAnimationBottom[i];
                        if (animation != null && mDealAnimationCount == 0 && !(mDragReturnTicks > 0 && mTouchedLocation == 10 + i)) {
                            animation.tick();
                            if (animation.isFinished()) {
                                animation.finish();
                                mDisableAnimationBottom[i] = null;
                                mCardAnimationCount--;
                            }
                        }
                    }
                    animation = mCardAnimationTop[i];
                    if (animation != null) {
                        if (!(mDragReturnTicks > 0 && mTouchedLocation == i)) {
                            animation.tick();
                            if (animation.isFinished()) {
                                animation.finish();
                                mCardAnimationTop[i] = null;
                                mCardAnimationCount--;
                            }
                        }
                    } else {
                        animation = mDisableAnimationTop[i];
                        if (animation != null && mDealAnimationCount == 0 && !(mDragReturnTicks > 0 && mTouchedLocation == i)) {
                            animation.tick();
                            if (animation.isFinished()) {
                                animation.finish();
                                mDisableAnimationTop[i] = null;
                                mCardAnimationCount--;
                            }
                        }
                    }
                }
                if (mCardAnimationCount == 0) {
                    processMove();
                }
            }
            if (isCoinAnimated) { //Coin animation
                if (mCoinAnimation != null) {
                    mCoinAnimation.tick();
                    if (mCoinAnimation.isFinished()) {
                        mCoinAnimation.finish();
                        mCoinAnimation = null;
                        isCoinAnimated = false;
                    }
                }
                if (!isCoinAnimated) {
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
                for (int i = 0; i < 4; i++) {
                    Animation animation = mReceiveAnimationBottom[i];
                    if (animation != null) {
                        animation.tick();
                        if (animation.isFinished()) {
                            animation.finish();
                            mReceiveAnimationBottom[i] = null;
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
            if (mDealAnimationCount > 0 && !isHeroAnimated /*&& mCardAnimationCount == 0*/ && mReceiveAnimationCount == 0 && mDropAnimationCount == 0) { //Deal animations
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
    private void animateReceiveCard(Card card, int source, boolean returnToDeck) {
        Position position = (source < 10 ? mRowTop[source] : mRowBottom[source - 10]);
        if (card != null) { //Has card to deal
            mReceiveAnimationCount++;
            if (source < 10) {
                mReceiveAnimationTop[source] = new DeckReceiveAnimation(card, position, returnToDeck);
            } else {
                mReceiveAnimationBottom[source - 10] = new DeckReceiveAnimation(card, position, returnToDeck);
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
     * Start card disappearing animation
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
     * Start card suffer animation
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
     * Start hero quaff animation
     * @param target  Target location
     * @param newCard Card to which to transform
     */
    private void animateCardTransform(int target, Card newCard) {
        Position position = (target < 10 ? mRowTop[target] : mRowBottom[target - 10]);
        mCardAnimationCount++;
        if (target < 10) {
            mCardAnimationTop[target] = new CardTransformAnimation(target, position, newCard);
        } else {
            mCardAnimationBottom[target - 10] = new CardTransformAnimation(target, position, newCard);
        }
        invalidate();
    }

    /**
     * Start card disable animation
     * @param target Target location
     */
    private void animateCardDisable(int target) {
        Position position = (target < 10 ? mRowTop[target] : mRowBottom[target - 10]);
        mCardAnimationCount++;
        if (target < 10) {
            mDisableAnimationTop[target] = new CardDisableAnimation(position);
        } else {
            mDisableAnimationBottom[target - 10] = new CardDisableAnimation(position);
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
        private boolean returnToDeck;

        private float curRelX;
        private float curRelY;
        private float curScale;

        private float dx;
        private float dy;
        private float ds;

        private int ticksLeft;

        public DeckReceiveAnimation(Card card, Position position, boolean returnToDeck) {
            this.card = card;
            this.position = position;
            this.returnToDeck = returnToDeck;
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
            if (returnToDeck) {
                mDeck.receive(this.card);
            }
        }

    }

    //----------------------------------------------------------------------------------------------

    /**
     * Animation class for card dropping
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
            destroyCard(mRowBottom[1]);
            isGameOver = true;
            if (isRestarting) {
                begin();
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Animation class for adding coins
     */
    private class CoinAddAnimation implements Animation {

        private final int PERIOD = 3;

        private int amount;
        private int ticksLeft;

        public CoinAddAnimation(int amount) {
            this.amount = amount;
            this.ticksLeft = PERIOD;
        }

        public void addAmount(int amount) {
            this.amount += amount;
        }

        @Override
        public boolean isFinished() {
            return amount == 0;
        }

        @Override
        public void tick() {
            ticksLeft--;
            if (ticksLeft <= 0) {
                if (amount > 0) { //Add coins
                    amount--;
                    mCoins++;
                } else { //Remove coins
                    amount++;
                    mCoins--;
                }
                ticksLeft = PERIOD;
            }
        }

        @Override
        public void draw(Canvas canvas) {
            RectF rect = mRowBottom[1].getRect();
            mTextPaint.setTextSize(mFontSize * 1.2f);
            mTextPaint.setColor(COLOR_EMPH);
            String text = String.format("+%d", amount);
            canvas.drawText(text, rect.left + mFontPadding * 3 / 2, rect.bottom - mTextPaint.ascent() - (mFontSize + mFontPadding) * 2, mTextPaint);
        }

        @Override
        public void finish() {
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
            destroyCard(position);
        }
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Animation class for card disabling
     */
    private class CardDisableAnimation implements Animation {
        private Position position;
        private float curRadius;
        private float dr;

        private Path path;
        private int ticksLeft;

        public CardDisableAnimation(Position position) {
            this.position = position;
            RectF rect = position.getRect();
            float radius = rect.width() / 7;
            path = new Path();
            path.addRoundRect(rect, radius, radius, Path.Direction.CW);
            path.close();
            curRadius = rect.width() / 7;
            ticksLeft = 8;
            dr = (float) Math.sqrt(rect.width() * rect.width() + rect.height() * rect.height()) / 2 / ticksLeft;
        }

        @Override
        public boolean isFinished() {
            return ticksLeft <= 0;
        }

        @Override
        public void tick() {
            curRadius += dr;
            ticksLeft--;
        }

        @Override
        public void draw(Canvas canvas) {
            RectF rect = position.getRect();
            float cx = (rect.right + rect.left) / 2;
            float cy = (rect.top + rect.bottom) / 2;

            mPaint.setColor(COLOR_BG_TINT);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.save();
            canvas.clipPath(path, Region.Op.REPLACE);
            canvas.drawCircle(cx, cy, curRadius, mPaint);
            canvas.restore();
            mPaint.setAlpha(255);
        }

        @Override
        public void finish() {
            this.position.getCard().setActive(false);
        }
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Animation class for card transforming
     */
    private class CardTransformAnimation implements Animation {

        private int target;
        private Position position;
        private RectF oldRect;
        private RectF newRect;
        private Card newCard;
        private float curX;
        private float dx;

        private Path path;
        private int ticksLeft;

        public CardTransformAnimation(int target, Position position, Card newCard) {
            this.target = target;
            this.position = position;
            this.newCard = newCard;
            oldRect = position.getRect();
            newRect = new RectF(oldRect);
            newRect.offset(oldRect.width(), 0f);
            float radius = oldRect.width() / 7;
            path = new Path();
            path.addRoundRect(oldRect, radius, radius, Path.Direction.CW);
            path.close();
            ticksLeft = 9;
            curX = 0f;
            dx = -oldRect.width() / ticksLeft;
        }

        @Override
        public boolean isFinished() {
            return ticksLeft <= 0;
        }

        @Override
        public void tick() {
            curX += dx;
            ticksLeft--;
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.save();
            canvas.clipPath(path, Region.Op.REPLACE);
            canvas.translate(curX, 0f);
            drawCard(canvas, position.getCard(), oldRect, CardState.SHARP);
            drawCard(canvas, newCard, newRect, CardState.SHARP);
            mPaint.setStrokeWidth(STROKE_WIDTH);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(COLOR_REGULAR);
            canvas.drawLine(newRect.left, newRect.top, newRect.left, newRect.bottom, mPaint);
            canvas.restore();
            drawCard(canvas, null, oldRect, CardState.MOVED);
        }

        @Override
        public void finish() {
            position.setCard(newCard);
            if (newCard.getType() == Card.Type.POTION && newCard.getAbility() != Card.Ability.POISON && (target == LOC_LEFT_HAND || target == LOC_RIGHT_HAND)) {
                animateCardDisable(target);
                mRowBottom[1].getCard().setValue(Math.min(Card.HERO_MAX + mHealthAddition, mRowBottom[1].getCard().getValue() + newCard.getValue()));
                animateCardImprove(LOC_HERO);
                newCard.setValue(0);
            }
            if (newCard.getType() == Card.Type.COIN && (target == LOC_LEFT_HAND || target == LOC_RIGHT_HAND || target == LOC_BACKPACK)) {
                animateCardDisable(target);
                addCoins(newCard.getValue());
                newCard.setValue(0);
            }
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
            destroyCard(position);
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
            RectF rect = position.getRect();
            canvas.save();
            canvas.translate(curRelX, curRelY);
            canvas.scale(curScale, curScale, rect.left + rect.width() / 2, rect.top + rect.height() / 2);
            drawPosition(canvas, position, CardState.REGULAR);
            canvas.restore();
        }

        @Override
        public void finish() {
            isHeroWon = true;
        }

    }

}