package com.yunsimon.smartview.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;


import com.yunsimon.smartview.R;

/**
 * Description: 贴纸TagView
 * Author: yunsimon
 * Date: 2015年7月2日 上午11:15:24
 */
public class TagView extends ViewGroup {

	private Drawable delDrawable;
	private Drawable symmetryDrawable;
	private Drawable controlDrawable;
	
	public int drawableWidth;
	
	/** 编辑边框Paint */
	private Paint outlinePaint;
	public int outlinePadding;
	
	private int tagWidth, tagHeight;
	
	//tagView的matrix与camera
	public Matrix matrix = new Matrix();
	public Matrix inMatrix = null;
	public Camera camera = new Camera();
	
	/** 是否左右对称转换tag图片 */
	public boolean isSymmetry = false;
	
	public final static float NONE = -999999f;
	//旋转角度与缩放大小
	public double lastRotateZDegree = NONE;
	public double rotateZDegree = NONE;
	public double allRotateZDegree = 0;
	//画布缩放
	public float scaleValue = 1.0f;
	//缩放计算的参考值
	private float scaleTagWidth = NONE;
	private float lastScaleTagWidth = NONE;
	private float comparedTagWidth = NONE;
	private float comparedScaleValue = NONE;
	//旋转与缩放中心点参照坐标
	private int conCenterX = -1;
	private int conCenterY = -1;
	private float splitX, splitY;
	
	//平移距离
	public float allTranslateX = 0;
	public float allTranslateY = 0;
	private float translateX = NONE;
	private float lastTranslateX = NONE;
	private float translateY = NONE;
	private float lastTranslateY = NONE;
	
	//操作按钮响应区域
	private Rect selfRect = new Rect();
	private RectF frameRect = new RectF();
	private Rect contolRect = new Rect();
	private Rect delRect = new Rect();
	private Rect symmetryRect = new Rect();
	public boolean hasInitRect = false;
	
	public boolean onNormalState = false;
	
	private PaintFlagsDrawFilter antiAliasFilter;
	
	private boolean isOnMultiControl = false;
	
	private boolean isFirstDraw = true;
	
	public String tagLocalPath = null;
	
	/** 是否显示点击态 */
	public boolean isShowClickState = false;

	/** 点击地缩小的系数 */
	private float onTouchScale = 0.9f;
	
	public int sourceDrawable;

	public static TagView createTagView(Context context, int drawableId){
		TagView tagView = new TagView(context, drawableId);
		ImageView imageView = new ImageView(context);
		Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), drawableId);
		imageView.setImageBitmap(bitmap);
		tagView.addTagContent(imageView);
		return tagView;
	}

	public static class LayoutParams extends FrameLayout.LayoutParams {
		public int x, y;

		public LayoutParams(int width, int height) {
			super(width, height);
		}
	}

	public TagView(Context context, int drawableId) {
		super(context);
		init(drawableId);
	}

	private void init(int drawableId){
		this.sourceDrawable = drawableId;

		Resources res = getContext().getResources();
        delDrawable = res.getDrawable(R.drawable.launcher_tagview_edit_del);
        symmetryDrawable = res.getDrawable(R.drawable.launcher_tagview_edit_symmetry);
        controlDrawable = res.getDrawable(R.drawable.launcher_tagview_edit_control);
        drawableWidth = dp2px(getContext(), 26);
        
        outlinePaint = new Paint();
        outlinePaint.setColor(0xFFdcdcdc);
		outlinePaint.setStrokeWidth(2);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setAntiAlias(true);
        outlinePadding = drawableWidth / 2;
        
        antiAliasFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	}
	
	public void setTagWH(int w, int h){
		tagWidth = w;
		tagHeight = h;
	}
	
	/**
	 * Description: 添加png,jpg的贴纸
	 * Author: yunsimon
	 * Date: 2015年7月21日 下午1:55:30
	 * @param mImageView
	 */
	public void addTagContent(ImageView mImageView){
		addView(mImageView);
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			View child = getChildAt(i);
			int leftPadding = outlinePadding;
			int topPadding = outlinePadding;
			int left = (r - l - tagWidth) / 2 + leftPadding;
			int top = (b - t - tagHeight) / 2 + topPadding;
			child.layout(left, top, left + tagWidth - leftPadding*2, top + tagHeight - topPadding*2);
		}
	}
	
	/**
	 * Description: 处理编辑态下的事件
	 * Author: yunsimon
	 * Date: 2015年9月17日 下午4:01:15
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(onNormalState)
			return false;
		return super.onTouchEvent(event);
	}
	

	@Override
	protected void dispatchDraw(Canvas canvas) {
		canvas.save();
		if(onNormalState){
			canvas.translate(allTranslateX, allTranslateY);
			canvas.scale(scaleValue, scaleValue, getWidth()/2, getHeight()/2);
			if(isFirstDraw){
				camera.rotateZ((float) allRotateZDegree);
			}
			camera.getMatrix(matrix);
			matrix.preTranslate(-getWidth() / 2, -getHeight() / 2);
			matrix.postTranslate(getWidth() / 2, getHeight() / 2);
			canvas.concat(matrix);
			// 记录矩阵
			matrix = canvas.getMatrix();
			if(inMatrix == null){	
				inMatrix = new Matrix();
				matrix.invert(inMatrix);
			}
			
			if(hasInitRect){
				selfRect.left = delRect.left + outlinePadding;
				selfRect.top = delRect.top + outlinePadding;
				selfRect.right = contolRect.right - outlinePadding;
				selfRect.bottom = contolRect.bottom - outlinePadding;
			}else{
				selfRect.left = (getWidth() - tagWidth) / 2 + outlinePadding;
				selfRect.top = (getHeight() - tagHeight) / 2 + outlinePadding;
				selfRect.right = selfRect.left + tagWidth - 2*outlinePadding;
				selfRect.bottom = selfRect.top + tagHeight - 2*outlinePadding;
			}
			
			drawTag(canvas);
		}else {
			// 进行旋转缩放平移操作
			if (lastTranslateX != NONE) {// 平移
				allTranslateX += translateX - lastTranslateX;
				allTranslateY += (translateY - lastTranslateY);
			}
			canvas.translate(allTranslateX, allTranslateY);
			if (lastScaleTagWidth != NONE && comparedTagWidth > 0) {// 缩放
				scaleValue += (scaleTagWidth - lastScaleTagWidth)/comparedTagWidth*comparedScaleValue;
				scaleValue = Math.max(scaleValue, 0.05f);
			}
			canvas.scale(scaleValue, scaleValue, getWidth() / 2, getHeight() / 2);
			if(isFirstDraw){
				camera.rotateZ((float) allRotateZDegree);
			}
			if(lastRotateZDegree != NONE){// 旋转
				allRotateZDegree += rotateZDegree - lastRotateZDegree;
				camera.rotateZ((float) (rotateZDegree - lastRotateZDegree));
			}
			camera.getMatrix(matrix);
			matrix.preTranslate(-getWidth() / 2, -getHeight() / 2);
			matrix.postTranslate(getWidth() / 2, getHeight() / 2);
			canvas.concat(matrix);
			lastRotateZDegree = rotateZDegree;
			lastTranslateX = translateX;
			lastTranslateY = translateY;
			lastScaleTagWidth = scaleTagWidth;

			// 记录矩阵
			matrix = canvas.getMatrix();
			if(inMatrix == null){	
				inMatrix = new Matrix();
			}
			matrix.invert(inMatrix);

			// 画编辑边框
			int w = (int) (drawableWidth / scaleValue);
			int padding = (int) outlinePadding;
			int left = (getWidth() - tagWidth) / 2;
			int top = (getHeight() - tagHeight) / 2;

			canvas.save();
			Path path = new Path();
			frameRect.left = padding + left;
			frameRect.top = padding + top;
			frameRect.right = left + tagWidth - padding;
			frameRect.bottom = top + tagHeight - padding;
			path.addRect(frameRect, Path.Direction.CW);
			try {				
				canvas.clipPath(path, Region.Op.DIFFERENCE);
			} catch (Exception e) {
				e.printStackTrace();
			}
			canvas.restore();
			canvas.drawPath(path, outlinePaint);

			delRect.left = (int) (frameRect.left - w/2);
			delRect.top = (int) (frameRect.top - w/2);
			delRect.right = delRect.left + w;
			delRect.bottom = delRect.top + w;
			symmetryRect.left = (int) (frameRect.right - w/2);
			symmetryRect.top = (int) (frameRect.top - w/2);
			symmetryRect.right = symmetryRect.left + w;
			symmetryRect.bottom = symmetryRect.top + w;
			contolRect.left = (int) (frameRect.right - w/2);
			contolRect.top = (int) (frameRect.bottom - w/2);
			contolRect.right = contolRect.left + w;
			contolRect.bottom = contolRect.top + w;
			selfRect.left = delRect.left + outlinePadding;
			selfRect.top = delRect.top + outlinePadding;
			selfRect.right = contolRect.right - outlinePadding;
			selfRect.bottom = contolRect.bottom - outlinePadding;
			hasInitRect = true;
			
			drawTag(canvas);
			
			delDrawable.setBounds(delRect);
			delDrawable.draw(canvas);
			
			symmetryDrawable.setBounds(symmetryRect);
			symmetryDrawable.draw(canvas);
			
			controlDrawable.setBounds(contolRect);
			controlDrawable.draw(canvas);
		} 
		
		canvas.restore();
		isFirstDraw = false;
	}
	
	private void drawTag(Canvas canvas) {
		if(onNormalState && isShowClickState){//绘制点击态
			canvas.scale(onTouchScale, onTouchScale, getWidth()/2, getHeight()/2);
			postInvalidateDelayed(150);
			isShowClickState = false;
		}
		
		if (isSymmetry) {//处理左右翻转
			canvas.save();
			canvas.setDrawFilter(antiAliasFilter);
			Matrix matrix = new Matrix();
			float[] values = { -1f, 0.0f, 0.0f, 0.0f, 1f, 0.0f, 0.0f, 0.0f, 1.0f };
			matrix.setValues(values);
			matrix.preTranslate(-getWidth() / 2, -getHeight() / 2);
			matrix.postTranslate(getWidth() / 2, getHeight() / 2);
			canvas.concat(matrix);
			super.dispatchDraw(canvas);
			canvas.restore();
		} else {
			canvas.setDrawFilter(antiAliasFilter);
			super.dispatchDraw(canvas);
		}
	}
	
	/**
	 * Description: 是否点击控制control按钮
	 * Author: yunsimon
	 * Date: 2015年7月2日 上午11:07:10
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean isOnTouchControlView(float x, float y){
		int padding = dp2px(getContext(), 3);
		Rect r = new Rect(contolRect.left - padding, contolRect.top - padding, contolRect.right + padding, contolRect.bottom + padding);
		return isOnTouchView(x, y, r);
	}
	
	/**
	 * Description:是否点击删除del按钮
	 * Author: yunsimon
	 * Date: 2015年7月2日 上午11:28:20
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean isOnTouchDelView(float x, float y){
		int padding = dp2px(getContext(), 3);
		Rect r = new Rect(delRect.left - padding, delRect.top - padding, delRect.right + padding, delRect.bottom + padding);
		return isOnTouchView(x, y, r);
	}
	
	/**
	 * Description: 是否点击删除Symmetry按钮
	 * Author: yunsimon
	 * Date: 2015年7月3日 上午10:21:37
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean isOnTouchSymmetryView(float x, float y){
		int padding = dp2px(getContext(), 3);
		Rect r = new Rect(symmetryRect.left - padding, symmetryRect.top - padding, symmetryRect.right + padding, symmetryRect.bottom + padding);
		return isOnTouchView(x, y, r);
	}
	
	/**
	 * Description:是否点击编辑区
	 * Author: yunsimon
	 * Date: 2015年7月2日 下午2:43:12
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean isOnTouchSelfView(float x, float y){
		return isOnTouchView(x, y, selfRect);
	}
	
	private boolean isOnTouchView(float x, float y, Rect rect){
		if(inMatrix == null)
			return false;
		RectF tmpContolRectF = new RectF(rect);
		float[] dest = new float[2];
		inMatrix.mapPoints(dest, new float[]{x, y});
		return tmpContolRectF.contains(dest[0], dest[1]);
	}
	
	/**
	 * Description: 旋转与缩放tagView
	 * Author: yunsimon
	 * Date: 2015年7月2日 上午10:55:48
	 * @param x
	 * @param y
	 */
	public void rotateAndScale(float x, float y) {
		//计算旋转与缩放中心点参照坐标
		if (conCenterX < 0 && conCenterY < 0) {
			int[] xy = new int[2];
			getLocationOnScreen(xy);
			TagViewContainer.LayoutParams lp = (TagViewContainer.LayoutParams) getLayoutParams();
			conCenterX = lp.x + lp.width / 2;
			conCenterY = lp.y + lp.height / 2;

		}
		
		// 计算旋转与缩放值
		int degreeSplit = 0;
		float deltaX = x + splitX  - (allTranslateX + conCenterX);
		float deltaY = y + splitY - (allTranslateY + conCenterY);
		if (deltaY < 0) {
			degreeSplit = -180;//角度偏移
		}
		rotateZDegree = Math.toDegrees(Math.atan(deltaX / deltaY)) + degreeSplit;
		if (lastRotateZDegree == NONE) {
			lastRotateZDegree = rotateZDegree;
		}
		scaleTagWidth = (float) ((float)Math.sqrt(deltaX * deltaX + deltaY * deltaY) * 2 * Math.sin(Math.atan(tagWidth * 1.0f /tagHeight)));
		if(lastScaleTagWidth == NONE){
			lastScaleTagWidth = scaleTagWidth;
			comparedTagWidth = tagWidth;
			comparedScaleValue = 1.0f;
		}
		invalidate();
	}
	
	/**
	 * Description:旋转与缩放tagView,用于双手操作
	 * Author: yunsimon
	 * Date: 2015年8月3日 下午6:11:43
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 */
	public void rotateAndScale(float x1, float y1, float x2, float y2) {
		float deltaX = x2 - x1;
		float deltaY = y2 - y1;
		int degreeSplit = 0;
		if (deltaY < 0) {
			degreeSplit = -180;//角度偏移
		}
		rotateZDegree = Math.toDegrees(Math.atan(deltaX / deltaY)) + degreeSplit;
		if (lastRotateZDegree == NONE || !isOnMultiControl) {
			lastRotateZDegree = rotateZDegree;
		}
		scaleTagWidth = (float)Math.sqrt(deltaX * deltaX + deltaY * deltaY);
		if(lastScaleTagWidth == NONE || !isOnMultiControl){
			lastScaleTagWidth = scaleTagWidth;
			comparedTagWidth = scaleTagWidth;
			comparedScaleValue = scaleValue;
		}
		isOnMultiControl = true;
		invalidate();
	}
	
	/**
	 * Description: 平移tagView
	 * Author: yunsimon
	 * Date: 2015年7月2日 上午10:56:05
	 * @param x
	 * @param y
	 */
	public void translateXY(float x, float y){
		translateX = x;
		if(lastTranslateX == NONE){
			lastTranslateX = translateX;
		}
		translateY = y;
		if(lastTranslateY == NONE){
			lastTranslateY = translateY;
		}
		invalidate();
	}
	
	public void removeSelf(){
		if (getParent() != null){//处理删除
			((ViewGroup)getParent()).removeView(this);
		}
		allTranslateX = 0;
		allTranslateY = 0;
	}
	
	/**
	 * Description:tag图片左右对称转换
	 * Author: yunsimon
	 * Date: 2015年7月3日 上午11:56:05
	 */
	public void symmetryTagView() {
		isSymmetry = !isSymmetry;
		invalidate();
	}
	
	public void resetState(){
		lastTranslateX = NONE;
		lastTranslateY = NONE;
		lastScaleTagWidth = NONE;
		lastRotateZDegree = NONE;
		isOnMultiControl = false;
	}

	public TagView copyTagView(){
		TagView newTagView = TagView.createTagView(getContext(), this.sourceDrawable);
		newTagView.onNormalState = true;
		newTagView.setTag(this.getTag());
		newTagView.allRotateZDegree = this.allRotateZDegree;
		newTagView.allTranslateX = this.allTranslateX;
		newTagView.allTranslateY = this.allTranslateY;
		newTagView.scaleValue = this.scaleValue;
		newTagView.isSymmetry = this.isSymmetry;
		newTagView.setTagWH(dp2px(getContext(), 120), dp2px(getContext(), 120));
		return newTagView;
	}

	private int dp2px(Context context,float dpValue) {
		float scale = context.getResources().getDisplayMetrics().density;
		return (int)(dpValue * scale + 0.5f);
	}


}
