package com.brace.examverse.theme;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

/** Shared, bounded bitmap cache. Each card has its own crop and scrim. */
public class ThemeArtDrawable extends Drawable {
    private static final LruCache<Integer, Bitmap> CACHE = new LruCache<>(6);
    private final Bitmap bitmap;
    private final float radius;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF();
    private final Path clip = new Path();
    private int alpha = 255;
    public ThemeArtDrawable(Context c, String key, float radius) {
        int id = ThemeManager.artworkResource(key);
        Bitmap cached = CACHE.get(id);
        if (cached == null) {
            BitmapFactory.Options options = new BitmapFactory.Options(); options.inScaled = false;
            cached = BitmapFactory.decodeResource(c.getResources(), id, options);
            CACHE.put(id, cached);
        }
        bitmap = cached; this.radius = radius;
    }
    @Override public void draw(Canvas c) {
        Rect b = getBounds(); if (b.isEmpty()) return;
        destination.set(b); clip.reset(); clip.addRoundRect(destination, radius, radius, Path.Direction.CW);
        int save = c.save(); c.clipPath(clip);
        float scale = Math.max(b.width() / (float)bitmap.getWidth(), b.height() / (float)bitmap.getHeight());
        float width = bitmap.getWidth() * scale, height = bitmap.getHeight() * scale;
        RectF image = new RectF(b.centerX() - width / 2, b.centerY() - height / 2, b.centerX() + width / 2, b.centerY() + height / 2);
        paint.setShader(null); paint.setAlpha(alpha); c.drawBitmap(bitmap, null, image, paint);
        paint.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom, new int[]{0xd908101b,0x9008101b,0x7808101b}, null, Shader.TileMode.CLAMP));
        c.drawRect(destination, paint); paint.setShader(null); c.restoreToCount(save);
    }
    @Override public void setAlpha(int value) { alpha = value; invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
