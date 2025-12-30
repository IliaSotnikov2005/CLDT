package su.softcom.cldt.testing.gtest.ui.models;

/**
 * Представляет ошибку при выполнении теста.
 */
public record Failure(String failure, String type) {
}