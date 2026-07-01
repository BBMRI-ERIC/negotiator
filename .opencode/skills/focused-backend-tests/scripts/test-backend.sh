#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="${ROOT_DIR}/backend"
REPORTS_DIR="${BACKEND_DIR}/target/surefire-reports"
RUN_LOG_FILE="${BACKEND_DIR}/target/test-run.log"

usage() {
  cat <<'EOF'
Usage:
  scripts/test-backend.sh <selector> [maven args...]
  scripts/test-backend.sh --selector <selector> [maven args...]
  scripts/test-backend.sh --all [maven args...]
  scripts/test-backend.sh --verbose <selector> [maven args...]

Selectors use Maven Surefire syntax, for example:
  NetworkTest
  NetworkTest#testDefaultValues
  '*ServiceTest'
  'NetworkTest,RequestServiceTest'

Examples:
  scripts/test-backend.sh NetworkTest
  scripts/test-backend.sh 'RequestServiceTest#shouldCreate'
  scripts/test-backend.sh '*Webhook*Test'
  scripts/test-backend.sh --all -Dspring.profiles.active=test

Notes:
  - Focused runs are the default; full suite requires --all.
  - On failure, this script prints a compact summary from surefire reports.
  - Full Maven output is saved to backend/target/test-run.log.
EOF
}

has_compile_errors() {
  if [[ ! -f "${RUN_LOG_FILE}" ]]; then
    return 1
  fi

  grep -Eq "Failed to execute goal .*:testCompile|COMPILATION ERROR" "${RUN_LOG_FILE}"
}

print_compact_compile_errors() {
  if [[ ! -f "${RUN_LOG_FILE}" ]]; then
    return
  fi

  if ! has_compile_errors; then
    return
  fi

  echo
  echo "Detected test compilation errors:"

  local -a all_compile_lines
  mapfile -t all_compile_lines < <(grep -E "^\[ERROR\] /.*src/test/java/.*:\[[0-9]+,[0-9]+\]" "${RUN_LOG_FILE}" || true)

  if [[ ${#all_compile_lines[@]} -eq 0 ]]; then
    grep -E "^\[ERROR\] (COMPILATION ERROR|Failed to execute goal|\[Help 1\])" "${RUN_LOG_FILE}" | head -n 20 || true
    return
  fi

  local max_lines=20
  local shown_count=${#all_compile_lines[@]}
  if [[ ${shown_count} -gt ${max_lines} ]]; then
    shown_count=${max_lines}
  fi

  local -a compile_lines
  compile_lines=("${all_compile_lines[@]:0:${shown_count}}")

  declare -A files_seen
  local line
  local path
  for line in "${compile_lines[@]}"; do
    path="${line#\[ERROR\] }"
    path="${path%%:[[]*}"
    files_seen["${path}"]=1
  done

  echo "Showing ${shown_count} of ${#all_compile_lines[@]} compile errors across ${#files_seen[@]} test file(s):"
  printf '%s\n' "${compile_lines[@]}"
}

print_failure_context() {
  if [[ ! -d "${REPORTS_DIR}" ]]; then
    echo "No surefire reports found at ${REPORTS_DIR}."
    return
  fi

  local -a newest_reports
  mapfile -t newest_reports < <(ls -1t "${REPORTS_DIR}"/*.txt 2>/dev/null | head -n 5 || true)

  if [[ ${#newest_reports[@]} -eq 0 ]]; then
    echo "No surefire text reports found at ${REPORTS_DIR}."
    return
  fi

  local found_failures=0
  local report
  for report in "${newest_reports[@]}"; do
    if grep -Eq "<<< FAILURE!|Failures: [1-9]|Errors: [1-9]" "${report}"; then
      found_failures=1
      echo
      echo "--- $(basename "${report}") ---"
      grep -E "<<< FAILURE!|Tests run:|Failures:|Errors:|Skipped:|Caused by:|Exception:|Assertion" "${report}" | head -n 50 || true
    fi
  done

  if [[ ${found_failures} -eq 0 ]]; then
    echo
    echo "No explicit failure markers found in the latest reports."
    echo "Most recent report tail ($(basename "${newest_reports[0]}")):"
    tail -n 40 "${newest_reports[0]}" || true
  fi
}

run_all=false
verbose=false
selector=""
declare -a maven_args
maven_args=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help)
      usage
      exit 0
      ;;
    --all)
      run_all=true
      ;;
    --verbose)
      verbose=true
      ;;
    -s|--selector)
      shift
      if [[ $# -eq 0 ]]; then
        echo "Missing selector value for --selector."
        usage
        exit 2
      fi
      selector="$1"
      ;;
    --)
      shift
      maven_args+=("$@")
      break
      ;;
    *)
      if [[ -z "${selector}" && "$1" != -* ]]; then
        selector="$1"
      else
        maven_args+=("$1")
      fi
      ;;
  esac
  shift
done

if [[ "${run_all}" == "false" && -z "${selector}" ]]; then
  echo "Focused selector is required unless --all is set."
  usage
  exit 2
fi

declare -a cmd
cmd=(
  mvn
  -f
  "${BACKEND_DIR}"
  -q
  --no-transfer-progress
  -DtrimStackTrace=true
  -Dstyle.color=never
  -Dsurefire.useFile=true
  -Dmaven.test.redirectTestOutputToFile=true
)

if [[ "${run_all}" == "false" ]]; then
  cmd+=("-Dtest=${selector}")
  echo "Running focused backend tests: ${selector}"
else
  echo "Running full backend test suite (--all explicitly requested)."
fi

cmd+=(test)

if [[ ${#maven_args[@]} -gt 0 ]]; then
  cmd+=("${maven_args[@]}")
fi

mkdir -p "${BACKEND_DIR}/target"
rm -f "${RUN_LOG_FILE}"

if [[ "${verbose}" == "true" ]]; then
  echo "Verbose mode enabled: streaming full Maven output."
fi

set +e
if [[ "${verbose}" == "true" ]]; then
  "${cmd[@]}" | tee "${RUN_LOG_FILE}"
  status=${PIPESTATUS[0]}
else
  "${cmd[@]}" >"${RUN_LOG_FILE}" 2>&1
  status=$?
fi
set -e

if [[ ${status} -eq 0 ]]; then
  echo "Test run passed."
  echo "Run log: ${RUN_LOG_FILE}"
  exit 0
fi

echo "Test run failed with exit code ${status}."
if has_compile_errors; then
  print_compact_compile_errors
else
  print_failure_context
fi
echo
echo "Run log: ${RUN_LOG_FILE}"
echo "Full report directory: ${REPORTS_DIR}"
exit "${status}"
