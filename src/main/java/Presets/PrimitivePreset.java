package Presets;

public class PrimitivePreset {
	private String type;
	private boolean normalized;
	private String primitive;

	public PrimitivePreset(String type, boolean normalized, String primitive) {
		this.primitive = primitive;
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public boolean isNormalized() {
		return normalized;
	}

	public String getPrimitive() {
		return primitive;
	}

}
