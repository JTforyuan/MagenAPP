package com.example.maclient.Views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.maclient.R;

/**
 * Created by JiaTang on 2018/2/5.
 */

public class AudioRecordView extends RelativeLayout {

    public static final String TAG = "AudioRecordView";

    private Context mContext;

    private TextView mRecordTimeTextview;
    private ImageView mMicrophoneImageview;

    // 要显示的已录音时长
    private int mRecordedTime;

    // 屏幕宽高
    private int mScreenWidth;
    private int mScreenHeigth;

    // 控件中心位置
    private int mMicroPosition_x;
    private int mMicroPosition_y;

    private int mColor = Color.BLACK;
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public AudioRecordView(Context context) {
        super(context);

        mContext = context;
        init();
    }

    public AudioRecordView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0 );
    }

    public AudioRecordView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mContext = context;
        //新创建了attrs.xml文件自定义View属性后，需要在自定义View的构造方法中处理自定义属性
        //获取自定义View的属性集合并从中获取自定义属性（红色为默认值）
        //TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircleView);
        //mColor = a.getColor(R.styleable.CircleView_circle_color, Color.RED);
        //使用完TypedArray后，需要及时释放资源
        //a.recycle();

        init();
    }

    public void setText(String text) {
        mRecordTimeTextview.setText(text);
    }

    public CharSequence getText() {
        return mRecordTimeTextview.getText();
    }

    public void setImageLevel (int level) {
        mMicrophoneImageview.setImageLevel(level);
    }

    public Drawable getImageDrawable() {
        return mMicrophoneImageview.getDrawable();
    }

    private void init() {

        // 因为是组合控件。设置进行绘制，否则不会自动绘制
        setWillNotDraw(false);

        LayoutInflater.from(mContext).inflate(R.layout.microphone_view, this);
        // 从控件对象中获取属性
        mRecordTimeTextview = findViewById(R.id.tv_record_time);
        mMicrophoneImageview = findViewById(R.id.image_microphone);

        // 用LayoutParams 获取属性，单位为像素px，不是dp
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mMicrophoneImageview.getLayoutParams();

        mPaint.setColor(mColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(10);

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

        // DisplayMetrics 主要存放各种显示信息，屏幕宽高等，通过将新建对象传入方法getMetrics()中获得
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);

        //int width = dm.widthPixels;         // 屏幕宽度（像素）
        //int height = dm.heightPixels;       // 屏幕高度（像素）
        //float density = dm.density;         // 屏幕密度（0.75 / 1.0 / 1.5）
        //int densityDpi = dm.densityDpi;     // 屏幕密度dpi（120 / 160 / 240）
        // 屏幕宽度算法:屏幕宽度（像素）/屏幕密度
        //int screenWidth = (int) (width / density);  // 屏幕宽度(dp)
        //int screenHeight = (int) (height / density);// 屏幕高度(dp)

        mScreenWidth = dm.widthPixels;
        mScreenHeigth = dm.heightPixels;

        mMicroPosition_x = mScreenWidth / 2 ;
        mMicroPosition_y = ( lp.height / 4 )* 3 + lp.topMargin;

        Log.i(TAG, "init: " + dm.widthPixels + " - " + dm.heightPixels + " - " + dm.density + " - "
                + dm.densityDpi + " - " + lp.topMargin);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawCircle(mMicroPosition_x, mMicroPosition_y, 350, mPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
