---
name: focused-runtests
description: "Run focused tests with runTests during development. Use for single-test and file-level reruns, and for troubleshooting No tests found in the files, passed=0 failed=0 no-op runs, selector mismatches, and large redirected test output."
argument-hint: 'Scope and target, for example: single test, file-level retest, package subset, or coverage run'
---

# Focused Test Execution with runTests

## What This Skill Produces
- Fast, scoped test execution using the native runTests tool
- Minimal-noise feedback loops for development sessions
- Reliable fallback behavior for discovery and selector mismatches
- Correct interpretation of aggregate counts and container-level nodes

## When to Use
- Validate one test method while iterating on a fix
- Re-run one test file after local changes
- Run tests in a package subtree without running the full suite
- Run tests in a package subtree including subpackages by default
- Collect targeted coverage for changed areas
- Troubleshoot `No tests found in the files`
- Troubleshoot runs returning `passed=0 failed=0`

## Procedure
1. Determine the narrowest scope that matches the request.
- Single test: files + one testNames entry
- Single file/class: files only
- Package subset: enumerate files first, then pass files list
- Package subset defaults to including subpackages unless the request needs tighter feedback

2. Build the first runTests request with focused inputs.
- Prefer mode: run unless coverage is explicitly requested
- Use coverage only when needed, with coverageFiles for targeted reporting
- Use workspace-relative file paths by default

3. Execute with explicit file targeting when possible.
- Start with the smallest file set that can satisfy the request

4. Use a selector ladder for single-test requests.
- Start with a unique method fragment
- If unresolved, retry with method name including parameter list
- If unresolved, run one diagnostic probe using testNames only
- Re-apply file constraints after probe success

5. If discovery fails with "No tests found in the files", apply fallback in order.
- Verify the file path points to a real test file
- Retry with workspace-relative file paths
- If files + testNames was requested, run one diagnostic probe using testNames only
- Use the probe only to confirm discovery, then re-apply file constraints for the final scoped run
- If scoped rerun still fails, report a discovery/path issue and list what was attempted

6. If a run returns `passed=0 failed=0`, treat it as a soft failure state.
- Consider it a selector mismatch or non-actionable run
- Retry with a broader selector for the same file
- If needed, remove selector and run file-level once, then re-narrow

7. If a "single test" run appears to include extra tests, tighten the selector.
- testNames behaves like case-insensitive substring matching
- Use a more specific or unique test name fragment

8. For package-level runs, enumerate files before running.
- Collect test files under the package path, including subpackages by default
- Exclude subpackages when faster, localized feedback is needed
- Exclude helper/config files that are not test classes
- Pass only runnable test files to runTests

9. Handle redirected large outputs.
- If runTests reports that output was written to a file, read that artifact before concluding results

10. Report results in a concise, actionable way.
- Include passed/failed totals
- List failing test case names
- Include first actionable stack location for each failure

## Decision Points
- Need fastest feedback: run one test
- Need confidence in local change area: run one file or package subset
- Need broader confidence in an area: include subpackages in package runs
- Need highly localized feedback: exclude subpackages and run only closest files
- Need deeper quality signal: run coverage mode on targeted files
- Need diagnosis: re-run only the failing tests after first package run

## Completion Checks
- Requested scope is honored (single test, file, or package subset)
- No accidental broad test execution
- `No tests found in the files` and `passed=0 failed=0` outcomes are either resolved or explicitly documented
- Result includes numeric pass/fail summary
- Failures are mapped to actionable test names and locations
- Any fallback path used is documented in the response
- Aggregate totals are interpreted correctly before conclusions are made

## Interpreting Test Counts
- runTests summaries can include both test containers and test cases in totals
- A single targeted test run can show passed=2, failed=0 when one test case passes and its parent container is also counted as passed
- Container-level failure entries can appear alongside case-level failures
- Do not assume the total equals the number of distinct test methods; verify by reported test names and failure entries

## Guardrails
- Prefer runTests over terminal-based test commands when native tooling is available
- Keep test runs as narrow as possible during active development
- Treat path resolution, selector specificity, and test discovery as separate concerns and debug them independently
