package su.softcom.cldt.internal.core.cmake.tokens;

/**
 * Technical class for separate comment tokens.
 */
public abstract class CommentToken extends CMakeToken {

	CommentToken(int offset) {
		super(offset);
	}

}
