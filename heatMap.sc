/*

"heatMap" extension to SuperCollider.
(c) 2007-2008 Dan Stowell.
Released under the GPL.

See heatMap.html for examples of usage.

*/
+ SequenceableCollection {

	heatMap { |numChannels, bounds, xLabels=false, yLabels=false, 
				title="Heat map", win, showVals=true, colscheme=\cbr|
		var max, min, ownWin, cols, maptocolindex, maptotextcolour, maptoposition, patches, compview, numCols, width, height, xoff, yoff, patchWidth, patchHeight;
		
		if(numChannels.isNil and:{ this[0].isArray }){
			// Assume a 2D array-of-arrays passed in, treat appropriately
			^this.flatten.heatMap(this[0].size, bounds, xLabels, yLabels, 
							title, win, showVals, colscheme);
		};
		
		if(bounds.isNil){ bounds = Rect(0,0, 400, 400)}; 
		xoff = bounds.left;
		yoff = bounds.top;
		width= bounds.width;
		height=bounds.height;
		
		numCols = (this.size/numChannels).floor;
		
		patchWidth = yLabels.switch(
					false, {width / numCols},
					true,  {width / (numCols+1)},
					       {(width-100) / numCols}).ceil;
		
		patchHeight = xLabels.switch(
					false, {height / numChannels},
						  {(height-10) / (numChannels+1)}).ceil;
		
		max = this.maxItem;
		min = this.minItem;
		
		ownWin = win.isNil;
		if(ownWin){
			// SCWindow
			win = GUI.window.new(title, 
						Rect(100, 400, 
							width,
							height
						)
				);
			win.view.decorator = FlowLayout( win.view.bounds );
		};
		
		colscheme.switch(
			\red,
			{
				// Black as low, red as high
				cols = (0.005, 0.01 .. 1).collect{|hot| Color(hot, 0, 0)};
				maptotextcolour = {|val| if(val.linlin(min, max, -1, 1) < -0.85){Color.grey(0.15)}{Color.black} };
			},
			\brw,
			{
				// Black as low, red as medium, white as high
				cols = (1, 0.99 .. 0).collect{|cold| Color(1-cold, 0, 0)} ++
					  (0.01, 0.02 .. 1).collect{|hot| Color(1, hot, hot)};
				maptotextcolour = {|val| if(val.linlin(min, max, -1, 1) < -0.85){Color.grey(0.15)}{Color.black} };
			},
			\bw,
			{
				// Black as low, white as high
				cols = (1, 0.99 .. 0).collect{|cold| Color(1-cold, 1-cold, 1-cold)};
				maptotextcolour = {|val| if(val.linlin(min, max, -1, 1) < 0){Color.grey(0.2)}{Color.black} };
			},
			\coals,
			{
				// Black->red->yellow->white
				cols = (1, 0.99 .. 0).collect{|cold| Color(1-cold, 0, 0)} ++
					  (1, 0.99 .. 0).collect{|cold| Color(1, 1-cold, 0)} ++
					  (0.01, 0.02 .. 1).collect{|hot| Color(1, 1, hot)};
				maptotextcolour = {|val| if(val.linlin(min, max, -1, 1) < -0.85){Color.grey(0.15)}{Color.black} };
			},
			// \cbr is default
			{
				// Cyan as low, red as high, black as medium
				cols = (1, 0.99 .. 0).collect{|cold| Color(0, cold * 0.1, cold)} ++
					  (0.01, 0.02 .. 1).collect{|hot| Color(hot, 0, 0)};
				maptotextcolour = {|val| if(val.linlin(min, max, -1, 1).abs < 0.15){Color.grey(0.15)}{Color.black} };
			}
		);
		maptocolindex = {|val| (val - min) * cols.lastIndex / (max - min) };
		
		maptoposition = {|index| 
			Rect(patchWidth * (index / numChannels).floor + xoff, 
				patchHeight * (index % numChannels)       + yoff, 
				patchWidth, patchHeight)
		};
		
		
		// The SCCompositeView
		compview = GUI.compositeView.new(win, Rect(100, 400, 
						width, height
						)).resize_(5);
		
		// Now let's create the actual patches of heat!
		// SCStaticText
		patches = this.collect{|val, index|
			GUI.staticText.new(compview, maptoposition.value(index))
					.string_(if(showVals, {val.asFloat.asStringPrec(5)}, ""))
					.background_(cols[maptocolindex.value(val)])
					.stringColor_(maptotextcolour.value(val))
					.resize_(1)
		};
		
		// Autogenerate labels if required
		if(xLabels==true){
			xLabels = (0..numCols - 1)
		};
		if(yLabels==true){
			yLabels = (0..numChannels-1)
		};
		// Now paint the labels if required (whether auto or passed in)
		if(xLabels!=false){
			xLabels.do{|label, index|
				GUI.staticText.new(compview, 
					Rect(patchWidth * index + xoff,
						patchHeight * numChannels + yoff,
						patchWidth,
						patchHeight)
				).string_(label.asString).background_(Color.white)
				.resize_(4)
			};
		};
		if(yLabels!=false){
			yLabels.do{|label, index|
				GUI.staticText.new(compview, 
					Rect(patchWidth * numCols + xoff,
						patchHeight * index + yoff,
						100,
						patchHeight)
				).string_(label.asString).background_(Color.white)
				.resize_(2)
			};
		};
		
		if(ownWin){ win.front };
		
		^compview;
	}
	
}


+ Buffer {
	heatMap { |bounds, xLabels=false, yLabels=false, 
				title="Heat map", win, showVals=false, colscheme=\cbr|
		var gui;
		gui = GUI.current;
		this.loadToFloatArray(action: { |array, buf| 
			{
				GUI.use( gui, {
					array.heatMap(buf.numChannels, bounds, xLabels, yLabels, title, win, showVals, colscheme);
				});
			}.defer;
		});
	}
}
