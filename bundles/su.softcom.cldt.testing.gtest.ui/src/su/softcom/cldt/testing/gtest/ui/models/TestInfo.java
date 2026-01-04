package su.softcom.cldt.testing.gtest.ui.models;

import java.util.List;

/**
 * Представляет результат выполнения тестового случая.
 */
public record TestInfo(
	    String name,
	    String file,
	    int line,
	    String status,
	    String result,
	    String timestamp,
	    String time,
	    String classname,
	    List<Failure> failures,
	    List<Skipped> skipped
	) {
		/**
		 * Проверяет провален ли тест.
		 * @return {@code true}, если провелен, иначе {@code false}.
		 */
	    public boolean isFailed() {
	        return failures != null && !failures.isEmpty();
	    }
	    
	    /**
	     * Проверяет пройден ли тест.
	     * @return {@code true}, если пройден, иначе {@code false}.
	     */
	    public boolean isPassed() {
	        return "COMPLETED".equals(result) && !isFailed() && !isSkipped();
	    }
	    
	    /**
	     * Проверяет пропущен ли тест.
	     * @return {@code true}, если пропущен, иначе {@code false}.
	     */
	    public boolean isSkipped() {
	        return "SKIPPED".equals(result) || (skipped != null && !skipped.isEmpty());
	    }
	    
	    /**
	     * Проверяет отключен ли тест.
	     * @return {@code true}, если отключен, иначе {@code false}.
	     */
	    public boolean isDisabled() {
	        return "SUPPRESSED".equals(result) || "NOTRUN".equals(status);
	    }
	    
	    /**
	     * Получает отображаемое имя теста.
	     * @return отображаемое имя теста.
	     */
	    public String getDisplayName() {
	        String name = this.name;
	        if (isFailed()) {
	            name += " [ПРОВАЛЕН]";
	        } else if (isSkipped()) {
	            name += " [ПРОПУЩЕН]";
	        } else if (isDisabled()) {
	            name += " [ОТКЛЮЧЕН]";
	        }
	        
	        return name;
	    }
	}