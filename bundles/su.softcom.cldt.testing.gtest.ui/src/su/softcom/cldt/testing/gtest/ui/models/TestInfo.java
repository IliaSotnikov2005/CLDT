package su.softcom.cldt.testing.gtest.ui.models;

import java.util.List;

/**
 * Представляет тестовый случай.
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
		 * @return true, если провелен, иначе false.
		 */
	    public boolean isFailed() {
	        return failures != null && !failures.isEmpty();
	    }
	    
	    /**
	     * Проверяет пройден ли тест.
	     * @return true, если пройден, иначе false.
	     */
	    public boolean isPassed() {
	        return "COMPLETED".equals(result) && !isFailed() && !isSkipped();
	    }
	    
	    /**
	     * Проверяет пропущен ли тест.
	     * @return true, если пропущен, иначе false.
	     */
	    public boolean isSkipped() {
	        return "SKIPPED".equals(result) || (skipped != null && !skipped.isEmpty());
	    }
	    
	    /**
	     * Проверяет отключен ли тест.
	     * @return true, если отключен, иначе false.
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