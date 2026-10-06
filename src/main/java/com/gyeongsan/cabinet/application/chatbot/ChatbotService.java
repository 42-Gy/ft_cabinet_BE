package com.gyeongsan.cabinet.application.chatbot;

import com.gyeongsan.cabinet.domain.chatbot.model.ChatbotAnswer;
import com.gyeongsan.cabinet.domain.chatbot.model.EmbeddingUnavailableException;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalIntent;
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
    private final PersonalIntentRouter personalRouter;

    public ChatbotService(
            FaqIndexManager indexManager,
            EmbeddingPort embedding,
            ChatbotMetricsPort metrics,
            ChatbotSettings settings) {
        this(indexManager, embedding, metrics, settings, null);
    }

    /** personalRouter 가 있으면 "내 정보" 질문에 개인화 칩을 함께 제안한다(없으면 기존 동작 그대로). */
    public ChatbotService(
            FaqIndexManager indexManager,
            EmbeddingPort embedding,
            ChatbotMetricsPort metrics,
            ChatbotSettings settings,
            PersonalIntentRouter personalRouter) {
        this.personalRouter = personalRouter;
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

        long started = System.nanoTime();
        float[] vector = embedQuestion(question);
        // 1·2위 차이를 보려면 최소 2개, 자동 답변의 대안 후보를 위해 1위 + 대안 개수까지 필요하다.
        int limit =
                Math.max(Math.max(settings.maxSuggestions(), settings.matchAlternatives() + 1), 2);
        List<FaqIndex.Hit> hits = indexManager.current().search(vector, limit);

        ChatbotAnswer answer = decide(hits);
        PersonalIntent action = personalAction(vector, hits);
        if (action != null) {
            answer = answer.withPersonalAction(action);
            metrics.recordPersonal(action, "chip_shown");
        }
        metrics.recordAsk(answer.result());
        metrics.recordAskDuration(System.nanoTime() - started);
        log.debug("[Chatbot] 결과={}, 최고 유사도={}", answer.result(), answer.score());
        return answer;
    }

    /**
     * 질문이 "내 정보" 질문처럼 보이면 칩을 제안한다. FAQ 후보 하한과 같은 기준(suggest-threshold)을 넘고 가장 비슷한 FAQ 만큼 가까울 때만
     * 붙인다. 칩은 제안일 뿐이라 틀려도 사용자가 누르지 않으면 아무 일도 없다.
     */
    private PersonalIntent personalAction(float[] vector, List<FaqIndex.Hit> hits) {
        if (personalRouter == null) {
            return null;
        }
        double bestFaq = hits.isEmpty() ? 0 : hits.get(0).score();
        return personalRouter
                .route(vector)
                .filter(h -> h.score() >= settings.suggestThreshold() && h.score() >= bestFaq)
                .map(PersonalIntentRouter.Hit::intent)
                .orElse(null);
    }

    private ChatbotAnswer decide(List<FaqIndex.Hit> hits) {
        if (hits.isEmpty()) {
            return ChatbotAnswer.unmatched(0);
        }
        FaqIndex.Hit best = hits.get(0);
        double second = hits.size() > 1 ? hits.get(1).score() : 0;
        // 점수가 높아도 2위와 거의 같으면(헷갈리는 FAQ 쌍) 자동 답변하지 않고 후보로 보여 준다.
        if (best.score() >= settings.matchThreshold()
                && best.score() - second >= settings.matchMargin()) {
            return new ChatbotAnswer(
                    ChatbotAnswer.Result.MATCHED,
                    best.faq(),
                    best.matchedQuestion(),
                    best.score(),
                    alternatives(hits));
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

    /** 자동 답변이 틀렸을 때 바로 고를 수 있는 "혹시 이 질문인가요?" 대안. 후보 제안 하한 이상인 2위부터 채운다. */
    private List<ChatbotAnswer.Suggestion> alternatives(List<FaqIndex.Hit> hits) {
        return hits.stream()
                .skip(1)
                .filter(h -> h.score() >= settings.suggestThreshold())
                .limit(settings.matchAlternatives())
                .map(
                        h ->
                                new ChatbotAnswer.Suggestion(
                                        h.faq().id(), h.faq().representativeQuestion(), h.score()))
                .toList();
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
