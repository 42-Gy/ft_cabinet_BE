#!/usr/bin/env python3
"""챗봇 임베딩의 '정답 벡터'를 파이썬 참조 구현으로 만든다.

Java 어댑터(ONNX Runtime + DJL 토크나이저 + 직접 구현한 평균 풀링)가 실제 모델에서도 올바른지 확인하려면, 구현과 무관한 기준이
필요하다. 여기서는 모델 저장소의 원본(PyTorch) 가중치를 sentence-transformers 로 돌려 벡터를 만든다. 풀링, 정규화,
토큰 길이 자르기를 라이브러리가 모델 설정대로 처리하므로 Java 쪽 구현과 겹치는 부분이 없다.

사용:
  python3 scripts/chatbot/golden_vectors.py --model-dir build/chatbot-models/e5 \
      --texts src/test/resources/chatbot/golden-texts.json
결과: <model-dir>/golden.json
  {"modelId", "repo", "revision", "prefix", "maxTokens", "reference", "texts": [...], "vectors": [[...], ...]}
  vectors 는 prefix 를 붙여 계산한 길이 1 벡터다. Java 쪽은 prefix 를 직접 붙이므로 texts 에는 접두어가 없는 원문을 둔다.

필요: pip install sentence-transformers (CPU 용 torch)
model-dir 에는 fetch-model.sh 가 만든 model.properties(repo/revision/prefix/maxTokens)가 있어야 한다.
"""
import argparse
import json
import sys
from pathlib import Path


def read_properties(path: Path) -> dict:
    props = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            props[key.strip()] = value
    return props


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model-dir", required=True)
    parser.add_argument("--texts", required=True)
    parser.add_argument("--out", default=None)
    args = parser.parse_args()

    model_dir = Path(args.model_dir)
    props = read_properties(model_dir / "model.properties")
    repo = props["repo"]
    revision = props.get("revision") or None
    prefix = props.get("prefix", "")
    max_tokens = int(props.get("maxTokens", "128"))
    texts = json.loads(Path(args.texts).read_text(encoding="utf-8"))
    if not isinstance(texts, list) or not all(isinstance(t, str) for t in texts):
        print("texts 파일은 문자열 배열이어야 합니다.", file=sys.stderr)
        return 1

    import sentence_transformers
    from sentence_transformers import SentenceTransformer

    model = SentenceTransformer(repo, revision=revision, device="cpu")
    # Java 어댑터가 쓰는 최대 토큰 수와 같게 맞춘다(길이가 긴 입력의 자르기 결과를 비교하려는 것).
    model.max_seq_length = max_tokens
    vectors = model.encode(
        [prefix + t for t in texts],
        normalize_embeddings=True,
        convert_to_numpy=True,
        batch_size=8,
        show_progress_bar=False,
    )

    out = Path(args.out) if args.out else model_dir / "golden.json"
    out.write_text(
        json.dumps(
            {
                "modelId": props.get("modelId", ""),
                "repo": repo,
                "revision": revision,
                "prefix": prefix,
                "maxTokens": max_tokens,
                "reference": f"sentence-transformers {sentence_transformers.__version__} (PyTorch, fp32)",
                "texts": texts,
                "vectors": [[float(x) for x in row] for row in vectors],
            },
            ensure_ascii=False,
        ),
        encoding="utf-8",
    )
    print(f"골든 벡터 {len(texts)}개를 {out} 에 썼습니다(차원 {vectors.shape[1]}).", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
