package com.chetbox.mousecursor;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

public class StatusBarHelper {
    private Context mContext;
    private View mStatusBarHelperView;
    private static StatusBarHelper mStatusBarHelper = new StatusBarHelper();
    private static final int MSG_INIT_STATUS_BAR = 1;
    private static final int MSG_REMOVE_STATUS_BAR = 2;
    private static final long MSG_REMOVE_DELAY_TIME = 500L;

    private StatusBarHelperViewLayoutListener mStatusBarHelperViewLayoutListener;

    private Handler mStatusBarHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INIT_STATUS_BAR:
                    mStatusBarHandler.removeMessages(MSG_REMOVE_STATUS_BAR);
                    initStatusBarHelperView();
                    break;
                case MSG_REMOVE_STATUS_BAR:
                    removeStatusBarView();
                    break;
            }
        }
    };

    public static StatusBarHelper getInstance() {
        return mStatusBarHelper;
    }

    private void initStatusBarHelperView() {
        if (mStatusBarHelperView != null) {
            return;
        }
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mStatusBarHelperView = new View(mContext);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        lp.gravity = Gravity.LEFT | Gravity.TOP;
        lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        lp.format = PixelFormat.TRANSLUCENT;
        lp.alpha = 0.1f;
        mStatusBarHelperView.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);
        wm.addView(mStatusBarHelperView, lp);
    }

    public void addStatusBarHelperView(Context context) {
        mContext = context;
        Message message = mStatusBarHandler.obtainMessage(StatusBarHelper.MSG_INIT_STATUS_BAR);
        mStatusBarHandler.sendMessage(message);
    }

    public void removeStatusBarHelperView() {
        Message message = mStatusBarHandler.obtainMessage(StatusBarHelper.MSG_REMOVE_STATUS_BAR);
        mStatusBarHandler.sendMessageDelayed(message, StatusBarHelper.MSG_REMOVE_DELAY_TIME);
    }

    public int getStatusBarHeight() {
        if (mStatusBarHelperView == null) {
            return 0;
        }
        int[] windowParams = new int[2];
        int[] screenParams = new int[2];
        mStatusBarHelperView.getLocationInWindow(windowParams);
        mStatusBarHelperView.getLocationOnScreen(screenParams);
        return screenParams[1] - windowParams[1];
    }

    public Point getScreenSize()
    {
        if (mStatusBarHelperView == null) {
            return null;
        }
        int[] screenParams = new int[2];
        mStatusBarHelperView.getLocationOnScreen(screenParams);
        return new Point(screenParams[0] + mStatusBarHelperView.getRight(),screenParams[1] + mStatusBarHelperView.getBottom());
    }

    public Point cutHeadSize()
    {
        if (mStatusBarHelperView == null) {
            return null;
        }
        int[] screenParams = new int[2];
        mStatusBarHelperView.getLocationOnScreen(screenParams);
        return new Point(screenParams[0],screenParams[1]);
    }

    private void removeStatusBarView() {
        if (mStatusBarHelperView != null) {
            mStatusBarHelperView.getViewTreeObserver().removeOnGlobalLayoutListener(mGlobalLayoutListener);
            WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            wm.removeView(mStatusBarHelperView);
            mStatusBarHelperView = null;
        }
    }

    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            if (mStatusBarHelperViewLayoutListener != null) {
                mStatusBarHelperViewLayoutListener.updateStatusBarHeightWhenGlobalLayout();
            }
        }
    };

    public interface StatusBarHelperViewLayoutListener {
        void updateStatusBarHeightWhenGlobalLayout();
    }

    public void setStatusBarHelperViewLayoutListener(StatusBarHelperViewLayoutListener listener) {
        mStatusBarHelperViewLayoutListener = listener;
    }

}