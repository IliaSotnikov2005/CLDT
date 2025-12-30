package su.softcom.cldt.testing.gtest.ui.models;

import java.util.List;

/**
 * Представляет тестовый набор.
 */
public record TestSuite(
    String name,
    int tests,
    int failures,
    int disabled,
    int errors,
    String timestamp,
    String time,
    List<TestInfo> testsuite
) {
	/**
	 * Проверяет есть ли не пройденные тесты.
	 * @return true, если есть непройденные тесты, иначе false.
	 */
    public boolean hasFailures() {
        return failures > 0;
    }
}