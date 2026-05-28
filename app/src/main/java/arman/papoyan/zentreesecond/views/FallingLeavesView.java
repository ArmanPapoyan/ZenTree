package arman.papoyan.zentreesecond.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
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
    private String currentSkin;
    private int currentAlpha = 70;

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

        SharedPreferences prefs = getContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE);
        int savedAlphaPercent = prefs.getInt("leaves_alpha", 20);
        currentAlpha = savedAlphaPercent * 255 / 100;
        leafPaint.setAlpha(currentAlpha);

        Log.d("FallingLeaves", "init - загружена прозрачность: " + savedAlphaPercent + "% -> alpha=" + currentAlpha);

        currentSkin = prefs.getString("leaf_skin", "spring");

        loadSkin();
        createLeaves(25);
        startAnimation();
    }

    private void loadSkin() {
        int drawableId;
        switch (currentSkin) {
            case "autumn":
                drawableId = R.drawable.leaf_autumn;
                break;
            case "snow":
                drawableId = R.drawable.leaf_snow;
                break;
            case "rain":
                drawableId = R.drawable.leaf_rain;
                break;
            default:
                drawableId = R.drawable.leaf;
                currentSkin = "spring";
                break;
        }

        Drawable leafDrawable = ContextCompat.getDrawable(getContext(), drawableId);
        if (leafDrawable != null) {
            leafBitmap = Bitmap.createBitmap(
                    leafDrawable.getIntrinsicWidth(),
                    leafDrawable.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(leafBitmap);
            leafDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            leafDrawable.draw(canvas);
        } else {
            createFallbackBitmap();
        }
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
        boolean isRain = currentSkin.equals("rain");

        for (int i = 0; i < count; i++) {
            Leaf leaf = new Leaf();
            leaf.x = random.nextFloat() * 2000;
            leaf.y = random.nextFloat() * 3000;
            leaf.speed = 1 + random.nextFloat() * 3;

            if (isRain) {
                leaf.rotation = 0;
                leaf.rotationSpeed = 0;
            } else {
                leaf.rotation = random.nextFloat() * 360;
                leaf.rotationSpeed = 0.5f + random.nextFloat() * 2;
            }

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
        boolean isRain = currentSkin.equals("rain");

        for (Leaf leaf : leaves) {
            leaf.y += leaf.speed;

            if (!isRain) {
                leaf.rotation += leaf.rotationSpeed;
            }

            if (leaf.y > height + 100) {
                leaf.y = -100;
                leaf.x = random.nextFloat() * width;
                if (!isRain) {
                    leaf.rotation = random.nextFloat() * 360;
                } else {
                    leaf.rotation = 0;
                }
            }

            if (!isRain) {
                leaf.x += (float) (Math.sin(leaf.y / 150) * 0.5f);
            }

            if (leaf.x < -50) leaf.x = -50;
            if (leaf.x > width + 50) leaf.x = width + 50;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;

        boolean isRain = currentSkin.equals("rain");

        for (Leaf leaf : leaves) {
            leaf.x = random.nextFloat() * width;
            leaf.y = random.nextFloat() * height;
            if (isRain) {
                leaf.rotation = 0;
            }
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

    public void setLeafAlpha(int alpha) {
        currentAlpha = alpha;
        if (leafPaint != null) {
            leafPaint.setAlpha(alpha);
            invalidate();
        }
        Log.d("FallingLeaves", "setLeafAlpha - установлена прозрачность: alpha=" + alpha);
    }

    public int getLeafAlpha() {
        return currentAlpha;
    }

    public void setSkin(String skin) {
        this.currentSkin = skin;
        loadSkin();

        leaves.clear();
        createLeaves(25);
        invalidate();
    }
}