package su.softcom.cldt.testing.gtest.ui.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.SymbolKind;

import su.softcom.cldt.core.source.ISourceElement;
import su.softcom.cldt.core.source.ISourceFile;

/**
 * Контейнер данных для анализа заголовочного файла.
 * Предлагает структурированное представление о содержимом заголовочного файла.
 */
public final class HeaderData {
	private ISourceFile sourceFile;
	private List<ISourceElement> allElements;
	private Map<String, List<ISourceElement>> classMethods = new HashMap<>();
	private List<ISourceElement> globalFunctions = new ArrayList<>();
	private Map<String, List<ISourceElement>> namespaceFunctions = new HashMap<>();

	/**
	 * Создает контейнер данных для указанного исходного файла.
	 * @param sourceFile анализируемый заголовочный файл
	 */
	public HeaderData(ISourceFile sourceFile) {
		this.sourceFile = sourceFile;
	}

	/**
     * Возвращает исходный файл, связанный с данными.
     * 
     * @return исходный файл, никогда не {@code null}
     */
	public ISourceFile getSourceFile() {
		return sourceFile;
	}

	/**
     * Возвращает все элементы файла в виде плоского списка.
     * <p>
     * Список включает все элементы независимо от их типа и вложенности.
     * </p>
     * 
     * @return неизменяемый список всех элементов, может быть {@code null}
     *         до вызова {@link #addElements(List)}
     */
	public List<ISourceElement> getAllElements() {
		return allElements;
	}

	/**
     * Возвращает методы, сгруппированные по полным именам классов.
     * 
     * @return неизменяемый словарь "имя класса → список методов"
     */
	public Map<String, List<ISourceElement>> getClassMethods() {
		return classMethods;
	}

	/**
     * Возвращает функции вне пространств имён.
     * @return неизменяемый список глобальных функций
     */
	public List<ISourceElement> getGlobalFunctions() {
		return globalFunctions;
	}

	/**
     * Проверяет, содержит ли файл объявления классов или структур.
     * 
     * @return {@code true} если есть хотя бы один класс или структура,
     *         {@code false} в противном случае
     */
	public boolean hasClasses() {
		return !classMethods.isEmpty();
	}

	/**
     * Проверяет, содержит ли файл глобальные функции.
     * 
     * @return {@code true} если есть хотя бы одна глобальная функция,
     *         {@code false} в противном случае
     */
	public boolean hasFunctions() {
		return !globalFunctions.isEmpty();
	}

	/**
     * Добавляет и обрабатывает элементы исходного файла.
     * @param elements список элементов для обработки
     */
	public void addElements(List<ISourceElement> elements) {
		this.allElements = elements;
		processElementsRecursive(elements, "");
	}

	
	/**
     * Возвращает функции, сгруппированные по пространствам имён.
     * @return неизменяемый словарь "пространство имён → список функций"
     */
	public Map<String, List<ISourceElement>> getNamespaceFunctions() {
		return namespaceFunctions;
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
	
	private boolean isMethod(SymbolKind kind) {
		return kind == SymbolKind.Method || kind == SymbolKind.Function || kind == SymbolKind.Constructor;
	}
}