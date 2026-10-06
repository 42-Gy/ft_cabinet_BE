# 챗봇 임베딩 모델 내려받기 단계.
# CHATBOT_MODEL(예: minilm, e5)을 지정하면 chatbot-models.lock 에 고정된 revision/SHA256 으로만 받고,
# 하나라도 다르거나 고정되지 않았으면(PENDING) 이미지 빌드가 실패한다.
# 지정하지 않으면(기본) 아무것도 받지 않아 기존과 똑같이 빌드되고, 챗봇은 꺼진 채로 둔다.
FROM amazoncorretto:17 AS chatbot-model
ARG CHATBOT_MODEL=""
WORKDIR /work
COPY chatbot-models.lock ./
COPY scripts/chatbot/fetch-model.sh scripts/chatbot/fetch-model.sh
RUN mkdir -p /model \
    && if [ -n "$CHATBOT_MODEL" ]; then \
         bash scripts/chatbot/fetch-model.sh --mode strict --id "$CHATBOT_MODEL" --out /model; \
       fi

FROM amazoncorretto:17

WORKDIR /app

COPY --from=chatbot-model /model /app/chatbot-model
COPY build/libs/*SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
