package Presets;

public class FunctionPreset {
    private String type;
    private String function;
    private String[] functions;

    public FunctionPreset(String type, String function) {
        this.type = type;
        this.function = function;
    }

    public FunctionPreset(String type, String[] functions) {
        this.type = type;
        this.functions = functions;
    }

    public String getType() {
        return type;
    }

    public String[] getFunctions() {
        return functions;
    }

    public String getFunction() {
        return function;
    }
}
