package com.lovejjfg.blogdemo.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.lovejjfg.blogdemo.R;

import java.util.ArrayList;

import butterknife.BindDimen;
import butterknife.BindInt;


/**
 * Created by Joe on 2016/4/3.
 * Email lovejjfg@gmail.com
 */
public class TouchCircleView extends View {
    @BindDimen(R.dimen.reduce_height)
    int reduceHeight;
    @BindDimen(R.dimen.reduce_width)
    int reducewidth;
    @BindDimen(R.dimen.first_range)
    float firstRange;
    @BindDimen(R.dimen.sec_range)
    float secRange;
    @BindDimen(R.dimen.third_range)
    float thirdRange;

    private RectF outRectF;
    private RectF innerRectf;
    private RectF secondRectf;
    private Paint innerPaint;
    private Paint paint;
    private Paint circlePaint;

    private Path path;
    private Path mArrow;
    private Path mHook;
    private Path mError;

    private ObjectAnimator mObjectAnimatorSweep;
    private ObjectAnimator mObjectAnimatorAngle;
    private ValueAnimator fractionAnimator;

    private static final Interpolator ANGLE_INTERPOLATOR = new LinearInterpolator();
    private static final Interpolator SWEEP_INTERPOLATOR = new AccelerateDecelerateInterpolator();
    private static final int ANGLE_ANIMATOR_DURATION = 1000;//转速
    private static final int SWEEP_ANIMATOR_DURATION = 800;
    private static final int DELAY_TIME = 2000;
    private static final int START_ANGLE = 270;
    private static final int RESULT_TIME = 300;
    private static final int ALPHA_FULL = 255;
    private static final int MIN_SWEEP_ANGLE = 30;

    private float mCurrentGlobalAngleOffset;
    private float mCurrentGlobalAngle;
    private float mCurrentSweepAngle;

    public static final int STATE_DRAW_IDLE = 0;
    public static final int STATE_DRAW_ARC = 1;
    public static final int STATE_DRAW_PATH = 2;//大圆到小圆的渐变
    public static final int STATE_DRAW_OUT_PATH = 3;//小圆到大圆的渐变
    public static final int STATE_DRAW_CIRCLE = 4;
    public static final int STATE_DRAW_ARROW = 5;
    public static final int STATE_DRAW_PROGRESS = 6;
    public static final int STATE_DRAW_ERROR = 7;
    public static final int STATE_DRAW_SUCCESS = 8;
    private float currentOffset;
    private float density;
    private int defaultOffset;
    private final Runnable finishAction = new Runnable() {
        @Override
        public void run() {
            finish();
        }
    };
    private Runnable startLoadingAction = new Runnable() {
        @Override
        public void run() {
            updateRectF();
            startLoading();
        }
    };
    private float outPahtMax;
    private float pahtMax;
    //    private boolean isBack;

    public int getCurrentState() {
        return currentState;
    }

    public void setCurrentState(int currentState) {
        this.currentState = currentState;
        invalidate();
    }

    private int currentState;
    private int centerY;
    private int centerX;
    private float mBorderWidth = 4;
    private float mRingCenterRadius;
    private boolean mModeAppearing;
    private float mArrowScale = 1.0f;
    private float fraction;
    private Paint mHookPaint;

    public boolean ismRunning() {
        return mRunning;
    }

    private boolean mRunning;
    private boolean isDrawTriangle;

    //设置默认半径
    @SuppressWarnings("unused")
    public void setOutCirRadius(int outCirRadius) {
        this.outCirRadius = outCirRadius;
        Log.i("默认半径：", outCirRadius + "");

    }

    @SuppressWarnings("unused")
    public int getOutCirRadius() {
        return outCirRadius;
    }

    private int outCirRadius = 100;
    private int secondRadius = (int) (outCirRadius * 1f);
    private int innerCirRadius = outCirRadius - 30;
    private static int ARROW_WIDTH = 20 * 2;
    private static int ARROW_HEIGHT = 10 * 2;


    private long angle;
    private long paths;
    private long backpaths;

    public TouchCircleView(Context context) {
        this(context, null);
    }

    public TouchCircleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TouchCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = context.getResources().getDisplayMetrics().density;
        defaultOffset = (int) (16 * density);
        firstRange = (int) (80f * density);
        secRange = (int) (114f * density);
        thirdRange = (int) (158f * density);

        outPahtMax = 80 * density;
        pahtMax = 60 * density;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HeaderProgress, defStyleAttr, 0);
        mBorderWidth = a.getDimension(R.styleable.HeaderProgress_circleBorderWidth,
                2 * density);
        outCirRadius = (int) a.getDimension(R.styleable.HeaderProgress_outRadius, 20 * density);
        innerCirRadius = (int) a.getDimension(R.styleable.HeaderProgress_innerRadius, 12 * density);
        secondRadius = (int) (outCirRadius * 1.2f);
        a.recycle();
        initView();
    }


    private void initView() {
        setupAnimations();
        innerRectf = new RectF();
        secondRectf = new RectF();
        outRectF = new RectF();
        ARROW_WIDTH = (int) (mBorderWidth * 1.5f);
        ARROW_HEIGHT = (int) (mBorderWidth * 0.75f);
        initPaintPath();
    }

    public void handleOffset(int dy) {
        float percent;
        if (dy < 0) {
            dy = 0;
        }
        mModeAppearing = true;
        percent = dy / firstRange;
        if (dy != angle && dy >= 0 && dy <= firstRange) {
            paint.setAlpha((int) (percent * ALPHA_FULL));
            updateState(STATE_DRAW_ARC, false);
            resetAngle();
            currentOffset = percent * defaultOffset;
            updateRectF();
            angle = (long) (percent * 360);
            Log.i("TAG", "onTouchEvent: " + angle);
            invalidate();
            return;
        }
        if (dy > firstRange && dy <= secRange) {
            innerPaint.setAlpha(ALPHA_FULL);
            outRectF.set(centerX - outCirRadius, currentOffset, centerX + outCirRadius
                    , centerY + outCirRadius + currentOffset);
            updateState(STATE_DRAW_ARROW, false);
            percent = (dy - firstRange) / (secRange - firstRange);
            mCurrentSweepAngle = mCurrentGlobalAngle = percent * 100;
            invalidate();
            return;
        }
        //正常顺序到达这里
        if ((currentState == STATE_DRAW_ARROW || currentState == STATE_DRAW_PATH) && dy > secRange && dy <= thirdRange) {
            Log.e("TAG", "handleOffset: 画正常的PATH了" + dy);
            updateState(STATE_DRAW_PATH, false);
//            paths = dy - 380;
            percent = (dy - secRange)  / (thirdRange - secRange);
            paths = (long) (percent * pahtMax>20*density?percent * pahtMax:20*density);
            innerPaint.setAlpha((int) ((1 - percent) * ALPHA_FULL));
//                mArrowScale = precent;
            outRectF.set(centerX - outCirRadius + percent * 5 * density, 3 * density * percent + currentOffset, centerX + outCirRadius - percent * 5 * density
                    , centerY + outCirRadius + 4 * density * percent + currentOffset);
            invalidate();
            return;
        }

        if ((currentState == STATE_DRAW_CIRCLE || currentState == STATE_DRAW_OUT_PATH) && dy > secRange && dy <= thirdRange) {
            Log.e("TAG", "handleOffset: 画PATH了" + dy);
            updateState(STATE_DRAW_OUT_PATH, false);
            percent = (thirdRange - dy) / (thirdRange-secRange);
            Log.e("percent", "handleOffset: " + percent);
//            secondRectf.set(centerX - outCirRadius, currentOffset + outCirRadius * 2, centerX + outCirRadius
//                    , centerY + outCirRadius + currentOffset + outCirRadius * 2);
            secondRectf.set(centerX - secondRadius + percent * 5 * density, currentOffset + secondRadius * 2 + percent * 10 * density, centerX + secondRadius - percent * 5 * density
                    , centerY + secondRadius + currentOffset + secondRadius * 2 + percent * 2 * density);
            backpaths = (long) (outPahtMax * percent>30f?outPahtMax * percent:30*density);
            invalidate();
            return;
        }


        if (dy > thirdRange) {
            Log.e("TAG", "handleOffset: 画第二个圆形了" + dy);
            updateState(STATE_DRAW_CIRCLE, false);
            secondRectf.set(centerX - secondRadius, centerY - secondRadius + currentOffset + secondRadius * 2, centerX + secondRadius
                    , centerY + secondRadius + currentOffset + secondRadius * 2);
            invalidate();
        }
    }

    private void updateState(int state, boolean hide) {
        updateState(state, hide, false);

    }

    private void updateState(final int state, final boolean hide, boolean delay) {
        currentState = state;
        if (listeners != null) {
            for (final OnLoadingListener listener : listeners) {
                if (delay) {
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            listener.onProgressStateChange(state, hide);
                        }
                    }, RESULT_TIME);
                } else {
                    listener.onProgressStateChange(state, hide);
                }
            }
        }
    }

    public void resetTouch() {
        if (mRunning) {
            return;
        }
        if (STATE_DRAW_ARROW == currentState || STATE_DRAW_PATH == currentState) {
            outRectF.set(centerX - outCirRadius, currentOffset, centerX + outCirRadius
                    , centerY + outCirRadius + currentOffset);
            innerPaint.setAlpha(ALPHA_FULL);
            updateState(STATE_DRAW_PROGRESS, false);
            start();
            return;
        }
        stop();
        updateState(STATE_DRAW_IDLE, true);
        invalidate();

    }

    private void startLoading() {
        innerPaint.setAlpha(ALPHA_FULL);
        start();
    }

    private void initPaintPath() {
        circlePaint = new Paint();
        circlePaint.setColor(Color.BLUE);
        circlePaint.setStrokeWidth(10);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setAntiAlias(true);
        circlePaint.setAlpha(50);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(20);
        paint.setColor(Color.RED);
        paint.setStrokeCap(Paint.Cap.SQUARE);

        innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerPaint.setStyle(Paint.Style.STROKE);
        innerPaint.setAntiAlias(true);
        innerPaint.setColor(Color.WHITE);
        innerPaint.setStrokeWidth(mBorderWidth);


        mHookPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHookPaint.setStyle(Paint.Style.STROKE);
        mHookPaint.setStrokeCap(Paint.Cap.ROUND);
        mHookPaint.setStrokeWidth(mBorderWidth);
        mHookPaint.setColor(Color.WHITE);

        path = new Path();
        mArrow = new Path();
        mHook = new Path();
        mError = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2;
        centerY = outCirRadius;
        updateRectF();
        mRingCenterRadius = Math.min(innerRectf.centerX() - innerRectf.left, innerRectf.centerY() - innerRectf.top) - mBorderWidth;

    }

    private void updateRectF() {
        outRectF.set(centerX - outCirRadius, currentOffset, centerX + outCirRadius
                , centerY + outCirRadius + currentOffset);
        innerRectf.set(centerX - innerCirRadius, centerY - innerCirRadius + currentOffset, centerX + innerCirRadius, centerY + innerCirRadius + currentOffset);
        secondRectf.set(centerX - secondRadius, currentOffset + secondRadius * 2, centerX + secondRadius
                , centerY + secondRadius + currentOffset + secondRadius * 2);
    }

    //    注意：onDraw每次被调用时canvas画布都是一个干净的、空白的、透明的，他不会记录以前画上去的
    @Override
    protected void onDraw(Canvas canvas) {
        //动画的
        switch (currentState) {
            case STATE_DRAW_IDLE:
                break;
            case STATE_DRAW_ARC:
                canvas.drawArc(outRectF, START_ANGLE, angle, true, paint);
                canvas.drawArc(outRectF, START_ANGLE, angle, false, circlePaint);
                break;
            case STATE_DRAW_ARROW:
                isDrawTriangle = true;
                canvas.drawArc(outRectF, 0, 360, true, paint);
                drawArc(canvas);
                break;
            case STATE_DRAW_PROGRESS:
                isDrawTriangle = false;
                canvas.drawArc(outRectF, 0, 360, true, paint);
                drawArc(canvas);
                break;
            case STATE_DRAW_SUCCESS:
                drawHook(canvas);
                break;
            case STATE_DRAW_ERROR:
                drawError(canvas);
                break;
            case STATE_DRAW_PATH:
                path.reset();
                path.moveTo((float) (outRectF.centerX() - Math.cos(180 / Math.PI * 30) * (outRectF.centerX() - outRectF.left)), (float) (outRectF.centerY() - Math.sin(180 / Math.PI * 30) * (outRectF.centerY() - outRectF.top)));
//                path.quadTo(outRectF.centerX(), outRectF.centerY() + paths, (float) (outRectF.centerX() + Math.cos(180 / Math.PI * 30) * (outRectF.centerX() - outRectF.left)), (float) (outRectF.centerY() - Math.sin(180 / Math.PI * 30) * (outRectF.centerY() - outRectF.top)));
                path.cubicTo(outRectF.centerX() + 10 * density, outRectF.centerY() + paths, outRectF.centerX() - 10 * density, outRectF.centerY() + paths, (float) (outRectF.centerX() + Math.cos(180 / Math.PI * 30) * (outRectF.centerX() - outRectF.left)), (float) (outRectF.centerY() - Math.sin(180 / Math.PI * 30) * (outRectF.centerY() - outRectF.top)));
                canvas.drawArc(outRectF, 0, 360, true, paint);
                canvas.drawPath(path, paint);
                drawArc(canvas);
                break;
            case STATE_DRAW_OUT_PATH:
                path.reset();
                path.moveTo((float) (secondRectf.centerX() + Math.cos(180 / Math.PI * 30) * (secondRectf.centerX() - secondRectf.left)), (float) (secondRectf.centerY() + Math.sin(180 / Math.PI * 30) * (secondRectf.centerY() - secondRectf.top)));
                path.cubicTo(secondRectf.centerX() - 10*density, secondRectf.centerY() - backpaths, secondRectf.centerX() + 10*density, secondRectf.centerY() - backpaths, (float) (secondRectf.centerX() - Math.cos(180 / Math.PI * 30) * (secondRectf.centerX() - secondRectf.left)), (float) (secondRectf.centerY() + Math.sin(180 / Math.PI * 30) * (secondRectf.centerY() - secondRectf.top)));
//                path.quadTo(secondRectf.centerX(), secondRectf.centerY() - backpaths, (float) (secondRectf.centerX() - Math.cos(180 / Math.PI * 30) * (secondRectf.centerX() - secondRectf.left)), (float) (secondRectf.centerY() + Math.sin(180 / Math.PI * 30) * (secondRectf.centerY() - secondRectf.top)));
                canvas.drawArc(secondRectf, 0, 360, true, paint);
                canvas.drawPath(path, paint);
//                drawArc(canvas);
                break;
            case STATE_DRAW_CIRCLE:
                canvas.drawArc(secondRectf, 0, 360, true, paint);
                break;
        }


//        // 最小圆形
//        canvas.drawCircle(width / 2, height / 2, minRadius, circlePaint);
//        circlePaint.setAlpha(120);
    }

    private void drawArc(Canvas canvas) {
        float startAngle = mCurrentGlobalAngle - mCurrentGlobalAngleOffset + START_ANGLE;
        float sweepAngle = mCurrentSweepAngle;
        if (mModeAppearing) {
//            paint.setColor(gradient(mColors[mCurrentColorIndex], mColors[mNextColorIndex],
//                    mCurrentSweepAngle / (360 - MIN_SWEEP_ANGLE * 2)));
            sweepAngle += MIN_SWEEP_ANGLE;
        } else {
            startAngle = startAngle + sweepAngle;
            sweepAngle = 360 - sweepAngle - MIN_SWEEP_ANGLE;
        }
        canvas.drawArc(innerRectf, startAngle, sweepAngle, false, innerPaint);
        if (isDrawTriangle) {
            drawTriangle(canvas, startAngle, sweepAngle);
        }
    }

    public void drawTriangle(Canvas c, float startAngle, float sweepAngle) {
        if (mArrow == null) {
            mArrow = new Path();
            mArrow.setFillType(Path.FillType.EVEN_ODD);
        } else {
            mArrow.reset();
        }

        // Adjust the position of the triangle so that it is inset as
        // much as the arc, but also centered on the arc.
//        float inset = (int) mStrokeInset / 2 * mArrowScale;
        float x = (float) (mRingCenterRadius * Math.cos(0) + innerRectf.centerX());
        float y = (float) (mRingCenterRadius * Math.sin(0) + innerRectf.centerY());

        // Update the path each time. This works around an issue in SKIA
        // where concatenating a rotation matrix to a scale matrix
        // ignored a starting negative rotation. This appears to have
        // been fixed as of API 21.
        mArrow.moveTo(0, 0);
        mArrow.lineTo(ARROW_WIDTH * mArrowScale, 0);
        mArrow.lineTo((ARROW_WIDTH * mArrowScale / 2), (ARROW_HEIGHT
                * mArrowScale));
        mArrow.offset(x, y);
        mArrow.close();
        // draw a triangle
        c.rotate(startAngle + sweepAngle, innerRectf.centerX(),
                innerRectf.centerY());
        c.drawPath(mArrow, innerPaint);

    }

    /**
     * 画勾
     */
    private void drawHook(Canvas canvas) {
        mHook.reset();
        mHook.moveTo(innerRectf.centerX() - innerRectf.width() * 0.25f * fraction, innerRectf.centerY());
        mHook.lineTo(innerRectf.centerX() - innerRectf.width() * 0.1f * fraction, innerRectf.centerY() + innerRectf.height() * 0.18f * fraction);
        mHook.lineTo(innerRectf.centerX() + innerRectf.width() * 0.25f * fraction, innerRectf.centerY() - innerRectf.height() * 0.20f * fraction);
        canvas.drawArc(outRectF, 0, 360, false, paint);
        canvas.drawPath(mHook, mHookPaint);

    }

    /**
     * 画×
     */
    private void drawError(Canvas canvas) {
        mError.reset();
        mError.moveTo(innerRectf.centerX() + innerRectf.width() * 0.2f * fraction, innerRectf.centerY() - innerRectf.height() * 0.2f * fraction);
        mError.lineTo(innerRectf.centerX() - innerRectf.width() * 0.2f * fraction, innerRectf.centerY() + innerRectf.height() * 0.2f * fraction);
        mError.moveTo(innerRectf.centerX() - innerRectf.width() * 0.2f * fraction, innerRectf.centerY() - innerRectf.height() * 0.2f * fraction);
        mError.lineTo(innerRectf.centerX() + innerRectf.width() * 0.2f * fraction, innerRectf.centerY() + innerRectf.height() * 0.2f * fraction);
        canvas.drawArc(outRectF, 0, 360, false, paint);
        canvas.drawPath(mError, mHookPaint);
    }


    @SuppressWarnings("unused")
    private static int gradient(int color1, int color2, float p) {
        int r1 = (color1 & 0xff0000) >> 16;
        int g1 = (color1 & 0xff00) >> 8;
        int b1 = color1 & 0xff;
        int r2 = (color2 & 0xff0000) >> 16;
        int g2 = (color2 & 0xff00) >> 8;
        int b2 = color2 & 0xff;
        int newr = (int) (r2 * p + r1 * (1 - p));
        int newg = (int) (g2 * p + g1 * (1 - p));
        int newb = (int) (b2 * p + b1 * (1 - p));
        return Color.argb(ALPHA_FULL, newr, newg, newb);
    }

    private void start() {
        if (mRunning) {
            return;
        }
        mRunning = true;
        dispatchListener();
        mObjectAnimatorAngle.setFloatValues(mCurrentGlobalAngle, 360f);
        mObjectAnimatorSweep.setFloatValues(mCurrentSweepAngle, 360f - MIN_SWEEP_ANGLE * 2);
//        mCurrentState = STATE_LOADING;
        mObjectAnimatorAngle.start();
        mObjectAnimatorSweep.start();
//        postDelayed(finishAction, DELAY_TIME);
        invalidate();
    }

    private void dispatchListener() {
        if (listeners != null) {
            for (OnLoadingListener listener : listeners) {
                listener.onProgressStateChange(STATE_DRAW_PROGRESS, false);
                listener.onProgressLoading();
            }

        }
    }

    public void finish() {
        stop();
        resetAngle();
        if (!fractionAnimator.isRunning()) {
            fractionAnimator.start();
        }
        updateState(currentState, true, true);
        invalidate();
    }

    private void resetAngle() {
        if (mCurrentSweepAngle != 0 || mCurrentGlobalAngle != 0 || mCurrentGlobalAngleOffset != 0) {
            mCurrentSweepAngle = 0;
            mCurrentGlobalAngle = 0;
            mCurrentGlobalAngleOffset = 0;
        }
    }

    private void stop() {
        if (!mRunning) {
            return;
        }
        mRunning = false;
        mObjectAnimatorAngle.cancel();
        mObjectAnimatorSweep.cancel();
    }

    private void toggleAppearingMode() {
        mModeAppearing = !mModeAppearing;
        if (mModeAppearing) {
//            mCurrentColorIndex = ++mCurrentColorIndex % 4;
//            mNextColorIndex = ++mNextColorIndex % 4;
            mCurrentGlobalAngleOffset = (mCurrentGlobalAngleOffset + MIN_SWEEP_ANGLE * 2) % 360;
        }
    }

    private Property<TouchCircleView, Float> mAngleProperty = new Property<TouchCircleView, Float>(Float.class, "angle") {
        @Override
        public Float get(TouchCircleView object) {
            return object.getCurrentGlobalAngle();
        }

        @Override
        public void set(TouchCircleView object, Float value) {
            object.setCurrentGlobalAngle(value);
        }
    };

    private Property<TouchCircleView, Float> mSweepProperty = new Property<TouchCircleView, Float>(Float.class, "arc") {
        @Override
        public Float get(TouchCircleView object) {
            return object.getCurrentSweepAngle();
        }

        @Override
        public void set(TouchCircleView object, Float value) {
            object.setCurrentSweepAngle(value);
        }
    };

    private void setupAnimations() {
        mObjectAnimatorAngle = ObjectAnimator.ofFloat(this, mAngleProperty, mCurrentGlobalAngle, 360f);
        mObjectAnimatorAngle.setInterpolator(ANGLE_INTERPOLATOR);
        mObjectAnimatorAngle.setDuration(ANGLE_ANIMATOR_DURATION);
        mObjectAnimatorAngle.setRepeatMode(ValueAnimator.RESTART);
        mObjectAnimatorAngle.setRepeatCount(ValueAnimator.INFINITE);
        mObjectAnimatorAngle.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationRepeat(Animator animation) {
                mObjectAnimatorAngle.setFloatValues(360f);
            }
        });

        mObjectAnimatorSweep = ObjectAnimator.ofFloat(this, mSweepProperty, mCurrentSweepAngle, 360f - MIN_SWEEP_ANGLE * 2);
        mObjectAnimatorSweep.setInterpolator(SWEEP_INTERPOLATOR);
        mObjectAnimatorSweep.setDuration(SWEEP_ANIMATOR_DURATION);
        mObjectAnimatorSweep.setRepeatMode(ValueAnimator.RESTART);
        mObjectAnimatorSweep.setRepeatCount(ValueAnimator.INFINITE);
        mObjectAnimatorSweep.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationRepeat(Animator animation) {
                mObjectAnimatorSweep.setFloatValues(360f - MIN_SWEEP_ANGLE * 2);
                toggleAppearingMode();
            }
        });

        fractionAnimator = ValueAnimator.ofInt(0, ALPHA_FULL);
        fractionAnimator.setInterpolator(ANGLE_INTERPOLATOR);
        fractionAnimator.setDuration(100);
        fractionAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                fraction = animation.getAnimatedFraction();
                mHookPaint.setAlpha((Integer) animation.getAnimatedValue());

                invalidate();
            }
        });
    }

    public void setCurrentGlobalAngle(float currentGlobalAngle) {
        mCurrentGlobalAngle = currentGlobalAngle;
        invalidate();
    }

    public float getCurrentGlobalAngle() {
        return mCurrentGlobalAngle;
    }

    public void setCurrentSweepAngle(float currentSweepAngle) {
        mCurrentSweepAngle = currentSweepAngle;
        invalidate();
    }

    public float getCurrentSweepAngle() {
        return mCurrentSweepAngle;
    }

    ArrayList<OnLoadingListener> listeners = new ArrayList<>();

    public void addLoadingListener(@Nullable OnLoadingListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public boolean removeLoadingListener(@NonNull OnLoadingListener listener) {
        return listeners != null && listeners.remove(listener);
    }


//    @Nullable
//    OnLoadingListener listener;

    public void setRefresh(boolean mRefresh) {
        currentState = mRefresh ? STATE_DRAW_PROGRESS : STATE_DRAW_IDLE;
        updateState(currentState, !mRefresh);
        if (mRefresh) {
            currentOffset = defaultOffset;
            post(startLoadingAction);
        } else {
            currentOffset = 0;
            updateRectF();
            invalidate();
        }

    }

    public void setRefreshError() {
        currentState = STATE_DRAW_ERROR;
        finish();
    }

    public void setRefreshSuccess() {
        currentState = STATE_DRAW_SUCCESS;
        finish();
    }

    public interface OnLoadingListener {
        void onProgressStateChange(int state, boolean hide);

        void onProgressLoading();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }


}
