package ca.yorku.eecs.mack.demotiltball78040;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.View;
import android.media.MediaPlayer;
import android.graphics.Path;

import android.util.Log;


import java.util.Locale;

public class RollingBallPanel extends View
{
    final static float DEGREES_TO_RADIANS = 0.0174532925f;

    // the ball diameter will be min(width, height) / this_value
    final static float BALL_DIAMETER_ADJUST_FACTOR = 30;

    final static int DEFAULT_LABEL_TEXT_SIZE = 20; // tweak as necessary
    final static int DEFAULT_STATS_TEXT_SIZE = 10;
    final static int DEFAULT_GAP = 7; // between lines of text
    final static int DEFAULT_OFFSET = 10; // from bottom of display

    final static int MODE_NONE = 0;
    final static int PATH_TYPE_SQUARE = 1;
    final static int PATH_TYPE_CIRCLE = 2;
    final static int PATH_TYPE_MAZE = 3;
    final static int PATH_TYPE_MAZE2 = 4;
    final static int PATH_TYPE_MAZE3 = 5;

    final static float PATH_WIDTH_MEDIUM = 4f; // ... x ball diameter

    float radiusOuter, radiusInner;

    Bitmap ball, decodedBallBitmap;
    int ballDiameter;

    float dT; // time since last sensor event (seconds)

    float width, height, pixelDensity;
    int labelTextSize, statsTextSize, gap, offset;

    RectF innerRectangle, outerRectangle, innerShadowRectangle, outerShadowRectangle, ballNow;
//    Lap line
    float lap_line_x,lap_line_y,lap_line_x_1,lap_line_y_1;
//    Touch Lap line flag
    boolean touch_lap_line_flag;
//    Cheating flag
    int cheating_flag;
    boolean cheating_flag_1, fell_flag;

    int mode;
    // obstacles[0] starting point, obstacles[1] goal point
    RectF[] obstacles;
    int goal_point;

    final static String MYDEBUG = "MYDEBUG";
//  Inside flag
    boolean was_inside_flag;

    boolean was_inside;

    boolean touchFlag, touching_line;
    Vibrator vib;


    // Statistics
    int wallHits;
    Double accuracy, time_per_trial;
    //    Lap time counter
    long start_time, lap_time, start_time_outside, missed_time;



    float xBall, yBall; // top-left of the ball (for painting)
    float xBallCenter, yBallCenter; // center of the ball

    float pitch, roll;
    float tiltAngle, tiltMagnitude;

    // parameters from Setup dialog
    float gain, pathWidth;
    int pathType , lapNumbers;

    float velocity,max_velocity; // in pixels/second (velocity = tiltMagnitude * tiltVelocityGain
    float dBall; // the amount to move the ball (in pixels): dBall = dT * velocity
    float xCenter, yCenter; // the center of the screen
    long now, lastT;
    Paint statsPaint, labelPaint, linePaint, fillPaint, backgroundPaint;
    float[] updateY;

    Point[] path_1;
    Point[] path_2;

    Point[] best_path;

    String x_positions;
    String y_positions;
    String t_positions;


//    To manualy reset some variables
    public void reset(){
        lastT = System.nanoTime();

        touchFlag = false;
        was_inside_flag =  true;
        start_time=System.nanoTime();
        start_time_outside=0;
        missed_time=0;
        lap_time=0;
        fell_flag=false;
        wallHits = 0;
        goal_point=1;
        onWindowFocusChanged(true);

        was_inside=true;

        x_positions="";
        y_positions="";
        t_positions="";
    }

    public RollingBallPanel(Context contextArg)
    {
        super(contextArg);
        initialize(contextArg);
    }

    public RollingBallPanel(Context contextArg, AttributeSet attrs)
    {
        super(contextArg, attrs);
        initialize(contextArg);
    }

    public RollingBallPanel(Context contextArg, AttributeSet attrs, int defStyle)
    {
        super(contextArg, attrs, defStyle);
        initialize(contextArg);
    }

    // things that can be initialized from within this View
    private void initialize(Context c)
    {
        was_inside=true;

        linePaint = new Paint();
//        linePaint.setColor(Color.RED);
        linePaint.setColor(Color.BLUE);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2);
        linePaint.setAntiAlias(true);

        fillPaint = new Paint();
        fillPaint.setColor(Color.parseColor("#a5c7ea"));
//        fillPaint.setColor(0xffccbbbb);
        fillPaint.setStyle(Paint.Style.FILL);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.LTGRAY);
        backgroundPaint.setStyle(Paint.Style.FILL);

        labelPaint = new Paint();
        labelPaint.setColor(Color.BLACK);
        labelPaint.setTextSize(DEFAULT_LABEL_TEXT_SIZE);
        labelPaint.setAntiAlias(true);

        statsPaint = new Paint();
        statsPaint.setAntiAlias(true);
        statsPaint.setTextSize(DEFAULT_STATS_TEXT_SIZE);

        // NOTE: we'll create the actual bitmap in onWindowFocusChanged
        decodedBallBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ball);

        lastT = System.nanoTime();
        this.setBackgroundColor(Color.LTGRAY);
        touchFlag = false;
        outerRectangle = new RectF();
        innerRectangle = new RectF();
        innerShadowRectangle = new RectF();
        outerShadowRectangle = new RectF();

        lap_line_x=0;
        lap_line_x_1=0;
        lap_line_y=0;
        lap_line_y_1=0;
        touch_lap_line_flag=false;
        cheating_flag_1=false;
        cheating_flag =0;
        was_inside_flag =  true;
        start_time=System.nanoTime();
        start_time_outside=0;
        missed_time=0;
        lap_time=0;
        obstacles= new RectF[2];
        obstacles[0]= new RectF();
        obstacles[1]= new RectF();

        goal_point=1;
        fell_flag=false;

        ballNow = new RectF();
        wallHits = 0;

        vib = (Vibrator)c.getSystemService(Context.VIBRATOR_SERVICE);

        x_positions= "";
        y_positions= "";
        t_positions= "0";
    }

    /**
     * Called when the window hosting this view gains or looses focus.  Here we initialize things that depend on the
     * view's width and height.
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        if (!hasFocus)
            return;

        width = this.getWidth();
        height = this.getHeight();


        // the ball diameter is nominally 1/30th the smaller of the view's width or height
        ballDiameter = width < height ? (int)(width / BALL_DIAMETER_ADJUST_FACTOR)
                : (int)(height / BALL_DIAMETER_ADJUST_FACTOR);

        // now that we know the ball's diameter, get a bitmap for the ball
        ball = Bitmap.createScaledBitmap(decodedBallBitmap, ballDiameter, ballDiameter, true);

        // center of the view
        xCenter = width / 2f;
        yCenter = height / 2f;

        // top-left corner of the ball
//        xBall = xCenter;
//        yBall = yCenter;



        // center of the ball
        xBallCenter = xBall + ballDiameter / 2f;
        yBallCenter = yBall + ballDiameter / 2f;

        //first position of the ball
        x_positions= Float.toString(xBallCenter);
        y_positions= Float.toString(yBallCenter);

        // configure outer rectangle of the path
        radiusOuter = width < height ? 0.40f * width : 0.40f * height;
        outerRectangle.left = xCenter - radiusOuter;
        outerRectangle.top = yCenter - radiusOuter;
        outerRectangle.right = xCenter + radiusOuter;
        outerRectangle.bottom = yCenter + radiusOuter;

        // configure inner rectangle of the path
        // NOTE: medium path width is 4 x ball diameter
        radiusInner = radiusOuter - pathWidth * ballDiameter;
        innerRectangle.left = xCenter - radiusInner;
        innerRectangle.top = yCenter - radiusInner;
        innerRectangle.right = xCenter + radiusInner;
        innerRectangle.bottom = yCenter + radiusInner;


        lap_line_x = innerRectangle.left;
        lap_line_y = innerRectangle.top+(innerRectangle.bottom-innerRectangle.top)/2;
        lap_line_x_1 = outerRectangle.left;
        lap_line_y_1 = lap_line_y;

        xBall = outerRectangle.left + (innerRectangle.left - outerRectangle.left) / 2 - ballDiameter / 2f;
        yBall = innerRectangle.top + (innerRectangle.bottom - innerRectangle.top) / 2 + ballDiameter * 2f;


        if (pathType == PATH_TYPE_MAZE2 || pathType == PATH_TYPE_MAZE3 ) {
            xBall = outerRectangle.left +  ballDiameter;
            yBall = outerRectangle.top + (innerRectangle.top - outerRectangle.top)/ 2 - ballDiameter / 2f;
        }
        if (pathType == PATH_TYPE_MAZE ) {
            xBall = outerRectangle.left +  (innerRectangle.left - outerRectangle.left)/ 2 - ballDiameter/ 2f;
            yBall = outerRectangle.top + ballDiameter ;
        }
        //starting point obstacle[0]
        obstacles[0].left = xBall;
        obstacles[0].top = yBall ;
        obstacles[0].right = obstacles[0].left + ballDiameter * (3 / 2);
        obstacles[0].bottom =  obstacles[0].top + ballDiameter * (3 / 2);

        // configure outer shadow rectangle (needed to determine wall hits)
        // NOTE: line thickness (aka stroke width) is 2
        outerShadowRectangle.left = outerRectangle.left + ballDiameter - 2f;
        outerShadowRectangle.top = outerRectangle.top + ballDiameter - 2f;
        outerShadowRectangle.right = outerRectangle.right - ballDiameter + 2f;
        outerShadowRectangle.bottom = outerRectangle.bottom - ballDiameter + 2f;

        // configure inner shadow rectangle (needed to determine wall hits)
        innerShadowRectangle.left = innerRectangle.left + ballDiameter - 2f;
        innerShadowRectangle.top = innerRectangle.top + ballDiameter - 2f;
        innerShadowRectangle.right = innerRectangle.right - ballDiameter + 2f;
        innerShadowRectangle.bottom = innerRectangle.bottom - ballDiameter + 2f;

        // initialize a few things (e.g., paint and text size) that depend on the device's pixel density
        pixelDensity = this.getResources().getDisplayMetrics().density;
        labelTextSize = (int)(DEFAULT_LABEL_TEXT_SIZE * pixelDensity + 0.5f);
        labelPaint.setTextSize(labelTextSize);

        statsTextSize = (int)(DEFAULT_STATS_TEXT_SIZE * pixelDensity + 0.5f);
        statsPaint.setTextSize(statsTextSize);

        gap = (int)(DEFAULT_GAP * pixelDensity + 0.5f);
        offset = (int)(DEFAULT_OFFSET * pixelDensity + 0.5f);

        // compute y offsets for painting stats (bottom-left of display)
        updateY = new float[6]; // up to 6 lines of stats will appear
        for (int i = 0; i < updateY.length; ++i)
            updateY[i] = height - offset - i * (statsTextSize + gap);

        config_path();
    }


    /*
     * Do the heavy lifting here! Update the ball position based on the tilt angle, tilt
     * magnitude, order of control, etc.
     */
    public boolean updateBallPosition(float pitchArg, float rollArg, float tiltAngleArg, float tiltMagnitudeArg)
    {
        was_inside_flag = is_inside();

        touching_line= ballTouchingLine();
        pitch = pitchArg; // for information only (see onDraw)
        roll = rollArg; // for information only (see onDraw)


//        tiltAngle = tiltAngleArg - 45;
        tiltAngle = tiltAngleArg ;
        tiltMagnitude = tiltMagnitudeArg;

        // get current time and delta since last onDraw
        now = System.nanoTime();
        dT = (now - lastT) / 1000000000f; // seconds
        lastT = now;

        // don't allow tiltMagnitude to exceed 45 degrees
        final float MAX_MAGNITUDE = 45f;
        tiltMagnitude = tiltMagnitude > MAX_MAGNITUDE ? MAX_MAGNITUDE : tiltMagnitude;

        // compute ball velocity (depends on the tilt of the device and the gain setting)
        velocity = tiltMagnitude * gain;
        max_velocity= MAX_MAGNITUDE*gain;

        // compute how far the ball should move (depends on the velocity and the elapsed time since last update)
        dBall = dT * velocity; // make the ball move this amount (pixels)

        // compute the ball's new coordinates (depends on the angle of the device and dBall, as just computed)
        float dx = (float)Math.sin(tiltAngle * DEGREES_TO_RADIANS) * dBall;
        float dy = -(float)Math.cos(tiltAngle * DEGREES_TO_RADIANS) * dBall;


        xBall += dx;
        yBall += dy;

        return do_update();
    }

    public boolean updateBallPosition(int angle, int strength)
    {
        was_inside_flag = is_inside();

        touching_line= ballTouchingLine();

        // get current time and delta since last onDraw
        now = System.nanoTime();
        dT = (now - lastT) / 1000000000f; // seconds
        lastT = now;

        // compute ball velocity (depends on the tilt of the device and the gain setting)
        velocity = strength/20 * gain;

        max_velocity = 5*gain;

        // compute how far the ball should move (depends on the velocity and the elapsed time since last update)
        dBall = dT * velocity; // make the ball move this amount (pixels)

//        angle = angle+45;

        float dx = (float)Math.cos(angle* DEGREES_TO_RADIANS)  * dBall;
        float dy = -(float)Math.sin(angle* DEGREES_TO_RADIANS) * dBall;

        xBall += dx;
        yBall += dy;

        return do_update();
    }

    public boolean updateBallPosition(int angle_left, int strength_left,int angle_right, int strength_right)
    {
        was_inside_flag = is_inside();

        touching_line= ballTouchingLine();

        // get current time and delta since last onDraw
        now = System.nanoTime();
        dT = (now - lastT) / 1000000000f; // seconds
        lastT = now;

        // compute ball velocity (depends on the tilt of the device and the gain setting)
        velocity = (Math.max(strength_left,strength_right))/20 * gain;
        max_velocity = 5*gain;
        // compute how far the ball should move (depends on the velocity and the elapsed time since last update)
        dBall = dT * velocity; // make the ball move this amount (pixels)

        // compute the ball's new coordinates (depends on the angle of the device and dBall, as just computed)
        float dx = strength_right!=0? (float)Math.cos(angle_right* DEGREES_TO_RADIANS)* dBall: 0;
        float dy = angle_left!=0? -(float)Math.sin(angle_left* DEGREES_TO_RADIANS) * dBall:0;

        xBall += dx;
        yBall += dy;

        return do_update();
    }

    public boolean updateBallPosition(boolean right, boolean left, boolean up, boolean down)
    {
        was_inside_flag = is_inside();
        touching_line= ballTouchingLine();

        tiltAngle = 1;
        // get current time and delta since last onDraw
        now = System.nanoTime();
        dT = (now - lastT) / 1000000000f; // seconds
        lastT = now;

        // compute ball velocity (depends on the tilt of the device and the gain setting)
        velocity = 5*gain;
        max_velocity=velocity;

        // compute how far the ball should move (depends on the velocity and the elapsed time since last update)
        dBall = dT * velocity; // make the ball move this amount (pixels)

        float dx = 0;
        if((right || left) && right!=left)
            dx = right? dBall: -dBall;

        float dy = 0;
        if((down || up) && down!=up)
            dy = down? dBall: -dBall;

        xBall += dx;
        yBall += dy;

        return do_update();
    }

    public boolean do_update()
    {
        // make an adjustment, if necessary, to keep the ball visible (also, restore if NaN)
        if (Float.isNaN(xBall) || xBall < 0)
            xBall = 0;
        else if (xBall > width - ballDiameter)
            xBall = width - ballDiameter;
        if (Float.isNaN(yBall) || yBall < 0)
            yBall = 0;
        else if (yBall > height - ballDiameter)
            yBall = height - ballDiameter;

        // oh yea, don't forget to update the coordinate of the center of the ball (needed to determine wall  hits)
        xBallCenter = xBall + ballDiameter / 2f;
        yBallCenter = yBall + ballDiameter / 2f;

        x_positions+= xBallCenter +",";
        y_positions+= yBallCenter +",";
        t_positions+=  "0,";

        checkCheating();

//        boolean is_inside_flag = is_inside();

        boolean is_touching_line= ballTouchingLine();

        // if ball touches wall, vibrate and increment wallHits count
        // NOTE: We also use a boolean touchFlag so we only vibrate on the first touch

        if (is_touching_line && !touchFlag )
        {
            touchFlag = true; // the ball has *just* touched the line: set the touchFlag
            //if the ball was inside before moving
            if(was_inside_flag) {
                vib.vibrate(50); // 50 ms vibrotactile pulse
                ++wallHits;
                start_time_outside = System.nanoTime();
            }else{
                missed_time += System.nanoTime()-start_time_outside;
                MediaPlayer mp = MediaPlayer.create(this.getContext(), R.raw.my_tone);
                mp.start();
            }

        } else if (!touching_line && touchFlag)
            touchFlag = false; // the ball is no longer touching the line: clear the touchFlag


        //If touched the goal point FIRST ROUND
        if(did_fall() && goal_point==1 && cheating_flag>=1)
        {
            Log.i(MYDEBUG,"sound");
            MediaPlayer mp = MediaPlayer.create(this.getContext(), R.raw.my_tone);
            mp.start();
            cheating_flag=0;
            goal_point=0;
        }
//        If touched the goal point
        if(did_fall() && goal_point==0 && cheating_flag>=1)
        {
            lap_time = System.nanoTime() - start_time;
//            start_time = System.nanoTime();
            Log.i(MYDEBUG,"Reached END POINT");
            MediaPlayer mp = MediaPlayer.create(this.getContext(), R.raw.my_tone);
            mp.start();
            cheating_flag=0;

//              Statistics
            long temporal = ((lap_time - missed_time)*100)/lap_time;
            accuracy =Math.round(temporal * 100.0) / 100.0;
            time_per_trial = Math.round( ( (lap_time / 1000000000f)/lapNumbers) * 100.0) / 100.0;

            return true;
        }


        invalidate(); // force onDraw to redraw the screen with the ball in its new position
        return false;
    }

    private void drawArrow(Canvas canvas, float x0, float y0, float x1, float y1) {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);

        float deltaX = x1 - x0;
        float deltaY = y1 - y0;
        double distance = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
        float frac = (float) (1 / (distance / 30));

        float point_x_1 = x0 + (float) ((1 - frac) * deltaX + frac * deltaY);
        float point_y_1 = y0 + (float) ((1 - frac) * deltaY - frac * deltaX);

        float point_x_2 = x1;
        float point_y_2 = y1;

        float point_x_3 = x0 + (float) ((1 - frac) * deltaX - frac * deltaY);
        float point_y_3 = y0 + (float) ((1 - frac) * deltaY + frac * deltaX);

        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);

        path.moveTo(point_x_1, point_y_1);
        path.lineTo(point_x_2, point_y_2);
        path.lineTo(point_x_3, point_y_3);
        path.lineTo(point_x_1, point_y_1);
        path.lineTo(point_x_1, point_y_1);
        path.close();

        canvas.drawPath(path, paint);
        paint.setStrokeWidth(7);
        canvas.drawLine(x0,y0,x1,y1, paint);
    }

    public void config_path(){
        switch (pathType){
            case(PATH_TYPE_SQUARE):
                best_path = new Point[6];
                int difference_ =(int)(Math.abs(outerRectangle.left-innerRectangle.left)/2);
                best_path[0]= new Point((int)outerRectangle.left+difference_,(int)(innerRectangle.top + (innerRectangle.bottom - innerRectangle.top) / 2 + ballDiameter * 2f));
                best_path[1]= new Point((int)outerRectangle.left+difference_, (int)outerRectangle.bottom-difference_);
                best_path[2]= new Point((int)outerRectangle.right-difference_, (int)outerRectangle.bottom-difference_);
                best_path[3]= new Point((int)outerRectangle.right-difference_, (int)outerRectangle.top+difference_);
                best_path[4]= new Point((int)outerRectangle.left+difference_, (int)outerRectangle.top+difference_);
                best_path[5]= new Point((int)outerRectangle.left+difference_,  (int)(yCenter - (yCenter - outerRectangle.top)/2 ));

            case(PATH_TYPE_CIRCLE):
            case(PATH_TYPE_MAZE2):
                init_path_maz_2();
                break;
            case(PATH_TYPE_MAZE3):
                init_path_maz_3();
                break;
            case(PATH_TYPE_MAZE):
                init_path_maz_1();
                break;
            default:
                pathType = MODE_NONE;
                break;
        }
    }

    protected void init_path_maz_1(){
        path_1= new Point[6];

        int difference_ = (int)Math.abs(outerRectangle.left-innerRectangle.left);

        path_1[0]= new Point((int)outerRectangle.left, (int)outerRectangle.top);
        path_1[1]= new Point((int)outerRectangle.left, (int)outerRectangle.bottom);
        path_1[2]= new Point((int)(outerRectangle.left+Math.abs(outerRectangle.left-outerRectangle.right)/2), (int)outerRectangle.bottom);
        path_1[3]= new Point((int)(outerRectangle.left+Math.abs(outerRectangle.left-outerRectangle.right)/2), (int)(outerRectangle.top+Math.abs(outerRectangle.bottom-outerRectangle.top)/3));
        path_1[4]= new Point((int)outerRectangle.right-difference_, (int)(outerRectangle.top+Math.abs(outerRectangle.bottom-outerRectangle.top)/3));
        path_1[5]= new Point((int)outerRectangle.right-difference_, (int)outerRectangle.bottom+ difference_);


        path_2= new Point[6];

        path_2[0]= new Point((int)outerRectangle.left + difference_, (int)outerRectangle.top);
        path_2[1]= new Point((int)outerRectangle.left + difference_, (int)outerRectangle.bottom- difference_);
        path_2[2]= new Point((int)(outerRectangle.left+Math.abs(outerRectangle.left-outerRectangle.right)/2) - difference_, (int)outerRectangle.bottom-difference_);
        path_2[3]= new Point((int)(outerRectangle.left+Math.abs(outerRectangle.left-outerRectangle.right)/2) - difference_, (int)(outerRectangle.top+Math.abs(outerRectangle.bottom-outerRectangle.top)/3)- difference_);
        path_2[4]= new Point((int)outerRectangle.right  , (int)(outerRectangle.top+Math.abs(outerRectangle.bottom-outerRectangle.top)/3) - difference_);
        path_2[5]= new Point((int)outerRectangle.right  , (int)outerRectangle.bottom+ difference_ );

        best_path = new Point[6];
        difference_ = difference_/2;
        best_path[0]= new Point(path_1[0].x+difference_, path_1[0].y+ ballDiameter);
        best_path[1]= new Point(path_1[0].x+difference_, path_1[1].y-difference_);
        best_path[2]= new Point(path_1[2].x-difference_, path_1[1].y-difference_);
        best_path[3]= new Point(path_1[2].x-difference_, path_1[3].y-difference_);
        best_path[4]= new Point(path_1[4].x+difference_, path_1[3].y-difference_);
        best_path[5]= new Point(path_1[5].x+difference_, path_1[5].y);

    }

    protected void init_path_maz_2(){
        int difference_ = (int)Math.abs(outerRectangle.left-innerRectangle.left);
        path_1= new Point[6];

        path_1[0]= new Point((int)outerRectangle.left, (int)outerRectangle.top);
        path_1[1]= new Point((int)outerRectangle.right, (int)outerRectangle.top);
        path_1[2]= new Point((int)outerRectangle.right, (int)(outerRectangle.top + Math.abs(outerRectangle.top - outerRectangle.bottom)/2)+ difference_);
        path_1[3]= new Point((int)outerRectangle.left + difference_, (int)(outerRectangle.top + Math.abs(outerRectangle.top - outerRectangle.bottom)/2)+ difference_);
        path_1[4]= new Point((int)outerRectangle.left + difference_, (int)(outerRectangle.top + Math.abs(outerRectangle.top - outerRectangle.bottom)/2)+ 3*difference_);
        path_1[5]= new Point((int)outerRectangle.right, (int)(outerRectangle.top + Math.abs(outerRectangle.top - outerRectangle.bottom)/2)+ 3*difference_);


        path_2= new Point[6];

        path_2[0]= new Point(path_1[0].x , path_1[0].y + difference_);
        path_2[1]= new Point(path_1[1].x - difference_ , path_1[1].y + difference_);
        path_2[2]= new Point(path_1[2].x - difference_, path_1[2].y - difference_);
        path_2[3]= new Point(path_1[3].x - difference_, path_1[3].y - difference_);
        path_2[4]= new Point(path_1[4].x - difference_, path_1[4].y + difference_);
        path_2[5]= new Point(path_1[5].x , path_1[5].y + difference_);

        best_path = new Point[6];
        difference_ = difference_/2;
        best_path[0]= new Point(path_1[0].x+ballDiameter, path_1[0].y+ difference_);
        best_path[1]= new Point(path_1[1].x-difference_, path_1[1].y+difference_);
        best_path[2]= new Point(path_1[2].x-difference_, path_1[2].y-difference_);
        best_path[3]= new Point(path_1[3].x-difference_, path_1[3].y-difference_);
        best_path[4]= new Point(path_1[4].x-difference_, path_1[4].y+difference_);
        best_path[5]= new Point(path_1[5].x-ballDiameter, path_1[5].y+difference_);


    }

    protected void init_path_maz_3(){
        int difference_ = (int)Math.abs(outerRectangle.left-innerRectangle.left);
        int fraction_x = (int)(Math.abs(outerRectangle.left-outerRectangle.right)/4);
        int fraction_y = (int)(Math.abs(outerRectangle.top-outerRectangle.bottom)/4);
        path_1= new Point[9];

        path_1[0]= new Point((int)outerRectangle.left, (int)outerRectangle.top);
        path_1[1]= new Point((int)outerRectangle.left + fraction_x, (int)outerRectangle.top);
        path_1[2]= new Point((int)outerRectangle.left + fraction_x, (int)(outerRectangle.top+ fraction_y));
        path_1[3]= new Point((int)outerRectangle.left + 2*fraction_x, (int)(outerRectangle.top+ fraction_y));
        path_1[4]= new Point((int)outerRectangle.left + 2*fraction_x, (int)(outerRectangle.top+ 2*fraction_y));
        path_1[5]= new Point((int)outerRectangle.left + 3*fraction_x, (int)(outerRectangle.top+ 2*fraction_y));
        path_1[6]= new Point((int)outerRectangle.left + 3*fraction_x, (int)(outerRectangle.top+ 3*fraction_y));
        path_1[7]= new Point((int)outerRectangle.left + 4*fraction_x, (int)(outerRectangle.top+ 3*fraction_y));
        path_1[8]= new Point((int)outerRectangle.left + 4*fraction_x, (int)(outerRectangle.top+ 4*fraction_y + difference_));



        path_2= new Point[9];

        path_2[0]= new Point(path_1[0].x  , path_1[0].y + difference_);
        path_2[1]= new Point(path_1[1].x - difference_ , path_1[1].y + difference_);
        path_2[2]= new Point(path_1[2].x - difference_ , path_1[2].y + difference_);
        path_2[3]= new Point(path_1[3].x - difference_ , path_1[3].y + difference_);
        path_2[4]= new Point(path_1[4].x - difference_ , path_1[4].y + difference_);
        path_2[5]= new Point(path_1[5].x - difference_ , path_1[5].y + difference_);
        path_2[6]= new Point(path_1[6].x - difference_ , path_1[6].y + difference_);
        path_2[7]= new Point(path_1[7].x - difference_ , path_1[7].y + difference_);
        path_2[8]= new Point(path_1[8].x - difference_ , path_1[8].y );


        best_path = new Point[9];
        difference_ = difference_/2;
        best_path[0]= new Point(path_1[0].x+ballDiameter, path_1[0].y+ difference_);
        best_path[1]= new Point(path_1[1].x-difference_, path_1[1].y+difference_);
        best_path[2]= new Point(path_1[2].x-difference_, path_1[2].y+difference_);
        best_path[3]= new Point(path_1[3].x-difference_, path_1[3].y+difference_);
        best_path[4]= new Point(path_1[4].x-difference_, path_1[4].y+difference_);

        best_path[5]= new Point(path_1[5].x-difference_, path_1[5].y+difference_);
        best_path[6]= new Point(path_1[6].x-difference_, path_1[6].y+difference_);
        best_path[7]= new Point(path_1[7].x-difference_, path_1[7].y+difference_);
        best_path[8]= new Point(path_1[8].x-difference_, path_1[8].y-ballDiameter);

    }


    protected void onDraw(Canvas canvas)
    {
        if (pathType == PATH_TYPE_MAZE ||pathType == PATH_TYPE_MAZE2 ||pathType == PATH_TYPE_MAZE3)
        {
            Paint temp = new Paint();
            temp.setStyle(Paint.Style.FILL);
            temp.setColor(Color.GREEN);

            Path path = new Path();
            path.setFillType(Path.FillType.EVEN_ODD);
            path.moveTo(path_1[0].x, path_1[0].y);
            for(int i=0; i<path_1.length;i++){
//                path.moveTo(path_1[i-1].x,path_1[i-1].y);
                path.lineTo(path_1[i].x,path_1[i].y);
            }
//            path.moveTo(path_1[path_1.length-1].x,path_1[path_1.length-1].y);
            path.lineTo(path_2[path_2.length-1].x,path_2[path_2.length-1].y);

            for (int i=path_2.length; i>0;i-- ){
//                path.moveTo(path_2[i].x,path_2[i].y);
                path.lineTo(path_2[i-1].x,path_2[i-1].y);
            }
//            path.moveTo(path_2[0].x,path_2[0].y);
            path.lineTo(path_1[0].x,path_1[0].y);

            path.close();

            canvas.drawPath(path, fillPaint);
            canvas.drawPath(path, linePaint);


            if  (pathType == PATH_TYPE_MAZE){
                if(goal_point==0)
                    drawArrow(canvas,path_1[path_1.length-1].x-100,path_1[path_1.length-1].y,path_1[path_1.length-1].x-100,path_1[path_1.length-1].y-150);
                else
                    drawArrow(canvas,lap_line_x_1-100,lap_line_y_1,lap_line_x_1-100,lap_line_y_1+150);

                obstacles[1].left = path_1[path_1.length-1].x + (outerRectangle.right-innerRectangle.right)/2 - ballDiameter/2f;
                obstacles[1].top = path_1[path_1.length-1].y - ballDiameter ;
                obstacles[1].right = obstacles[1].left + ballDiameter * (3 / 2);
                obstacles[1].bottom =  obstacles[1].top + ballDiameter * (3 / 2);
            }else if(pathType == PATH_TYPE_MAZE2)
            {
                if(goal_point==0)
                    drawArrow(canvas, path_1[path_1.length-1].x,path_1[path_1.length-1].y -100,path_1[path_1.length-1].x-150,path_1[path_1.length-1].y -100);
                else
                    drawArrow(canvas,path_1[0].x,path_1[0].y -100,path_1[0].x +150,path_1[0].y -100);

                obstacles[1].left = path_1[path_1.length-1].x - ballDiameter;
                obstacles[1].top = path_1[path_1.length-1].y + (outerRectangle.right-innerRectangle.right)/2 - ballDiameter/2f;
                obstacles[1].right = obstacles[1].left + ballDiameter * (3 / 2);
                obstacles[1].bottom =  obstacles[1].top + ballDiameter * (3 / 2);
            }else {

                if(goal_point==0){
                    drawArrow(canvas,path_1[path_1.length-2].x ,path_1[path_1.length-2].y -100, path_1[path_1.length-2].x-150,path_1[path_1.length-2].y -100);
//                    drawArrow(canvas,path_1[0].x +150,path_1[0].y -100, path_1[0].x,path_1[0].y -100);
//                    drawArrow(canvas,path_1[2].x+100,path_1[1].y+150,path_1[1].x+100,path_1[1].y);
                    drawArrow(canvas,path_1[path_1.length-1].x+50,path_1[path_1.length-1].y,path_1[path_1.length-1].x+50,path_1[path_1.length-1].y-150);
                }
                else
                {
                    drawArrow(canvas, path_1[0].x,path_1[0].y -100,path_1[0].x +150,path_1[0].y -100);
                    drawArrow(canvas,path_1[1].x+100,path_1[1].y,path_1[2].x+100,path_1[1].y+150);
                }

                obstacles[1].left = path_1[path_1.length-1].x - (outerRectangle.right-innerRectangle.right)/2 - ballDiameter/2f;
                obstacles[1].top = path_1[path_1.length-1].y -  ballDiameter;
                obstacles[1].right = obstacles[1].left + ballDiameter * (3 / 2);
                obstacles[1].bottom =  obstacles[1].top + ballDiameter * (3 / 2);
            }

        }else if (pathType == PATH_TYPE_SQUARE)
        {

            // draw fills
            canvas.drawRect(outerRectangle, fillPaint);
            canvas.drawRect(innerRectangle, backgroundPaint);

            // draw lines
            canvas.drawRect(outerRectangle, linePaint);
            canvas.drawRect(innerRectangle, linePaint);

            linePaint.setStrokeWidth(7);
            linePaint.setColor(Color.BLUE);
            canvas.drawLine(lap_line_x,lap_line_y,lap_line_x_1,lap_line_y_1, linePaint);
            linePaint.setStrokeWidth(3);
            linePaint.setColor(Color.BLUE);
            if(goal_point==0)
                drawArrow(canvas,lap_line_x_1-100,lap_line_y_1,lap_line_x_1-100,lap_line_y_1-150);
            else
                drawArrow(canvas,lap_line_x_1-100,lap_line_y_1,lap_line_x_1-100,lap_line_y_1+150);

            obstacles[1].left = outerRectangle.left + (outerRectangle.right-innerRectangle.right)/2 - ballDiameter/2f;
            obstacles[1].top = yCenter - (yCenter - outerRectangle.top)/2 ;
            obstacles[1].right = obstacles[1].left + ballDiameter * (3 / 2);
            obstacles[1].bottom =  obstacles[1].top + ballDiameter * (3 / 2);

        } else if (pathType == PATH_TYPE_CIRCLE)
        {
            // draw fills
            canvas.drawOval(outerRectangle, fillPaint);
            canvas.drawOval(innerRectangle, backgroundPaint);

            // draw lines
            canvas.drawOval(outerRectangle, linePaint);
            canvas.drawOval(innerRectangle, linePaint);

            linePaint.setStrokeWidth(7);
            linePaint.setColor(Color.BLUE);
            canvas.drawLine(lap_line_x,lap_line_y,lap_line_x_1,lap_line_y_1, linePaint);
            linePaint.setColor(Color.BLUE);
            linePaint.setStrokeWidth(3);

            if(goal_point==0)
                drawArrow(canvas,lap_line_x_1-100,lap_line_y_1,lap_line_x_1-100,lap_line_y_1-150);
            else
                drawArrow(canvas,lap_line_x_1-100,lap_line_y_1,lap_line_x_1-100,lap_line_y_1+150);


            obstacles[1].left = outerRectangle.left + (outerRectangle.right-innerRectangle.right)/2 + ballDiameter/2f;
            obstacles[1].top = yCenter - (yCenter - outerRectangle.top)/2 ;
            obstacles[1].right = obstacles[1].left + ballDiameter * (3 / 2);
            obstacles[1].bottom =  obstacles[1].top + ballDiameter * (3 / 2);

        }

        // draw label
//        canvas.drawText("Demo_TiltBall", 6f, labelTextSize, labelPaint);

//         draw stats (pitch, roll, tilt angle, tilt magnitude)
//        if (pathType == PATH_TYPE_SQUARE || pathType == PATH_TYPE_CIRCLE)
//        {
//        canvas.drawText("Wall hits = " + wallHits, 6f, updateY[5], statsPaint);
//            canvas.drawText("-----------------", 6f, updateY[4], statsPaint);
//        }
//        canvas.drawText(String.format(Locale.CANADA, "Tablet pitch (degrees) = %.2f", pitch), 6f, updateY[3],
//                statsPaint);
//        canvas.drawText(String.format(Locale.CANADA, "Tablet roll (degrees) = %.2f", roll), 6f, updateY[2], statsPaint);
        canvas.drawText("wall hits   "+wallHits , 6f, updateY[1], statsPaint);
        canvas.drawText("was inside?  "+was_inside_flag, 6f, updateY[0], statsPaint);
        canvas.drawText("Best Time  "+get_best_time(), 6f, updateY[2], statsPaint);
        canvas.drawText("V "+velocity, 6f, updateY[3], statsPaint);

        //draw the goal/starting point
        linePaint.setColor(Color.BLACK);
        backgroundPaint.setColor(Color.BLACK);
        canvas.drawOval(obstacles[goal_point], backgroundPaint);
        canvas.drawOval(obstacles[goal_point], linePaint);
        linePaint.setColor(Color.BLUE);
        backgroundPaint.setColor(Color.LTGRAY);

        // draw the ball in its new location
        canvas.drawBitmap(ball, xBall, yBall, null);

    } // end onDraw

    /*
     * Configure the rolling ball panel according to setup parameters
     */
    public void configure(String pathMode, int gainArg, int modeArg)
    {
        // square vs. circle
        switch (pathMode){
            case("Square"):
                pathType = PATH_TYPE_SQUARE;
                break;
            case("Circle"):
                pathType = PATH_TYPE_CIRCLE;
                break;
            case("Maze2"):
                pathType = PATH_TYPE_MAZE2;
                break;
            case("Maze3"):
                pathType = PATH_TYPE_MAZE3;
                break;
            case("Maze"):
                pathType = PATH_TYPE_MAZE;
                break;
            default:
                pathType = MODE_NONE;
                break;
        }

        pathWidth = PATH_WIDTH_MEDIUM;

        gain = gainArg;

        lapNumbers=1;
        mode=modeArg;
//        Log.i(MYDEBUG, Integer.toString(mode)+" BBBBBBBBBB");
    }

    // returns true if the ball is touching (i.e., overlapping) the line of the inner or outer path border
    public boolean ballTouchingLine()
    {
        if (pathType == PATH_TYPE_MAZE || pathType == PATH_TYPE_MAZE2 || pathType == PATH_TYPE_MAZE3)
        {
            ballNow.left = xBall;
            ballNow.top = yBall;
            ballNow.right = xBall + ballDiameter;
            ballNow.bottom = yBall + ballDiameter;

            for( int i=1; i< path_1.length; i++){

                RectF temp= new RectF();
                temp.top = Math.min(path_1[i - 1].y, path_1[i].y);
                temp.bottom = Math.max(path_1[i - 1].y, path_1[i].y);
                temp.right = Math.max(path_1[i-1].x, path_1[i].x);
                temp.left = Math.min(path_1[i-1].x, path_1[i].x);;

                RectF temp_shadow = new RectF();
                temp_shadow.left = temp.left + ballDiameter - 2f;
                temp_shadow.top = temp.top + ballDiameter - 2f;
                temp_shadow.right = temp.right - ballDiameter + 2f;
                temp_shadow.bottom = temp.bottom - ballDiameter + 2f;

                if (RectF.intersects(ballNow,temp) && !RectF.intersects(ballNow, temp_shadow))
//                if (RectF.intersects(ballNow,temp))
                    return true;
            }
            for( int i=1; i< path_2.length; i++){

                RectF temp= new RectF();
                temp.top = Math.min(path_2[i - 1].y, path_2[i].y);
                temp.bottom = Math.max(path_2[i - 1].y, path_2[i].y);
                temp.right = Math.max(path_2[i-1].x, path_2[i].x);
                temp.left = Math.min(path_2[i-1].x, path_2[i].x);

                RectF temp_shadow = new RectF();
                temp_shadow.left = temp.left + ballDiameter - 2f;
                temp_shadow.top = temp.top + ballDiameter - 2f;
                temp_shadow.right = temp.right - ballDiameter + 2f;
                temp_shadow.bottom = temp.bottom - ballDiameter + 2f;

                if (RectF.intersects(ballNow,temp) && !RectF.intersects(ballNow, temp_shadow))
//                if (RectF.intersects(ballNow,temp))
                    return true;
            }
        }else if (pathType == PATH_TYPE_SQUARE)
        {
            ballNow.left = xBall;
            ballNow.top = yBall;
            ballNow.right = xBall + ballDiameter;
            ballNow.bottom = yBall + ballDiameter;

            if (RectF.intersects(ballNow, outerRectangle) && !RectF.intersects(ballNow, outerShadowRectangle)) {
                return true; // touching outside rectangular border
            }
            if (RectF.intersects(ballNow, innerRectangle) && !RectF.intersects(ballNow, innerShadowRectangle)) {
                return true; // touching inside rectangular border
            }
        } else if (pathType == PATH_TYPE_CIRCLE)
        {
            final float ballDistance = (float)Math.sqrt((xBallCenter - xCenter) * (xBallCenter - xCenter)
                    + (yBallCenter - yCenter) * (yBallCenter - yCenter));

            if (Math.abs(ballDistance - radiusOuter) < (ballDiameter / 2f)) {
                return true; // touching outer circular border
            }
            if (Math.abs(ballDistance - radiusInner) < (ballDiameter / 2f)) {
                return true; // touching inner circular border
            }
        }
        return false;
    }

    /*Checking if the ball fall on any obstacle (goal point or starting point)*/
    public boolean did_fall()
    {
        ballNow.left = xBall;
        ballNow.top = yBall;
        ballNow.right = xBall + ballDiameter;
        ballNow.bottom = yBall + ballDiameter;

        if(RectF.intersects(ballNow,obstacles[goal_point]))
            return true;

        return false;
    }


    public void checkCheating()
    {
        ballNow.left = xBall;
        ballNow.top = yBall;
        ballNow.right = xBall + ballDiameter;
        ballNow.bottom = yBall + ballDiameter;

        RectF temp= new RectF();
        temp.bottom = 100000;
        temp.top = 0;
        temp.right = xCenter;
        temp.left = xCenter;

        RectF temp_shadow = new RectF();

        temp_shadow.left = temp.left + ballDiameter - 2f;
        temp_shadow.top = temp.top + ballDiameter - 2f;
        temp_shadow.right = temp.right - ballDiameter + 2f;
        temp_shadow.bottom = temp.bottom - ballDiameter + 2f;


        if (RectF.intersects(ballNow,temp) && !RectF.intersects(ballNow, temp_shadow) && cheating_flag%2==0) {
            cheating_flag ++;
//            Log.i(MYDEBUG,"Bottom "+Integer.toString(cheating_flag));
        }

        temp.bottom = yCenter;
        temp.top = 0;

        temp_shadow.left = temp.left + ballDiameter - 2f;
        temp_shadow.top = temp.top + ballDiameter - 2f;
        temp_shadow.right = temp.right - ballDiameter + 2f;
        temp_shadow.bottom = temp.bottom - ballDiameter + 2f;

        if (RectF.intersects(ballNow,temp) && !RectF.intersects(ballNow, temp_shadow) && cheating_flag%2==1) {
            cheating_flag ++;
//            Log.i(MYDEBUG,"top "+Integer.toString(cheating_flag));
        }

    }

    public boolean is_inside(){
        ballNow.left = xBall;
        ballNow.top = yBall;
        ballNow.right = xBall + ballDiameter;
        ballNow.bottom = yBall + ballDiameter;


        if (pathType == PATH_TYPE_SQUARE)
        {
            if( RectF.intersects(ballNow, innerRectangle))
                return false;
            if( RectF.intersects(ballNow, outerRectangle))
                return true;
            return false;

        } else if (pathType == PATH_TYPE_CIRCLE)
        {
            final float ballDistance = (float)Math.sqrt((xBallCenter - xCenter) * (xBallCenter - xCenter)
                    + (yBallCenter - yCenter) * (yBallCenter - yCenter));

            if (ballDistance > radiusInner && ballDistance<radiusOuter)
                return true;
            return false;
        } else if (pathType == PATH_TYPE_MAZE2)
        { //Check if the it is MAze2 or whichever it is
            RectF temp= new RectF();

            temp.top= path_1[0].y;
            temp.bottom= path_2[0].y;
            temp.left= path_1[0].x;
            temp.right= path_1[1].x;
            if(RectF.intersects(ballNow, temp))
                return true;

            temp.top= path_1[1].y;
            temp.bottom= path_2[2].y;
            temp.left= path_2[1].x;
            temp.right= path_1[1].x;
            if(RectF.intersects(ballNow, temp))
                return true;

            temp.top= path_2[3].y;
            temp.bottom= path_1[2].y;
            temp.left= path_2[3].x;
            temp.right= path_1[2].x;
            if(RectF.intersects(ballNow, temp))
                return true;

            temp.top= path_2[3].y;
            temp.bottom= path_2[4].y;
            temp.left= path_2[3].x;
            temp.right= path_1[3].x;
            if(RectF.intersects(ballNow, temp))
                return true;

            temp.top= path_1[4].y;
            temp.bottom= path_2[4].y;
            temp.left= path_2[4].x;
            temp.right= path_2[5].x;
            if(RectF.intersects(ballNow, temp))
                return true;

            return false;
        } else if (pathType == PATH_TYPE_MAZE3)
        { //Check if the it is LAst one or whichever it is
            RectF temp= new RectF();

            for (int i=0; i<8; i++){
                if(i%2==0){
                    temp.top= path_1[i].y;
                    temp.bottom= path_2[i].y;
                    temp.left= path_2[i].x;
                    temp.right= path_1[i+1].x;
                }else{
                    temp.top= path_1[i].y;
                    temp.bottom= path_2[i+1].y;
                    temp.left= path_2[i].x;
                    temp.right= path_1[i].x;
                }
                if(RectF.intersects(ballNow, temp))
                    return true;
            }
            return false;
        }else if (pathType == PATH_TYPE_MAZE)
        { //Check if the it is Second (vertical one) or whichever it is
            RectF temp= new RectF();

            temp.top= path_2[0].y;
            temp.bottom= path_1[1].y;
            temp.left= path_1[0].x;
            temp.right= path_2[0].x;
            if(RectF.intersects(ballNow, temp))
                return true;

            temp.top= path_2[4].y;
            temp.bottom= path_1[5].y;
            temp.left= path_1[4].x;
            temp.right= path_2[4].x;
            if(RectF.intersects(ballNow, temp))
                return true;

            temp.top= path_2[3].y;
            temp.bottom= path_1[2].y;
            temp.left= path_2[2].x;
            temp.right= path_1[2].x;
            if(RectF.intersects(ballNow, temp))
                return true;

            temp.top= path_2[1].y;
            temp.bottom= path_1[1].y;
            temp.left= path_1[1].x;
            temp.right= path_1[2].x;
            if(RectF.intersects(ballNow, temp))
                return true;

            temp.top= path_2[3].y;
            temp.bottom= path_1[3].y;
            temp.left= path_2[3].x;
            temp.right= path_2[4].x;
            if(RectF.intersects(ballNow, temp))
                return true;


            return false;
        }
        return false;

    }

    public double get_best_time(){
        double distance=1;



        if (pathType==PATH_TYPE_CIRCLE)
            distance = Math.PI*2*(radiusOuter-(radiusOuter-radiusInner)/2);
        else
            for (int i=0; i<best_path.length-1; i++)
                distance += Math.sqrt((best_path[i].x-best_path[i+1].x)*(best_path[i].x-best_path[i+1].x) + (best_path[i].y-best_path[i+1].y)*(best_path[i].y-best_path[i+1].y));
        double temp =    Math.round(((distance/max_velocity))*100.00)/100.00;
        return temp*2;
    }

}
