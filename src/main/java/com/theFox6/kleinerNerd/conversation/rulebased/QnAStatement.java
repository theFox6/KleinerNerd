package com.theFox6.kleinerNerd.conversation.rulebased;

import net.dv8tion.jda.api.entities.Message;

public class QnAStatement extends ExpectedStatement {

	private String answer;
	private ExpectedStatement.Action after;

	public QnAStatement(String q, String a, ExpectedStatement.Action andThen) {
		super(q, null);
		answer = a;
		after = andThen;
		setAction(this::sendAnswer);
	}
	
	public QnAStatement(String q, String a) {
		this(q,a,ExpectedStatement.Action.NONE);
	}

	public ExpectedStatement.Action sendAnswer(Message msg) {
		msg.getChannel().sendMessage(answer).queue();
		return after;
	}
}
