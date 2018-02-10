import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.macro.Interpreter;
import ij.macro.Program;
import ij.macro.Tokenizer;
import ij.plugin.filter.ImageMath;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.*;

public class PrimitiveImageSynthesizer {

	public void primitiveToImageOLD(ImagePlus imagePlus, double[] min, double[] max, String macro) throws RuntimeException {

		ImageProcessor ip = imagePlus.getProcessor();

		macro="code=v=sin(x)";

		int PCStart = 23;
		int width = ip.getWidth();
		int height = ip.getHeight();
		int slices = imagePlus.getNSlices();

		String code =
				"var v,x,y,z,w,h;\n"+
						"function dummy() {}\n"+
						macro+";\n";
		Interpreter interpreter = new Interpreter();
		interpreter.run(code);
		if (interpreter.wasError()) return;

		interpreter.setVariable("w", width);
		interpreter.setVariable("h", height);

		int pos;
		float v;

		for(int z = 0; z < slices; z++) {
			ip = imagePlus.getProcessor();

			double dz = min[2] + ((max[2] - min[2]) / (slices - 1)) * z; // 0..z to min..max
			if (Double.isNaN(dz)) dz = min[2];
			interpreter.setVariable("z", dz);

			float[] pixels = (float[])ip.getPixels();

			for (int y = 0; y < height; y++) {

				double dy = min[1]+((max[1]-min[1])/(height-1))*y; // 0..y to min..max
				interpreter.setVariable("y", dy);

				for (int x = 0; x < width; x++) {
					pos = y * width + x;
					v = pixels[pos];
					interpreter.setVariable("v", v);

					double dx = min[0]+((max[0]-min[0])/(width-1))*x; // 0..x to min..max
					interpreter.setVariable("x", dx);

					interpreter.run(PCStart);
					pixels[pos] = (float) interpreter.getVariable("v");
				}
			}
			ip.setPixels(pixels);
		}
	}

	public void primitiveToImage(ImagePlus imagePlus, double[] min, double[] max, String macro) throws RuntimeException {

		ImageProcessor ip = imagePlus.getProcessor();

		int PCStart = 23;
		Program pgm = (new Tokenizer()).tokenize(macro);
		boolean hasX = pgm.hasWord("x");
		boolean hasZ = pgm.hasWord("z");
		boolean hasA = pgm.hasWord("a");
		boolean hasD = pgm.hasWord("d");
		boolean hasGetPixel = pgm.hasWord("getPixel");
		int width = ip.getWidth();
		int height = ip.getHeight();
		String code =
				"var v,x,y,z,w,h,d,a;\n"+
						"function dummy() {}\n"+
						macro+";\n"; // code starts at program counter location 'PCStart'
		Interpreter interpreter = new Interpreter();
		interpreter.run(code, null);
		if (interpreter.wasError()) return;

		interpreter.setVariable("w", width);
		interpreter.setVariable("h", height);
		interpreter.setVariable("z", ip.getSliceNumber()-1);
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
							interpreter.setVariable("v", red);
							interpreter.run(PCStart);
							red = (int) interpreter.getVariable("v");
							if (red < 0) red = 0;
							if (red > 255) red = 255;
							interpreter.setVariable("v", green);
							interpreter.run(PCStart);
							green = (int) interpreter.getVariable("v");
							if (green < 0) green = 0;
							if (green > 255) green = 255;
							interpreter.setVariable("v", blue);
							interpreter.run(PCStart);
							blue = (int) interpreter.getVariable("v");
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
}
