import java.io.Serializable;

public class FunctionPreset implements Serializable {
    private String type;
    private String[] functions;

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
}
