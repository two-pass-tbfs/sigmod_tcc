#!/bin/bash
# Accept variables from the command line, with fallbacks
VERTEX_COUNT=${1:-"n10000"}
P_CONSTANT=${2:-"50"}
PROGRAM=${3:-"tcc-1.0-two-tbfs-app"}
BASE_DIR="."

DATASET="erdos_reyni/$VERTEX_COUNT"
INPUT_DIR="$BASE_DIR/datasets/$DATASET"
OUTPUT_DIR="$BASE_DIR/output/$DATASET"

echo "Starting synthetic run: $VERTEX_COUNT | p$P_CONSTANT | Program: $PROGRAM"
echo "Tailing output to: $OUTPUT_DIR/p${P_CONSTANT}/${PROGRAM}.log"

# Ensure the output directory exists
mkdir -p "${OUTPUT_DIR}/p${P_CONSTANT}"

# Run the command using the variables
nohup /usr/bin/time -v java \
  -Djava.util.logging.config.file=${BASE_DIR}/two-tbfs-tcc/tcc/src/main/resources/logging.properties \
  -Xmx16g \
  -Ddirected=false \
  -Dvertices="${INPUT_DIR}/vertices.csv" \
  -Dstream="${INPUT_DIR}/p${P_CONSTANT}/graph.csv" \
  -jar ${BASE_DIR}/two-tbfs-tcc/tcc/target/${PROGRAM}.jar > "$OUTPUT_DIR/p${P_CONSTANT}/${PROGRAM}.log" &

echo "Job submitted to background. To view progress, run: tail -f $OUTPUT_DIR/p${P_CONSTANT}/${PROGRAM}.log"