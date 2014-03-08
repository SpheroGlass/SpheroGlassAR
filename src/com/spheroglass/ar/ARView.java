package com.spheroglass.ar;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.util.Pair;
import android.view.View;

public class ARView extends View implements CustomCameraView.Listener {

	private Paint p;
	private int color = Color.RED;
	
	private int x = 0;
	private int y = 0;

	private List<Pair<Integer, Integer>> points = new ArrayList<Pair<Integer,Integer>>();

	public ARView(Context context) {
		super(context);

		p = new Paint();
		p.setTextAlign(Align.CENTER);
		p.setTextSize(20);
	}

	public void onDraw(Canvas c) {
		//c.drawColor(Color.BLACK);
		p.setColor(color);
		//c.drawText("X", c.getWidth() / 2 + x, c.getHeight() / 2 - y, p);
		p.setColor(Color.GREEN);
		c.drawText("o", c.getWidth() / 2, c.getHeight() / 2, p);
		p.setColor(Color.YELLOW);
		c.drawLine(0, c.getHeight()/2, c.getWidth(), c.getHeight()/2, p);
		c.drawLine(0, c.getHeight()/2 + 15*360/CustomCameraView.HEIGHT, c.getWidth(), c.getHeight()/2 + 15*360/CustomCameraView.HEIGHT, p);
		c.drawLine(0, c.getHeight()/2 - 15*360/CustomCameraView.HEIGHT, c.getWidth(), c.getHeight()/2 - 15*360/CustomCameraView.HEIGHT, p);
		c.drawLine(0, c.getHeight()/2 + 40*360/CustomCameraView.HEIGHT, c.getWidth(), c.getHeight()/2 + 40*360/CustomCameraView.HEIGHT, p);
		c.drawLine(0, c.getHeight()/2 - 40*360/CustomCameraView.HEIGHT, c.getWidth(), c.getHeight()/2 - 40*360/CustomCameraView.HEIGHT, p);
		p.setColor(Color.RED);
		for(Pair<Integer, Integer> point : points) {
			c.drawText("x", c.getWidth() / 2 + point.first * 640/CustomCameraView.WIDTH, c.getHeight() / 2 - point.second * 360/CustomCameraView.HEIGHT, p);
			c.drawText("x: "+point.first+", y: "+point.second, 50, 50, p);
		}
	}

	@Override
	public void setPoint(int x, int y) {
		this.x = x;
		this.y = y;
		points.add(new Pair<Integer, Integer>(x, y));
		this.invalidate();
	}

	@Override
	public void setPoints(List<Pair<Integer, Integer>> points) {
		this.points = points;
		this.invalidate();
	}
}
