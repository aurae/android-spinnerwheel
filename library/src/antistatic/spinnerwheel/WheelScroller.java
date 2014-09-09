/*
 * android-spinnerwheel
 * https://github.com/ai212983/android-spinnerwheel
 *
 * based on
 *
 * Android Wheel Control.
 * https://code.google.com/p/android-wheel/
 *
 * Copyright 2011 Yuri Kanivets
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package antistatic.spinnerwheel;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.animation.Interpolator;
import android.widget.Scroller;

/**
 * Scroller class handles scrolling events and updates the spinnerwheel
 */
public abstract class WheelScroller {
	/**
	 * Scrolling listener interface
	 */
	public interface ScrollingListener {
		/**
		 * Scrolling callback called when scrolling is performed.
		 * 
		 * @param distance
		 *            the distance to scroll
		 */
		void onScroll(int distance);

		/**
		 * This callback is invoked when scroller has been touched
		 */
		void onTouch();

		/**
		 * This callback is invoked when touch is up
		 */
		void onTouchUp();

		/**
		 * Starting callback called when scrolling is started
		 */
		void onStarted();

		/**
		 * Finishing callback called after justifying
		 */
		void onFinished();

		/**
		 * Justifying callback called to justify a view when scrolling is ended
		 */
		void onJustify();
	}

	/** Scrolling duration */
	private static final int SCROLLING_DURATION = 400;

	/** Stylus scroll delay, in milliseconds */
	private static final int STYLUS_SCROLL_DELAY = 50;

	/** Minimum delta for scrolling */
	public static final int MIN_DELTA_FOR_SCROLLING = 1;

	// Listener
	private ScrollingListener listener;

	// Context
	private Context context;

	// Scrolling
	private GestureDetector gestureDetector;
	protected Scroller scroller;
	private int lastScrollPosition;
	private float lastTouchedPosition;
	private float velocityFactor = 1f;
	private boolean isScrollingPerformed;
	private long lastTouchedTimestamp;

	// animation handler
	private Handler animationHandler;

	/**
	 * Constructor
	 * 
	 * @param context
	 *            the current context
	 * @param listener
	 *            the scrolling listener
	 */
	public WheelScroller(Context context, ScrollingListener listener) {
		this.gestureDetector = new GestureDetector(context, new SimpleOnGestureListener() {
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				// Do scrolling in onTouchEvent() since onScroll() are not call immediately
				// when user touch and move the spinnerwheel
				return true;
			}

			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				lastScrollPosition = 0;
				scrollerFling(lastScrollPosition, (int) (velocityX * velocityFactor), (int) (velocityY * velocityFactor));
				setNextMessage(MESSAGE_SCROLL);
				return true;
			}

			// public boolean onDown(MotionEvent motionEvent);

		});
		this.gestureDetector.setIsLongpressEnabled(false);

		this.scroller = new Scroller(context);
		this.animationHandler = new AnimationHandler(this);

		this.listener = listener;
		this.context = context;
	}

	/**
	 * Set the fling velocity factor to be applied. Default value is 1f
	 * 
	 * @param factor
	 *            the velocity factor to apply to consecutive flings
	 */
	public void setVelocityFactor(float factor) {
		velocityFactor = factor;
	}

	/**
	 * Set the specified scrolling interpolator
	 * 
	 * @param interpolator
	 *            the interpolator
	 */
	public void setInterpolator(Interpolator interpolator) {
		scroller.forceFinished(true);
		scroller = new Scroller(context, interpolator);
	}

	/**
	 * Scroll the spinnerwheel
	 * 
	 * @param distance
	 *            the scrolling distance
	 * @param time
	 *            the scrolling duration
	 */
	public void scroll(int distance, int time) {
		scroller.forceFinished(true);
		lastScrollPosition = 0;
		scrollerStartScroll(distance, time != 0 ? time : SCROLLING_DURATION);
		setNextMessage(MESSAGE_SCROLL);
		startScrolling();
	}

	/**
	 * Stops scrolling
	 */
	public void stopScrolling() {
		scroller.forceFinished(true);
	}

	/**
	 * Handles Touch event
	 * 
	 * @param event
	 *            the motion event
	 * @return
	 */
	public boolean onTouchEvent(MotionEvent event) {

		switch (event.getAction()) {

		case MotionEvent.ACTION_DOWN:
			lastTouchedTimestamp = System.currentTimeMillis();
			lastTouchedPosition = getMotionEventPosition(event);
			scroller.forceFinished(true);
			clearMessages();
			listener.onTouch();
			break;

		case MotionEvent.ACTION_UP:
			if (scroller.isFinished()) listener.onTouchUp();
			lastTouchedTimestamp = 0L;
			break;

		case MotionEvent.ACTION_MOVE:
			// perform scrolling
			int distance = (int) (getMotionEventPosition(event) - lastTouchedPosition);

			// Stylus scrolling needs to be restricted and started delayed
			boolean isStylus = (AbstractWheel.supportGetToolType(event, 0) == 2);

			// For all non-stylus movements, just check the traveled distance.
			// For stylus movements, check the last touched timestamp as well and
			// potentially start the scrolling action delayed
			long elapsedTimeSinceDown = System.currentTimeMillis() - lastTouchedTimestamp;
			if ((!isStylus || elapsedTimeSinceDown > STYLUS_SCROLL_DELAY) && distance != 0) {
				startScrolling();
				listener.onScroll(distance);
				lastTouchedPosition = getMotionEventPosition(event);
			}
			break;
		}

		if (!gestureDetector.onTouchEvent(event) && event.getAction() == MotionEvent.ACTION_UP) {
			justify();
		}

		return true;
	}

	// Messages
	private static final int MESSAGE_SCROLL = 0;
	private static final int MESSAGE_JUSTIFY = 1;

	/**
	 * Set next message to queue. Clears queue before.
	 * 
	 * @param message
	 *            the message to set
	 */
	private void setNextMessage(int message) {
		clearMessages();
		animationHandler.sendEmptyMessage(message);
	}

	/**
	 * Clears messages from queue
	 */
	private void clearMessages() {
		animationHandler.removeMessages(MESSAGE_SCROLL);
		animationHandler.removeMessages(MESSAGE_JUSTIFY);
	}

	/**
	 * Justifies spinnerwheel
	 */
	private void justify() {
		listener.onJustify();
		setNextMessage(MESSAGE_JUSTIFY);
	}

	/**
	 * Starts scrolling
	 */
	private void startScrolling() {
		if (!isScrollingPerformed) {
			isScrollingPerformed = true;
			listener.onStarted();
		}
	}

	/**
	 * Finishes scrolling
	 */
	protected void finishScrolling() {
		if (isScrollingPerformed) {
			listener.onFinished();
			isScrollingPerformed = false;
		}
	}

	protected abstract int getCurrentScrollerPosition();

	protected abstract int getFinalScrollerPosition();

	protected abstract float getMotionEventPosition(MotionEvent event);

	protected abstract void scrollerStartScroll(int distance, int time);

	protected abstract void scrollerFling(int position, int velocityX, int velocityY);

	/**
	 * Handler implementation for animations
	 */
	private static class AnimationHandler extends android.os.Handler {

		private WheelScroller wheelScroller;

		private AnimationHandler(WheelScroller scroller) {
			this.wheelScroller = scroller;
		}

		@Override
		public void handleMessage(Message msg) {
			wheelScroller.scroller.computeScrollOffset();
			int currPosition = wheelScroller.getCurrentScrollerPosition();
			int delta = wheelScroller.lastScrollPosition - currPosition;
			wheelScroller.lastScrollPosition = currPosition;
			if (delta != 0) {
				wheelScroller.listener.onScroll(delta);
			}

			// scrolling is not finished when it comes to final Y
			// so, finish it manually
			if (Math.abs(currPosition - wheelScroller.getFinalScrollerPosition()) < MIN_DELTA_FOR_SCROLLING) {
				// currPosition = getFinalScrollerPosition();
				wheelScroller.scroller.forceFinished(true);
			}
			if (!wheelScroller.scroller.isFinished()) {
				this.sendEmptyMessage(msg.what);
			} else if (msg.what == MESSAGE_SCROLL) {
				wheelScroller.justify();
			} else {
				wheelScroller.finishScrolling();
			}
		}
	}
}
