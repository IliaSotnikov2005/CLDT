package su.softcom.cldt.core.source;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import su.softcom.cldt.core.CMakeCorePlugin;
import su.softcom.cldt.core.cmake.ICMakeProject;
import su.softcom.cldt.core.cmake.Target;
import su.softcom.cldt.core.lsp.symbols.ISymbolService;
import su.softcom.cldt.core.lsp.symbols.SymbolSnapshot;
import su.softcom.cldt.core.preferences.CMakePreferences;

/**
 * Реализация ISourceFile.
 */
public class SourceFile extends AbstractSourcePart implements ISourceFile {

	private static final String SRC_IS_GENERATED = "src_gen"; //$NON-NLS-1$

	/**
	 * Типы исходных файлов, поддерживаемые в CMake-проектах.
	 * <p>
	 * Используется для классификации файлов в системе сборки.
	 * </p>
	 */
	public enum SourceType {

		/**
		 * Исходный файл на языке C.
		 * <p>
		 * Примеры расширений: .c
		 * </p>
		 * <p>
		 * Идентификатор: 1
		 * </p>
		 */
		C_FILE(1),

		/**
		 * Исходный файл на языке C++.
		 * <p>
		 * Примеры расширений: .c, .cpp, .cxx
		 * </p>
		 * <p>
		 * Идентификатор: 2
		 * </p>
		 */
		CPP_FILE(2),

		/**
		 * Файл неопределенного или неподдерживаемого типа.
		 * <p>
		 * Идентификатор: 0
		 * </p>
		 */
		UNDEFINED(0);

		private final int fileTypeCode;

		/**
		 * Конструктор типа файла.
		 * 
		 * @param code числовой идентификатор типа
		 */
		SourceType(int code) {
			this.fileTypeCode = code;
		}

		/**
		 * Возвращает числовой идентификатор типа файла.
		 * 
		 * @return уникальный код типа
		 */
		public int getFileTypeCode() {
			return fileTypeCode;
		}
	}

	private boolean isGenerated;
	private IFile file;

	private final ICMakeProject cmakeProject;
	private final List<Target> targets = new ArrayList<>();
	private final String name;
	private final SourceType type;

	/**
	 * Восстановление или создание SourceFile по пути и таргету.
	 * 
	 * @param projectRelPath путь к SourceFile, может быть отностительным проекту
	 * @param target         цель сборки, в который включен этот SourceFile
	 */
	public SourceFile(IPath projectRelPath, Target target) {
		this(projectRelPath.lastSegment(), target.getProject());
		load(projectRelPath, target).ifPresent(sf -> {
			this.file = sf.file;
			this.isGenerated = sf.isGenerated;
			addTarget(target);
		});
	}

	public SourceFile(IPath projectRelPath, ICMakeProject project) {
		this(projectRelPath.lastSegment(), project);
		IProject prj = project.getProject();
		var res = prj.findMember(projectRelPath);
		if (!(res instanceof IFile)) {
			file = null;
		}

		file = (IFile) res;
		isGenerated = false;
	}

	private SourceFile(String name, ICMakeProject project) {
		super(project.getProject());
		this.cmakeProject = project;
		this.type = getSourceType(name);
		this.name = name;
	}

	@Override
	public String getName() {
		if (name == null) {
			return ""; //$NON-NLS-1$
		}
		return name;
	}

	@Override
	public boolean isGenerated() {
		return isGenerated;
	}

	@Override
	public IFile getFile() {
		return file;
	}

	/**
	 * @return type of this CppSource
	 */
	public SourceType getSourceType() {
		return type;
	}

	/**
	 * @param target
	 */
	public void save(Target target) {
		if (file == null)
			return;
		Preferences n = CMakePreferences.nodeForPath(cmakeProject.getProject(), target, file.getProjectRelativePath());
		n.putBoolean(SRC_IS_GENERATED, isGenerated);
		try {
			n.flush();
		} catch (BackingStoreException e) {
			CMakeCorePlugin.logError(Messages.SourceFile_0.formatted(this.name), e);
		}
	}

	/**
	 * Добавление цели сборки к этому SourceFile. Если такая цель сборки уже есть,
	 * то ничего не добавит
	 * 
	 * @param target
	 */
	@Override
	public void addTarget(Target target) {
		if (targets.contains(target)) {
			return;
		}
		targets.add(target);
	}

	/**
	 * Цели сборки в который включен этот исходный файл
	 * 
	 * @return targets
	 */
	@Override
	public List<Target> getTargets() {
		return targets;
	}

	@Override
	public String toString() {
		return " %s in %s".formatted(this.getName(), this.cmakeProject.toString()); //$NON-NLS-1$
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		Assert.isNotNull(adapter);
		if (adapter.isInstance(this)) {
			return (T) this;
		}
		if (IFile.class.equals(adapter)) {
			return (T) this.file;
		}
		if (IResource.class.equals(adapter)) {
			return (T) this.file;
		}
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		SourceFile other = (SourceFile) o;
		if (this.file == null || other.file == null)
			return false;
		return Objects.equals(this.file.getFullPath(), other.file.getFullPath());
	}

	@Override
	public int hashCode() {
		return file != null ? file.getFullPath().hashCode() : 0;
	}

	/**
	 * Может вернуть пустой список, если символы ещё не получены.
	 */
	@Override
	public List<ISourceElement> getElements() {
		ISymbolService symbolService = CMakeCorePlugin.getDefault().getSymbolService();
		SymbolSnapshot symbolSnapshot = symbolService.getSnapshot(file);
		if (symbolSnapshot != null && symbolSnapshot.isReady()) {
			return symbolSnapshot.getSymbols().stream()
					.map(sym -> SourcePartFactory.getDefault().getSourceElement(sym, this)).toList();
		}
		var snap = symbolService.getSnapshot(file);
		return snap.getSymbols().stream().map(sym -> SourcePartFactory.getDefault().getSourceElement(sym, this))
				.toList();
	}

	/**
	 * Асинхронно возвращает элементы, инициируя запрос символов при необходимости.
	 * 
	 * Безопасно вызывать из UI
	 * 
	 * @return CompletableFuture<List<ISourceElement>>
	 */
	@Override
	public CompletableFuture<List<ISourceElement>> getElementsAsync() {
		ISymbolService symbolService = CMakeCorePlugin.getDefault().getSymbolService();
		var snap = symbolService.getSnapshot(file);
		if (snap.isReady()) {
			return CompletableFuture.completedFuture(snap.getSymbols().stream()
					.map(sym -> SourcePartFactory.getDefault().getSourceElement(sym, this)).toList());
		}
		return symbolService.loadAsync(file).thenApply(s -> s.getSymbols().stream()
				.map(sym -> SourcePartFactory.getDefault().getSourceElement(sym, this)).toList());
	}

	/**
	 * Блокирует текущий поток до получения элементов или таймаута. Не вызывать из
	 * UI потока!
	 * 
	 * @param timeout
	 * @return List of ISourceElement
	 * @throws CoreException
	 */
	public List<ISourceElement> getElementsBlocking(Duration timeout) throws CoreException {
		try {
			return getElementsAsync().orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS).join();
		} catch (CompletionException e) {
			throw new CoreException(
					new Status(IStatus.ERROR, CMakeCorePlugin.PLUGIN_ID, Messages.ErrorFetchSymbols_21, e));
		}
	}

	@Override
	public String getIdentifierName() {
		return name;
	}

	private SourceType getSourceType(String fileStr) {
		if (fileStr == null) {
			return SourceType.UNDEFINED;
		}
		IPath filePath = new Path(fileStr);
		String extension = filePath.getFileExtension();
		if (extension == null) {
			return SourceType.UNDEFINED;
		}

		return switch (extension.toLowerCase()) {
		case "c", "h" -> SourceType.C_FILE; //$NON-NLS-1$ //$NON-NLS-2$
		case "cpp", "cc", "cxx", "hpp", "hxx" -> SourceType.CPP_FILE; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		default -> SourceType.UNDEFINED;
		};
	}

	private static Optional<SourceFile> load(IPath projectRelPath, Target target) {
		IProject prj = target.getProject().getProject();
		Preferences n = CMakePreferences.nodeForPath(prj, target, projectRelPath);
		var res = prj.findMember(projectRelPath);
		if (!(res instanceof IFile f))
			return Optional.empty();
		boolean gen = n.getBoolean(SRC_IS_GENERATED, false);
		SourceFile sf = new SourceFile(f.getName(), target.getProject());
		sf.file = f;
		sf.isGenerated = gen;
		return Optional.of(sf);
	}
}