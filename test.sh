#!/bin/bash
# 医学问答助手 — 命令行测试脚本
# 用法: ./test.sh "头疼应该挂什么科"

Q="${1:-头疼应该挂什么科}"
BASE="http://localhost:9090/api/baymd"

echo "=== 测试: $Q ==="
echo ""

curl -s -N -G "$BASE/rag/v3/chat" --data-urlencode "question=$Q" 2>&1 | while IFS= read -r line; do
  if [[ "$line" == data:* ]]; then
    json="${line#data:}"
    [[ "$json" == "[DONE]" ]] && continue
    content=$(echo "$json" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('content',''))" 2>/dev/null)
    [[ -n "$content" ]] && printf "%s" "$content"
  fi
done

echo ""
echo "=== 完成 ==="
