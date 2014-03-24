package com.spheroglass.ar;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CustomCameraView extends SurfaceView {

	public static final int HEIGHT = 144;
	public static final int WIDTH = 256;
	private static final int STEP = 1;
	private static final int DIVISOR = 1;

	public static final int RESIZE_QUALITY = 100;

	Camera camera;
	SurfaceHolder previewHolder;
	private List<Listener> listeners = new ArrayList<CustomCameraView.Listener>();
	private FindSpheroTask findSpheroTask;
	private int divider = 0;
	private int color = Color.GREEN;

	public CustomCameraView(Context context) {
		super(context);

		previewHolder = this.getHolder();
		previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		previewHolder.addCallback(surfaceHolderListener);
	}

	SurfaceHolder.Callback surfaceHolderListener = new SurfaceHolder.Callback() {

		public void surfaceCreated(SurfaceHolder holder) {
			camera=Camera.open();

			try {
				camera.setPreviewDisplay(previewHolder);
				camera.getParameters().setPreviewFormat(PixelFormat.JPEG);
				camera.setPreviewCallback(new Camera.PreviewCallback() {
					@Override
					public void onPreviewFrame(byte[] data, Camera camera) {
						try {
							if(findSpheroTask == null) {
								if(divider++ % DIVISOR == 0) {
									findSpheroTask = new FindSpheroTask();
									findSpheroTask.execute(data);
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
			catch (Throwable t){ }
		}
		
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			Parameters params = camera.getParameters();
			params.setPictureFormat(ImageFormat.JPEG);
			params.setPreviewFpsRange(5000, 5000); // 5 FPS
			params.setPreviewSize(WIDTH, HEIGHT);
			camera.setParameters(params);
			camera.startPreview();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			camera.stopPreview();
			camera.setPreviewCallback(null);
			camera.release();
			camera = null;
		}
	};

	public interface Listener {
		void setPoint(int x, int y);
		void setPoints(List<Pair<Integer, Integer>> points);
	}

	public void addListener(Listener listener) {
		this.listeners.add(listener);
	}

	private class FindSpheroTask extends AsyncTask<byte[], Void, List<Pair<Integer, Integer>>> {

		@Override
		protected List<Pair<Integer, Integer>> doInBackground(byte[]... params) {
			List<Pair<Integer, Integer>> list = new ArrayList<Pair<Integer,Integer>>();
			YuvImage yuvImage = new YuvImage(params[0], ImageFormat.NV21, WIDTH, HEIGHT, null);
			Rect rect = new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight());
			ByteArrayOutputStream output_stream = new ByteArrayOutputStream();
			yuvImage.compressToJpeg(rect, RESIZE_QUALITY, output_stream);
			Bitmap bmp = BitmapFactory.decodeByteArray(output_stream.toByteArray(), 0, output_stream.size());
			int maxSphero = 0;
			int maxSpheroX = 0;
			int maxSpheroY = 0;
			for(int i=0; i<bmp.getWidth(); i+=STEP) {
				for(int j=0; j<bmp.getHeight(); j+=STEP) {
					int pixel = bmp.getPixel(i, j);
					int isSphero = isSphero(pixel);
					if(isSphero > 0) {
						if(isBetterMatch(maxSphero, maxSpheroX, maxSpheroY, isSphero, i, j)) {
							maxSphero = isSphero;
							maxSpheroX = i;
							maxSpheroY = j;
						}
					}
				}
			}
			if(maxSphero > 0) {
				list.add(new Pair<Integer, Integer>(maxSpheroX - WIDTH/2, HEIGHT/2 - maxSpheroY));
			}
			return list;
		}

		private boolean isBetterMatch(int maxSphero, int maxSpheroX, int maxSpheroY, int isSphero, int i, int j) {
			if(isSphero > maxSphero) {
				// better color match
				return true;
			} else if(isSphero == maxSphero) {
				// same color match, but smaller distance to center
				return Math.abs(i-WIDTH/2)+Math.abs(j-HEIGHT/2) < Math.abs(maxSpheroX-WIDTH/2)+Math.abs(maxSpheroY-HEIGHT/2);
			} else {
				return false;
			}
		}

		private int isSphero(int pixel) {
			int red = Color.red(pixel);
			int green = Color.green(pixel);
			int blue = Color.blue(pixel);

			// RED
			//return red > 150 && green < 50 && blue < 50;
			//return red > 150;
			//return (red > green + 50) && (red > blue + 50);
			//return (red > green) && (red > blue);
			
			// GREEN
			//return green > 150 && red < 50 && blue < 50;
			//return (green > 100) && (green > red + 25) && (green > blue + 25);
			//return (green > 100) && (green > red + 20) && (green > blue + 20);
			//return Math.max(green - red, 0) + Math.max(green - blue, 0);
			//return Math.max(green - red - 20, 0) + Math.max(green - blue - 20, 0);
			/*if((green > 100) && (green > red + 20) && (green > blue + 20)) {
				return Math.max(green - red - 20, 0) + Math.max(green - blue - 20, 0);
			} else {
				return 0;
			}*/
			
			if(color == Color.RED) { 
				return isColor(red, green, blue);
			} else if(color == Color.BLUE) {
				return isColor(blue, red, green);
			} else {
				return isColor(green, red, blue);
			}
			//return isColor(green, red, blue);
			/*return
					((green > 100) && (green > red + 20) && (green > blue + 20))
					||
					((red > 250) && (green > 250) && (blue > 250));*/
			
			// WHITE
			//return (red > 250) && (green > 250) && (blue > 250);
		}

		private int isColor(int correct, int incorrect1, int incorrect2) {
			if((correct > 100) && (correct > incorrect1 + 20) && (correct > incorrect2 + 20)) {
				return Math.max(correct - incorrect1 - 20, 0) + Math.max(correct - incorrect2 - 20, 0);
			} else {
				return 0;
			}
		}

		@Override
		protected void onPostExecute(List<Pair<Integer, Integer>> points) {
			super.onPostExecute(points);
			for(Listener listener : listeners) {
				listener.setPoints(points);
			}
			findSpheroTask = null;
		}
	}

	public void setColor(int color) {
		this.color = color;
	}
}
