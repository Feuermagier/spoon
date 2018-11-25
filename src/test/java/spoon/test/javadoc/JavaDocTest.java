/**
 * Copyright (C) 2006-2018 INRIA and contributors
 * Spoon - http://spoon.gforge.inria.fr/
 *
 * This software is governed by the CeCILL-C License under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/or redistribute the software under the terms of the CeCILL-C license as
 * circulated by CEA, CNRS and INRIA at http://www.cecill.info.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the CeCILL-C License for more details.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 */
package spoon.test.javadoc;

import org.junit.Test;
import spoon.Launcher;
import spoon.OutputType;
import spoon.SpoonAPI;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtJavaDoc;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.factory.Factory;
import spoon.reflect.visitor.CtScanner;
import spoon.support.reflect.code.CtJavaDocImpl;
import spoon.test.javadoc.testclasses.Bar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static spoon.testing.utils.Check.assertCtElementEquals;

public class JavaDocTest {

	@Test
	public void testJavaDocReprint() {
		SpoonAPI launcher = new Launcher();
		launcher.getEnvironment().setAutoImports(true);
		launcher.getEnvironment().setNoClasspath(true);
		launcher.getEnvironment().setCommentEnabled(true);
		launcher.getEnvironment().setCopyResources(false);
		launcher.addInputResource("./src/test/java/spoon/test/javadoc/testclasses/");
		launcher.setSourceOutputDirectory("./target/spooned/");
		launcher.run();
		Factory factory = launcher.getFactory();
		CtClass<?> aClass = factory.Class().get(Bar.class);

		assertEquals("public class Bar {" + System.lineSeparator()
				+ "    /**" + System.lineSeparator()
				+ "     * Creates an annotation type." + System.lineSeparator()
				+ "     *" + System.lineSeparator()
				+ "     * @param owner" + System.lineSeparator()
				+ "     * \t\tthe package of the annotation type" + System.lineSeparator()
				+ "     * @param simpleName" + System.lineSeparator()
				+ "     * \t\tthe name of annotation" + System.lineSeparator()
				+ "     */" + System.lineSeparator()
				+ "    public <T> CtAnnotationType<?> create(CtPackage owner, String simpleName) {" + System.lineSeparator()
				+ "        return null;" + System.lineSeparator()
				+ "    }" + System.lineSeparator()
				+ "}", aClass.toString());

		// contract: getDocComment never returns null, it returns an empty string if no comment
		assertEquals("", aClass.getDocComment());

		// contract: getDocComment returns the comment content together with the tag content
		CtMethod<?> method = aClass.getMethodsByName("create").get(0);
		assertEquals("Creates an annotation type." + System.lineSeparator() + System.lineSeparator()
				+ "@param owner" + System.lineSeparator()
				+ "\t\tthe package of the annotation type" + System.lineSeparator()
				+ "@param simpleName" + System.lineSeparator()
				+ "\t\tthe name of annotation" + System.lineSeparator()
				, method.getDocComment());

		CtJavaDoc ctJavaDoc = method.getComments().get(0).asJavaDoc();
		assertEquals(2, ctJavaDoc.getTags().size());

		assertEquals(2, ctJavaDoc.clone().getTags().size());
		assertCtElementEquals(ctJavaDoc, ctJavaDoc.clone());

	}

	@Test
	public void testJavadocNotPresentInAST() {
		Launcher launcher = new Launcher();
		launcher.getEnvironment().setCommentEnabled(false);
		launcher.getEnvironment().setNoClasspath(true);
		launcher.getEnvironment().setOutputType(OutputType.NO_OUTPUT);
		launcher.addInputResource("./src/test/java/spoon/test/javadoc/testclasses/");
		launcher.run();

		new CtScanner() {
			@Override
			public void scan(CtElement element) {
				if (element != null) {
					assertEquals(0, element.getComments().size());
				}
				super.scan(element);
			}

			@Override
			public void visitCtComment(CtComment comment) {
				fail("Shouldn't have comment in the model.");
				super.visitCtComment(comment);
			}
		}.scan(launcher.getModel().getRootPackage());
	}

	@Test
	public void testBugSetContent() {
		// contract: call to setContent directly should also set tags.
		CtJavaDoc j = (CtJavaDoc) new Launcher().getFactory().createComment("/** sd\n@see foo */", CtComment.CommentType.JAVADOC);
		assertEquals("sd", j.getLongDescription());
		assertEquals(1, j.getTags().size());
		assertEquals("foo", j.getTags().get(0).getContent());
	}

	}
