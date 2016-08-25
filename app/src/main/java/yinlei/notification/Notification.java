package yinlei.notification;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.IntEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.security.spec.MGF1ParameterSpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Administrator on 2016/8/24.
 */
public class Notification implements View.OnTouchListener {

    private static final int DIRECTION_LEFT = -1;
    private static final int DIRECTION_NONE = 0;
    private static final int DIRECTION_RIGHT = 1;

    private static final int DISMISS_INTERVAL = 4000;

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private View contentView;
    private Context context;
    private int screenWidth = 0;
    private int statusBarHeight = 0;

    private boolean isShowing = false;
    private ValueAnimator restoreAnimator = null;
    private ValueAnimator dismissAnimator = null;

    private ImageView tvIcon;
    private TextView tvTitle;
    private TextView tvContent;
    private TextView tvTime;

    public Notification(Builder builder) {
        context = builder.getContext();

        statusBarHeight = getStatusBarHeight();
        screenWidth = context.getResources().getDisplayMetrics().widthPixels;

        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        layoutParams.windowAnimations = R.style.NotificationAnim;
        layoutParams.x = 0;
        layoutParams.y = -statusBarHeight;

        setContentView(context, builder);
    }

    /**
     * 设置真实布局
     *
     * @param context  上下文
     * @param builder  构建者
     */
    private void setContentView(Context context, Builder builder) {
        contentView = LayoutInflater.from(context).inflate(R.layout.layout_notification, null);
        View view_state = contentView.findViewById(R.id.v_state_bar);
        ViewGroup.LayoutParams layoutParams = view_state.getLayoutParams();
        layoutParams.height = statusBarHeight;
        view_state.setLayoutParams(layoutParams);

        tvIcon = (ImageView) contentView.findViewById(R.id.iv_icon);
        tvTitle = (TextView) contentView.findViewById(R.id.tv_title);
        tvContent = (TextView) contentView.findViewById(R.id.tv_content);
        tvTime = (TextView) contentView.findViewById(R.id.tv_time);

        setIcon(builder.imgRes);
        setTitle(builder.title);
        setContent(builder.content);
        setTime(builder.time);

        contentView.setOnTouchListener(this);
    }


    //隐藏window
    private static final int HIDE_WINDOW = 0;


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HIDE_WINDOW:
                    dismiss();
                    break;
            }
        }
    };


    private int downX = 0;

    //移除方向
    private int direction = DIRECTION_NONE;

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (isAnimatorRunning()) {
            return false;
        }
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = (int) motionEvent.getRawX();
                break;
            case MotionEvent.ACTION_MOVE:
                handler.removeMessages(HIDE_WINDOW);
                int moveX = (int) (motionEvent.getRawX() - downX);
                if (moveX > 0) {
                    direction = DIRECTION_RIGHT;
                } else {
                    direction = DIRECTION_LEFT;
                }
                updateWindowLocation(moveX, layoutParams.y);
                break;
            case MotionEvent.ACTION_UP:
                if (Math.abs(layoutParams.x) > screenWidth / 2) {
                    startDismissAnimator(direction);
                } else {
                    startRestorAnimator();
                }
                break;
        }
        return true;
    }

    /**
     * 开始动画
     */
    private void startRestorAnimator() {
        restoreAnimator = new ValueAnimator().ofInt(layoutParams.x, 0);
        restoreAnimator.setDuration(300);
        restoreAnimator.setEvaluator(new IntEvaluator());

        restoreAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                updateWindowLocation((Integer) valueAnimator.getAnimatedValue(), -statusBarHeight);
            }
        });

        restoreAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                restoreAnimator = null;
                autoDismiss();
            }
        });
        restoreAnimator.start();
    }

    /**
     * 消失动画
     * @param direction  消失方向
     */
    private void startDismissAnimator(int direction) {
        if (direction == DIRECTION_LEFT)
            dismissAnimator = new ValueAnimator().ofInt(layoutParams.x, -screenWidth);
        else {
            dismissAnimator = new ValueAnimator().ofInt(layoutParams.x, screenWidth);
        }
        dismissAnimator.setDuration(300);
        dismissAnimator.setEvaluator(new IntEvaluator());

        dismissAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                updateWindowLocation((Integer) animation.getAnimatedValue(), -statusBarHeight);
            }
        });
        dismissAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                restoreAnimator = null;
                dismiss();
            }
        });
        dismissAnimator.start();
    }

    /**
     * 是否是正在运行的动画
     * @return  true / false
     */
    private boolean isAnimatorRunning() {
        return (restoreAnimator != null && restoreAnimator.isRunning()) || (dismissAnimator != null && dismissAnimator.isRunning());

    }

    /**
     * 更新window位置
     * @param x  x
     * @param y  y
     */
    public void updateWindowLocation(int x, int y) {
        if (isShowing) {
            layoutParams.x = x;
            layoutParams.y = y;
            windowManager.updateViewLayout(contentView, layoutParams);
        }

    }

    /**
     * 移除
     */
    public void dismiss() {
        if (isShowing) {
            resetState();
            windowManager.removeView(contentView);
        }
    }

    /**
     * 显示
     */
    public void show() {
        if (!isShowing) {
            isShowing = true;
            windowManager.addView(contentView, layoutParams);
            autoDismiss();
        }
    }

    /**
     * 获取状态栏的高度
     */
    public int getStatusBarHeight() {
        int height = 0;
        int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            height = context.getResources().getDimensionPixelSize(resId);
        }
        return height;
    }

    /**
     * 设置图标
     * @param imgRes  图片资源
     */
    public void setIcon(int imgRes) {
        if (-1 != imgRes) {
            tvIcon.setVisibility(View.VISIBLE);
            tvIcon.setImageResource(imgRes);
        }
    }

    /**
     * 设置标题
     * @param title  标题
     */
    public void setTitle(String title) {
        if (!TextUtils.isEmpty(title)) {
            tvTitle.setVisibility(View.VISIBLE);
            tvTitle.setText(title);
        }
    }

    /**
     * 设置内容
     * @param content  内容
     */
    public void setContent(String content) {
        tvContent.setText(content);
    }

    /**
     * 设置显示的时间
     * @param time   要显示的时间
     */
    public void setTime(long time) {
        SimpleDateFormat formatDateTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tvTime.setText(formatDateTime.format(new Date(time)));
    }

    /**
     * 重置状态
     */
    private void resetState() {
        isShowing = false;
        layoutParams.x = 0;
        layoutParams.y = -statusBarHeight;
    }


    /**
     * 自动隐藏通知
     */
    private void autoDismiss() {
        handler.removeMessages(HIDE_WINDOW);
        handler.sendEmptyMessageDelayed(HIDE_WINDOW, DISMISS_INTERVAL);
    }


    /**
     * 构建者   使用建造者模式
     */
    public static class Builder {
        private Context context;
        private int imgRes = -1;
        private String title;
        private String content = "";

        public Context getContext() {
            return context;
        }

        private long time = System.currentTimeMillis();

        public Builder setContext(Context context) {
            this.context = context;
            return this;
        }

        public Builder setImgRes(int imgRes) {
            this.imgRes = imgRes;
            return this;
        }


        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }


        public Builder setContent(String content) {
            this.content = content;
            return this;
        }


        public Builder setTime(long time) {
            this.time = time;
            return this;
        }


        public Notification build() {
            if (null == content) {
                throw new IllegalArgumentException("the context is null");
            }
            return new Notification(this);
        }
    }


}
