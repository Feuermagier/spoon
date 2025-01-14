/*
 * SPDX-License-Identifier: (MIT OR CECILL-C)
 *
 * Copyright (C) 2006-2019 INRIA and contributors
 *
 * Spoon is available either under the terms of the MIT License (see LICENSE-MIT.txt) of the Cecill-C License (see LICENSE-CECILL-C.txt). You as the user are entitled to choose the terms under which to adopt Spoon.
 */
package spoon.support.compiler.jdt;

import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.CompilationProgress;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.support.Level;


class TreeBuilderCompiler extends org.eclipse.jdt.internal.compiler.Compiler {

	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private boolean ignoreSyntaxErrors;

	private Level level;

	TreeBuilderCompiler(INameEnvironment environment, IErrorHandlingPolicy policy, CompilerOptions options,
						ICompilerRequestor requestor, IProblemFactory problemFactory, PrintWriter out,
						boolean ignoreSyntaxErrors, Level level, CompilationProgress progress) {
		super(environment, policy, options, requestor, problemFactory, out, progress);
		this.ignoreSyntaxErrors = ignoreSyntaxErrors;
		this.level = level;
	}

	// This code is directly inspired from Compiler class.
	private void sortModuleDeclarationsFirst(ICompilationUnit[] sourceUnits) {
		Arrays.sort(sourceUnits, (u1, u2) -> {
			char[] fn1 = u1.getFileName();
			char[] fn2 = u2.getFileName();
			boolean isMod1 = CharOperation.endsWith(fn1, JDTConstants.MODULE_INFO_FILE_NAME) || CharOperation.endsWith(fn1, JDTConstants.MODULE_INFO_CLASS_NAME);
			boolean isMod2 = CharOperation.endsWith(fn2, JDTConstants.MODULE_INFO_FILE_NAME) || CharOperation.endsWith(fn2, JDTConstants.MODULE_INFO_CLASS_NAME);
			if (isMod1 == isMod2) {
				return 0;
			}
			return isMod1 ? -1 : 1;
		});
	}

	// this method is not meant to be in the public API
	protected CompilationUnitDeclaration[] buildUnits(CompilationUnit[] sourceUnits) {

		// //////////////////////////////////////////////////////////////////////////
		// This code is largely inspired from JDT's
		// CompilationUnitResolver.resolve

		this.reportProgress(Messages.compilation_beginningToCompile);

		this.sortModuleDeclarationsFirst(sourceUnits);

		CompilationUnit[] filteredSourceUnits = null;
		if (ignoreSyntaxErrors || level.toInt() > Level.ERROR.toInt()) {
			// syntax is optionally checked here to prevent crashes inside JDT
			filteredSourceUnits = ignoreSyntaxErrors(sourceUnits);
		}
		// build and record parsed units
		if (ignoreSyntaxErrors) {
			beginToCompile(filteredSourceUnits);
		} else {
			beginToCompile(sourceUnits);
		}

		CompilationUnitDeclaration unit;
		int i = 0;

		// process all units (some more could be injected in the loop by the lookup environment)
		for (; i < this.totalUnits; i++) {
			unit = unitsToProcess[i];
			this.reportProgress(Messages.bind(Messages.compilation_processing, new String(unit.getFileName())));
			this.parser.getMethodBodies(unit);

			// fault in fields & methods
			if (unit.scope != null) {
				unit.scope.faultInTypes();
			}

			// verify inherited methods
			if (unit.scope != null) {
				unit.scope.verifyMethods(lookupEnvironment.methodVerifier());
			}

			// type checking
			unit.resolve();
			// flow analysis
			unit.analyseCode();

			unit.ignoreFurtherInvestigation = false;
			requestor.acceptResult(unit.compilationResult);
			this.reportWorked(1, i);
		}

		ArrayList<CompilationUnitDeclaration> unitsToReturn = new ArrayList<>();
		for (CompilationUnitDeclaration cud : this.unitsToProcess) {
			if (cud != null) {
				unitsToReturn.add(cud);
			}
		}
		return unitsToReturn.toArray(new CompilationUnitDeclaration[0]);
	}

	private CompilationUnit[] ignoreSyntaxErrors(CompilationUnit[] sourceUnits) {
		ArrayList<CompilationUnit> sourceUnitList = new ArrayList<>();
		int maxUnits = sourceUnits.length;
		for (int i = 0; i < maxUnits; i++) {
			CompilationResult unitResult = new CompilationResult(sourceUnits[i], i, maxUnits, this.options.maxProblemsPerUnit);
			CompilationUnitDeclaration parsedUnit = this.parser.parse(sourceUnits[i], unitResult);
			if (parsedUnit.hasErrors()) {
				LOGGER.warn("Syntax error detected in: " + String.valueOf(sourceUnits[i].getFileName()));
			} else {
				sourceUnitList.add(sourceUnits[i]);
			}
		}
		this.initializeParser();
		return sourceUnitList.toArray(new CompilationUnit[0]);
	}
}
