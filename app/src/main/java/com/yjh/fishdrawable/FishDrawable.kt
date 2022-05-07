package com.yjh.fishdrawable

import android.animation.ValueAnimator
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator
import kotlin.math.sin

/*
* 自定义Drawable画一条游动的小鱼
* */
class FishDrawable : Drawable() {

    private var mPath: Path
    private var mPaint: Paint

    private val OTHER_ALPHA = 110 //身体外的透明度
    private val BODY_ALPHA = 160 //身体的透明度
    private val HEAD_RADIUS = 50f //鱼头半径

    private var middlePoint: PointF //重心

    private val fishMainAngle = 90f //初始鱼头偏移的角度

    private val BODY_LENGTH = 3.2f * HEAD_RADIUS //身体长度
    private val FIND_FINS_LENGTH = 0.9f * HEAD_RADIUS //根据鱼头寻找鱼鳍起点的线长
    private val FINS_LENGTH = 1.3f * HEAD_RADIUS //鱼鳍长度

    //---------------鱼尾-------------
    //尾部大圆的半径(圆心就是身体底部的中点)
    private val BIG_CIRCLE_RADIUS = HEAD_RADIUS * 0.7f;
    //尾部中圆的半径
    private val MIDDLE_CIRCLE_RADIUS = BIG_CIRCLE_RADIUS * 0.6f;
    //尾部小圆的半径
    private val SMALL_CIRCLE_RADIUS = MIDDLE_CIRCLE_RADIUS * 0.4f;
    //--寻找尾部中圆圆心的线长
    private val FIND_MIDDLE_CIRCLE_LENGTH = BIG_CIRCLE_RADIUS + MIDDLE_CIRCLE_RADIUS;
    //--寻找尾部小圆圆心的线长
    private val FIND_SMALL_CIRCLE_LENGTH = MIDDLE_CIRCLE_RADIUS * (0.4f + 2.7f);
    //--寻找大三角形底边中心点的线长
    private val FIND_TRIANGLE_LENGTH = MIDDLE_CIRCLE_RADIUS * 2.7f;

    private var currentValue: Float = 0f


    init {
        //-----------------------------准备画笔和路径--------------------------
        mPath = Path() //路径
        mPaint = Paint() //画笔
        mPaint.isAntiAlias = true //抗锯齿
        mPaint.isDither = true //防抖
        mPaint.style = Paint.Style.FILL //画笔类型填充
        mPaint.setARGB(OTHER_ALPHA, 244, 92, 71) //设置透明度和颜色

        middlePoint = PointF(4.18f * HEAD_RADIUS, 4.18f * HEAD_RADIUS)

        //------------------------------创建属性动画---------------------------
        val valueAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 2000
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                currentValue = animator.animatedValue as Float //currentValue的变化促使draw函数的刷新
                invalidateSelf()
            }
            start()
        }

    }

    //根据（起始点坐标，两点距离，两点连线和x轴夹角）计算终点坐标
    private fun calculatePoint(startPoint: PointF, length: Float, angle: Float): PointF {
        val deltaX = length * Math.cos(Math.toRadians(angle.toDouble()))
        val deltaY = length * Math.sin(Math.toRadians((angle - 180).toDouble()))
        return PointF((startPoint.x + deltaX).toFloat(), (startPoint.y + deltaY).toFloat())
    }

    private fun makeFins(
        canvas: Canvas,
        startPoint: PointF,
        fishAngle: Float,
        isRightFins: Boolean
    ) {
        val controlAngle = 115f

        //二阶贝塞尔曲线的控制点坐标
        val controlPoint = calculatePoint(
            startPoint,
            1.8f * FINS_LENGTH,
            if (isRightFins) fishAngle - controlAngle else fishAngle + controlAngle
        )
        //结束点坐标
        val endPoint = calculatePoint(startPoint, FINS_LENGTH, fishAngle - 180f)

        mPath.reset()
        mPath.moveTo(startPoint.x, startPoint.y)
        mPath.quadTo(controlPoint.x, controlPoint.y, endPoint.x, endPoint.y)
        canvas.drawPath(mPath, mPaint)
    }

    //-----------------------绘制，类似自定义View中的onDraw方法-----------------------------------
    override fun draw(canvas: Canvas) {
        val fishAngle: Float = (fishMainAngle + sin(Math.toRadians(currentValue.toDouble())) * 10).toFloat()
        //绘制鱼头
        val headPoint = calculatePoint(middlePoint, BODY_LENGTH / 2, fishAngle)
        canvas.drawCircle(headPoint.x, headPoint.y, HEAD_RADIUS, mPaint)
        //绘制右鳍
        val rightFinsPoint = calculatePoint(headPoint, FIND_FINS_LENGTH, fishAngle - 110)
        makeFins(canvas, rightFinsPoint, fishAngle, true)
        //绘制左鳍
        val leftFinsPoint = calculatePoint(headPoint, FIND_FINS_LENGTH, fishAngle + 110)
        makeFins(canvas, leftFinsPoint, fishAngle, false)
        //身体底部的中心点
        val bodyBottomCenterPoint = calculatePoint(headPoint, BODY_LENGTH, fishAngle - 180)
        //画节肢1
        val middleCircleCenterPoint = makeSegment(
            canvas, bodyBottomCenterPoint, BIG_CIRCLE_RADIUS, MIDDLE_CIRCLE_RADIUS,
            FIND_MIDDLE_CIRCLE_LENGTH, fishAngle, true
        )
        //画节肢2
        makeSegment(
            canvas,
            middleCircleCenterPoint,
            MIDDLE_CIRCLE_RADIUS,
            SMALL_CIRCLE_RADIUS,
            FIND_SMALL_CIRCLE_LENGTH,
            fishAngle,
            false
        )
        //画尾巴-- findEdgeLength是一个变化值
        makeTriangle(
            canvas,
            middleCircleCenterPoint,
            FIND_TRIANGLE_LENGTH,
            BIG_CIRCLE_RADIUS,
            fishAngle
        );
        makeTriangle(
            canvas,
            middleCircleCenterPoint,
            FIND_TRIANGLE_LENGTH - 10,
            BIG_CIRCLE_RADIUS - 20,
            fishAngle
        );
        //画身体
        makeBody(canvas, headPoint, bodyBottomCenterPoint, fishAngle);

    }

    //设置透明度
    override fun setAlpha(alpha: Int) {
        mPaint.alpha = alpha
    }

    //设置颜色过滤器，被绘制内容的每一个像素都会被颜色过滤器改变
    override fun setColorFilter(colorFilter: ColorFilter?) {
        mPaint.setColorFilter(colorFilter)
    }

    //完全不透明，透明，半透明
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun getIntrinsicWidth(): Int {
        return (8.38f * HEAD_RADIUS).toInt()
    }

    override fun getIntrinsicHeight(): Int {
        return (8.38f * HEAD_RADIUS).toInt()
    }

    private fun makeSegment(
        canvas: Canvas, bottomCenterPoint: PointF, bigRadius: Float,
        smallRadius: Float, findSmallCircleLength: Float, fishAngle: Float, hasBigCircle: Boolean
    ): PointF {
        val segmentAngle: Float = if (hasBigCircle){
            (fishAngle + sin(Math.toRadians((currentValue * 2).toDouble())) * 30).toFloat()
        }else{
            (fishAngle + sin(Math.toRadians((currentValue * 2).toDouble())) * 50).toFloat()
        }
        //梯形上底的中心点(中等大的圆的圆心)
        val upperCenterPoint =
            calculatePoint(bottomCenterPoint, findSmallCircleLength, segmentAngle - 180);
        //梯形的四个点
        val bottomLeftPoint = calculatePoint(bottomCenterPoint, bigRadius, segmentAngle + 90)
        val bottomRightPoint = calculatePoint(bottomCenterPoint, bigRadius, segmentAngle - 90)
        val upperLeftPoint = calculatePoint(upperCenterPoint, smallRadius, segmentAngle + 90)
        val upperRightPoint = calculatePoint(upperCenterPoint, smallRadius, segmentAngle - 90)
        if (hasBigCircle) {
            //画大圆
            canvas.drawCircle(bottomCenterPoint.x, bottomCenterPoint.y, bigRadius, mPaint);
        }
        //画小圆
        canvas.drawCircle(upperCenterPoint.x, upperCenterPoint.y, smallRadius, mPaint)
        //画梯形
        mPath.reset();
        mPath.moveTo(bottomLeftPoint.x, bottomLeftPoint.y)
        mPath.lineTo(upperLeftPoint.x, upperLeftPoint.y)
        mPath.lineTo(upperRightPoint.x, upperRightPoint.y)
        mPath.lineTo(bottomRightPoint.x, bottomRightPoint.y)
        canvas.drawPath(mPath, mPaint)
        //绘制节肢2和三角形的起始点,必须在这儿返回
        return upperCenterPoint
    }

    private fun makeTriangle(
        canvas: Canvas, startPoint: PointF,
        findCenterLength: Float, findEdgeLength: Float, fishAngle: Float
    ) {
        val triangleAngle = (fishAngle + sin(Math.toRadians((currentValue * 2).toDouble())) * 50).toFloat()
        //三角形底边的中心点
        val centerPoint = calculatePoint(startPoint, findCenterLength, triangleAngle - 180)
        //三角形底边的两点
        val leftPoint = calculatePoint(centerPoint, findEdgeLength, triangleAngle + 90)
        val rightPoint = calculatePoint(centerPoint, findEdgeLength, triangleAngle - 90);
        //绘制三角形
        mPath.reset();
        mPath.moveTo(startPoint.x, startPoint.y)
        mPath.lineTo(leftPoint.x, leftPoint.y)
        mPath.lineTo(rightPoint.x, rightPoint.y)
        canvas.drawPath(mPath, mPaint);
    }

    private fun makeBody(
        canvas: Canvas, headPoint: PointF, bodyBottomCenterPoint: PointF, fishAngle: Float)
    {
        //身体的四个点
        val topLeftPoint = calculatePoint (headPoint, HEAD_RADIUS, fishAngle+90)
        val topRightPoint = calculatePoint (headPoint, HEAD_RADIUS, fishAngle-90)
        val bottomLeftPoint = calculatePoint(bodyBottomCenterPoint, BIG_CIRCLE_RADIUS, fishAngle+90)
        val bottomRightPoint = calculatePoint (bodyBottomCenterPoint, BIG_CIRCLE_RADIUS, fishAngle-90)
        //二阶贝塞尔曲线的控制点，决定鱼的胖瘦
        val controlLeft = calculatePoint (headPoint, BODY_LENGTH * 0.56f, fishAngle+130);
        val controlRight = calculatePoint (headPoint, BODY_LENGTH * 0.56f, fishAngle-130);
        //画身体
        mPath.reset();
        mPath.moveTo(topLeftPoint.x, topLeftPoint.y);
        mPath.quadTo(controlLeft.x, controlLeft.y, bottomLeftPoint . x, bottomLeftPoint.y)
        mPath.lineTo(bottomRightPoint.x, bottomRightPoint.y)
        mPath.quadTo(controlRight.x, controlRight.y, topRightPoint.x, topRightPoint.y)
        mPaint.alpha = BODY_ALPHA;
        canvas.drawPath(mPath, mPaint);

    }
}