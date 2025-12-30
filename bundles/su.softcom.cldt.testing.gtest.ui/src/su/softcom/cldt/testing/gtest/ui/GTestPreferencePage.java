package su.softcom.cldt.testing.gtest.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.BackingStoreException;

import com.fasterxml.jackson.core.JsonProcessingException;

import su.softcom.cldt.testing.gtest.core.GTestConstants;
import su.softcom.cldt.testing.gtest.core.model.GTestInstance;

/**
 * Страница “Preferences ▸ Google Test”.
 * <p>
 * Показывает список экземпляров GTest с их версиями и путями.
 * </p>
 */
public final class GTestPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String ID = "su.softcom.cldt.testing.gtest.ui.GtestPreferencePage";

	private TableViewer tableViewer;
	private Button removeButton;
	private List<GTestInstance> gTestInstances;

	@Override
	public void init(IWorkbench workbench) {
		// empty
	}

	@Override
	protected Control createContents(Composite parent) {
		gTestInstances = GTestInstancesManager.getRefreshedInstanses();

		Composite root = new Composite(parent, SWT.NONE);
		root.setLayout(new GridLayout(2, false));

		createTable(root);
		createButtons(root);

		tableViewer.setInput(gTestInstances);
		updateButtonsState();

		return root;
	}

	private void createTable(Composite parent) {
		String[] tableColumns = { "Путь", "Версия", "Тип" };
		int[] tableColumnWidths = { 400, 80, 160 };

		Label label = new Label(parent, SWT.NONE);
		label.setText("Экземпляры Google Test:");
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		tableViewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		Table table = tableViewer.getTable();
		GridData tableGridData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 4);
		tableGridData.minimumHeight = 200;
		table.setLayoutData(tableGridData);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		for (int i = 0; i < tableColumns.length; i++) {
			TableColumn column = new TableColumn(table, SWT.LEFT);
			column.setText(tableColumns[i]);
			column.setWidth(tableColumnWidths[i]);
		}

		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setLabelProvider(new GTestTableLabelProvider());

		tableViewer.addSelectionChangedListener(event -> updateButtonsState());
	}

	private void createButtons(Composite parent) {
		Button addButton = new Button(parent, SWT.PUSH);
		addButton.setText("Добавить");
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		addButton.addListener(SWT.Selection, e -> addGTestInstance());

		removeButton = new Button(parent, SWT.PUSH);
		removeButton.setText("Удалить");
		removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		removeButton.addListener(SWT.Selection, e -> removeGTestInstance());

		new Label(parent, SWT.NONE).setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 2));
	}

	private void addGTestInstance() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setText("Выберите папку с Google Test");
		dialog.setMessage("Укажите корневую папку GTest");

		String selectedPath = dialog.open();
		if (selectedPath != null) {
			String errorMessage = validateNewGTestPath(selectedPath);
			if (errorMessage == null) {
				GTestInstance instance = new GTestInstance(selectedPath, detectGTestVersion(selectedPath),
						GTestInstance.USER_TEXT);

				gTestInstances.add(instance);
				tableViewer.refresh();
				updateButtonsState();
			} else {
				MessageDialog.openError(getShell(), "Ошибка валидации", errorMessage);
			}
		}
	}

	private void removeGTestInstance() {
		IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
		if (!selection.isEmpty()) {
			List<GTestInstance> instancesToRemove = new ArrayList<>();
			List<GTestInstance> embeddedInstances = new ArrayList<>();

			for (Object selected : selection.toList()) {
				GTestInstance instance = (GTestInstance) selected;
				if (GTestInstance.EMBEDDED_TEXT.equals(instance.type())) {
					embeddedInstances.add(instance);
				} else {
					instancesToRemove.add(instance);
				}
			}

			if (!embeddedInstances.isEmpty()) {
				MessageDialog.openWarning(getShell(), "Невозможно удалить",
						"Встроенные экземпляры GTest нельзя удалить.");
			}

			if (!instancesToRemove.isEmpty()) {
				boolean confirm = MessageDialog.openConfirm(getShell(), "Подтверждение удаления",
						"Удалить выбранные экземпляры GTest?");

				if (confirm) {
					gTestInstances.removeAll(instancesToRemove);
					tableViewer.refresh();
					updateButtonsState();
				}
			}
		}
	}

	private void updateButtonsState() {
		IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();

		if (!selection.isEmpty()) {
			boolean hasUserInstances = false;
			for (Object selected : selection.toList()) {
				GTestInstance instance = (GTestInstance) selected;
				if (!GTestInstance.EMBEDDED_TEXT.equals(instance.type())) {
					hasUserInstances = true;
					break;
				}
			}

			removeButton.setEnabled(hasUserInstances);
		} else {
			removeButton.setEnabled(false);
		}
	}

	private String validateNewGTestPath(String path) {
		boolean isDublicate = gTestInstances.stream().anyMatch(instance -> instance.path().equals(path));
		if (isDublicate) {
			return "Экземпляр GTest с таким путем уже существует.";
		}

		Path dirPath = Paths.get(path);
		if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
			return "Папка не существует или недоступна";
		}

		String version = detectGTestVersion(path);
		if (version == null) {
			return "Не удалось определить версию GTest";
		}

		return null;
	}

	private String detectGTestVersion(String gTestPath) {
		try {
			Path path = Paths.get(gTestPath);
			Path versionFile = path.resolve("..").resolve(GTestConstants.CMAKELISTS);
			if (!Files.exists(versionFile)) {
				versionFile = path.resolve(GTestConstants.CMAKELISTS);
			}

			if (Files.exists(versionFile)) {
				String content = Files.readString(versionFile);
				Pattern pattern = Pattern.compile("VERSION\\s+(\\d+\\.\\d+\\.\\d+)");
				Matcher matcher = pattern.matcher(content);
				if (matcher.find()) {
					return matcher.group(1);
				}
			}
		} catch (IOException e) {
			log(e);
		}

		return null;
	}

	@Override
	public boolean performOk() {
		List<GTestInstance> invalidInstances = searchInvalidInstances();

		if (!invalidInstances.isEmpty()) {
			boolean confirm = showValidationDialog(invalidInstances);

			if (confirm) {
				gTestInstances.removeAll(invalidInstances);

				tableViewer.refresh();
				updateButtonsState();
			}

			return false;
		}

		IEclipsePreferences store = InstanceScope.INSTANCE.getNode(GTestConstants.GTEST_NODE);

		try {
			store.put(GTestConstants.GTEST_INSTANCES_KEY, GTestInstance.listToJson(gTestInstances));
			store.flush();
		} catch (JsonProcessingException | BackingStoreException e) {
			log(e);
		}

		return true;
	}

	private List<GTestInstance> searchInvalidInstances() {
		if (gTestInstances == null || gTestInstances.isEmpty()) {
			return new ArrayList<>();
		}

		return gTestInstances.stream().filter(instance -> !isValidInstance(instance)).toList();
	}

	private boolean isValidInstance(GTestInstance instance) {
	    if (instance.path() == null || instance.path().isEmpty()) {
	        return false;
	    }
	    
        Path path = instance.asPath();
        
        if (!Files.exists(path)) {
            return false;
        }
        
        if (GTestInstance.SYSTEM_TEXT.equals(instance.type())) {
        	if (Files.isRegularFile(path)) {
                return path.endsWith(GTestConstants.CONFIG_FILE_NAME);
        	}
        	
            return Files.exists(path.resolve(GTestConstants.CONFIG_FILE_NAME));
            
        }
        
        else if (GTestInstance.USER_TEXT.equals(instance.type()) || 
                 GTestInstance.EMBEDDED_TEXT.equals(instance.type())) {
            Path cmakeFile = path.resolve(GTestConstants.CMAKELISTS);
            if (!Files.exists(cmakeFile)) {
                cmakeFile = path.resolve("googletest/CMakeLists.txt");
            }
            
            return Files.exists(cmakeFile);
        }
        
        return false;

	}

	private boolean showValidationDialog(List<GTestInstance> invalidInstances) {
		if (invalidInstances == null || invalidInstances.isEmpty()) {
			return true;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Экземпляры GTest по путям:\n\n");

		for (GTestInstance instance : invalidInstances) {
			sb.append("• ").append(instance.path());
			if (instance.version() != null && !instance.version().isEmpty()) {
				sb.append(" (версия: ").append(instance.version()).append(")");
			}

			sb.append("\n");
		}

		sb.append("\nНе найдены и будут удалены из списка.");
		String message = sb.toString();

		MessageDialog dialog = new MessageDialog(getShell(), "Проверка путей GTest", null, message,
				MessageDialog.WARNING, new String[] { "Продолжить", "Отмена" }, 0);

		return dialog.open() == 0;
	}

	@Override
	protected void performDefaults() {
		String embeddedPath = embeddedPathText();
		String embeddedVersion = versionText();

		gTestInstances.clear();
		gTestInstances.add(new GTestInstance(embeddedPath, embeddedVersion, GTestInstance.EMBEDDED_TEXT));

		tableViewer.refresh();
		updateButtonsState();
	}

	private String versionText() {
		return Activator.getLibrary() != null ? Activator.getLibrary().version().toString() : "";
	}

	private String embeddedPathText() {
		return Activator.getLibrary() != null
				? Activator.getLibrary().includeDir().getParent().toAbsolutePath().toString()
				: "";
	}

	private static void log(Throwable t) {
		Bundle bundle = FrameworkUtil.getBundle(GTestPreferencePage.class);
		IStatus status = new Status(IStatus.ERROR, bundle.getSymbolicName(),
				t.getMessage() == null ? t.toString() : t.getMessage(), t);
		Platform.getLog(bundle).log(status);
	}

	private static class GTestTableLabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public String getColumnText(Object element, int columnIndex) {
			GTestInstance instance = (GTestInstance) element;
			switch (columnIndex) {
			case 0:
				return !instance.tag().isEmpty() ? instance.tag() : instance.path();
			case 1:
				return instance.version();
			case 2:
				return instance.type();
			default:
				return "";
			}
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
	}
}
