#!/usr/bin/env python3
"""
Test runner for PostgreSQL MCP Server
"""

import subprocess
import sys
import argparse
import os


def run_command(command, description):
    """Run a command and return the result."""
    print(f"\n{'='*60}")
    print(f"Running: {description}")
    print(f"Command: {command}")
    print(f"{'='*60}")

    result = subprocess.run(command, shell=True, capture_output=True, text=True)

    if result.stdout:
        print("STDOUT:")
        print(result.stdout)

    if result.stderr:
        print("STDERR:")
        print(result.stderr)

    if result.returncode != 0:
        print(f"❌ Command failed with return code {result.returncode}")
        return False
    else:
        print("✅ Command completed successfully")
        return True


def run_unit_tests():
    """Run unit tests."""
    return run_command(
        "python -m pytest tests/unit/ -v --tb=short",
        "Unit Tests"
    )


def run_integration_tests():
    """Run integration tests."""
    return run_command(
        "python -m pytest tests/integration/ -v --tb=short",
        "Integration Tests"
    )


def run_all_tests():
    """Run all tests."""
    return run_command(
        "python -m pytest tests/ -v --tb=short",
        "All Tests"
    )


def run_coverage():
    """Run tests with coverage report."""
    commands = [
        ("python -m pytest tests/ --cov=src --cov-report=term-missing --cov-report=html", "Coverage Tests"),
    ]

    success = True
    for command, description in commands:
        if not run_command(command, description):
            success = False

    if success:
        print("\n📊 Coverage report generated in htmlcov/index.html")

    return success


def run_lint():
    """Run code linting."""
    commands = [
        ("python -m black --check src/ tests/", "Black Code Formatting Check"),
        ("python -m ruff check src/ tests/", "Ruff Linting"),
        ("python -m mypy src/", "MyPy Type Checking"),
    ]

    success = True
    for command, description in commands:
        if not run_command(command, description):
            success = False

    return success


def fix_formatting():
    """Fix code formatting."""
    commands = [
        ("python -m black src/ tests/", "Black Code Formatting"),
        ("python -m ruff check --fix src/ tests/", "Ruff Auto-fix"),
    ]

    success = True
    for command, description in commands:
        if not run_command(command, description):
            success = False

    return success


def install_dev_dependencies():
    """Install development dependencies."""
    commands = [
        ("pip install -e .[dev,test]", "Install Development Dependencies"),
        ("pip install coverage pytest-cov", "Install Coverage Tools"),
    ]

    success = True
    for command, description in commands:
        if not run_command(command, description):
            success = False

    return success


def main():
    """Main test runner."""
    parser = argparse.ArgumentParser(description="PostgreSQL MCP Server Test Runner")
    parser.add_argument(
        "command",
        choices=["unit", "integration", "all", "coverage", "lint", "fix", "install"],
        help="Test command to run"
    )
    parser.add_argument(
        "--verbose",
        "-v",
        action="store_true",
        help="Enable verbose output"
    )

    args = parser.parse_args()

    # Set environment variables for testing
    os.environ["PYTHONPATH"] = os.path.join(os.path.dirname(__file__), "src")

    print("🚀 PostgreSQL MCP Server Test Runner")
    print(f"Working directory: {os.getcwd()}")
    print(f"Python executable: {sys.executable}")
    print(f"Python version: {sys.version}")

    success = False

    if args.command == "install":
        success = install_dev_dependencies()
    elif args.command == "unit":
        success = run_unit_tests()
    elif args.command == "integration":
        success = run_integration_tests()
    elif args.command == "all":
        success = run_all_tests()
    elif args.command == "coverage":
        success = run_coverage()
    elif args.command == "lint":
        success = run_lint()
    elif args.command == "fix":
        success = fix_formatting()

    if success:
        print("\n🎉 All operations completed successfully!")
        sys.exit(0)
    else:
        print("\n💥 Some operations failed!")
        sys.exit(1)


if __name__ == "__main__":
    main()