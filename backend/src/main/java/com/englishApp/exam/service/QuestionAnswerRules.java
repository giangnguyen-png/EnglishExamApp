package com.englishApp.exam.service;

import com.englishApp.exam.model.enums.QuestionType;

public final class QuestionAnswerRules {
	private QuestionAnswerRules() {
	}

	public static boolean isSingleCorrectChoice(QuestionType type) {
		return type == QuestionType.SINGLE_CHOICE || type == QuestionType.TRUE_FALSE_NOT_GIVEN
				|| type == QuestionType.FILL_IN_BLANK || type == QuestionType.MATCHING;
	}

	public static boolean isMultipleCorrectChoice(QuestionType type) {
		return type == QuestionType.MULTIPLE_CHOICE;
	}

	public static boolean usesChoiceAnswers(QuestionType type) {
		return isSingleCorrectChoice(type) || isMultipleCorrectChoice(type);
	}

	public static boolean doesNotUseChoiceAnswers(QuestionType type) {
		return type == QuestionType.ESSAY || type == QuestionType.SPEAKING;
	}
}
