package spoon.javadoc.external.elements;

public class JavadocText implements JavadocElement {
	private final String rawFragment;

	public JavadocText(String rawFragment) {
		this.rawFragment = rawFragment;
	}
}
