package su.softcom.cldt.core.cmake;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Assert;

import su.softcom.cldt.internal.core.cmake.tokens.BracketedToken;
import su.softcom.cldt.internal.core.cmake.tokens.CMakeToken;
import su.softcom.cldt.internal.core.cmake.tokens.QuotedToken;
import su.softcom.cldt.internal.core.cmake.tokens.RawToken;

/**
 * Node for representation command in CMake files
 * 
 * @author Petr Babanov (petr.babanov@softcom.su) - initial implementation.
 */
public class CommandNode extends CMakeNode implements ICMakeCommand {

	private final String name;
	List<ArgumentNode> args = new ArrayList<>();

	public CommandNode(String name, CMakeNode root) {
		super(root);
		Assert.isNotNull(name);
		this.name = name;
	}

	public CommandNode(RawToken name, List<CMakeToken> args) {
		this(name.getValue(), null);
		Assert.isNotNull(args);
		for (CMakeToken arg : args) {
			if (arg instanceof QuotedToken) {
				ArgumentNode argNode = new ArgumentNode(this, arg.getValue());
				argNode.makeQuoted();
				addArg(argNode);
			} else if (arg instanceof BracketedToken) {
				ArgumentNode argNode = new ArgumentNode(this, arg.getValue());
				argNode.makeBracketed();
				addArg(argNode);
			} else {
				addArg(arg.getValue());
			}
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<ArgumentNode> getArgs() {
		return new ArrayList<>(args);
	}

	@Override
	public void addArg(ArgumentNode argument) {
		Assert.isNotNull(argument);
		args.add(argument);
	}

	public void addArg(String argument) {
		Assert.isNotNull(argument);
		addArg(new ArgumentNode(this, argument));
	}

	@Override
	public void addAllArg(List<ArgumentNode> argument) {
		Assert.isNotNull(argument);
		args.addAll(argument);
	}

	@Override
	public void setArgs(List<ArgumentNode> arguments) {
		Assert.isNotNull(arguments);
		args.clear();
		args.addAll(arguments);
	}

	public void clearArgs() {
		args.clear();
	}

	@Override
	public void accept(ICMakeVisitor visitor) {
		visitor.visitCommand(this);
	}

	@Override
	public CharSequence toText() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append("("); //$NON-NLS-1$
		sb.append(args.stream().map(ArgumentNode::toText).collect(Collectors.joining(" "))); //$NON-NLS-1$
		sb.append(")\n"); //$NON-NLS-1$
		return sb;
	}

	@Override
	public String toString() {
		return "CommandNode: " + name; //$NON-NLS-1$
	}

}
