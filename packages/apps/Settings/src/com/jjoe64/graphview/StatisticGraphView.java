/*
 * Copyright (C) 2014 B. Henne, C. Kater,
 *   Distributed Computing & Security Group,
 *   Leibniz Universitaet Hannover, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jjoe64.graphview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.util.AttributeSet;

public class StatisticGraphView extends BarGraphView{
	private boolean midTiles;

	public StatisticGraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public StatisticGraphView(Context context,  String title, boolean midTiles) {
		super(context, title);
		this.midTiles = midTiles;
	}

	@Override
	protected void drawHorizontalLabels(Canvas canvas, float border,
			float horstart, float height, String[] horlabels, float graphwidth) {
		float barwidth = graphwidth/horlabels.length;
		float x = horstart;
		paint.setColor(graphViewStyle.getGridColor());
		canvas.drawLine(x, height - border, x, border, paint);
		x = horstart + graphwidth;
		canvas.drawLine(x, height - border, x, border, paint);
		for (int i = 0; i < horlabels.length; i++) {
			if(midTiles){
            	x = barwidth * ((float) i + 0.5f) + horstart;
        	} else {
            	x = barwidth * i + horstart;
        	}
            if(getShowHorizontalLabels() && i % 4 == 3) {            	
    			paint.setColor(graphViewStyle.getGridColor());
    			canvas.drawLine(x, height - border, x, height - border + 20, paint);

                paint.setColor(graphViewStyle.getHorizontalLabelsColor());
                paint.setTextAlign(Align.RIGHT);
                canvas.save();
                canvas.rotate(-45, x, height - 70);
                canvas.drawText(horlabels[i], x, height - 70, paint);
                canvas.restore();
            } else {
    			paint.setColor(graphViewStyle.getGridColor());
    			canvas.drawLine(x, height - border, x, height - border + 10, paint);
            }
		}
	}
    
    @Override
	public float getBorder() {
		return 90.f;
	}

}
