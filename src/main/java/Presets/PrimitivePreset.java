package Presets;

public class PrimitivePreset {
	private String primitive;
	private String type;

	public PrimitivePreset(String primitive, String type) {
		this.primitive = primitive;
		this.type = type;
	}

	public String getPrimitive() {
		return primitive;
	}

	public String getType() {
		return type;
	}
}
