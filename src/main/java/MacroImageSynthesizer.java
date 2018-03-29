import ij.IJ;
import ij.ImagePlus;
import ij.gui.Toolbar;
import ij.macro.Interpreter;
import ij.macro.Program;
import ij.macro.Tokenizer;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.*;

public class MacroImageSynthesizer {

	// Constants
	private static final int PREVIEW_SIZE = 256 ;

	/*--- macro to Image ---*/

	public void macroToImage(ImagePlus imagePlus, double[] min, double[] max, String macro) throws RuntimeException {

		ImageProcessor ip = imagePlus.getProcessor();

		int PCStart = 25;
		Program pgm = (new Tokenizer()).tokenize(macro);
		boolean hasX = pgm.hasWord("x");
		boolean hasZ = pgm.hasWord("z");
		boolean hasA = pgm.hasWord("a");
		boolean hasD = pgm.hasWord("d");
		boolean hasR = pgm.hasWord("r");
		boolean hasG = pgm.hasWord("g");
		boolean hasB = pgm.hasWord("b");
		boolean hasE = pgm.hasWord("E");
		boolean hasGetPixel = pgm.hasWord("getPixel");
		int width = ip.getWidth();
		int height = ip.getHeight();
		String code =
				"var v,r,g,b,x,y,z,w,h,s,d,a,E;\n"+
						"function dummy() {}\n"+
						macro+";\n"; // code starts at program counter location 'PCStart'
		Interpreter interpreter = new Interpreter();
		interpreter.run(code, null);
		if (interpreter.wasError()) return;

		interpreter.setVariable("w", Math.abs(max[0]-min[0]));
		interpreter.setVariable("h", Math.abs(max[1]-min[1]));
		interpreter.setVariable("s", Math.abs(max[2]-min[2]));
		if(hasE) interpreter.setVariable("E", Math.exp(1));

		int bitDepth = ip.getBitDepth();
		Rectangle r = ip.getRoi();
		int inc = r.height/50;
		if (inc<1) inc = 1;
		int slices = imagePlus.getNSlices();
		double v;
		int pos, v2;
		if (bitDepth==8) { // 8-Bit
			for(int z = 0; z < slices; z++) {
				ip = imagePlus.getImageStack().getProcessor(z + 1);

				if (hasZ) {
					double dz = min[2] + ((max[2] - min[2]) / (slices - 1)) * z; // 0..z to min..max
					if (Double.isNaN(dz)) dz = min[2];
					interpreter.setVariable("z", dz);
				}

				byte[] pixels1 = (byte[]) ip.getPixels();
				byte[] pixels2 = pixels1;
				if (hasGetPixel) pixels2 = new byte[width * height];

				for (int y = r.y; y < (r.y + r.height); y++) {
					if (y % inc == 0) IJ.showProgress(y - r.y, r.height);

					double dy = min[1]+((max[1]-min[1])/(height-1))*y; // 0..y to min..max
					interpreter.setVariable("y", dy);

					for (int x = r.x; x < (r.x + r.width); x++) {
						pos = y * width + x;
						v = pixels1[pos] & 255;
						interpreter.setVariable("v", v);

						double dx = min[0]+((max[0]-min[0])/(width-1))*x; // 0..x to min..max
						if (hasX) interpreter.setVariable("x", dx);

						if (hasA) interpreter.setVariable("a", getA(dx, dy));
						if (hasD) interpreter.setVariable("d", Math.hypot(dx,dy));
						interpreter.run(PCStart);
						v2 = (int) interpreter.getVariable("v");
						if (v2 < 0) v2 = 0;
						if (v2 > 255) v2 = 255;
						pixels2[pos] = (byte) v2;
					}
				}
				if (hasGetPixel) System.arraycopy(pixels2, 0, pixels1, 0, width * height);
			}
		} else if (bitDepth==24) { // RGB
			for(int z = 0; z < slices; z++) {
				ip = imagePlus.getImageStack().getProcessor(z + 1);

				if (hasZ) {
					double dz = min[2] + ((max[2] - min[2]) / (slices - 1)) * z; // 0..z to min..max
					if (Double.isNaN(dz)) dz = min[2];
					interpreter.setVariable("z", dz);
				}

				int rgb, red, green, blue;
				int[] pixels1 = (int[]) ip.getPixels();
				int[] pixels2 = pixels1;
				if (hasGetPixel) pixels2 = new int[width * height];

				for (int y = r.y; y < (r.y + r.height); y++) {
					if (y % inc == 0) IJ.showProgress(y - r.y, r.height);

					double dy = min[1]+((max[1]-min[1])/(height-1))*y; // 0..y to min..max
					interpreter.setVariable("y", dy);

					for (int x = r.x; x < (r.x + r.width); x++) {
						double dx = min[0]+((max[0]-min[0])/(width-1))*x; // 0..x to min..max
						if (hasX) interpreter.setVariable("x", dx);

						if (hasA) interpreter.setVariable("a", getA(dx, dy));
						if (hasD) interpreter.setVariable("d", Math.hypot(dx,dy));
						pos = y * width + x;
						rgb = pixels1[pos];
						if (hasGetPixel) {
							interpreter.setVariable("v", rgb);
							interpreter.run(PCStart);
							rgb = (int) interpreter.getVariable("v");
						} else {
							red = (rgb & 0xff0000) >> 16;
							green = (rgb & 0xff00) >> 8;
							blue = rgb & 0xff;
							if(hasR) interpreter.setVariable("r", red);
							else interpreter.setVariable("v", red);
							interpreter.run(PCStart);
							red = (int) interpreter.getVariable(hasR?"r":"v");
							if (red < 0) red = 0;
							if (red > 255) red = 255;
							if(hasG) interpreter.setVariable("g", green);
							else interpreter.setVariable("v", green);
							interpreter.run(PCStart);
							green = (int) interpreter.getVariable(hasG?"g":"v");
							if (green < 0) green = 0;
							if (green > 255) green = 255;
							if(hasB) interpreter.setVariable("b", blue);
							else interpreter.setVariable("v", blue);
							interpreter.run(PCStart);
							blue = (int) interpreter.getVariable(hasB?"b":"v");
							if (blue < 0) blue = 0;
							if (blue > 255) blue = 255;
							rgb = 0xff000000 | ((red & 0xff) << 16) | ((green & 0xff) << 8) | blue & 0xff;
						}
						pixels2[pos] = rgb;
					}
				}
				if (hasGetPixel) System.arraycopy(pixels2, 0, pixels1, 0, width * height);
			}
		} else if (ip.isSigned16Bit()) {
			for(int z = 0; z < slices; z++) {
				ip = imagePlus.getImageStack().getProcessor(z + 1);

				if (hasZ) {
					double dz = min[2] + ((max[2] - min[2]) / (slices - 1)) * z; // 0..z to min..max
					if (Double.isNaN(dz)) dz = min[2];
					interpreter.setVariable("z", dz);
				}

				for (int y = r.y; y < (r.y + r.height); y++) {
					if (y % inc == 0) IJ.showProgress(y - r.y, r.height);

					double dy = min[1]+((max[1]-min[1])/(height-1))*y; // 0..y to min..max
					interpreter.setVariable("y", dy);

					for (int x = r.x; x < (r.x + r.width); x++) {
						double dx = min[0]+((max[0]-min[0])/(width-1))*x; // 0..x to min..max
						if (hasX) interpreter.setVariable("x", dx);

						v = ip.getPixelValue(x, y);
						interpreter.setVariable("v", v);

						if (hasA) interpreter.setVariable("a", getA(dx, dy));
						if (hasD) interpreter.setVariable("d", Math.hypot(dx, dy));
						interpreter.run(PCStart);
						ip.putPixelValue(x, y, interpreter.getVariable("v"));
					}
				}
			}
		} else if (bitDepth==16) {
			for(int z = 0; z < slices; z++) {
				ip = imagePlus.getImageStack().getProcessor(z + 1);

				if (hasZ) {
					double dz = min[2] + ((max[2] - min[2]) / (slices - 1)) * z; // 0..z to min..max
					if (Double.isNaN(dz)) dz = min[2];
					interpreter.setVariable("z", dz);
				}

				short[] pixels1 = (short[]) ip.getPixels();
				short[] pixels2 = pixels1;
				if (hasGetPixel) pixels2 = new short[width * height];

				for (int y = r.y; y < (r.y + r.height); y++) {
					if (y % inc == 0) IJ.showProgress(y - r.y, r.height);

					double dy = min[1]+((max[1]-min[1])/(height-1))*y; // 0..y to min..max
					interpreter.setVariable("y", dy);

					for (int x = r.x; x < (r.x + r.width); x++) {

						double dx = min[0]+((max[0]-min[0])/(width-1))*x; // 0..x to min..max
						if (hasX) interpreter.setVariable("x", dx);

						pos = y * width + x;
						v = pixels1[pos] & 65535;
						interpreter.setVariable("v", v);

						if (hasA) interpreter.setVariable("a", getA(dx, dy));
						if (hasD) interpreter.setVariable("d",Math.hypot(dx,dy));
						interpreter.run(PCStart);
						v2 = (int) interpreter.getVariable("v");
						if (v2 < 0) v2 = 0;
						if (v2 > 65535) v2 = 65535;
						pixels2[pos] = (short) v2;
					}
				}
				if (hasGetPixel) System.arraycopy(pixels2, 0, pixels1, 0, width * height);
			}
		} else {  //32-bit
			for(int z = 0; z < slices; z++) {
				ip = imagePlus.getImageStack().getProcessor(z + 1);

				if (hasZ) {
					double dz = min[2] + ((max[2] - min[2]) / (slices - 1)) * z; // 0..z to min..max
					if (Double.isNaN(dz)) dz = min[2];
					interpreter.setVariable("z", dz);
				}

				float[] pixels1 = (float[])ip.getPixels();
				float[] pixels2 = pixels1;
				if (hasGetPixel) pixels2 = new float[width*height];

				for (int y = r.y; y < (r.y + r.height); y++) {
					if (y % inc == 0) IJ.showProgress(y - r.y, r.height);

					double dy = min[1]+((max[1]-min[1])/(height-1))*y; // 0..y to min..max
					interpreter.setVariable("y", dy);

					for (int x = r.x; x < (r.x + r.width); x++) {
						pos = y * width + x;
						v = pixels1[pos];
						interpreter.setVariable("v", v);

						double dx = min[0]+((max[0]-min[0])/(width-1))*x; // 0..x to min..max
						if (hasX) interpreter.setVariable("x", dx);

						if (hasA) interpreter.setVariable("a", getA(dx, dy));
						if (hasD) interpreter.setVariable("d", Math.hypot(dx,dy));
						interpreter.run(PCStart);
						pixels2[pos] = (float) interpreter.getVariable("v");
					}
				}
				if (hasGetPixel) System.arraycopy(pixels2, 0, pixels1, 0, width*height);
			}
		}
		IJ.showProgress(1.0);
	}

	public void macroToNormalizedImage(ImagePlus imagePlus, double[] min, double[] max, String macro, boolean global) throws RuntimeException {
		ImageProcessor ip = imagePlus.getProcessor();

		int PCStart = 25;
		Program pgm = (new Tokenizer()).tokenize(macro);
		boolean hasX = pgm.hasWord("x");
		boolean hasZ = pgm.hasWord("z");
		boolean hasA = pgm.hasWord("a");
		boolean hasD = pgm.hasWord("d");
		boolean hasR = pgm.hasWord("r");
		boolean hasG = pgm.hasWord("g");
		boolean hasB = pgm.hasWord("b");
		boolean hasE = pgm.hasWord("E");
		int width = ip.getWidth();
		int height = ip.getHeight();
		String code =
				"var v,r,g,b,x,y,z,w,h,s,d,a,E;\n"+
						"function dummy() {}\n"+
						macro+";\n"; // code starts at program counter location 'PCStart'
		Interpreter interpreter = new Interpreter();
		interpreter.run(code, null);
		if (interpreter.wasError()) return;

		interpreter.setVariable("w", Math.abs(max[0]-min[0]));
		interpreter.setVariable("h", Math.abs(max[1]-min[1]));
		interpreter.setVariable("s", Math.abs(max[2]-min[2]));
		if(hasE) interpreter.setVariable("E", Math.exp(1));

		Rectangle r = ip.getRoi();
		int inc = r.height/50;
		if (inc<1) inc = 1;
		int slices = imagePlus.getNSlices();
		int pos, v;
		int bitDepth = imagePlus.getBitDepth();

		if (bitDepth==8) { // 8-Bit
			for(int z = 0; z < slices; z++) {
				ip = imagePlus.getImageStack().getProcessor(z + 1);

				if (hasZ) {
					double dz = min[2] + ((max[2] - min[2]) / (slices - 1)) * z; // 0..z to min..max
					if (Double.isNaN(dz)) dz = min[2];
					interpreter.setVariable("z", dz);
				}

				byte[] pixels = (byte[]) ip.getPixels();
				double[] values = new double[pixels.length];


				for (int y = r.y; y < (r.y + r.height); y++) {
					if (y % inc == 0) IJ.showProgress(y - r.y, r.height);

					double dy = min[1]+((max[1]-min[1])/(height-1))*y; // 0..y to min..max
					interpreter.setVariable("y", dy);

					for (int x = r.x; x < (r.x + r.width); x++) {
						pos = y * width + x;
						v = pixels[pos] & 255;
						interpreter.setVariable("v", v);

						double dx = min[0]+((max[0]-min[0])/(width-1))*x; // 0..x to min..max
						if (hasX) interpreter.setVariable("x", dx);

						if (hasA) interpreter.setVariable("a", getA(dx, dy));
						if (hasD) interpreter.setVariable("d", Math.hypot(dx,dy));
						interpreter.run(PCStart);
						values[pos] = interpreter.getVariable("v");
					}
				}
				FloatProcessor floatProcessor = new FloatProcessor(width, height, values);
				floatProcessor.resetMinAndMax();
				if(slices>1) imagePlus.getStack().setProcessor(floatProcessor.convertToByteProcessor(true), z+1);
				else imagePlus.setProcessor(floatProcessor.convertToByteProcessor(true));
			}
		} else if (bitDepth==16) {
			for(int z = 0; z < slices; z++) {
				ip = imagePlus.getImageStack().getProcessor(z + 1);

				if (hasZ) {
					double dz = min[2] + ((max[2] - min[2]) / (slices - 1)) * z; // 0..z to min..max
					if (Double.isNaN(dz)) dz = min[2];
					interpreter.setVariable("z", dz);
				}

				short[] pixels = (short[]) ip.getPixels();
				double[] values = new double[pixels.length];

				for (int y = r.y; y < (r.y + r.height); y++) {
					if (y % inc == 0) IJ.showProgress(y - r.y, r.height);

					double dy = min[1]+((max[1]-min[1])/(height-1))*y; // 0..y to min..max
					interpreter.setVariable("y", dy);

					for (int x = r.x; x < (r.x + r.width); x++) {

						double dx = min[0]+((max[0]-min[0])/(width-1))*x; // 0..x to min..max
						if (hasX) interpreter.setVariable("x", dx);

						pos = y * width + x;
						v = pixels[pos] & 65535;
						interpreter.setVariable("v", v);

						if (hasA) interpreter.setVariable("a", getA(dx, dy));
						if (hasD) interpreter.setVariable("d",Math.hypot(dx,dy));
						interpreter.run(PCStart);
						values[pos] = interpreter.getVariable("v");
					}
				}
				FloatProcessor floatProcessor = new FloatProcessor(width, height, values);
				floatProcessor.resetMinAndMax();
				if(slices>1) imagePlus.getStack().setProcessor(floatProcessor.convertToShortProcessor(true), z+1);
				else imagePlus.setProcessor(floatProcessor.convertToShortProcessor(true));
			}
		} else if(bitDepth==24) {
			double minimum = Double.MAX_VALUE; // minimum init with greatest possible value
			double maximum = -Double.MAX_VALUE; // maximum init with smallest possible value

			FloatProcessor[] redFloatProcessors = new FloatProcessor[slices];
			FloatProcessor[] greenFloatProcessors = new FloatProcessor[slices];
			FloatProcessor[] blueFloatProcessors = new FloatProcessor[slices];
			for(int z = 0; z < slices; z++) {
				ip = imagePlus.getImageStack().getProcessor(z + 1);

				if (hasZ) {
					double dz = min[2] + ((max[2] - min[2]) / (slices - 1)) * z; // 0..z to min..max
					if (Double.isNaN(dz)) dz = min[2];
					interpreter.setVariable("z", dz);
				}

				int rgb;
				double red, green, blue;
				int[] pixels = (int[]) ip.getPixels();
				double[] redPixels = new double[pixels.length],
						greenPixels = new double[pixels.length],
						bluePixels = new double[pixels.length];

				for (int y = r.y; y < (r.y + r.height); y++) {
					if (y % inc == 0) IJ.showProgress(y - r.y, r.height);

					double dy = min[1] + ((max[1] - min[1]) / (height - 1)) * y; // 0..y to min..max
					interpreter.setVariable("y", dy);

					for (int x = r.x; x < (r.x + r.width); x++) {
						double dx = min[0] + ((max[0] - min[0]) / (width - 1)) * x; // 0..x to min..max
						if (hasX) interpreter.setVariable("x", dx);

						if (hasA) interpreter.setVariable("a",getA(dx, dy));
						if (hasD) interpreter.setVariable("d", Math.hypot(dx,dy));
						pos = y * width + x;
						rgb = pixels[pos];

						red = (rgb & 0xff0000) >> 16;
						green = (rgb & 0xff00) >> 8;
						blue = rgb & 0xff;
						interpreter.setVariable(hasR?"r":"v", red);
						interpreter.run(PCStart);
						redPixels[pos] = interpreter.getVariable(hasR?"r":"v");
						interpreter.setVariable(hasG?"g":"v", green);
						interpreter.run(PCStart);
						greenPixels[pos] = interpreter.getVariable(hasG?"g":"v");
						interpreter.setVariable(hasB?"b":"v", blue);
						interpreter.run(PCStart);
						bluePixels[pos] = interpreter.getVariable(hasB?"b":"v");
					}
				}

				redFloatProcessors[z] = new FloatProcessor(width, height, redPixels);
				greenFloatProcessors[z] = new FloatProcessor(width, height, greenPixels);
				blueFloatProcessors[z] = new FloatProcessor(width, height, bluePixels);

				minimum = Math.min(minimum, redFloatProcessors[z].getMin());
				minimum = Math.min(minimum, greenFloatProcessors[z].getMin());
				minimum = Math.min(minimum, blueFloatProcessors[z].getMin());

				maximum = Math.max(maximum, redFloatProcessors[z].getMax());
				maximum = Math.max(maximum, greenFloatProcessors[z].getMax());
				maximum = Math.max(maximum, blueFloatProcessors[z].getMax());
			}

			// convert float processors to RGB Stack
			for (int z = 0; z < slices; z++) {
				ip = imagePlus.getImageStack().getProcessor(z + 1);

				if(global) redFloatProcessors[z].setMinAndMax(minimum, maximum);
				((ColorProcessor) ip).setChannel(1, redFloatProcessors[z].convertToByteProcessor(true));

				if(global) greenFloatProcessors[z].setMinAndMax(minimum, maximum);
				((ColorProcessor) ip).setChannel(2, greenFloatProcessors[z].convertToByteProcessor(true));

				if(global) blueFloatProcessors[z].setMinAndMax(minimum, maximum);
				((ColorProcessor) ip).setChannel(3, blueFloatProcessors[z].convertToByteProcessor(true));
			}
		}

		IJ.showProgress(1.0);
	}

	private void macroToFrame(ImagePlus imagePlus, double[] min, double[] max, int z, int slices, String macro) throws RuntimeException{
		ImageProcessor ip = imagePlus.getProcessor();

		int PCStart = 25;
		Program pgm = (new Tokenizer()).tokenize(macro);
		boolean hasX = pgm.hasWord("x");
		boolean hasZ = pgm.hasWord("z");
		boolean hasA = pgm.hasWord("a");
		boolean hasD = pgm.hasWord("d");
		boolean hasR = pgm.hasWord("r");
		boolean hasG = pgm.hasWord("g");
		boolean hasB = pgm.hasWord("b");
		boolean hasE = pgm.hasWord("E");
		boolean hasGetPixel = pgm.hasWord("getPixel");
		int width = ip.getWidth();
		int height = ip.getHeight();
		String code =
				"var v,r,g,b,x,y,z,w,h,s,d,a,E;\n"+
						"function dummy() {}\n"+
						macro+";\n"; // code starts at program counter location 'PCStart'
		Interpreter interpreter = new Interpreter();
		interpreter.run(code, null);
		if (interpreter.wasError()) return;

		interpreter.setVariable("w", Math.abs(max[0]-min[0]));
		interpreter.setVariable("h", Math.abs(max[1]-min[1]));
		interpreter.setVariable("s", Math.abs(max[2]-min[2]));
		if(hasE) interpreter.setVariable("E", Math.exp(1));

		int bitDepth = ip.getBitDepth();
		Rectangle r = ip.getRoi();
		int inc = r.height/50;
		if (inc<1) inc = 1;
		double v;
		int pos, v2;
		if (bitDepth==8) { // 8-Bit

			if (hasZ) {
				double dz = min[2] + ((max[2] - min[2]) / (slices - 1)) * z; // 0..z to min..max
				if (Double.isNaN(dz)) dz = min[2];
				interpreter.setVariable("z", dz);
			}

			byte[] pixels1 = (byte[]) ip.getPixels();
			byte[] pixels2 = pixels1;
			if (hasGetPixel) pixels2 = new byte[width * height];

			for (int y = r.y; y < (r.y + r.height); y++) {
				if (y % inc == 0) IJ.showProgress(y - r.y, r.height);

				double dy = min[1]+((max[1]-min[1])/(height-1))*y; // 0..y to min..max
				interpreter.setVariable("y", dy);

				for (int x = r.x; x < (r.x + r.width); x++) {
					pos = y * width + x;
					v = pixels1[pos] & 255;
					interpreter.setVariable("v", v);

					double dx = min[0]+((max[0]-min[0])/(width-1))*x; // 0..x to min..max
					if (hasX) interpreter.setVariable("x", dx);

					if (hasA) interpreter.setVariable("a", getA(dx, dy));
					if (hasD) interpreter.setVariable("d", Math.hypot(dx,dy));
					interpreter.run(PCStart);
					v2 = (int) interpreter.getVariable("v");
					if (v2 < 0) v2 = 0;
					if (v2 > 255) v2 = 255;
					pixels2[pos] = (byte) v2;
				}
			}
			if (hasGetPixel) System.arraycopy(pixels2, 0, pixels1, 0, width * height);
		} else if (bitDepth==24) { // RGB

			if (hasZ) {
				double dz = min[2] + ((max[2] - min[2]) / (slices - 1)) * z; // 0..z to min..max
				if (Double.isNaN(dz)) dz = min[2];
				interpreter.setVariable("z", dz);
			}

			int rgb, red, green, blue;
			int[] pixels1 = (int[]) ip.getPixels();
			int[] pixels2 = pixels1;
			if (hasGetPixel) pixels2 = new int[width * height];

			for (int y = r.y; y < (r.y + r.height); y++) {
				if (y % inc == 0) IJ.showProgress(y - r.y, r.height);

				double dy = min[1]+((max[1]-min[1])/(height-1))*y; // 0..y to min..max
				interpreter.setVariable("y", dy);

				for (int x = r.x; x < (r.x + r.width); x++) {
					double dx = min[0]+((max[0]-min[0])/(width-1))*x; // 0..x to min..max
					if (hasX) interpreter.setVariable("x", dx);

					if (hasA) interpreter.setVariable("a", getA(dx, dy));
					if (hasD) interpreter.setVariable("d", Math.hypot(dx,dy));
					pos = y * width + x;
					rgb = pixels1[pos];
					if (hasGetPixel) {
						interpreter.setVariable("v", rgb);
						interpreter.run(PCStart);
						rgb = (int) interpreter.getVariable("v");
					} else {
						red = (rgb & 0xff0000) >> 16;
						green = (rgb & 0xff00) >> 8;
						blue = rgb & 0xff;
						if(hasR) interpreter.setVariable("r", red);
						else interpreter.setVariable("v", red);
						interpreter.run(PCStart);
						red = (int) interpreter.getVariable(hasR?"r":"v");
						if (red < 0) red = 0;
						if (red > 255) red = 255;
						if(hasG) interpreter.setVariable("g", green);
						else interpreter.setVariable("v", green);
						interpreter.run(PCStart);
						green = (int) interpreter.getVariable(hasG?"g":"v");
						if (green < 0) green = 0;
						if (green > 255) green = 255;
						if(hasB) interpreter.setVariable("b", blue);
						else interpreter.setVariable("v", blue);
						interpreter.run(PCStart);
						blue = (int) interpreter.getVariable(hasB?"b":"v");
						if (blue < 0) blue = 0;
						if (blue > 255) blue = 255;
						rgb = 0xff000000 | ((red & 0xff) << 16) | ((green & 0xff) << 8) | blue & 0xff;
					}
					pixels2[pos] = rgb;
				}
			}
			if (hasGetPixel) System.arraycopy(pixels2, 0, pixels1, 0, width * height);
		} else if (ip.isSigned16Bit()) {

			if (hasZ) {
				double dz = min[2] + ((max[2] - min[2]) / (slices - 1)) * z; // 0..z to min..max
				if (Double.isNaN(dz)) dz = min[2];
				interpreter.setVariable("z", dz);
			}

			for (int y = r.y; y < (r.y + r.height); y++) {
				if (y % inc == 0) IJ.showProgress(y - r.y, r.height);

				double dy = min[1]+((max[1]-min[1])/(height-1))*y; // 0..y to min..max
				interpreter.setVariable("y", dy);

				for (int x = r.x; x < (r.x + r.width); x++) {
					double dx = min[0]+((max[0]-min[0])/(width-1))*x; // 0..x to min..max
					if (hasX) interpreter.setVariable("x", dx);

					v = ip.getPixelValue(x, y);
					interpreter.setVariable("v", v);
					if (hasA) interpreter.setVariable("a", getA(dx, dy));
					if (hasD) interpreter.setVariable("d", Math.hypot(dx, dy));
					interpreter.run(PCStart);
					ip.putPixelValue(x, y, interpreter.getVariable("v"));
				}
			}
		} else if (bitDepth==16) {

			if (hasZ) {
				double dz = min[2] + ((max[2] - min[2]) / (slices - 1)) * z; // 0..z to min..max
				if (Double.isNaN(dz)) dz = min[2];
				interpreter.setVariable("z", dz);
			}

			short[] pixels1 = (short[]) ip.getPixels();
			short[] pixels2 = pixels1;
			if (hasGetPixel) pixels2 = new short[width * height];

			for (int y = r.y; y < (r.y + r.height); y++) {
				if (y % inc == 0) IJ.showProgress(y - r.y, r.height);

				double dy = min[1]+((max[1]-min[1])/(height-1))*y; // 0..y to min..max
				interpreter.setVariable("y", dy);

				for (int x = r.x; x < (r.x + r.width); x++) {

					double dx = min[0]+((max[0]-min[0])/(width-1))*x; // 0..x to min..max
					if (hasX) interpreter.setVariable("x", dx);

					pos = y * width + x;
					v = pixels1[pos] & 65535;
					interpreter.setVariable("v", v);

					if (hasA) interpreter.setVariable("a", getA(dx, dy));
					if (hasD) interpreter.setVariable("d",Math.hypot(dx,dy));
					interpreter.run(PCStart);
					v2 = (int) interpreter.getVariable("v");
					if (v2 < 0) v2 = 0;
					if (v2 > 65535) v2 = 65535;
					pixels2[pos] = (short) v2;
				}
			}
			if (hasGetPixel) System.arraycopy(pixels2, 0, pixels1, 0, width * height);
		} else {  //32-bit

			if (hasZ) {
				double dz = min[2] + ((max[2] - min[2]) / (slices - 1)) * z; // 0..z to min..max
				if (Double.isNaN(dz)) dz = min[2];
				interpreter.setVariable("z", dz);
			}

			float[] pixels1 = (float[])ip.getPixels();
			float[] pixels2 = pixels1;
			if (hasGetPixel) pixels2 = new float[width*height];

			for (int y = r.y; y < (r.y + r.height); y++) {
				if (y % inc == 0) IJ.showProgress(y - r.y, r.height);

				double dy = min[1]+((max[1]-min[1])/(height-1))*y; // 0..y to min..max
				interpreter.setVariable("y", dy);

				for (int x = r.x; x < (r.x + r.width); x++) {
					pos = y * width + x;
					v = pixels1[pos];
					interpreter.setVariable("v", v);

					double dx = min[0]+((max[0]-min[0])/(width-1))*x; // 0..x to min..max
					if (hasX) interpreter.setVariable("x", dx);

					if (hasA) interpreter.setVariable("a", getA(dx, dy));
					if (hasD) interpreter.setVariable("d", Math.hypot(dx,dy));
					interpreter.run(PCStart);
					pixels2[pos] = (float) interpreter.getVariable("v");
				}
			}
			if (hasGetPixel) System.arraycopy(pixels2, 0, pixels1, 0, width*height);
		}
		IJ.showProgress(1.0);
	}

	private void macroToNormalizedFrame(ImagePlus imagePlus, double[] min, double[] max, int z, int slices, String macro, boolean global) throws RuntimeException {
		ImageProcessor ip = imagePlus.getProcessor();

		int PCStart = 25;
		Program pgm = (new Tokenizer()).tokenize(macro);
		boolean hasX = pgm.hasWord("x");
		boolean hasZ = pgm.hasWord("z");
		boolean hasA = pgm.hasWord("a");
		boolean hasD = pgm.hasWord("d");
		boolean hasR = pgm.hasWord("r");
		boolean hasG = pgm.hasWord("g");
		boolean hasB = pgm.hasWord("b");
		boolean hasE = pgm.hasWord("E");
		int width = ip.getWidth();
		int height = ip.getHeight();
		String code =
				"var v,r,g,b,x,y,z,w,h,s,d,a,E;\n"+
						"function dummy() {}\n"+
						macro+";\n"; // code starts at program counter location 'PCStart'
		Interpreter interpreter = new Interpreter();
		interpreter.run(code, null);
		if (interpreter.wasError()) return;

		interpreter.setVariable("w", Math.abs(max[0]-min[0]));
		interpreter.setVariable("h", Math.abs(max[1]-min[1]));
		interpreter.setVariable("s", Math.abs(max[2]-min[2]));
		if(hasE) interpreter.setVariable("E", Math.exp(1));

		Rectangle r = ip.getRoi();
		int inc = r.height/50;
		if (inc<1) inc = 1;
		int pos, v;
		int bitDepth = imagePlus.getBitDepth();

		if (bitDepth==8) { // 8-Bit

			if (hasZ) {
				double dz = min[2] + ((max[2] - min[2]) / (slices - 1)) * z; // 0..z to min..max
				if (Double.isNaN(dz)) dz = min[2];
				interpreter.setVariable("z", dz);
			}

			byte[] pixels = (byte[]) ip.getPixels();
			double[] values = new double[pixels.length];


			for (int y = r.y; y < (r.y + r.height); y++) {
				if (y % inc == 0) IJ.showProgress(y - r.y, r.height);

				double dy = min[1]+((max[1]-min[1])/(height-1))*y; // 0..y to min..max
				interpreter.setVariable("y", dy);

				for (int x = r.x; x < (r.x + r.width); x++) {
					pos = y * width + x;
					v = pixels[pos] & 255;
					interpreter.setVariable("v", v);

					double dx = min[0]+((max[0]-min[0])/(width-1))*x; // 0..x to min..max
					if (hasX) interpreter.setVariable("x", dx);

					if (hasA) interpreter.setVariable("a", getA(dx, dy));
					if (hasD) interpreter.setVariable("d", Math.hypot(dx,dy));
					interpreter.run(PCStart);
					values[pos] = interpreter.getVariable("v");
				}
			}
			FloatProcessor floatProcessor = new FloatProcessor(width, height, values);
			floatProcessor.resetMinAndMax();
			if(imagePlus.isInvertedLut()) floatProcessor.invert();
			imagePlus.setProcessor(floatProcessor.convertToByteProcessor(true));
		} else if (bitDepth==16) {

			if (hasZ) {
				double dz = min[2] + ((max[2] - min[2]) / (slices - 1)) * z; // 0..z to min..max
				if (Double.isNaN(dz)) dz = min[2];
				interpreter.setVariable("z", dz);
			}

			short[] pixels = (short[]) ip.getPixels();
			double[] values = new double[pixels.length];

			for (int y = r.y; y < (r.y + r.height); y++) {
				if (y % inc == 0) IJ.showProgress(y - r.y, r.height);

				double dy = min[1]+((max[1]-min[1])/(height-1))*y; // 0..y to min..max
				interpreter.setVariable("y", dy);

				for (int x = r.x; x < (r.x + r.width); x++) {

					double dx = min[0]+((max[0]-min[0])/(width-1))*x; // 0..x to min..max
					if (hasX) interpreter.setVariable("x", dx);

					pos = y * width + x;
					v = pixels[pos] & 65535;
					interpreter.setVariable("v", v);

					if (hasA) interpreter.setVariable("a", getA(dx, dy));
					if (hasD) interpreter.setVariable("d",Math.hypot(dx,dy));
					interpreter.run(PCStart);
					values[pos] = interpreter.getVariable("v");
				}
			}
			FloatProcessor floatProcessor = new FloatProcessor(width, height, values);
			floatProcessor.resetMinAndMax();
			if(imagePlus.isInvertedLut()) floatProcessor.invert();
			imagePlus.setProcessor(floatProcessor.convertToShortProcessor(true));
		} else if(bitDepth==24) {
			if (hasZ) {
				double dz = min[2] + ((max[2] - min[2]) / (slices - 1)) * z; // 0..z to min..max
				if (Double.isNaN(dz)) dz = min[2];
				interpreter.setVariable("z", dz);
			}

			int rgb;
			double red, green, blue;
			int[] pixels = (int[]) ip.getPixels();
			double[] redPixels = new double[pixels.length],
					greenPixels = new double[pixels.length],
					bluePixels = new double[pixels.length];

			for (int y = r.y; y < (r.y + r.height); y++) {
				if (y % inc == 0) IJ.showProgress(y - r.y, r.height);

				double dy = min[1]+((max[1]-min[1])/(height-1))*y; // 0..y to min..max
				interpreter.setVariable("y", dy);

				for (int x = r.x; x < (r.x + r.width); x++) {
					double dx = min[0]+((max[0]-min[0])/(width-1))*x; // 0..x to min..max
					if (hasX) interpreter.setVariable("x", dx);

					if (hasA) interpreter.setVariable("a",getA(dx, dy));
					if (hasD) interpreter.setVariable("d", Math.hypot(dx,dy));
					pos = y * width + x;
					rgb = pixels[pos];

					red = (rgb & 0xff0000) >> 16;
					green = (rgb & 0xff00) >> 8;
					blue = rgb & 0xff;
					interpreter.setVariable(hasR?"r":"v", red);
					interpreter.run(PCStart);
					redPixels[pos] = interpreter.getVariable(hasR?"r":"v");
					interpreter.setVariable(hasG?"g":"v", green);
					interpreter.run(PCStart);
					greenPixels[pos] = interpreter.getVariable(hasG?"g":"v");
					interpreter.setVariable(hasB?"b":"v", blue);
					interpreter.run(PCStart);
					bluePixels[pos] = interpreter.getVariable(hasB?"b":"v");
				}
			}

			FloatProcessor redImageProcessor = new FloatProcessor(width, height, redPixels);
			FloatProcessor greenImageProcessor = new FloatProcessor(width, height, greenPixels);
			FloatProcessor blueImageProcessor = new FloatProcessor(width, height, bluePixels);

			if(global) {
				double minimum = Double.MAX_VALUE;
				double maximum = -Double.MAX_VALUE;

				minimum = Math.min(minimum, redImageProcessor.getMin());
				minimum = Math.min(minimum, greenImageProcessor.getMin());
				minimum = Math.min(minimum, blueImageProcessor.getMin());

				maximum = Math.max(maximum, redImageProcessor.getMax());
				maximum = Math.max(maximum, greenImageProcessor.getMax());
				maximum = Math.max(maximum, blueImageProcessor.getMax());

				redImageProcessor.setMinAndMax(minimum, maximum);
				greenImageProcessor.setMinAndMax(minimum, maximum);
				blueImageProcessor.setMinAndMax(minimum, maximum);
			}

			((ColorProcessor)ip).setChannel(1, redImageProcessor.convertToByteProcessor(true));
			((ColorProcessor)ip).setChannel(2, greenImageProcessor.convertToByteProcessor(true));
			((ColorProcessor)ip).setChannel(3, blueImageProcessor.convertToByteProcessor(true));
		}
		IJ.showProgress(1.0);
	}

	private double getA(double x, double y) {
		double angle = Math.atan2(y, x);
		if (angle < 0) angle += 2 * Math.PI;
		return angle;
	}

	/*--- PREVIEW ---*/

	public Image getPreview(ImagePlus imagePlus, double[] min, double[] max, int frame, String macro, boolean drawAxes, boolean normalize, boolean global, int interpolate) {

		ImageProcessor resized = downsize(imagePlus, frame, PREVIEW_SIZE, interpolate);
		ImagePlus preview = new ImagePlus("preview", resized);
		if(normalize) macroToNormalizedFrame(preview, min, max, frame-1, imagePlus.getNSlices(), macro, global);
		else macroToFrame(preview, min, max, frame-1, imagePlus.getNSlices(), macro);
		resized.resetMinAndMax();

		// interpolate if to small
		enlarge(preview, PREVIEW_SIZE, interpolate);

		ImageProcessor colorProcessor = preview.getProcessor().convertToColorProcessor();
		if(drawAxes) {
			drawAxes(colorProcessor, min, max);
			preview.setProcessor(colorProcessor);
		}
		return preview.getImage();
	}

	private ImageProcessor downsize(ImagePlus imagePlus, int frame, int maxSize, int interpolate) {
		int width = imagePlus.getWidth();
		int height = imagePlus.getHeight();

		// reduce size if to big
		if(width> maxSize) {
			height = height * maxSize / width;
			height = height<1 ? 1 : height;
			width = maxSize;
		}

		if(height> maxSize) {
			width = width * maxSize / height;
			width = width<1 ? 1 : width;
			height = maxSize;
		}
		ImageProcessor ip = imagePlus.getStack().getProcessor(frame);
		ip.setInterpolationMethod(interpolate);
		return ip.resize(width, height);
	}

	private void enlarge(ImagePlus imagePlus, int minSize, int interpolate) {
		int width = imagePlus.getWidth();
		int height = imagePlus.getHeight();

		if(width<minSize && height<minSize) {
			if(width<minSize && height<width) {
				height = height*minSize/width;
				width = minSize;
			} else if(height<minSize) {
				width = width*minSize/height;
				height = minSize;
			}
			ImageProcessor ip = imagePlus.getProcessor();
			ip.setInterpolationMethod(interpolate);
			ImageProcessor resized = ip.resize(width, height);
			imagePlus.setProcessor(resized);
		}
	}

	private void drawAxes(ImageProcessor colorProcessor, double[] min, double[] max) {
		int width = colorProcessor.getWidth();
		int height = colorProcessor.getHeight();
		colorProcessor.setColor(Toolbar.getForegroundColor());

		// x axis
		int xAxisPos = (int) ((-min[0]*(width-1))/(max[0]-min[0]));
		xAxisPos = xAxisPos==height?xAxisPos-1:xAxisPos;
		colorProcessor.drawLine(xAxisPos,0,xAxisPos,height);

		// y axis
		int yAxisPos = (int) ((-min[1]*(height-1))/(max[1]-min[1]));
		yAxisPos = yAxisPos==width?yAxisPos-1:yAxisPos;
		colorProcessor.drawLine(0,yAxisPos,width,yAxisPos);
	}
}
