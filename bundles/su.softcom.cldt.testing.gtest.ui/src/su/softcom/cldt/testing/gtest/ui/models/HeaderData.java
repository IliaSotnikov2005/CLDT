package su.softcom.cldt.testing.gtest.ui.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.SymbolKind;

import su.softcom.cldt.core.source.ISourceElement;
import su.softcom.cldt.core.source.ISourceFile;

public final class HeaderData {
	private ISourceFile sourceFile;
	private List<ISourceElement> allElements;
	private Map<String, List<ISourceElement>> classMethods = new HashMap<>();
	private List<ISourceElement> globalFunctions = new ArrayList<>();
	private Map<String, List<ISourceElement>> namespaceFunctions = new HashMap<>();

	public HeaderData(ISourceFile sourceFile) {
		this.sourceFile = sourceFile;
	}

	public ISourceFile getSourceFile() {
		return sourceFile;
	}

	public List<ISourceElement> getAllElements() {
		return allElements;
	}

	public Map<String, List<ISourceElement>> getClassMethods() {
		return classMethods;
	}

	public List<ISourceElement> getGlobalFunctions() {
		return globalFunctions;
	}

	public boolean hasClasses() {
		return !classMethods.isEmpty();
	}

	public boolean hasFunctions() {
		return !globalFunctions.isEmpty();
	}

	public void addElements(List<ISourceElement> elements) {
		this.allElements = elements;
		processElementsRecursive(elements, "");
	}

	private void processElementsRecursive(List<ISourceElement> elements, String currentNamespace) {
		for (ISourceElement element : elements) {
			switch (element.getSymbolKind()) {
			case Namespace:
				String newNamespace = currentNamespace.isEmpty() ? element.getName()
						: currentNamespace + "::" + element.getName();
				processElementsRecursive(element.getElements(), newNamespace);
				break;

			case Class, Struct:
				String fullClassName = currentNamespace.isEmpty() ? element.getName()
						: currentNamespace + "::" + element.getName();

				List<ISourceElement> methods = element.getElements().stream().filter(e -> isMethod(e.getSymbolKind()))
						.toList();

				classMethods.put(fullClassName, methods);
				break;

			case Function:
				if (!currentNamespace.isEmpty()) {
					List<ISourceElement> funcs = namespaceFunctions.getOrDefault(currentNamespace, new ArrayList<>());
					funcs.add(element);
					namespaceFunctions.put(currentNamespace, funcs);
				} else {
					globalFunctions.add(element);
				}

				break;

			default:
				break;
			}
		}
	}

	public Map<String, List<ISourceElement>> getNamespaceFunctions() {
		return namespaceFunctions;
	}

	private boolean isMethod(SymbolKind kind) {
		return kind == SymbolKind.Method || kind == SymbolKind.Function || kind == SymbolKind.Constructor;
	}
}