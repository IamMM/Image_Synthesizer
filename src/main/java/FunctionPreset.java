import java.io.Serializable;

public class FunctionPreset implements Serializable {
    private String type;
    private String function;
    private String[] functions;

    FunctionPreset(String type, String function) {
        this.type = type;
        this.function = function;
    }

    FunctionPreset(String type, String[] functions) {
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
