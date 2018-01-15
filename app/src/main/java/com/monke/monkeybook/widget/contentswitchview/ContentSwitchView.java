package com.monke.monkeybook.widget.contentswitchview;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.monke.monkeybook.help.ReadBookControl;
import com.monke.monkeybook.utils.DensityUtil;

import java.util.ArrayList;
import java.util.List;

public class ContentSwitchView extends FrameLayout implements BookContentView.SetDataListener {
    private final long animDuration = 300;
    public final static int NONE = -1;
    public final static int PRE_AND_NEXT = 0;
    public final static int ONLY_PRE = 1;
    public final static int ONLY_NEXT = 2;

    private int state = NONE;    //0是有上一页   也有下一页 ;  2是只有下一页  ；1是只有上一页;-1是没有上一页 也没有下一页；

    private int scrollX;
    private Boolean isMoving = false;
    private Boolean readAloud = false;

    private BookContentView durPageView;
    private List<BookContentView> viewContents;
    private ReadBookControl readBookControl;

    public interface OnBookReadInitListener {
        void success();
    }

    private OnBookReadInitListener bookReadInitListener;

    public ContentSwitchView(Context context) {
        super(context);
        init();
    }

    public ContentSwitchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ContentSwitchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ContentSwitchView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        readBookControl = ReadBookControl.getInstance();

        scrollX = DensityUtil.dp2px(getContext(), 30f);
        durPageView = new BookContentView(getContext());
        durPageView.setReadBookControl(readBookControl);

        viewContents = new ArrayList<>();
        viewContents.add(durPageView);

        this.addView(durPageView);
    }

    public void bookReadInit(OnBookReadInitListener bookReadInitListener) {
        this.bookReadInitListener = bookReadInitListener;
        durPageView.getTvContent().getViewTreeObserver().addOnGlobalLayoutListener(layoutInitListener);
    }

    public void startLoading() {
        int height = durPageView.getTvContent().getHeight();
        if (height > 0) {
            if (loadDataListener != null && durHeight != height) {
                durHeight = height;
                loadDataListener.initData(durPageView.getLineCount(height));
            }
        }
        durPageView.getTvContent().getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private float startX = -1;

    /**
     * 操作事件
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (!isMoving) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getX();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (viewContents.size() > 1) {
                        if (startX == -1)
                            startX = event.getX();
                        int durX = (int) (event.getX() - startX);
                        if (durX > 0 && (state == PRE_AND_NEXT || state == ONLY_PRE)) {
                            int tempX = durX - getWidth();
                            if (tempX < -getWidth())
                                tempX = -getWidth();
                            else if (tempX > 0)
                                tempX = 0;
                            viewContents.get(0).layout(tempX, viewContents.get(0).getTop(), tempX + getWidth(), viewContents.get(0).getBottom());
                        } else if (durX < 0 && (state == PRE_AND_NEXT || state == ONLY_NEXT)) {
                            int tempX = durX;
                            if (tempX > 0)
                                tempX = 0;
                            else if (tempX < -getWidth())
                                tempX = -getWidth();
                            int tempIndex = (state == PRE_AND_NEXT ? 1 : 0);
                            viewContents.get(tempIndex).layout(tempX, viewContents.get(tempIndex).getTop(), tempX + getWidth(), viewContents.get(tempIndex).getBottom());
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (startX == -1)
                        startX = event.getX();
                    if (event.getX() - startX > 0) {
                        if (state == PRE_AND_NEXT || state == ONLY_PRE) {
                            if (event.getX() - startX > scrollX) {
                                //向前翻页成功
                                initMoveSuccessAnim(viewContents.get(0), 0);
                            } else {
                                initMoveFailAnim(viewContents.get(0), -getWidth());
                            }
                        } else {
                            //没有上一页
                            noPre();
                        }
                    } else if (event.getX() - startX < 0) {
                        if (state == PRE_AND_NEXT || state == ONLY_NEXT) {
                            int tempIndex = (state == PRE_AND_NEXT ? 1 : 0);
                            if (startX - event.getX() > scrollX) {
                                //向后翻页成功
                                initMoveSuccessAnim(viewContents.get(tempIndex), -getWidth());
                            } else {
                                initMoveFailAnim(viewContents.get(tempIndex), 0);
                            }
                        } else {
                            //没有下一页
                            noNext();
                        }
                    } else {
                        //点击事件
                        if (readBookControl.getCanClickTurn()
                                && ((event.getX() <= getWidth() / 3)
                                || (event.getY() <= getHeight() / 3 && event.getX() <= getWidth() / 3 * 2))) {
                            //点击向前翻页
                            gotoPrePage();
                        } else if (readBookControl.getCanClickTurn()
                                && ((event.getX() >= getWidth() / 3 * 2)
                                || (event.getY() >= getHeight() / 3*2 && event.getX() >= getWidth() / 3))) {
                            //点击向后翻页
                            gotoNextPage();
                        } else {
                            //点击中间部位
                            if (loadDataListener != null)
                                loadDataListener.showMenu();
                        }
                    }
                    startX = -1;
                    break;
                default:
                    break;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (viewContents.size() > 0) {
            if (state == NONE && viewContents.size() >= 1) {
                viewContents.get(0).layout(0, top, getWidth(), bottom);
            } else if (state == PRE_AND_NEXT && viewContents.size() >= 3) {
                viewContents.get(0).layout(-getWidth(), top, 0, bottom);
                viewContents.get(1).layout(0, top, getWidth(), bottom);
                viewContents.get(2).layout(0, top, getWidth(), bottom);
            } else if (state == ONLY_PRE && viewContents.size() >= 2) {
                viewContents.get(0).layout(-getWidth(), top, 0, bottom);
                viewContents.get(1).layout(0, top, getWidth(), bottom);
            } else if (viewContents.size() >= 2) {
                viewContents.get(0).layout(0, top, getWidth(), bottom);
                viewContents.get(1).layout(0, top, getWidth(), bottom);
            }
        } else {
            super.onLayout(changed, left, top, right, bottom);
        }
    }

    /**
     * 翻页动画
     */
    private void initMoveSuccessAnim(final View view, final int orderX) {
        if (null != view) {
            long temp = Math.abs(view.getLeft() - orderX) / (getWidth() / animDuration);
            ValueAnimator tempAnim = ValueAnimator.ofInt(view.getLeft(), orderX).setDuration(temp);
            tempAnim.addUpdateListener(animation -> {
                int value = (int) animation.getAnimatedValue();
                view.layout(value, view.getTop(), value + getWidth(), view.getBottom());
            });
            tempAnim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    isMoving = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    isMoving = false;
                    if (orderX == 0) {
                        //翻向前一页
                        durPageView = viewContents.get(0);
                        if (state == PRE_AND_NEXT) {
                            ContentSwitchView.this.removeView(viewContents.get(viewContents.size() - 1));
                            viewContents.remove(viewContents.size() - 1);
                        }
                        state = ONLY_NEXT;
                        if (durPageView.getDurChapterIndex() - 1 >= 0 || durPageView.getDurPageIndex() - 1 >= 0) {
                            addPrePage(durPageView.getDurChapterIndex(), durPageView.getChapterAll(), durPageView.getDurPageIndex(), durPageView.getPageAll());
                            if (state == NONE)
                                state = ONLY_PRE;
                            else state = PRE_AND_NEXT;
                        }
                    } else {
                        //翻向后一页
                        if (state == ONLY_NEXT) {
                            durPageView = viewContents.get(1);
                        } else {
                            durPageView = viewContents.get(2);
                            ContentSwitchView.this.removeView(viewContents.get(0));
                            viewContents.remove(0);
                        }
                        state = ONLY_PRE;
                        if (durPageView.getDurChapterIndex() + 1 <= durPageView.getChapterAll() - 1 || durPageView.getDurPageIndex() + 1 <= durPageView.getPageAll() - 1) {
                            addNextPage(durPageView.getDurChapterIndex(), durPageView.getChapterAll(), durPageView.getDurPageIndex(), durPageView.getPageAll());
                            if (state == NONE)
                                state = ONLY_NEXT;
                            else state = PRE_AND_NEXT;
                        }
                    }
                    if (loadDataListener != null) {
                        loadDataListener.updateProgress(durPageView.getDurChapterIndex(), durPageView.getDurPageIndex());
                    }
                    if (readAloud && durPageView.getContent() != null) {
                        loadDataListener.readAloud(durPageView.getContent());
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            tempAnim.start();
        }
    }

    private void initMoveFailAnim(final View view, int orderX) {
        if (null != view) {
            long temp = Math.abs(view.getLeft() - orderX) / (getWidth() / animDuration);
            ValueAnimator tempAnim = ValueAnimator.ofInt(view.getLeft(), orderX).setDuration(temp);
            tempAnim.addUpdateListener(animation -> {
                int value = (int) animation.getAnimatedValue();
                view.layout(value, view.getTop(), value + getWidth(), view.getBottom());
            });
            tempAnim.start();
        }
    }

    public void setInitData(int durChapterIndex, int chapterAll, int durPageIndex) {
        updateOtherPage(durChapterIndex, chapterAll, durPageIndex, -1);
        durPageView.setLoadDataListener(loadDataListener, this);
        durPageView.loadData(null != loadDataListener ? loadDataListener.getChapterTitle(durChapterIndex) : "",
                durChapterIndex, chapterAll, durPageIndex);

        if (loadDataListener != null)
            loadDataListener.updateProgress(durPageView.getDurChapterIndex(), durPageView.getDurPageIndex());
    }

    private void updateOtherPage(int durChapterIndex, int chapterAll, int durPageIndex, int pageAll) {
        if (chapterAll > 1 || pageAll > 1) {
            if (((durChapterIndex == 0) && (pageAll == -1)) || ((durChapterIndex == 0) && (durPageIndex == 0))) {
                //ONLY_NEXT
                addNextPage(durChapterIndex, chapterAll, durPageIndex, pageAll);
                if (state == ONLY_PRE || state == PRE_AND_NEXT) {
                    this.removeView(viewContents.get(0));
                    viewContents.remove(0);
                }
                state = ONLY_NEXT;
            } else if ((durChapterIndex == chapterAll - 1 && pageAll == -1) || (durChapterIndex == chapterAll - 1 && durPageIndex == pageAll - 1 && pageAll != -1)) {
                //ONLY_PRE
                addPrePage(durChapterIndex, chapterAll, durPageIndex, pageAll);
                if (state == ONLY_NEXT || state == PRE_AND_NEXT) {
                    this.removeView(viewContents.get(2));
                    viewContents.remove(2);
                }
                state = ONLY_PRE;
            } else {
                //PRE_AND_NEXT
                addNextPage(durChapterIndex, chapterAll, durPageIndex, pageAll);
                addPrePage(durChapterIndex, chapterAll, durPageIndex, pageAll);
                state = PRE_AND_NEXT;
            }
        } else {
            //NONE
            if (state == ONLY_PRE) {
                this.removeView(viewContents.get(0));
                viewContents.remove(0);
            } else if (state == ONLY_NEXT) {
                this.removeView(viewContents.get(1));
                viewContents.remove(1);
            } else if (state == PRE_AND_NEXT) {
                this.removeView(viewContents.get(0));
                this.removeView(viewContents.get(2));
                viewContents.remove(2);
                viewContents.remove(0);
            }
            state = NONE;
        }
    }

    private void addNextPage(int durChapterIndex, int chapterAll, int durPageIndex, int pageAll) {
        if (state == ONLY_NEXT || state == PRE_AND_NEXT) {
            int temp = (state == ONLY_NEXT ? 1 : 2);
            if (pageAll > 0 && durPageIndex >= 0 && durPageIndex < pageAll - 1)
                viewContents.get(temp).loadData(null != loadDataListener ? loadDataListener.getChapterTitle(durChapterIndex) : "",
                        durChapterIndex, chapterAll, durPageIndex + 1);
            else
                viewContents.get(temp).loadData(null != loadDataListener ? loadDataListener.getChapterTitle(durChapterIndex + 1) : "",
                        durChapterIndex + 1, chapterAll, BookContentView.DurPageIndexBegin);
        } else if (state == ONLY_PRE || state == NONE) {
            BookContentView next = new BookContentView(getContext());
            next.setReadBookControl(readBookControl);
            next.setLoadDataListener(loadDataListener, this);
            if (pageAll > 0 && durPageIndex >= 0 && durPageIndex < pageAll - 1)
                next.loadData(null != loadDataListener ? loadDataListener.getChapterTitle(durChapterIndex) : "",
                        durChapterIndex, chapterAll, durPageIndex + 1);
            else
                next.loadData(null != loadDataListener ? loadDataListener.getChapterTitle(durChapterIndex + 1) : "",
                        durChapterIndex + 1, chapterAll, BookContentView.DurPageIndexBegin);
            viewContents.add(next);
            this.addView(next, 0);
        }
    }

    private void addPrePage(int durChapterIndex, int chapterAll, int durPageIndex, int pageAll) {
        if (state == ONLY_NEXT || state == NONE) {
            BookContentView pre = new BookContentView(getContext());
            pre.setReadBookControl(readBookControl);
            pre.setLoadDataListener(loadDataListener, this);
            if (pageAll > 0 && durPageIndex >= 0 && durPageIndex > 0)
                pre.loadData(null != loadDataListener ? loadDataListener.getChapterTitle(durChapterIndex) : "",
                        durChapterIndex, chapterAll, durPageIndex - 1);
            else
                pre.loadData(null != loadDataListener ? loadDataListener.getChapterTitle(durChapterIndex - 1) : "",
                        durChapterIndex - 1, chapterAll, BookContentView.DurPageIndexEnd);
            viewContents.add(0, pre);
            this.addView(pre);
        } else if (state == ONLY_PRE || state == PRE_AND_NEXT) {
            if (pageAll > 0 && durPageIndex >= 0 && durPageIndex > 0)
                viewContents.get(0).loadData(null != loadDataListener ? loadDataListener.getChapterTitle(durChapterIndex) : "",
                        durChapterIndex, chapterAll, durPageIndex - 1);
            else
                viewContents.get(0).loadData(null != loadDataListener ? loadDataListener.getChapterTitle(durChapterIndex - 1) : "",
                        durChapterIndex - 1, chapterAll, BookContentView.DurPageIndexEnd);
        }
    }

    /**
     * 翻下页
     */
    private void gotoNextPage() {
        if (state == PRE_AND_NEXT || state == ONLY_NEXT) {
            int tempIndex = (state == PRE_AND_NEXT ? 1 : 0);
            initMoveSuccessAnim(viewContents.get(tempIndex), -getWidth());
        } else {
            noNext();
        }
    }

    /**
     * 翻上页
     */
    private void gotoPrePage() {
        if (state == PRE_AND_NEXT || state == ONLY_PRE) {
            initMoveSuccessAnim(viewContents.get(0), 0);
        } else {
            noPre();
        }
    }

    @Override
    public void setDataFinish(BookContentView bookContentView, int durChapterIndex, int chapterAll, int durPageIndex, int pageAll, int fromPageIndex) {
        if (null != getDurContentView() && bookContentView == getDurContentView() && chapterAll > 0 && pageAll > 0) {
            updateOtherPage(durChapterIndex, chapterAll, durPageIndex, pageAll);
        }
    }

    @Override
    public void setReadAloud(BookContentView bookContentView, String content) {
        if (readAloud && null != getDurContentView() && bookContentView == getDurContentView()) {
            loadDataListener.readAloud(content);
        }
    }

    /**
     * 开始朗读
     */
    public void readAloudStart() {
        readAloud = true;
        loadDataListener.readAloud(durPageView.getContent());
    }

    /**
     * 朗读下一页
     */
    public void readAloudNext() {
        gotoNextPage();
    }

    /**
     * 停止朗读
     */
    public void readAloudStop() {
        readAloud = false;
    }

    public interface LoadDataListener {
        void loadData(BookContentView bookContentView, long tag, int chapterIndex, int pageIndex);

        void updateProgress(int chapterIndex, int pageIndex);

        String getChapterTitle(int chapterIndex);

        void initData(int lineCount);

        void showMenu();

        void readAloud(String content);
    }

    private LoadDataListener loadDataListener;

    public void setLoadDataListener(LoadDataListener loadDataListener) {
        this.loadDataListener = loadDataListener;
    }

    public BookContentView getDurContentView() {
        return durPageView;
    }

    private void noPre() {
        Toast.makeText(getContext(), "没有上一页", Toast.LENGTH_SHORT).show();
    }

    private void noNext() {
        Toast.makeText(getContext(), "没有下一页", Toast.LENGTH_SHORT).show();
    }

    private ViewTreeObserver.OnGlobalLayoutListener layoutInitListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            if (bookReadInitListener != null) {
                bookReadInitListener.success();
            }
            durPageView.getTvContent().getViewTreeObserver().removeOnGlobalLayoutListener(layoutInitListener);
        }
    };
    private ViewTreeObserver.OnGlobalLayoutListener layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            int height = durPageView.getTvContent().getHeight();
            if (height > 0) {
                if (loadDataListener != null && durHeight != height) {
                    durHeight = height;
                    loadDataListener.initData(durPageView.getLineCount(height));
                }
            }
        }
    };

    private int durHeight = 0;

    public Paint getTextPaint() {
        return durPageView.getTvContent().getPaint();
    }

    public int getContentWidth() {
        return durPageView.getTvContent().getWidth();
    }

    public void changeBg() {
        for (BookContentView item : viewContents) {
            item.setBg(readBookControl);
        }
    }

    public void changeTextSize() {
        for (BookContentView item : viewContents) {
            item.setTextKind(readBookControl);
        }
        loadDataListener.initData(durPageView.getLineCount(durHeight));
    }

    /**
     * 音量键翻页
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (readBookControl.getCanKeyTurn() && keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            gotoNextPage();
            return true;
        } else if (readBookControl.getCanKeyTurn() && keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            gotoPrePage();
            return true;
        }
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (readBookControl.getCanKeyTurn() && keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return true;
        } else if (readBookControl.getCanKeyTurn() && keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return true;
        }
        return false;
    }

    public void loadError() {
        if (durPageView != null) {
            durPageView.loadError();
        }
    }
}