package com.spheroglass.ar;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.util.Pair;
import android.view.View;

public class ARView extends View implements CustomCameraView.Listener {

	private Paint p;

	private List<Pair<Integer, Integer>> points = new ArrayList<Pair<Integer,Integer>>();

	public ARView(Context context) {
		super(context);

		p = new Paint();
		p.setTextAlign(Align.CENTER);
		p.setTextSize(20);
	}

	public void onDraw(Canvas c) {
		int canvasWidth = c.getWidth();
		int canvasHeight = c.getHeight();
		float widthFactor = canvasWidth/CustomCameraView.WIDTH;
		float heigthFactor = canvasHeight/CustomCameraView.HEIGHT;
		//c.drawColor(Color.BLACK);
		p.setColor(Color.GREEN);
		c.drawText("o", canvasWidth / 2, canvasHeight / 2, p);
		p.setColor(Color.YELLOW);
		p.setStyle(Style.STROKE);
		c.drawCircle(canvasWidth/2, canvasHeight/2, (int)(widthFactor*(SpheroGlassAR.MAX_SPEED_DIAMETER+SpheroGlassAR.STOP_DIAMETER)), p);
		c.drawCircle(canvasWidth/2, canvasHeight/2, (int)(widthFactor*SpheroGlassAR.STOP_DIAMETER), p);

		p.setColor(Color.RED);
		for(Pair<Integer, Integer> point : points) {
			c.drawText("x", canvasWidth / 2 + point.first * widthFactor, canvasHeight / 2 - point.second * heigthFactor, p);
			//c.drawText("x: "+point.first+", y: "+point.second, 50, 50, p);
		}
	}

	@Override
	public void setPoint(int x, int y) {
		points.add(new Pair<Integer, Integer>(x, y));
		this.invalidate();
	}

	@Override
	public void setPoints(List<Pair<Integer, Integer>> points) {
		this.points = points;
		this.invalidate();
	}
}
