package arman.papoyan.zentreesecond.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Random;

import arman.papoyan.zentreesecond.R;

public class FallingLeavesView extends View {

    private ArrayList<Leaf> leaves;
    private Random random;
    private Paint leafPaint;
    private Bitmap leafBitmap;
    private int width, height;
    private boolean isRunning = true;
    private Drawable leafDrawable;

    private static class Leaf {
        float x, y;
        float speed;
        float rotation;
        float rotationSpeed;
        float scale;
    }

    public FallingLeavesView(Context context) {
        super(context);
        init();
    }

    public FallingLeavesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        leaves = new ArrayList<>();
        random = new Random();
        leafPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        leafPaint.setAlpha(50);
        leafDrawable = ContextCompat.getDrawable(getContext(), R.drawable.leaf);
        if (leafDrawable == null) {
            createFallbackBitmap();
        } else {
            leafBitmap = Bitmap.createBitmap(
                    leafDrawable.getIntrinsicWidth(),
                    leafDrawable.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(leafBitmap);
            leafDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            leafDrawable.draw(canvas);
        }

        createLeaves(25);
        startAnimation();
    }
    private void createFallbackBitmap() {
        leafBitmap = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(leafBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xFF66BB6A);
        canvas.drawCircle(20, 20, 18, paint);
        paint.setColor(0xFFE8F5E9);
        paint.setStrokeWidth(2);
        canvas.drawLine(20, 5, 20, 35, paint);
    }

    private void createLeaves(int count) {
        for (int i = 0; i < count; i++) {
            Leaf leaf = new Leaf();
            leaf.x = 500 + random.nextFloat() * 500;
            leaf.y = random.nextFloat() * 2000;
            leaf.speed = 1 + random.nextFloat() * 3;
            leaf.rotation = random.nextFloat() * 360;
            leaf.rotationSpeed = 0.5f + random.nextFloat() * 2;
            leaf.scale = 0.4f + random.nextFloat() * 0.5f;
            leaves.add(leaf);
        }
    }


    private void startAnimation() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(50);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            if (isRunning && leafBitmap != null) {
                updateLeaves();
                invalidate();
            }
        });
        animator.start();
    }

    private void updateLeaves() {
        for (Leaf leaf : leaves) {
            leaf.y += leaf.speed;
            leaf.rotation += leaf.rotationSpeed;

            if (leaf.y > height + 100) {
                leaf.y = -100;
                leaf.x = random.nextFloat() * width;
                leaf.rotation = random.nextFloat() * 360;
            }

            leaf.x += (float) (Math.sin(leaf.y / 150) * 0.5f);

            if (leaf.x < -50) leaf.x = -50;
            if (leaf.x > width + 50) leaf.x = width + 50;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        for (Leaf leaf : leaves) {
            leaf.x = random.nextFloat() * width;
            leaf.y = random.nextFloat() * height;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (leafBitmap == null) return;

        for (Leaf leaf : leaves) {
            canvas.save();
            canvas.translate(leaf.x, leaf.y);
            canvas.rotate(leaf.rotation);
            canvas.scale(leaf.scale, leaf.scale);
            canvas.drawBitmap(leafBitmap, -leafBitmap.getWidth() / 2, -leafBitmap.getHeight() / 2, leafPaint);
            canvas.restore();
        }
    }

    public void stopAnimation() {
        isRunning = false;
    }

    public void startFalling() {
        isRunning = true;
        invalidate();
    }
}