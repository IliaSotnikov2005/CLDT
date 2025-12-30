package su.softcom.cldt.core.cmake;

import org.eclipse.core.runtime.Assert;

import su.softcom.cldt.internal.core.cmake.tokens.CommentToken;

/**
 * @author Petr Babanov (petr.babanov@softcom.su) - initial implementation
 */
public class CommentNode extends CMakeNode {

	final CommentToken token;

	/**
	 * Basic constructor
	 * 
	 * @param token - to set
	 */
	public CommentNode(CommentToken token) {
		super(null);
		Assert.isNotNull(token);
		this.token = token;
	}

	/**
	 * Simple constructor from text - creates token internally
	 * 
	 * @param commentText text of the comment
	 */
	public CommentNode(String commentText) {
		super(null);
		Assert.isNotNull(commentText);
		this.token = new StringCommentToken(0, commentText);
	}

	@Override
	public void accept(ICMakeVisitor visitor) {
		visitor.visitComment(this);
	}

	@Override
	public CharSequence toText() {
		return "#" + token.getValue() + "\n";
	}

	private static class StringCommentToken extends CommentToken {
		private final String text;

		StringCommentToken(int offset, String text) {
			super(offset);
			this.text = text;
			setValue(text);
		}

		@Override
		public String getValue() {
			return text;
		}

		@Override
		public String getTypeName() {
			return "StringCommentToken";
		}
	}
}
