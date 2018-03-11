package Presets;

public class ConditionalPreset {
	private String type;
	private boolean normalized;
	private String variables;
	private String condition;
	private String then_statement;
	private String else_statement;

	public ConditionalPreset(String type, boolean normalized, String variables, String condition,
							 String then_statement, String else_statement) {
		this.type = type;
		this.normalized = normalized;
		this.variables = variables;
		this.condition = condition;
		this.then_statement = then_statement;
		this.else_statement = else_statement;
	}

	public String getType() {
		return type;
	}

	public boolean isNormalized() {
		return normalized;
	}

	public String getVariables() {
		return variables;
	}

	public String getCondition() {
		return condition;
	}

	public String getThen_statement() {
		return then_statement;
	}

	public String getElse_statement() {
		return else_statement;
	}
}
