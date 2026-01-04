package su.softcom.cldt.testing.gtest.ui.models;

import java.util.List;

/**
 * Представляет результат запуска исполняемого файла с тестами.
 */
public record GTestResult(
    int tests,
    int failures,
    int disabled,
    int errors,
    String timestamp,
    String time,
    String name,
    List<TestSuite> testsuites
) {
	/**
	 * Получает количество пройденных тестов.
	 * @return количество пройденных тестов
	 */
    public int getPassedCount() {
        return tests - failures - errors - disabled;
    }
    
    /**
	 * Проверяет, есть ли ошибки при выполнении тестов.
	 * @return true, если есть непройденные тесты или ошибки при выполнении
	 */
    public boolean hasFailures() {
        return failures > 0 || errors > 0;
    }
    /**
	 * Получает количество запущеных тестов.
	 * @return количество запущенных тестов
	 */
    public int getExecutedCount() {
        return tests - disabled;
    }
}