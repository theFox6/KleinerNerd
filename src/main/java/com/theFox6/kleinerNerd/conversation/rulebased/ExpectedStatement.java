package com.theFox6.kleinerNerd.conversation.rulebased;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;

/**
 * A state/statement anywhere within a conversational flow.
 * Used to create simple conversational structures.
 * A bit like a Menu entry that has sub-entries.
 * When a message that matches one of the sub-entries is given then you will advance to the sub-entry.
 */
public class ExpectedStatement implements Cloneable {
	
	public enum Action {
		/**
		 * Indicates that no action was defined.
		 * Does nothing, just as STAY.
		 */
		NONE (0),
		/**
		 * The ExpectedStatement should become the current.
		 */
		STAY (0),
		/**
		 * The ExpectedStatement should become the current and should be deleted from it's parent.
		 */
		STAY_REMOVE (1),
		/**
		 * The ExpectedStatement that lead to this should still be the current after this one has run.
		 */
		RETURN (2),
		/**
		 * Return to the parent and remove this one from it's list.
		 */
		RETURN_REMOVE (3),
		/**
		 * The current ExpectedStatement should be set to null.
		 */
		QUIT (4),
		/**
		 * The current ExpectedStatement should be set to null
		 * and this one should be removed from it's parent.
		 */
		QUIT_REMOVE (5);
		
		private int flags;

		private Action(int bits) {
			flags = bits;
		}
		
		public boolean remove() {
			return (flags & 1) == 1;
		}
		
		public boolean returns() {
			return (flags & 2) == 2;
		}

		public boolean quit() {
			return (flags & 4) == 4;
		}
	}

	private Pattern pattern;
	private Function<Message, Action> action;
	private Function<Message,Action> unexpected;
	private Collection<ExpectedStatement> nextStatements = new LinkedList<>();
	/**
	 * construct a full expected statement
	 * @param pattern the expected message pattern to advance to this
	 * @param regex whether the pattern is a regular expression
	 * @param action what to do when advancing to this
	 * @param unexpected what to do when none of the options match
	 * @param unexRet whether to return to this if no option matches
	 * @param options expected statements that can be advanced to from this
	 */
	public ExpectedStatement(String pattern, boolean regex, Function<Message,Action> action,
			Function<Message,Action> unexpected, ExpectedStatement... options) {
		setValues(pattern, regex, action, unexpected);
		for (ExpectedStatement s : options) {
			addNext(s);
		}
	}
	
	/**
	 * construct a full expected statement
	 * @param pattern the expected message pattern to advance to this
	 * @param regex whether the pattern is a regular expression
	 * @param action what to do when advancing to this
	 * @param actRet whether to return to parent after running the action
	 * @param unexpected what to do when none of the options match
	 * @param unexRet whether to return to this if no option matches
	 * @param nextEntries a collection of expected statements that can be advanced to from this
	 */
	public ExpectedStatement(String pattern, boolean regex, Function<Message,Action> action,
			Function<Message,Action> unexpected, Collection<ExpectedStatement> nextEntries) {
		setValues(pattern, regex, action, unexpected);
		addNext(nextEntries);
	}
	
	/**
	 * construct an expected statement that defaults to being in the middle of a conversation
	 * @param pattern the expected message pattern to advance to this
	 * @param regex whether the pattern is a regular expression
	 * @param action what to do when advancing to this
	 */
	public ExpectedStatement(String pattern, boolean regex, Function<Message,Action> action) {
		this(pattern, regex, action, null);
	}

	/**
	 * construct an expected statement that defaults to being in the middle of a conversation
	 * @param text the text to advance to this
	 * @param action what to do when advancing to this
	 */
	public ExpectedStatement(String text, Function<Message,Action> action) {
		this(text, false, action);
	}

	/**
	 * set most non-container values
	 * @param pattern the expected message pattern to advance to this
	 * @param regex whether the pattern is a regular expression
	 * @param action what to do when advancing to this
	 * @param actRet whether to return to parent after running the action
	 * @param unexpected what to do when none of the options match
	 * @param unexRet whether to return to this if no option matches
	 * @return this
	 */
	public ExpectedStatement setValues(String pattern, boolean regex, Function<Message, Action> action,
			Function<Message,Action> unexpected) {
		setPattern(pattern, regex);
		setAction(action);
		setUnexpected(unexpected);
		return this;
	}

	/**
	 * Create a copy of this expected statement.
	 * The expected statements that can be advanced to from the new instance
	 * will stay in sync with this expected statement! 
	 */
	public ExpectedStatement clone() {
		ExpectedStatement result;
		try {
			result = (ExpectedStatement) super.clone();
		} catch (CloneNotSupportedException e) {
			QueuedLog.warning("clone failed", e);
			//create a new instance instead
			result = new ExpectedStatement(pattern.pattern(), true, action, unexpected, getNextEntries());
		}
		return result;
	}
	
	/**
	 * set the pattern that has to match to advance to this
	 * @param p the pattern as string
	 * @param regex whether the pattern is a regular expression
	 * @return this
	 */
	public ExpectedStatement setPattern(String p, boolean regex) {
		String pat = p;
		if (!regex) {
			pat = "\\A" + Pattern.quote(p) + "\\Z";
		}
		pattern = Pattern.compile(pat);
		return this;
	}
	
	/**
	 * @param raw the String to match
	 * @return whether the String matches the pattern of this ExpectedStatement
	 */
	public boolean rawMatches(String raw) {
		return pattern.asPredicate().test(raw);
	}

	/**
	 * replace {@literal "@FeralFox"} within the pattern by the actual mention
	 * @param jda the JDA to get the SelfUser from
	 * @return a clone of this ExpectedStatement with a new pattern
	 */
	public ExpectedStatement replaceSelfMention(JDA jda) {
		ExpectedStatement result = clone();
		String nPat = pattern.pattern().replace("@FeralFox", jda.getSelfUser().getAsMention());
		result.setPattern(nPat,true);
		return result;
	}
	
	/**
	 * @param action the action to be performed when advancing to this
	 * @return this
	 */
	public ExpectedStatement setAction(Function<Message,Action> action) {
		this.action = action;
		return this;
	}
	
	/**
	 * Set what to do when none of the next ExpectedStatements matches on advance.
	 * @param action a consumer that get's the non-matching message
	 * @return this
	 */
	public ExpectedStatement setUnexpected(Function<Message,Action> action) {
		unexpected = action;
		return this;
	}
		
	/**
	 * add an ExpectedStatement that can be advanced to from this
	 * @param s the ExpectedStatement that can follow this one
	 * @return this
	 */
	public ExpectedStatement addNext(ExpectedStatement s) {
		nextStatements.add(s);
		return this;
	}
	
	/**
	 * add a collection of ExpectedStatements that can be advanced to from this
	 * @param s the ExpectedStatements that can follow this one
	 * @return this
	 */
	public ExpectedStatement addNext(Collection<ExpectedStatement> nextEntries) {
		nextStatements.addAll(nextEntries);
		return this;
	}

	/**
	 * get the ExpectedStatements that can be advanced to from this
	 * @return the collection of statements that will be searched when advancing
	 */
	public Collection<ExpectedStatement> getNextEntries() {
		return nextStatements;
	}
	
	/*
	private ExpectedStatement[] getNextEntriesArray() {
		return nextStatements.toArray(new ExpectedStatement[nextStatements.size()]);
	}
	*/
	
	/**
	 * Try to find an ExpectedStatement that matches the message.
	 * If one matches run it's action, if not run the on-unexpected action.
	 * Also Evaluates the new ExpectedStatement that can be advanced from.
	 * @param msg the message to match
	 * @return the ExpectedStatement that should be used next.
	 */
	public ExpectedStatement advance(Message msg) {
		String raw = msg.getContentRaw();
		Optional<ExpectedStatement> firstMatch = nextStatements.stream()
				.filter((s) -> s.rawMatches(raw)).findFirst();
		if (firstMatch.isPresent()) {
			ExpectedStatement target = firstMatch.get();
			Action after = target.advancedTo(msg);
			if (after.remove()) {
				nextStatements.remove(target);
			}
			if (after.quit()) {
				return null;
			}
			if (after.returns()) {
				return this;
			}
			return target;
		} else {
			if (unexpected == null)
				return null;
			Action after = unexpected.apply(msg);
			if (after.remove()) {
				//perhaps add a reference to the parent
				QueuedLog.warning("ExpectedStatements cannot be removed if a message is unexpected");
			}
			if (after.quit()) {
				return null;
			}
			if (after.returns()) {
				//perhaps add a reference to the parent
				QueuedLog.warning("ExpectedStatements cannot return to their parent if a message is unexpected");
				return null;
			}
			if (after == Action.NONE) {
				QueuedLog.warning("unexpected action of an ExpectedStatemenent did not specify quit or stay");
				// this is more likely to stop unwanted behavior
				return null;
			}
			return this;
		}
	}

	/**
	 * Advance to this UnexpectedStatement.
	 * Runs the action if it's set.
	 * @param msg the message to pass to the action
	 */
	public Action advancedTo(Message msg) {
		if (action != null)
			return action.apply(msg);
		return Action.NONE;
	}

}
