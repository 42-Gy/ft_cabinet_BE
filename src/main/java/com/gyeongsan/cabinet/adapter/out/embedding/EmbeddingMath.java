package com.gyeongsan.cabinet.adapter.out.embedding;

/** 임베딩 후처리: 토큰별 벡터를 문장 벡터 하나로 합치고(평균 풀링), 길이를 1로 맞춘다(L2 정규화). */
final class EmbeddingMath {

    private EmbeddingMath() {}

    /** attention_mask 가 1 인 토큰만 평균낸다(패딩 제외). */
    static float[] meanPool(float[][] hidden, long[] attentionMask) {
        if (hidden.length == 0) {
            throw new IllegalArgumentException("토큰이 없습니다.");
        }
        int dim = hidden[0].length;
        double[] sum = new double[dim];
        int count = 0;
        for (int t = 0; t < hidden.length; t++) {
            if (t < attentionMask.length && attentionMask[t] == 0) {
                continue;
            }
            for (int d = 0; d < dim; d++) {
                sum[d] += hidden[t][d];
            }
            count++;
        }
        if (count == 0) {
            throw new IllegalArgumentException("유효한 토큰이 없습니다.");
        }
        float[] pooled = new float[dim];
        for (int d = 0; d < dim; d++) {
            pooled[d] = (float) (sum[d] / count);
        }
        return pooled;
    }

    /** 길이를 1로 만든다. 영벡터면 그대로 둔다(내적이 0 이 되어 어떤 FAQ 와도 비슷하지 않은 것으로 취급된다). */
    static float[] normalize(float[] vector) {
        double norm = 0;
        for (float v : vector) {
            norm += (double) v * v;
        }
        norm = Math.sqrt(norm);
        if (norm < 1e-12) {
            return vector.clone();
        }
        float[] result = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            result[i] = (float) (vector[i] / norm);
        }
        return result;
    }
}
