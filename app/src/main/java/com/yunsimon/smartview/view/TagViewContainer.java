package com.yunsimon.smartview.view;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;


/**
 * Description: tagView的编辑器View
 * Author: yunsimon 
 * Date: 2015年9月25日 上午10:43:57
 */
public class TagViewContainer extends ViewGroup implements View.OnTouchListener {

	private TagView mTargetTagView;

	private boolean isNotActionTouch = false;

	private int touchType = 0;
	private final int NORMAL_STATE_TYPE = 1;// 非编辑
	//private final int EDIT_STATE_TYPE = 2;// 编辑
	private final int DRAG_TYPE = 3;// 拖拽
	private final int DEL_TYPE = 4;// 删除
	private final int SYMMETRY_TYPE = 5;// 对称
	private final int CONTROL_TYPE = 6;// 缩放

	private int tagContentWH = 0;

	private TagView activeTagView;

	/** 多点触屏 */
	private boolean isActionMultiTouch = false;
	private boolean isOnMultiTouch = false;

	boolean isFirstTouchFromOutside = true;
	
	private Handler mHandler = new Handler();
	private final static int SCROLL_STATE_NORMAL = 0;
	private final static int SCROLL_STATE_WAITING = 1;
	private int scrollState = SCROLL_STATE_NORMAL;
	private float moveX;
	private ViewGroup parentView;

	public void setParentView(ViewGroup parentView){
		this.parentView = parentView;
	}

	private ScrollRunnable scrollRunnable = new ScrollRunnable();
	private class ScrollRunnable implements Runnable {
		private int direction = -1;
		
		@Override
		public void run() {
			scrollState = SCROLL_STATE_NORMAL;
		}
		
		public void setDirection(int d){
			direction = d;
		}
	}

	public TagViewContainer(Context context, AttributeSet attrs) {
		super(context, attrs);
		tagContentWH = dp2px(context, 120);
	}
	
	public void addTagView(int tagViewDrawableId) {
		saveCurrentTagViewIfNecessary();

		TagView mTagView = TagView.createTagView(getContext(), tagViewDrawableId);
		mTagView.setDrawingCacheEnabled(true);
		LayoutParams lp = new LayoutParams(getWidth(), getHeight());
		lp.x = 0;
		lp.y = 0;
		addView(mTagView, lp);
		mTagView.setTagWH(tagContentWH, tagContentWH);
		mTargetTagView = activeTagView = mTagView;
	}

	public static class LayoutParams extends FrameLayout.LayoutParams {
		public int x, y;

		public LayoutParams(int width, int height) {
			super(width, height);
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			View child = getChildAt(i);
			LayoutParams lp = (LayoutParams) child.getLayoutParams();
			child.layout(lp.x, lp.y, lp.x + lp.width, lp.y + lp.height);
		}
	}

	/**
	 * Description: 用于控制从普通状态到拖动状态的事件传递
	 * Author: yunsimon 
	 * Date: 2015年9月15日 下午6:30:29
	 */
	@Override
	public boolean onTouch(View v, MotionEvent ev) {
		if(ev.getY() > getHeight())
			return false;

		if(isFirstTouchFromOutside){
			isNotActionTouch = false;
			touchType = DRAG_TYPE;
			isFirstTouchFromOutside = false;
			return true;
		}else{
			return onTouchEvent(ev);
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		float x = ev.getX();
		float y = ev.getY();
		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			isNotActionTouch = false;
			isActionMultiTouch = false;
			handleOnTouchDown(x, y);
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			isNotActionTouch = false;
			if (activeTagView != null) {
				isActionMultiTouch = true;
				activeTagView.resetState();
			}
			isOnMultiTouch = true;
			break;
		case MotionEvent.ACTION_MOVE:
			moveX = x;
			if (isNotActionTouch)
				break;
			if (isActionMultiTouch && activeTagView != null && isOnMultiTouch) {
				try{					
					activeTagView.rotateAndScale(ev.getX(0), ev.getY(0), ev.getX(1), ev.getY(1));
				}catch(Exception e){
					e.printStackTrace();
				}
			} else {
				handleOnTouchMove(x, y);
			}
			break;
		case MotionEvent.ACTION_UP:
			isNotActionTouch = true;
			if (!isActionMultiTouch) {
				handleOnTouchUp(x, y);
			}
			isFirstTouchFromOutside = true;
			if(scrollState == SCROLL_STATE_WAITING){
				mHandler.removeCallbacks(scrollRunnable);
				scrollState = SCROLL_STATE_NORMAL;
			}
			break;
		case MotionEvent.ACTION_CANCEL:
			isFirstTouchFromOutside = true;
			isNotActionTouch = true;
			break;
		case MotionEvent.ACTION_POINTER_UP:
			isOnMultiTouch = false;
			isNotActionTouch = true;
			break;
		}
		return true;
	}

	private void handleOnTouchDown(float x, float y) {
		boolean findTarget = false;
		// 先判断是否触碰当前操作tagview的删除，对称，缩放按钮
		if (activeTagView != null) {
			activeTagView.resetState();
			if (activeTagView.isOnTouchDelView(x, y)) {
				touchType = DEL_TYPE;
				mTargetTagView = activeTagView;
				findTarget = true;
			} else if (activeTagView.isOnTouchControlView(x, y)) {
				touchType = CONTROL_TYPE;
				mTargetTagView = activeTagView;
				findTarget = true;
			} else if (activeTagView.isOnTouchSymmetryView(x, y)) {
				touchType = SYMMETRY_TYPE;
				mTargetTagView = activeTagView;
				findTarget = true;
			} else if (activeTagView.isOnTouchSelfView(x, y)) {
				touchType = DRAG_TYPE;
				mTargetTagView = activeTagView;
				findTarget = true;
			}
		}

		if (!findTarget) {
			touchType = NORMAL_STATE_TYPE;
		}
	}

	private void handleOnTouchMove(float x, float y) {
		if (mTargetTagView == null)
			return;
		if (touchType == DRAG_TYPE) {
			mTargetTagView.translateXY(x, y);
		} else if (touchType == CONTROL_TYPE) {
			mTargetTagView.rotateAndScale(x, y);
		} else if (touchType == DEL_TYPE || touchType == SYMMETRY_TYPE) {
			return;
		}
	}

	private void handleOnTouchUp(float x, float y) {
		if (touchType == DEL_TYPE) {// 处理删除
			activeTagView.removeSelf();
			activeTagView = null;
		} else if (touchType == SYMMETRY_TYPE) {// 图片左右对称转换
			mTargetTagView.symmetryTagView();
		} else if (touchType == NORMAL_STATE_TYPE) {
			saveCurrentTagViewIfNecessary();
		} else if (touchType == DRAG_TYPE || touchType == CONTROL_TYPE) {
		}

		touchType = 0;
	}

	private void saveCurrentTagViewIfNecessary() {
		if(activeTagView != null){
			parentView.addView(activeTagView.copyTagView(), parentView.getChildCount() - 1,
					new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			activeTagView.removeSelf();
			activeTagView = null;
		}
	}

	private int dp2px(Context context,float dpValue){
		float scale=context.getResources().getDisplayMetrics().density;
		return (int)(dpValue*scale+0.5f);
	}
}
