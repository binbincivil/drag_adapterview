package net.qingtian.drag_adapterview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.Toast;

/**
 * 可以拖动的gridview
 * 子类实现抽象方法，实现拖动的item不同外观
 *
 * Created by qingtian on 2015/12/22.
 *
 * @blog http://blog.csdn.net/bingospunky
 */
public abstract class BaseDragGrid extends GridView {

    private static final String TAG = "qingtian";

    /** 设置刚开始的某几个item不可以改变位置 */
    public int mFixedItemSize = 0;

    /** 点击时候的X位置 */
    public int mDownX;
    /** 点击时候的Y位置 */
    public int mDownY;
    /** 点击时候的相对于屏幕的X位置 */
    public int mDownRawX;
    /** 点击时候的相对于屏幕的Y位置 */
    public int mDownRawY;

    /** 长按时，触点在Item View里面的X方向上的偏移量，数值大小（正数） */
    private int mXOffsetBetweenDownPointAndTheItemLeftBoundary;

    /** 长按时，触点在Item View里面的Y方向上的偏移量，数值大小（正数） */
    private int mYOffsetBetweenDownPointAndTheItemTopBoundary;

    /** 开始拖动的ITEM的Position */
    private int mStartPosition;

    /**  Up后对应的ITEM的Position */
    private int mDropPosition;

    /** item height */
    private int mItemHeight;

    /** item width */
    private int mItemWidth;

    /** 拖动的时候对应ITEM的VIEW */
    private View mDragImageView = null;

    /** WindowManager */
    private WindowManager mWindowManager = null;

    /** */
    private WindowManager.LayoutParams mWindowParams = null;

    /** 是否在移动 */
    private boolean isMoving = false;

    /** 震动器 */
    private Vibrator mVibrator;

    /** 每个ITEM之间的水平间距 */
    private int mHorizontalSpacing = 2;

    /** 每个ITEM之间的竖直间距 */
    private int mVerticalSpacing = 2;

    /** 一行的ITEM数量 */
    private int mNumColumns = 4;

    /** 移动时候最后个动画的ID */
    private String mLastAnimationID;

    private IDragGridAdapter mDragGridAdapter;

    public BaseDragGrid(Context context) {
        super(context);
        init(context);
    }

    public BaseDragGrid(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public BaseDragGrid(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void init(Context context) {
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        setOnItemClickListener();
    }

    @Override
    public void setHorizontalSpacing(int horizontalSpacing) {
        super.setHorizontalSpacing(horizontalSpacing);
        this.mHorizontalSpacing = horizontalSpacing;
        Log.e(TAG, "setHorizontalSpacing:" + horizontalSpacing);
    }

    @Override
    public void setVerticalSpacing(int verticalSpacing) {
        super.setVerticalSpacing(verticalSpacing);
        this.mVerticalSpacing = verticalSpacing;
        Log.e(TAG, "verticalSpacing:" + verticalSpacing);
    }

    @Override
    public void setNumColumns(int numColumns) {
        super.setNumColumns(numColumns);
        this.mNumColumns = numColumns;
        Log.e(TAG, "setNumColumns:" + mNumColumns);
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
        if (adapter instanceof IDragGridAdapter) {
            mDragGridAdapter = (IDragGridAdapter) adapter;
            mNumColumns = mDragGridAdapter.getColumnCount();
        } else {
            Toast.makeText(getContext(), "适配器异常IDragGridAdapter", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 长按点击监听
     */
    public void setOnItemClickListener() {
        setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                mStartPosition = position; // 第一次点击的postion
                if (mStartPosition >= mFixedItemSize) {
                    ViewGroup dragViewGroup = (ViewGroup) getChildAt(mStartPosition - getFirstVisiblePosition());

                    mItemHeight = dragViewGroup.getHeight();
                    mItemWidth = dragViewGroup.getWidth();

                    // 如果特殊的这个不等于拖动的那个,并且不等于-1
                    if (mStartPosition != AdapterView.INVALID_POSITION) {
                        // 释放的资源使用的绘图缓存。如果你调用buildDrawingCache()手动没有调用setDrawingCacheEnabled(真正的),你应该清理缓存使用这种方法。
                        mXOffsetBetweenDownPointAndTheItemLeftBoundary = mDownX - dragViewGroup.getLeft();
                        mYOffsetBetweenDownPointAndTheItemTopBoundary = mDownY - dragViewGroup.getTop();

                        mVibrator.vibrate(50); // 设置震动时间

                        // 添加随手指移动的item图像的ImageView
                        Bitmap dragBitmap = generateDragBitmap(dragViewGroup);

                        startDrag(dragBitmap, mDownRawX, mDownRawY);

                        // 隐藏gridview里拖动的item
                        mDragGridAdapter.setDropItemPosition(mStartPosition);
                        mDragGridAdapter.setShowDropItem(false);

                        isMoving = false;
                        requestDisallowInterceptTouchEvent(true);
                        return true;
                    }
                }
                return false;

            }
        });
    }

    /**
     * 抽象方法，子类实现，通过当前的view得到拖拽的view的图像
     *
     * @param dragViewGroup
     * @return
     */
    public abstract Bitmap generateDragBitmap(ViewGroup dragViewGroup);

    /**
     * 在屏幕里添加一个ImageView，该ImageView的内容就是被长按的那个View
     *
     * @param dragBitmap
     * @param x
     * @param y
     */
    public void startDrag(Bitmap dragBitmap, int x, int y) {
        stopDrag();
        mWindowParams = new WindowManager.LayoutParams();// 获取WINDOW界面的
        // 这个必须加
        mWindowParams.gravity = Gravity.TOP | Gravity.LEFT;
        //得到preview左上角相对于屏幕的坐标
        mWindowParams.x = x - mXOffsetBetweenDownPointAndTheItemLeftBoundary;
        mWindowParams.y = y - mYOffsetBetweenDownPointAndTheItemTopBoundary;
        //设置拖拽item的宽和高
        mWindowParams.width = dragBitmap.getWidth(); // 放大dragScale倍，可以设置拖动后的倍数
        mWindowParams.height = dragBitmap.getHeight(); // 放大dragScale倍，可以设置拖动后的倍数
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        mWindowParams.format = PixelFormat.TRANSLUCENT;
        mWindowParams.windowAnimations = 0;
        ImageView iv = new ImageView(getContext());
        iv.setImageBitmap(dragBitmap);
        mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.addView(iv, mWindowParams);
        mDragImageView = iv;
    }

    /**
     * 停止拖动 ，释放并初始化
     */
    private void stopDrag() {
        if (mDragImageView != null) {
            mWindowManager.removeView(mDragImageView);
            mDragImageView = null;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // TODO Auto-generated method stub
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mDownX = (int) ev.getX();
            mDownY = (int) ev.getY();
            mDownRawX = (int) ev.getRawX();
            mDownRawY = (int) ev.getRawY();
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mDragImageView != null && mStartPosition != AdapterView.INVALID_POSITION) {

            // 移动时候的对应x,y位置
            int x = (int) ev.getX();
            int y = (int) ev.getY();

            switch (ev.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    onDrag((int) ev.getRawX(), (int) ev.getRawY());
                    if (!isMoving) {
                        onMove(x, y);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    stopDrag();
                    mDragGridAdapter.setShowDropItem(true);
                    requestDisallowInterceptTouchEvent(false);
                    break;
                default:
                    break;
            }
        }
        return super.onTouchEvent(ev);
    }

    /**
     * 在拖动的时候，被拖动的item随着手指移动
     */
    private void onDrag(int rawx, int rawy) {
        if (mDragImageView != null) {
            mWindowParams.alpha = 0.6f;
            mWindowParams.x = rawx - mXOffsetBetweenDownPointAndTheItemLeftBoundary;
            mWindowParams.y = rawy - mYOffsetBetweenDownPointAndTheItemTopBoundary;
            mWindowManager.updateViewLayout(mDragImageView, mWindowParams);
        }
    }

    /**
     * 移动的时候触发
     */
    public void onMove(int x, int y) {
        // 拖动的VIEW下方的POSTION
        int dPosition = pointToPosition(x, y);
        // 判断下方的POSTION是否是最开始2个不能拖动的
        if (dPosition >= mFixedItemSize) {

            mDropPosition = dPosition;

            int movecount = mDropPosition - mStartPosition;

            if (movecount != 0) {
                //dragGroup设置为不可见
                ViewGroup dragGroup = (ViewGroup) getChildAt(mStartPosition - getFirstVisiblePosition());
                dragGroup.setVisibility(View.INVISIBLE);

                float to_x;// 当前下方positon
                float to_y;// 当前下方右边positon
                //x_vlaue移动的距离百分比（相对于自己长度的百分比）
                float x_vlaue = ((float) mHorizontalSpacing / (float) mItemWidth) + 1.0f;
                //y_vlaue移动的距离百分比（相对于自己宽度的百分比）
                float y_vlaue = ((float) mVerticalSpacing / (float) mItemHeight) + 1.0f;

                for (int i = 0; i < Math.abs(movecount); i++) {
                    int holdPosition;
                    to_x = x_vlaue;
                    to_y = y_vlaue;
                    if (movecount > 0) {
                        // 拖动，使item序号变大
                        holdPosition = mStartPosition + i + 1;
                        if (mStartPosition / mNumColumns == holdPosition / mNumColumns) {
                            to_x = -x_vlaue;
                            to_y = 0;
                        } else if (holdPosition % 4 == 0) {
                            to_x = 3 * x_vlaue;
                            to_y = -y_vlaue;
                        } else {
                            to_x = -x_vlaue;
                            to_y = 0;
                        }
                    } else {
                        // 拖动，使item序号变小
                        holdPosition = mStartPosition - i - 1;
                        if (mStartPosition / mNumColumns == holdPosition / mNumColumns) {
                            to_x = x_vlaue;
                            to_y = 0;
                        } else if ((holdPosition + 1) % 4 == 0) {
                            to_x = -3 * x_vlaue;
                            to_y = y_vlaue;
                        } else {
                            to_x = x_vlaue;
                            to_y = 0;
                        }
                    }
                    ViewGroup moveViewGroup = (ViewGroup) getChildAt(holdPosition - getFirstVisiblePosition());
                    Animation moveAnimation = generateAnimation(to_x, to_y);
                    //如果是最后一个移动的，那么设置他的最后个动画ID为LastAnimationID
                    if (holdPosition == mDropPosition) {
                        mLastAnimationID = moveAnimation.toString();
                    }
                    moveAnimation.setAnimationListener(new AnimationListener() {

                        @Override
                        public void onAnimationStart(Animation animation) {
                            // TODO Auto-generated method stub
                            isMoving = true;
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                            // TODO Auto-generated method stub
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            // TODO Auto-generated method stub
                            // 如果为最后个动画结束，那执行下面的方法
                            if (animation.toString().equalsIgnoreCase(mLastAnimationID)) {
                                mDragGridAdapter.setDropItemPosition(mDropPosition);
                                mDragGridAdapter.exchange(mStartPosition, mDropPosition);
                                mStartPosition = mDropPosition;
                                isMoving = false;
                            }
                        }
                    });
                    moveViewGroup.startAnimation(moveAnimation);
                }
            }
        }
    }

    /**
     * 获取移动动画
     */
    public Animation generateAnimation(float toXValue, float toYValue) {
        TranslateAnimation mTranslateAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0F,
                Animation.RELATIVE_TO_SELF, toXValue,
                Animation.RELATIVE_TO_SELF, 0.0F,
                Animation.RELATIVE_TO_SELF, toYValue);// 当前位置移动到指定位置
        mTranslateAnimation.setFillAfter(false);
        mTranslateAnimation.setDuration(300L);
        return mTranslateAnimation;
    }

}