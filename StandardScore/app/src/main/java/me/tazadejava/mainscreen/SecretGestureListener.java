package me.tazadejava.mainscreen;

import android.app.Activity;
import android.content.Intent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class SecretGestureListener implements View.OnTouchListener {

    enum Gesture {
        UP(0), DOWN(1), LEFT(2), RIGHT(3), CLICK(4);

        public final int NUM;

        Gesture(int num) {
            NUM = num;
        }
    }

    private final GestureDetector gestureDetector;
    private Activity activity;

    public SecretGestureListener(Activity activity) {
        this.activity = activity;
        gestureDetector = new GestureDetector(activity, new GestureListener());
    }

    public abstract void onKonamiCodeSuccess();
    public abstract void onGesture(Gesture gesture);
    public abstract void onLongPressEvent();

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        private List<Gesture> previousGestures;
        private long lastGesture;

        GestureListener() {
            previousGestures = new ArrayList<>();
        }

        @Override
        public void onLongPress(MotionEvent e) {
            onLongPressEvent();
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            onSwipe(Gesture.CLICK, -1, -1);
            return true;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            if(e.getAction() == MotionEvent.ACTION_UP) {
                onSwipe(Gesture.CLICK, -1, -1);
                return true;
            }

            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;

            int x = (int) e2.getX();
            int y = (int) e2.getY();
            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();
            if(Math.abs(diffX) > Math.abs(diffY)) {
                if(Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if(diffX > 0) {
                        onSwipe(Gesture.RIGHT, x, y);
                    } else {
                        onSwipe(Gesture.LEFT, x, y);
                    }
                    result = true;
                }
            } else if(Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if(diffY > 0) {
                    onSwipe(Gesture.DOWN, x, y);
                } else {
                    onSwipe(Gesture.UP, x, y);
                }
                result = true;
            }

            return result;
        }

        public void onSwipe(Gesture gesture, int x, int y) {
            onGesture(gesture);
            if(System.currentTimeMillis() - lastGesture >= 1000) {
                previousGestures.clear();
            }

            lastGesture = System.currentTimeMillis();
            previousGestures.add(gesture);

            switch(previousGestures.size()) {
                case 10:
                    Gesture[] key = new Gesture[] {Gesture.UP, Gesture.UP, Gesture.DOWN, Gesture.DOWN, Gesture.LEFT, Gesture.RIGHT, Gesture.LEFT, Gesture.RIGHT, Gesture.CLICK, Gesture.CLICK};
                    if(Arrays.equals(previousGestures.toArray(new Gesture[10]), key)) {
                        onKonamiCodeSuccess();
                    }
                    break;
                case 7:
                    key = new Gesture[] {Gesture.CLICK, Gesture.CLICK, Gesture.CLICK, Gesture.LEFT, Gesture.RIGHT, Gesture.LEFT, Gesture.RIGHT};
                    if(Arrays.equals(previousGestures.toArray(new Gesture[7]), key)) {
                        viewServiceLog();
                    }
                    break;
            }
        }

        private void viewServiceLog() {
            activity.startActivity(new Intent(activity, ServiceLogActivity.class));
        }
    }
}
