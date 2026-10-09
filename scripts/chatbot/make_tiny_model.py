#!/usr/bin/env python3
"""챗봇 임베딩 어댑터 테스트용 '아주 작은' ONNX 모델과 토크나이저를 만든다.

실제 모델(수백 MB)은 레포에 넣지 않는다. 이 장난감 모델은 의미를 이해하지 못하고, 단어마다 고정된 무작위 벡터를 돌려줄 뿐이지만
토크나이저 로딩, 입력 이름 처리(input_ids / attention_mask / token_type_ids), 출력 형태(토큰별 은닉 상태 / 이미 풀링된 문장 벡터),
평균 풀링과 정규화가 Java 쪽에서 올바른지 검증하기에는 충분하다. 기대값은 같은 모델을 파이썬 onnxruntime 으로 돌려 계산한다.

사용: python3 scripts/chatbot/make_tiny_model.py <출력 디렉터리>
필요: pip install onnx onnxruntime tokenizers numpy
"""
import json
import sys
from pathlib import Path

import numpy as np
import onnx
import onnxruntime as ort
from onnx import TensorProto, helper, numpy_helper
from tokenizers import Tokenizer, models, pre_tokenizers, processors

SPECIALS = ["<s>", "<pad>", "</s>", "<unk>"]
WORDS = [
    "사물함", "대여", "반납", "연장", "이사", "패널티", "코인", "아이템", "수박", "로그인",
    "어떻게", "하나요", "언제", "얼마", "가능", "불가", "연체", "예약", "카카오", "구글",
    "the", "cabinet", "return", "extend", "how", "to", "penalty", "swap", "coin", "item",
]
DIM = 8
SENTENCES = [
    "사물함 대여 어떻게 하나요",
    "반납 연장 가능",
    "처음 보는 단어만 있는 문장",
    "how to return the cabinet",
    "",
]


def build_tokenizer(out: Path) -> Tokenizer:
    vocab = {tok: i for i, tok in enumerate(SPECIALS + WORDS)}
    tok = Tokenizer(models.WordLevel(vocab=vocab, unk_token="<unk>"))
    tok.pre_tokenizer = pre_tokenizers.Whitespace()
    tok.post_processor = processors.TemplateProcessing(
        single="<s> $A </s>",
        special_tokens=[("<s>", vocab["<s>"]), ("</s>", vocab["</s>"])],
    )
    tok.save(str(out / "tokenizer.json"))
    return tok


def make_model(out: Path, name: str, emb: np.ndarray, kind: str) -> Path:
    """kind: plain(토큰별 출력) | tti(token_type_ids 입력이 필수) | pooled(이미 풀링된 sentence_embedding 출력)."""
    inputs = [
        helper.make_tensor_value_info("input_ids", TensorProto.INT64, ["batch", "seq"]),
        helper.make_tensor_value_info("attention_mask", TensorProto.INT64, ["batch", "seq"]),
    ]
    nodes = [helper.make_node("Gather", ["emb", "input_ids"], ["hidden"], axis=0)]
    inits = [numpy_helper.from_array(emb, "emb")]
    last = "hidden"

    # attention_mask 가 실제로 결과에 쓰이도록: 마스크된(=1) 토큰마다 0.25 를 더한다.
    inits.append(numpy_helper.from_array(np.array([0.25], dtype=np.float32), "quarter"))
    inits.append(numpy_helper.from_array(np.array([2], dtype=np.int64), "axes_last"))
    nodes += [
        helper.make_node("Cast", ["attention_mask"], ["mask_f"], to=TensorProto.FLOAT),
        helper.make_node("Unsqueeze", ["mask_f", "axes_last"], ["mask_u"]),
        helper.make_node("Mul", ["mask_u", "quarter"], ["mask_bias"]),
        helper.make_node("Add", [last, "mask_bias"], ["hidden_m"]),
    ]
    last = "hidden_m"

    if kind == "tti":
        inputs.append(helper.make_tensor_value_info("token_type_ids", TensorProto.INT64, ["batch", "seq"]))
        inits.append(numpy_helper.from_array(np.array([100.0], dtype=np.float32), "hundred"))
        nodes += [
            helper.make_node("Cast", ["token_type_ids"], ["tti_f"], to=TensorProto.FLOAT),
            helper.make_node("Unsqueeze", ["tti_f", "axes_last"], ["tti_u"]),
            helper.make_node("Mul", ["tti_u", "hundred"], ["tti_bias"]),
            helper.make_node("Add", [last, "tti_bias"], ["hidden_t"]),
        ]
        last = "hidden_t"

    if kind == "pooled":
        nodes.append(helper.make_node("ReduceMean", [last], ["sentence_embedding"], axes=[1], keepdims=0))
        outputs = [helper.make_tensor_value_info("sentence_embedding", TensorProto.FLOAT, ["batch", DIM])]
    else:
        nodes.append(helper.make_node("Identity", [last], ["last_hidden_state"]))
        outputs = [helper.make_tensor_value_info("last_hidden_state", TensorProto.FLOAT, ["batch", "seq", DIM])]

    graph = helper.make_graph(nodes, name, inputs, outputs, initializer=inits)
    model = helper.make_model(graph, opset_imports=[helper.make_opsetid("", 17)])
    model.ir_version = 8
    onnx.checker.check_model(model)
    path = out / f"{name}.onnx"
    onnx.save(model, str(path))
    return path


def reference(path: Path, tok: Tokenizer, kind: str, text: str) -> list:
    sess = ort.InferenceSession(str(path), providers=["CPUExecutionProvider"])
    enc = tok.encode(text)
    ids = np.array([enc.ids], dtype=np.int64)
    feed = {"input_ids": ids, "attention_mask": np.ones_like(ids)}
    if kind == "tti":
        feed["token_type_ids"] = np.zeros_like(ids)
    out = sess.run(None, feed)[0]
    vec = out[0] if kind == "pooled" else out[0].mean(axis=0)
    vec = vec / max(np.linalg.norm(vec), 1e-12)
    return [float(x) for x in vec]


def main() -> None:
    out = Path(sys.argv[1])
    out.mkdir(parents=True, exist_ok=True)
    rng = np.random.default_rng(42)
    emb = rng.normal(size=(len(SPECIALS) + len(WORDS), DIM)).astype(np.float32)

    tok = build_tokenizer(out)
    expected = {}
    for kind, name in [("plain", "model"), ("tti", "model-tti"), ("pooled", "model-pooled")]:
        path = make_model(out, name, emb, kind)
        expected[name] = {s: reference(path, tok, kind, s) for s in SENTENCES}
    (out / "expected.json").write_text(json.dumps({"dimension": DIM, "vectors": expected}, ensure_ascii=False, indent=1), encoding="utf-8")
    print("생성 완료:", sorted(p.name for p in out.iterdir()))


if __name__ == "__main__":
    main()
