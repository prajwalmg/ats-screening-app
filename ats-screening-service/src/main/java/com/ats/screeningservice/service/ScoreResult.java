package com.ats.screeningservice.service;

/**
 * A strategy's raw contribution to screening: a 0-100 score plus the
 * one-line explanation of how it got there. Skill overlap (matched/missing)
 * and the hard experience gate are computed once by {@link ScreeningService}
 * regardless of strategy — only the score itself is pluggable.
 */
public record ScoreResult(int score, String reasoning) {
}
