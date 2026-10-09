#!/usr/bin/env bash
# Xuất mọi sơ đồ *.puml trong thư mục này ra PNG ở docs/diagrams/out/ (thư mục đã nằm trong .gitignore).
# Cần Java 17+. PlantUML được tải một lần từ Maven Central (phiên bản ghim cố định) và giữ trong out/.
# Sơ đồ dạng đồ thị dùng "!pragma layout smetana" nên không cần cài Graphviz.
set -euo pipefail

PLANTUML_VERSION=1.2025.4
DIR="$(cd "$(dirname "$0")" && pwd)"
OUT="$DIR/out"
JAR="$OUT/plantuml-$PLANTUML_VERSION.jar"

mkdir -p "$OUT"
if [ ! -f "$JAR" ]; then
  curl -fsSL -o "$JAR" \
    "https://repo1.maven.org/maven2/net/sourceforge/plantuml/plantuml/$PLANTUML_VERSION/plantuml-$PLANTUML_VERSION.jar"
fi

# Git Bash trên Windows: Java không hiểu đường dẫn dạng /c/..., phải đổi sang C:\...
to_native() { if command -v cygpath > /dev/null; then cygpath -w "$1"; else printf '%s' "$1"; fi; }

# -failfast2: dừng ngay nếu có sơ đồ sai cú pháp thay vì xuất ảnh báo lỗi
java -Djava.awt.headless=true -jar "$(to_native "$JAR")" -charset UTF-8 -failfast2 -tpng \
  -o "$(to_native "$OUT")" "$(to_native "$DIR")"

ls -1 "$OUT"/*.png
