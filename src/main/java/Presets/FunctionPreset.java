package Presets;

public class FunctionPreset {
    private String type;
    private boolean normalized;
    private boolean local;
    private String function;
    private String[] functions;

    public FunctionPreset(String type, boolean normalized, String function) {
        this.type = type;
        this. normalized = normalized;
        this.function = function;
    }

    public FunctionPreset(String type, boolean normalized, String[] functions) {
        this.type = type;
        this.normalized = normalized;
        this.functions = functions;
    }

    public String getType() {
        return type;
    }

    public boolean isNormalized() {
        return normalized;
    }

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public String[] getFunctions() {
        return functions;
    }

    public String getFunction() {
        return function;
    }
}
