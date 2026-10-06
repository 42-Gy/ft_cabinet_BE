package com.gyeongsan.cabinet.application.chatbot;

import com.gyeongsan.cabinet.domain.chatbot.model.ChatbotAnswer;
import com.gyeongsan.cabinet.domain.chatbot.model.EmbeddingUnavailableException;
import com.gyeongsan.cabinet.domain.chatbot.port.in.AskChatbotUseCase;
import com.gyeongsan.cabinet.domain.chatbot.port.out.ChatbotMetricsPort;
import com.gyeongsan.cabinet.domain.chatbot.port.out.EmbeddingPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.extern.log4j.Log4j2;

/** 질문과 가장 비슷한 FAQ 를 찾아 미리 정해 둔 답변을 돌려준다. 문장을 새로 만들어내지 않는다. 질문 원문은 저장하거나 기록하지 않는다(로그에도 남기지 않는다). */
@Log4j2
public class ChatbotService implements AskChatbotUseCase {

    private static final long GATE_WAIT_MILLIS = 2_000;

    private final FaqIndexManager indexManager;
    private final EmbeddingPort embedding;
    private final ChatbotMetricsPort metrics;
    private final ChatbotSettings settings;
    private final Semaphore embeddingGate;

    public ChatbotService(
            FaqIndexManager indexManager,
            EmbeddingPort embedding,
            ChatbotMetricsPort metrics,
            ChatbotSettings settings) {
        this.indexManager = indexManager;
        this.embedding = embedding;
        this.metrics = metrics;
        this.settings = settings;
        this.embeddingGate = new Semaphore(settings.maxConcurrentEmbeddings());
    }

    @Override
    public ChatbotAnswer ask(String rawQuestion) {
        String question = TextNormalizer.normalize(rawQuestion);
        if (question.isEmpty() || question.length() > settings.maxQuestionLength()) {
            throw new ServiceException(ErrorCode.CHATBOT_INVALID_QUESTION);
        }
        if (!indexManager.isReady()) {
            throw new ServiceException(ErrorCode.CHATBOT_NOT_READY);
        }

        float[] vector = embedQuestion(question);
        List<FaqIndex.Hit> hits =
                indexManager.current().search(vector, Math.max(settings.maxSuggestions(), 1));

        ChatbotAnswer answer = decide(hits);
        metrics.recordAsk(answer.result());
        log.debug("[Chatbot] 결과={}, 최고 유사도={}", answer.result(), answer.score());
        return answer;
    }

    private ChatbotAnswer decide(List<FaqIndex.Hit> hits) {
        if (hits.isEmpty()) {
            return ChatbotAnswer.unmatched(0);
        }
        FaqIndex.Hit best = hits.get(0);
        if (best.score() >= settings.matchThreshold()) {
            return new ChatbotAnswer(
                    ChatbotAnswer.Result.MATCHED,
                    best.faq(),
                    best.matchedQuestion(),
                    best.score(),
                    List.of());
        }
        if (best.score() >= settings.suggestThreshold() && settings.maxSuggestions() > 0) {
            List<ChatbotAnswer.Suggestion> suggestions =
                    hits.stream()
                            .filter(h -> h.score() >= settings.suggestThreshold())
                            .limit(settings.maxSuggestions())
                            .map(
                                    h ->
                                            new ChatbotAnswer.Suggestion(
                                                    h.faq().id(),
                                                    h.faq().representativeQuestion(),
                                                    h.score()))
                            .toList();
            return new ChatbotAnswer(
                    ChatbotAnswer.Result.SUGGESTED,
                    null,
                    best.matchedQuestion(),
                    best.score(),
                    suggestions);
        }
        return ChatbotAnswer.unmatched(best.score());
    }

    private float[] embedQuestion(String question) {
        boolean acquired = false;
        try {
            acquired = embeddingGate.tryAcquire(GATE_WAIT_MILLIS, TimeUnit.MILLISECONDS);
            if (!acquired) {
                // 챗봇이 서버 CPU 를 독점하지 않도록, 줄이 길면 기다리게 하지 않고 거절한다.
                throw new ServiceException(ErrorCode.CHATBOT_NOT_READY);
            }
            return embedding.embed(question);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException(ErrorCode.CHATBOT_NOT_READY);
        } catch (EmbeddingUnavailableException e) {
            throw new ServiceException(ErrorCode.CHATBOT_NOT_READY);
        } finally {
            if (acquired) {
                embeddingGate.release();
            }
        }
    }
}
