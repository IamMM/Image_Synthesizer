package Presets;

public class DimensionPreset {
	private double minX;
	private double maxX;
	private double minY;
	private double maxY;
	private double minZ;
	private double maxZ;

	public DimensionPreset(double[] dimensions) {
		this.minX = dimensions[0];
		this.maxX = dimensions[1];
		this.minY = dimensions[2];
		this.maxY = dimensions[3];
		this.minZ = dimensions[4];
		this.maxZ = dimensions[5];
	}

	public double getMinX() {
		return minX;
	}

	public double getMaxX() {
		return maxX;
	}

	public double getMinY() {
		return minY;
	}

	public double getMaxY() {
		return maxY;
	}

	public double getMinZ() {
		return minZ;
	}

	public double getMaxZ() {
		return maxZ;
	}
}
