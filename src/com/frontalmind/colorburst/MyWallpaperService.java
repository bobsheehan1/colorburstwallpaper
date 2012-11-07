package com.frontalmind.colorburst;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.frontalmind.ColorGrid;

public class MyWallpaperService extends WallpaperService{
	
	public static final String SHARED_PREFS_NAME = "com.frontalmind.colorburst.wallpaper";


	@Override
	public Engine onCreateEngine() {
	 
		return new MyWallpaperEngine();
	}
	
	

	private class MyWallpaperEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {
		
		private int  animationRate;
		private Timer timer;

		private final Handler handler = new Handler();
		private final Runnable drawRunner = new Runnable() {
			@Override
			public void run() {
				draw();
			}
		};
		
		private ColorGrid colorGrid;
		private boolean visible = true;
		private boolean touchEnabled;
		private SharedPreferences mPrefs = null;
		int width, height;

		public MyWallpaperEngine() {			
            mPrefs = MyWallpaperService.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
            mPrefs.registerOnSharedPreferenceChangeListener(this);
 
			colorGrid = new ColorGrid();
            onSharedPreferenceChanged(mPrefs, null);
            handler.post(drawRunner);
		}
		
		public void enableAnimation(boolean enable){
			if (timer != null){
				timer.cancel();
				timer.purge();
				timer = null;
			}
			if (enable){
	    		timer = new Timer();
	    		timer.scheduleAtFixedRate(new ColorTask(), 0, animationRate);
	    	}
		}
		
		class ColorTask extends TimerTask {

			@Override
			public void run() {
				try {
					colorGrid.updateColors();
					handler.post(drawRunner);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		 };

		@Override
		public void onVisibilityChanged(boolean visible) {
			this.visible = visible;
			if (visible) {
				handler.post(drawRunner);
				enableAnimation(true);
			} else {
				enableAnimation(false);
				handler.removeCallbacks(drawRunner);
			}
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			enableAnimation(false);
			
			super.onSurfaceDestroyed(holder);
			this.visible = false;
			handler.removeCallbacks(drawRunner);
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			
			int statucBarHeight = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getHeight() 
				    - height;

			Log.d("what", Integer.toString(statucBarHeight));
			this.colorGrid.createGrid(width, height);
			this.colorGrid.updateColors();
			enableAnimation(true);
			
			this.width = width;
			this.height = height;

			super.onSurfaceChanged(holder, format, width, height);
		}

		@Override
		public void onTouchEvent(MotionEvent event) {
			if (touchEnabled) {
				SurfaceHolder holder = getSurfaceHolder();
				Canvas canvas = null;
				try {
					canvas = holder.lockCanvas();
					if (canvas != null) {
						canvas.drawColor(Color.BLUE);
						colorGrid.draw(canvas);

					}
				} finally {
					if (canvas != null)
						holder.unlockCanvasAndPost(canvas);
				}
				super.onTouchEvent(event);
			}
		}

		private void draw() {
			SurfaceHolder holder = getSurfaceHolder();
			Canvas canvas = null;
			try {
				canvas = holder.lockCanvas();
				if (canvas != null) {
					canvas.drawColor(Color.BLACK);
					colorGrid.draw(canvas);
				}
			} finally {
				try {
				if (canvas != null)
					holder.unlockCanvasAndPost(canvas);
				} 
				catch (IllegalArgumentException iae)
				{
				// Catching this exception will save your wallpaper from crashing.
				}
			}
			handler.removeCallbacks(drawRunner);
			if (visible) {
				handler.postDelayed(drawRunner, 5000);
			}
		}
		
		private void loadPref() {
			enableAnimation(false);

			int animationRate = mPrefs.getInt("pref_rate", 100);
			this.setAnimationRate(animationRate);

			int blockSize = mPrefs.getInt("pref_block_size", 50);
			if (this.colorGrid != null){
				this.colorGrid.setBlockSize(blockSize);
			}

			String colorRange = mPrefs.getString("color_preference",
					"Blue");
			if (this.colorGrid != null)
				this.colorGrid.setColorRange(colorRange);

			int decayStep = mPrefs.getInt("pref_decay", 8);
			if (this.colorGrid != null)
				this.colorGrid.setDecayStep(decayStep);

			int strokeWidth = mPrefs.getInt("pref_stroke_width", 2);
			if (this.colorGrid != null){
				this.colorGrid.setStrokeWidth(strokeWidth);
			}

			int threshold = mPrefs.getInt("pref_threshold", 0);
			if (this.colorGrid != null)
				this.colorGrid.setThreshold(threshold);

			int padding = mPrefs.getInt("pref_padding", 4);
			if (this.colorGrid != null){
				this.colorGrid.setPadding(padding);
			}

			String shape = mPrefs.getString("pref_shape", "hexagon");
			if (this.colorGrid != null){
				this.colorGrid.setShape(shape);
			}

			int fillAlpha = mPrefs.getInt("pref_fill_alpha", 64);
			if (this.colorGrid != null)
				this.colorGrid.setFillAlpha(fillAlpha);

			int strokeAlpha = mPrefs
					.getInt("pref_stroke_alpha", 128);
			if (this.colorGrid != null)
				this.colorGrid.setStrokeAlpha(strokeAlpha);

			mPrefs.edit().commit();
			
			this.colorGrid.createGrid(this.width, this.height);
			enableAnimation(true);

		}

		public void setAnimationRate(int animationRate) {
			this.animationRate = animationRate;
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences arg0,
				String arg1) {
			loadPref();
			
		}
		
		
	}

}
