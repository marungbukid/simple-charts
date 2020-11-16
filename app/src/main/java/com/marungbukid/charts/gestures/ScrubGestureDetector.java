package com.marungbukid.charts.gestures;

import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

public class ScrubGestureDetector implements View.OnTouchListener {
	static final long LONG_PRESS_TIMEOUT_MS = 250;

	private final ScrubListener scrubListener;
	private final Handler handler;
	private final float touchSlop;

	private boolean enabled;
	private float downX, downY;

	public ScrubGestureDetector(
		@NonNull ScrubListener scrubListener,
		@NonNull Handler handler,
		float touchSlop
	) {
		this.scrubListener = scrubListener;
		this.handler = handler;
		this.touchSlop = touchSlop;
	}

	private final Runnable longPressRunnable = new Runnable() {
		@Override
		public void run() {
			scrubListener.onScrubbed(downX, downY);
		}
	};

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (!enabled) return false;

		final float x = event.getX();
		final float y = event.getY();

		switch (event.getActionMasked()) {

			case MotionEvent.ACTION_DOWN:
				// store the time to compute whether future events are 'long presses'
				downX = x;
				downY = y;

				handler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT_MS);
				return true;


			case MotionEvent.ACTION_MOVE:
				// calculate the elapsed time since the down event
				float timeDelta = event.getEventTime() - event.getDownTime();

				// if the user has intentionally long-pressed
				if (timeDelta >= LONG_PRESS_TIMEOUT_MS) {
					handler.removeCallbacks(longPressRunnable);
					scrubListener.onScrubbed(x, y);
				} else {
					// if we moved before longpress, remove the callback if we exceeded the tap slop
					float deltaX = x - downX;
					float deltaY = y - downY;
					if (deltaX >= touchSlop || deltaY >= touchSlop) {
						handler.removeCallbacks(longPressRunnable);
						// We got a MOVE event that exceeded tap slop but before the long-press
						// threshold, we don't care about this series of events anymore.
						return false;
					}
				}

				return true;


			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				handler.removeCallbacks(longPressRunnable);
				scrubListener.onScrubEnded();
				return true;
			default:
				return false;
		}
	}


	public interface ScrubListener {
		void onScrubbed(float x, float y);
		void onScrubEnded();
	}
}
