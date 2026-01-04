package su.softcom.cldt.testing.gtest.core;

/**
 * Константы, используемые для работы с GTest. 
 */
public final class GTestConstants {
	private GTestConstants() {
	}
	
	public static final String GTEST_NODE = "su.softcom.cldt.testing.gtest";
	public static final String GTEST_INSTANCES_KEY = "instances";
	public static final String GTEST_INSTANCE_KEY = "instance";
	public static final String TESTS_FOLDER_KEY = "testsDirectory";
	public static final String DEFAULT_TESTS_FOLDER_NAME = "tests";
	public static final String GTEST = "gtest";
	public static final String GTEST_MAIN = "gtest_main";
	public static final String GTEST_LIB = "GTest::gtest";
	public static final String GTEST_MAIN_LIB = "GTest::gtest_main";
	public static final String CMAKELISTS = "CMakeLists.txt";
	public static final String CONFIG_FILE_NAME = "GTestConfig.cmake";
}
