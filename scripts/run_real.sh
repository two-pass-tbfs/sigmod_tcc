#!/bin/bash
# Specify the file structure and program
DATASET=${1:-"bitcoin_alpha"}
PROGRAM=${2:-"tcc-1.0-two-tbfs-app"}

BASE_DIR="."

INPUT_DIR="$BASE_DIR/datasets/$DATASET"
OUTPUT_DIR="$BASE_DIR/output/$DATASET"

echo "Starting run for Dataset: $DATASET using Program: $PROGRAM"
echo "Tailing output to: $OUTPUT_DIR/${PROGRAM}.log"

# Ensure the output directory exists
mkdir -p "$OUTPUT_DIR"

# Run the command using the variables
nohup /usr/bin/time -v java \
  -Djava.util.logging.config.file=${BASE_DIR}/two-tbfs-tcc/tcc/src/main/resources/logging.properties \
  -Xmx16g \
  -Ddirected=true \
  -Dvertices="${INPUT_DIR}/vertices.csv" \
  -Dstream="${INPUT_DIR}/graph.csv" \
  -jar ${BASE_DIR}/two-tbfs-tcc/tcc/target/${PROGRAM}.jar > "$OUTPUT_DIR/${PROGRAM}.log" &

echo "Job submitted to background. To view progress, run: tail -f $OUTPUT_DIR/${PROGRAM}.log"