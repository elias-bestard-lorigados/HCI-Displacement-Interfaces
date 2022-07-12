package ca.yorku.eecs.mack.demotiltball78040;

import android.graphics.Rect;
import android.util.Log;
import android.view.View;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;

import android.graphics.Path;

public class PathAnalysis extends View {


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

    int ballDiameter;


    float width, height, pixelDensity;
    int labelTextSize, statsTextSize, gap, offset;

    RectF innerRectangle, outerRectangle, innerShadowRectangle, outerShadowRectangle;


    // obstacles[0] starting point, obstacles[1] goal point
    RectF[] obstacles;

    final static String MYDEBUG = "MYDEBUG";


    // parameters from Setup dialog
    float  pathWidth;
    int pathType;


    float xCenter, yCenter; // the center of the screen
    Paint statsPaint, labelPaint, linePaint, fillPaint, backgroundPaint;
    float[] updateY;

    Point[] path_1;
    Point[] path_2;

    Point[] best_path;

    String[] x_positions;
    String[] y_positions;
    int trial_no;

//    Stats
    int wallHits;
    double accuracy, lapTime,missed_path_time,best_time;


    public PathAnalysis(Context contextArg) {
        super(contextArg);
        initialize(contextArg);
    }

    public PathAnalysis(Context contextArg, AttributeSet attrs)
    {
        super(contextArg, attrs);
        initialize(contextArg);
    }

    public PathAnalysis(Context contextArg, AttributeSet attrs, int defStyle)
    {
        super(contextArg, attrs, defStyle);
        initialize(contextArg);
    }

    // things that can be initialized from within this View
    private void initialize(Context c)
    {

        linePaint = new Paint();
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

        this.setBackgroundColor(Color.LTGRAY);
        outerRectangle = new RectF();
        innerRectangle = new RectF();
        innerShadowRectangle = new RectF();
        outerShadowRectangle = new RectF();

        obstacles= new RectF[2];
        obstacles[0]= new RectF();
        obstacles[1]= new RectF();

        trial_no=0;

    }


    public void configure(String pathMode, int path_no, String[] x_pos, String[] y_pos,
                          float width, float height,int wall_hint,
                          double acc, double lap_time, double missed_lap_time ,
                          double best_time) {
        // square vs. circle
        trial_no=path_no;
        x_positions=x_pos;
        y_positions=y_pos;
        this.width=width;
        this.height=height;
        this.best_time=best_time;
        missed_path_time= missed_lap_time;


        wallHits=wall_hint;
        accuracy=acc;
        lapTime=lap_time;

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


        float xBall, yBall;

        if (pathType == PATH_TYPE_MAZE2 || pathType == PATH_TYPE_MAZE3 ) {
            xBall = outerRectangle.left +  ballDiameter;
            yBall = outerRectangle.top + (innerRectangle.top - outerRectangle.top)/ 2 - ballDiameter / 2f;
        }
        else if (pathType == PATH_TYPE_MAZE ) {
            xBall = outerRectangle.left +  (innerRectangle.left - outerRectangle.left)/ 2 - ballDiameter/ 2f;
            yBall = outerRectangle.top + ballDiameter ;
        }
        else {
            xBall = outerRectangle.left + (innerRectangle.left - outerRectangle.left) / 2 - ballDiameter / 2f;
            yBall = innerRectangle.top + (innerRectangle.bottom - innerRectangle.top) / 2 + ballDiameter * 2f;
        }


        obstacles[0].left = xBall;
        obstacles[0].top =yBall;
        obstacles[0].right = obstacles[0].left + ballDiameter * (3 / 2);
        obstacles[0].bottom =  obstacles[0].top + ballDiameter * (3 / 2);

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
                break;
            case(PATH_TYPE_CIRCLE):
            case(PATH_TYPE_MAZE2):
                init_path1();
                break;
            case(PATH_TYPE_MAZE3):
                init_path2();
                break;
            case(PATH_TYPE_MAZE):
                init_path();
                break;
            default:
                pathType = MODE_NONE;
                break;
        }
    }

    protected void init_path(){
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

    protected void init_path1(){
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

    protected void init_path2(){
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

    /**
     * Called when the window hosting this view gains or looses focus.  Here we initialize things that depend on the
     * view's width and height.
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        if (!hasFocus)
            return;

//        width = this.getWidth();
//        height = this.getHeight();

        // the ball diameter is nominally 1/30th the smaller of the view's width or height
        ballDiameter = width < height ? (int)(width / 30)
                : (int)(height / 30);
        // center of the view
        xCenter = width / 2f;
        yCenter = height / 2f;

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


        float xBall = outerRectangle.left + (innerRectangle.left - outerRectangle.left) / 2 - ballDiameter / 2f;
        float yBall = innerRectangle.top + (innerRectangle.bottom - innerRectangle.top) / 2 + ballDiameter * 2f;


        if (pathType == PATH_TYPE_MAZE2 || pathType == PATH_TYPE_MAZE3 ) {
            xBall = outerRectangle.left +  ballDiameter;
            yBall = outerRectangle.top + (innerRectangle.top - outerRectangle.top)/ 2 - ballDiameter / 2f;
        }
        else if (pathType == PATH_TYPE_MAZE ) {
            xBall = outerRectangle.left +  (innerRectangle.left - outerRectangle.left)/ 2 - ballDiameter/ 2f;
            yBall = outerRectangle.top + ballDiameter ;
        }
        else {
            xBall = outerRectangle.left + (innerRectangle.left - outerRectangle.left) / 2 - ballDiameter / 2f;
            yBall = innerRectangle.top + (innerRectangle.bottom - innerRectangle.top) / 2 + ballDiameter * 2f;
        }


        obstacles[0].left = xBall;
        obstacles[0].top =yBall;
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
            updateY[i] = (labelTextSize+gap) +offset + i * (statsTextSize + gap);
//            updateY[i] = height - offset - i * (statsTextSize + gap);

        config_path();
    }

    protected void onDraw(Canvas canvas)
    {
        if (pathType == PATH_TYPE_MAZE ||pathType == PATH_TYPE_MAZE2 ||pathType == PATH_TYPE_MAZE3)
        {
            Paint temp = new Paint();
            temp.setStyle(Paint.Style.FILL);
            temp.setColor(Color.BLUE);

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
                obstacles[1].left = path_1[path_1.length-1].x + (outerRectangle.right-innerRectangle.right)/2 - ballDiameter/2f;
                obstacles[1].top = path_1[path_1.length-1].y - ballDiameter ;
                obstacles[1].right = obstacles[1].left + ballDiameter * (3 / 2);
                obstacles[1].bottom =  obstacles[1].top + ballDiameter * (3 / 2);
            }else if(pathType == PATH_TYPE_MAZE2)
            {
                obstacles[1].left = path_1[path_1.length-1].x - ballDiameter;
                obstacles[1].top = path_1[path_1.length-1].y + (outerRectangle.right-innerRectangle.right)/2 - ballDiameter/2f;
                obstacles[1].right = obstacles[1].left + ballDiameter * (3 / 2);
                obstacles[1].bottom =  obstacles[1].top + ballDiameter * (3 / 2);
            }else {
                obstacles[1].left = path_1[path_1.length-1].x - (outerRectangle.right-innerRectangle.right)/2 - ballDiameter/2f;
                obstacles[1].top = path_1[path_1.length-1].y -  ballDiameter;
                obstacles[1].right = obstacles[1].left + ballDiameter * (3 / 2);
                obstacles[1].bottom =  obstacles[1].top + ballDiameter * (3 / 2);
            }
            Draw_best_path(canvas);
        }else if (pathType == PATH_TYPE_SQUARE)
        {

            // draw fills
            canvas.drawRect(outerRectangle, fillPaint);
            canvas.drawRect(innerRectangle, backgroundPaint);

            // draw lines
            canvas.drawRect(outerRectangle, linePaint);
            canvas.drawRect(innerRectangle, linePaint);

            obstacles[1].left = outerRectangle.left + (outerRectangle.right-innerRectangle.right)/2 - ballDiameter/2f;
            obstacles[1].top = yCenter - (yCenter - outerRectangle.top)/2 ;
            obstacles[1].right = obstacles[1].left + ballDiameter * (3 / 2);
            obstacles[1].bottom =  obstacles[1].top + ballDiameter * (3 / 2);

            Draw_best_path(canvas);

        } else if (pathType == PATH_TYPE_CIRCLE)
        {
            // draw fills
            canvas.drawOval(outerRectangle, fillPaint);
            canvas.drawOval(innerRectangle, backgroundPaint);

            // draw lines
            canvas.drawOval(outerRectangle, linePaint);
            canvas.drawOval(innerRectangle, linePaint);

            RectF best_path_rec = new RectF();

            Paint temp = new Paint();
            temp.setStyle(Paint.Style.STROKE);
            temp.setStrokeWidth(7);
            temp.setColor(Color.RED);

            int diff_=(int)(Math.abs(outerRectangle.left-innerRectangle.left)/2);
            best_path_rec.top= outerRectangle.top+diff_;
            best_path_rec.bottom= outerRectangle.bottom-diff_;
            best_path_rec.left= outerRectangle.left+diff_;
            best_path_rec.right= outerRectangle.right-diff_;

            canvas.drawOval(best_path_rec, temp);

            obstacles[1].left = outerRectangle.left + (outerRectangle.right-innerRectangle.right)/2 + ballDiameter/2f;
            obstacles[1].top = yCenter - (yCenter - outerRectangle.top)/2 ;
            obstacles[1].right = obstacles[1].left + ballDiameter * (3 / 2);
            obstacles[1].bottom =  obstacles[1].top + ballDiameter * (3 / 2);

        }

// draw label
        canvas.drawText("Statistics", 6f, labelTextSize, labelPaint);

        canvas.drawText("Lap Time = " + Double.toString(lapTime), 6f, updateY[0], statsPaint);
        canvas.drawText("Best Time = " + best_time, 6f, updateY[1], statsPaint);
        canvas.drawText("Missed Lap Time(%) = " + Double.toString(missed_path_time), 6f, updateY[2], statsPaint);
        canvas.drawText("Accuracy(%) = " + Double.toString(accuracy), 6f, updateY[3], statsPaint);
        canvas.drawText("Wall hits = " + wallHits, 6f, updateY[4], statsPaint);

//        canvas.drawText("Lap Time = " + Double.toString(lapTime), 6f, updateY[3], statsPaint);
//        canvas.drawText("Accuracy(%) = " + Double.toString(accuracy), 6f, updateY[4], statsPaint);
//        canvas.drawText("Wall hits = " + wallHits, 6f, updateY[5], statsPaint);



        //draw the goal/starting point
        linePaint.setColor(Color.BLACK);
        backgroundPaint.setColor(Color.BLACK);
        canvas.drawOval(obstacles[0], backgroundPaint);
        canvas.drawOval(obstacles[0], linePaint);
        canvas.drawOval(obstacles[1], backgroundPaint);
        canvas.drawOval(obstacles[1], linePaint);
        linePaint.setColor(Color.BLUE);
        backgroundPaint.setColor(Color.LTGRAY);

        Draw_path(canvas);

    } // end onDraw

    private void Draw_best_path(Canvas canvas){
        Paint temp = new Paint();
        temp.setStyle(Paint.Style.STROKE);
        temp.setStrokeWidth(7);
        temp.setColor(Color.RED);

        Path path = new Path();
        for(int i=0; i<best_path.length;i++){
            if(i==0)
                path.moveTo(best_path[0].x, best_path[0].y);
            else
                path.lineTo(best_path[i].x,best_path[i].y);
        }

        canvas.drawPath(path, temp);
    }
    public void Draw_path(Canvas canvas){

        Path path = new Path();
//        path.setFillType(Path.FillType.EVEN_ODD);

//        String[] path_x = x_positions[trial_no].split(",");
//        String[] path_y = y_positions[trial_no].split(",");


        path.moveTo(Float.parseFloat(x_positions[1]), Float.parseFloat(y_positions[1]));

        for(int i=1; i<x_positions.length;i++){
            path.lineTo(Float.parseFloat(x_positions[i]), Float.parseFloat(y_positions[i]));
        }
//        path.close();
        linePaint.setColor(Color.GREEN);
        linePaint.setStrokeWidth(7);
        canvas.drawPath(path, linePaint);
        linePaint.setColor(Color.BLUE);
        linePaint.setStrokeWidth(2);

    }

}
