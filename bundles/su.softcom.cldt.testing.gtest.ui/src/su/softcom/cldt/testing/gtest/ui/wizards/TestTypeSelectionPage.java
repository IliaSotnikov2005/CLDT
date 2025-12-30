package su.softcom.cldt.testing.gtest.ui.wizards;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import su.softcom.cldt.core.CMakeCorePlugin;
import su.softcom.cldt.core.cmake.ICMakeProject;
import su.softcom.cldt.core.source.ISourceElement;
import su.softcom.cldt.core.source.ISourceFile;
import su.softcom.cldt.core.source.SourceFile;
import su.softcom.cldt.testing.gtest.ui.models.HeaderData;
import su.softcom.cldt.testing.gtest.ui.wizards.TestFileWizard.TestConfiguration;

/**
 * Страница с параметрами генерируемого теста.
 */
public class TestTypeSelectionPage extends WizardPage {

	enum TestType {
		/**
		 * Пустой тест.
		 */
		EMPTY,

		/**
		 * Тестовый файл на основе заголовочного файла.
		 */
		HEADER_BASED
	}

	private Button emptyTestRadio;
	private Button headerBasedTestRadio;
	private TestType selectedTestType = TestType.EMPTY;

	private Composite emptyTestGroup;
	private Text testNameText;

	private Composite headerTestGroup;
	private Combo headerCombo;
	private Combo classCombo;
	private CheckboxTableViewer methodsViewer;
	private Button fixtureCheckbox;
	private Button browseButton;
	private Text headerTestNameText;

	private ICMakeProject cmakeProject;
	private Map<String, HeaderData> headers = new HashMap<>();
	private List<String> availableHeaderNames = new ArrayList<>();

	private static final String NO_HEADERS_MESSAGE = "Не найдены заголовочные файлы";
	private static final String NO_OBJECT_MESSAGE = "Не найдены";
	private static final String LOADING = "Загрузка...";

	/**
	 * Создаёт новую страницу настроек генерации теста.
	 * 
	 * @param project проект для которого генерируется тест.
	 */
	public TestTypeSelectionPage(IProject project) {
		super("Создание теста");
		setTitle("Создание тестового файла");
		setDescription("Выберите тип теста и настройте параметры");

		if (project != null) {
			cmakeProject = CMakeCorePlugin.getDefault().getProject(project);
		}
	}

	/**
	 * Получает выбранный тип теста.
	 * 
	 * @return
	 */
	public TestType getSelectedTestType() {
		return selectedTestType;
	}

	/**
	 * Получает конфигурацию теста.
	 * 
	 * @return конфигурация теста.
	 */
	public TestConfiguration getTestConfiguration() {
		if (selectedTestType == TestType.EMPTY) {
			return new TestConfiguration(testNameText.getText(), "", new ArrayList<>(), false, "", "");
		}

		String selectedItem = classCombo.getText();
		String className = "";
		String namespace = "";

		if (selectedItem.endsWith("::[Функции]")) {
			namespace = selectedItem.replace("::[Функции]", "");
		} else if (!selectedItem.equals("[Глобальные функции]") && !selectedItem.equals(NO_OBJECT_MESSAGE)
				&& !selectedItem.equals(LOADING) && !selectedItem.isEmpty()) {
			if (selectedItem.contains("::")) {
				int lastColon = selectedItem.lastIndexOf("::");
				namespace = selectedItem.substring(0, lastColon);
				className = selectedItem.substring(lastColon + 2);
			} else {
				className = selectedItem;
			}
		}

		String headerRelativePath = ".." + File.separator + getHeaderIncludePath(headerCombo.getText());
		List<String> methods = getSelectedMethods();
		String testName = headerTestNameText.getText().trim();
		boolean isFixtureNeeded = fixtureCheckbox.getSelection();

		return new TestConfiguration(testName, className, methods, isFixtureNeeded, headerRelativePath, namespace);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		container.setLayout(new GridLayout(1, false));

		Group typeSelectionGroup = new Group(container, SWT.NONE);
		typeSelectionGroup.setText("Тип теста");
		typeSelectionGroup.setLayout(new GridLayout(2, false));
		typeSelectionGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		emptyTestRadio = new Button(typeSelectionGroup, SWT.RADIO);
		emptyTestRadio.setText("Пустой тестовый файл");
		emptyTestRadio.setSelection(true);
		emptyTestRadio.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));

		headerBasedTestRadio = new Button(typeSelectionGroup, SWT.RADIO);
		headerBasedTestRadio.setText("Сгенерировать тесты по заголовку/классу");
		headerBasedTestRadio.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));

		emptyTestGroup = createEmptyTestGroup(container);
		headerTestGroup = createHeaderTestGroup(container);

		emptyTestGroup.setVisible(true);
		headerTestGroup.setVisible(false);

		emptyTestRadio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (emptyTestRadio.getSelection()) {
					selectedTestType = TestType.EMPTY;
					emptyTestGroup.setVisible(true);
					headerTestGroup.setVisible(false);

					setHeaderControlsEnabled(false);

					updatePageSize();
					container.layout();
					validatePage();
				}
			}
		});

		headerBasedTestRadio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (headerBasedTestRadio.getSelection()) {
					selectedTestType = TestType.HEADER_BASED;
					emptyTestGroup.setVisible(false);
					headerTestGroup.setVisible(true);

					setHeaderControlsEnabled(true);

					if (availableHeaderNames.isEmpty() && cmakeProject != null) {
						loadAvailableHeaders();
					}

					updatePageSize();
					container.layout();
					validatePage();
				}
			}
		});

		validatePage();
	}

	private Composite createEmptyTestGroup(Composite parent) {
		Composite group = new Composite(parent, SWT.NONE);
		group.setLayout(new GridLayout(2, false));
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.exclude = false;
		group.setLayoutData(gridData);

		Label testNameLabel = new Label(group, SWT.NONE);
		testNameLabel.setText("Имя теста:");
		testNameLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		testNameText = new Text(group, SWT.BORDER);
		testNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		testNameText.setText("simple_test");
		testNameText.addModifyListener(e -> validatePage());

		return group;
	}

	private Composite createHeaderTestGroup(Composite parent) {
		Composite group = new Composite(parent, SWT.NONE);
		group.setLayout(new GridLayout(4, false));
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.exclude = true;
		group.setLayoutData(gridData);

		fixtureCheckbox = new Button(group, SWT.CHECK);
		fixtureCheckbox.setText("Создать тестовое окружение");
		fixtureCheckbox.setSelection(false);
		fixtureCheckbox.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 4, 1));
		fixtureCheckbox.setToolTipText("Создает класс с методами SetUp() и TearDown() для настройки тестовой среды");
		fixtureCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				validatePage();
			}
		});

		Label headerLabel = new Label(group, SWT.NONE);
		headerLabel.setText("Заголовочный файл:");
		headerLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		headerCombo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
		headerCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		headerCombo.addModifyListener(e -> onHeaderSelected());
		headerCombo.setEnabled(false);

		browseButton = new Button(group, SWT.PUSH);
		browseButton.setText("Обзор...");
		browseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseForHeaderFile();
			}
		});
		browseButton.setEnabled(false);

		Label classLabel = new Label(group, SWT.NONE);
		classLabel.setText("Объект тестирования:");
		classLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		classCombo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
		classCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		classCombo.addModifyListener(e -> onClassSelected());
		classCombo.setEnabled(false);

		Label methodsLabel = new Label(group, SWT.NONE);
		methodsLabel.setText("Выбранные методы/функции:");
		methodsLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 4, 1));

		methodsViewer = CheckboxTableViewer.newCheckList(group, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
		Table methodsTable = methodsViewer.getTable();
		GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
		tableData.heightHint = 150;
		methodsTable.setLayoutData(tableData);
		methodsTable.setEnabled(false);

		methodsViewer.setContentProvider(ArrayContentProvider.getInstance());
		methodsViewer.setLabelProvider(new LabelProvider());
		methodsViewer.addCheckStateListener(event -> validatePage());

		Composite methodButtonsPanel = new Composite(group, SWT.NONE);
		methodButtonsPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		methodButtonsPanel.setLayout(new GridLayout(1, true));

		Button selectAllButton = new Button(methodButtonsPanel, SWT.PUSH);
		selectAllButton.setText("Выбрать все");
		selectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		selectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectAllMethods();
			}
		});

		Button deselectAllButton = new Button(methodButtonsPanel, SWT.PUSH);
		deselectAllButton.setText("Отменить выбор");
		deselectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		deselectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deselectAllMethods();
			}
		});

		Label headerTestNameLabel = new Label(group, SWT.NONE);
		headerTestNameLabel.setText("Имя теста:");
		headerTestNameLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.BOTTOM, false, false));

		headerTestNameText = new Text(group, SWT.BORDER);
		headerTestNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		headerTestNameText.addModifyListener(e -> validatePage());
		headerTestNameText.setEnabled(false);

		return group;
	}

	private void setHeaderControlsEnabled(boolean enabled) {
		if (fixtureCheckbox != null)
			fixtureCheckbox.setEnabled(enabled);
		if (headerCombo != null)
			headerCombo.setEnabled(enabled);
		if (classCombo != null)
			classCombo.setEnabled(enabled);
		if (methodsViewer != null)
			methodsViewer.getTable().setEnabled(enabled);
		if (browseButton != null)
			browseButton.setEnabled(enabled);
		if (headerTestNameText != null)
			headerTestNameText.setEnabled(enabled);
	}

	private void updatePageSize() {
		Composite container = (Composite) getControl();
		Shell shell = container.getShell();

		if (shell == null)
			return;

		GridData emptyData = (GridData) emptyTestGroup.getLayoutData();
		GridData headerData = (GridData) headerTestGroup.getLayoutData();

		emptyData.exclude = (selectedTestType != TestType.EMPTY);
		headerData.exclude = (selectedTestType != TestType.HEADER_BASED);

		emptyTestGroup.setVisible(selectedTestType == TestType.EMPTY);
		headerTestGroup.setVisible(selectedTestType == TestType.HEADER_BASED);

		container.layout(true, true);

		shell.pack();

		Point packedSize = shell.getSize();
		int minHeight = (selectedTestType == TestType.EMPTY) ? 350 : 550;
		int minWidth = shell.getSize().x;
		;

		if (packedSize.y < minHeight || packedSize.x < minWidth) {
			shell.setSize(Math.max(packedSize.x, minWidth), Math.max(packedSize.y, minHeight));
		}
	}

	private void loadAvailableHeaders() {
		availableHeaderNames.clear();
		headers.clear();

		if (cmakeProject == null) {
			setMessage("Проект CMake не доступен", WARNING);
			return;
		}

		headerCombo.removeAll();
		classCombo.removeAll();

		List<ISourceFile> sources = cmakeProject.getSources();
		List<ISourceFile> headerFiles = sources.stream().filter(source -> isHeaderFile(source.getName())).toList();

		for (ISourceFile header : headerFiles) {
			addHeader(header);
		}

		updateHeaderCombo();

		if (!availableHeaderNames.isEmpty()) {
			onHeaderSelected();
		}
	}

	private void addHeader(ISourceFile sourceFile) {
		String fileName = sourceFile.getName();

		if (sourceFile.getFile() == null) {
			return;
		}

		if (headers.containsKey(fileName)) {
			return;
		}

		HeaderData data = new HeaderData(sourceFile);
		headers.put(fileName, data);
		availableHeaderNames.add(fileName);

		sourceFile.getElementsAsync().thenAccept(elements -> {
			data.addElements(elements);
			Display.getDefault().asyncExec(() -> {
				if (fileName.equals(headerCombo.getText())) {
					updateClassCombo(data);
				}
			});
		}).exceptionally(ex -> {
			Platform.getLog(getClass()).error(ex.getMessage());
			return null;
		});
	}

	private void updateHeaderCombo() {
		headerCombo.removeAll();

		if (availableHeaderNames.isEmpty()) {
			headerCombo.add(NO_HEADERS_MESSAGE);
			headerCombo.select(0);
			headerCombo.setEnabled(false);
			updateClassCombo(null);
			return;
		}

		availableHeaderNames.sort(String::compareToIgnoreCase);

		for (String headerName : availableHeaderNames) {
			headerCombo.add(headerName);
		}

		headerCombo.setEnabled(true);
		browseButton.setEnabled(true);
		headerCombo.select(0);
	}

	private void updateClassCombo(HeaderData data) {
		classCombo.removeAll();

		if (data != null) {
			if (!data.getClassMethods().isEmpty()) {
				List<String> classNames = new ArrayList<>(data.getClassMethods().keySet());
				classNames.sort(String::compareToIgnoreCase);
				for (String className : classNames) {
					classCombo.add(className);
				}
			}

			if (!data.getNamespaceFunctions().isEmpty()) {
				List<String> namespaceNames = new ArrayList<>(data.getNamespaceFunctions().keySet());
				namespaceNames.sort(String::compareToIgnoreCase);
				for (String namespace : namespaceNames) {
					classCombo.add(namespace + "::[Функции]");
				}
			}

			if (!data.getGlobalFunctions().isEmpty()) {
				classCombo.add("[Глобальные функции]");
			}
		}

		if (classCombo.getItemCount() == 0) {
			classCombo.add(NO_OBJECT_MESSAGE);
			classCombo.select(0);
			classCombo.setEnabled(false);
			return;
		}

		classCombo.setEnabled(true);
		classCombo.select(0);
		onClassSelected();
	}

	private void onHeaderSelected() {
		String selectedHeader = headerCombo.getText();
		HeaderData data = headers.get(selectedHeader);

		if (data != null) {
			if (data.getAllElements() != null) {
				updateClassCombo(data);
			} else {
				classCombo.removeAll();
				classCombo.add(LOADING);
				classCombo.select(0);
				classCombo.setEnabled(false);
			}
		}

		validatePage();
	}

	private void onClassSelected() {
		String selectedItem = classCombo.getText();
		String selectedHeader = headerCombo.getText();
		HeaderData data = headers.get(selectedHeader);

		if (data == null)
			return;

		List<ISourceElement> elements = new ArrayList<>();

		if (selectedItem.endsWith("::[Функции]")) {
			String namespace = selectedItem.replace("::[Функции]", "");
			elements = data.getNamespaceFunctions().get(namespace);
			headerTestNameText.setText(namespace.replace("::", "_") + "_functions_test");
		} else if (selectedItem.equals("[Глобальные функции]")) {
			elements = data.getGlobalFunctions();
			headerTestNameText.setText(selectedHeader.replace(".h", "") + "_global_functions_test");
		} else if (!selectedItem.equals(NO_OBJECT_MESSAGE) && !selectedItem.equals(LOADING)) {
			elements = data.getClassMethods().get(selectedItem);
			String testName = selectedItem.contains("::") ? selectedItem.replace("::", "_") + "_test"
					: selectedItem + "_test";
			headerTestNameText.setText(testName);
		}

		updateMethodsList(elements);
		validatePage();
	}

	private void updateMethodsList(List<ISourceElement> elements) {
		if (elements == null) {
			methodsViewer.setInput(new Object[0]);
			return;
		}

		List<String> methodNames = elements.stream().map(ISourceElement::getName)
				.filter(name -> !name.startsWith("operator")).distinct().toList();

		methodsViewer.setInput(methodNames.toArray());
		methodsViewer.setAllChecked(true);
	}

	private void browseForHeaderFile() {
		if (cmakeProject == null)
			return;

		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
		dialog.setText("Выберите заголовочный файл");
		dialog.setFilterExtensions(new String[] { "*.h;*.hpp;*.hxx;*.hh", "*.*" });

		IPath projectPath = cmakeProject.getProject().getLocation();
		if (projectPath == null) {
			setMessage("Не найден проект " + cmakeProject.getProject().getName(), WARNING);
			return;
		}

		dialog.setFilterPath(projectPath.toOSString());

		String selectedFile = dialog.open();
		if (selectedFile == null) {
			return;
		}

		IPath filePath = new Path(selectedFile);

		if (projectPath.isPrefixOf(filePath)) {
			IPath relativePath = filePath.makeRelativeTo(projectPath);
			ISourceFile sourceFile = new SourceFile(relativePath, cmakeProject);
			addHeader(sourceFile);

			String fileName = sourceFile.getName();
			if (availableHeaderNames.contains(fileName)) {
				updateHeaderCombo();
			}
		} else {
			setMessage("Файл должен находиться внутри проекта", WARNING);
		}
	}

	private boolean isHeaderFile(String fileName) {
		if (fileName == null)
			return false;
		String lowerName = fileName.toLowerCase();
		return lowerName.endsWith(".h") || lowerName.endsWith(".hpp") || lowerName.endsWith(".hxx")
				|| lowerName.endsWith(".hh");
	}

	private void selectAllMethods() {
		Object[] methods = (Object[]) methodsViewer.getInput();
		if (methods != null && methods.length > 0) {
			methodsViewer.setAllChecked(true);
		}
		validatePage();
	}

	private void deselectAllMethods() {
		methodsViewer.setAllChecked(false);
		validatePage();
	}

	private void validatePage() {
		String errorMessage = null;

		if (selectedTestType == TestType.EMPTY) {
			if (testNameText.getText().trim().isEmpty()) {
				errorMessage = "Имя теста не может быть пустым";
			}
		} else if (selectedTestType == TestType.HEADER_BASED) {
			String testName = headerTestNameText.getText().trim();
			if (testName.isEmpty()) {
				errorMessage = "Имя теста не может быть пустым";
			} else if (getSelectedMethods().isEmpty()) {
				errorMessage = "Выберите хотя бы один метод/функцию для тестирования";
			} else if (classCombo.getText().isEmpty() || classCombo.getText().equals(NO_OBJECT_MESSAGE)
					|| classCombo.getText().equals(LOADING)) {
				errorMessage = "Выберите объект тестирования";
			}
		}

		setPageComplete(errorMessage == null);
		setErrorMessage(errorMessage);
	}

	private List<String> getSelectedMethods() {
		if (methodsViewer == null)
			return new ArrayList<>();
		Object[] checkedElements = methodsViewer.getCheckedElements();
		return Arrays.stream(checkedElements).map(Object::toString).toList();
	}

	private String getHeaderIncludePath(String headerName) {
		HeaderData data = headers.get(headerName);
		if (data == null || data.getSourceFile() == null) {
			return "";
		}

		IFile file = data.getSourceFile().getFile();
		if (file == null) {
			return "";
		}

		IPath projectRelativePath = file.getProjectRelativePath();
		if (projectRelativePath.segmentCount() == 1) {
			return projectRelativePath.lastSegment();
		}

		return projectRelativePath.toString();
	}
}
