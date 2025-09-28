#!/usr/bin/env python3
"""
BDD 測試報告產生器
用於分析 MCP Registry Java 專案的 BDD 測試結果並產生詳細的 HTML 報告
"""

import os
import sys
import xml.etree.ElementTree as ET
import json
import argparse
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Any, Optional
from dataclasses import dataclass, asdict
from jinja2 import Template

@dataclass
class TestCase:
    """測試案例資料結構"""
    name: str
    classname: str
    time: float
    status: str  # passed, failed, error, skipped
    failure_message: Optional[str] = None
    error_message: Optional[str] = None
    system_out: Optional[str] = None
    system_err: Optional[str] = None

@dataclass
class TestSuite:
    """測試套件資料結構"""
    name: str
    tests: int
    failures: int
    errors: int
    skipped: int
    time: float
    timestamp: str
    test_cases: List[TestCase]

@dataclass
class ModuleReport:
    """模組報告資料結構"""
    name: str
    test_suites: List[TestSuite]
    total_tests: int
    total_failures: int
    total_errors: int
    total_skipped: int
    total_time: float
    success_rate: float

class BDDReportGenerator:
    """BDD 測試報告產生器"""

    def __init__(self, reports_dir: str):
        self.reports_dir = Path(reports_dir)
        self.modules = ['mcp-core', 'mcp-postgresql-server', 'mcp-mysql-server']

    def parse_surefire_xml(self, xml_file: Path) -> Optional[TestSuite]:
        """解析 Surefire XML 測試報告"""
        try:
            tree = ET.parse(xml_file)
            root = tree.getroot()

            # 獲取測試套件基本資訊
            suite_name = root.get('name', '')
            tests = int(root.get('tests', 0))
            failures = int(root.get('failures', 0))
            errors = int(root.get('errors', 0))
            skipped = int(root.get('skipped', 0))
            time = float(root.get('time', 0))
            timestamp = root.get('timestamp', '')

            # 解析測試案例
            test_cases = []
            for testcase in root.findall('.//testcase'):
                case_name = testcase.get('name', '')
                classname = testcase.get('classname', '')
                case_time = float(testcase.get('time', 0))

                # 檢查測試狀態
                failure_elem = testcase.find('failure')
                error_elem = testcase.find('error')
                skipped_elem = testcase.find('skipped')

                if failure_elem is not None:
                    status = 'failed'
                    failure_message = failure_elem.get('message', '')
                    error_message = None
                elif error_elem is not None:
                    status = 'error'
                    failure_message = None
                    error_message = error_elem.get('message', '')
                elif skipped_elem is not None:
                    status = 'skipped'
                    failure_message = None
                    error_message = None
                else:
                    status = 'passed'
                    failure_message = None
                    error_message = None

                # 獲取標準輸出和錯誤輸出
                system_out_elem = testcase.find('system-out')
                system_err_elem = testcase.find('system-err')
                system_out = system_out_elem.text if system_out_elem is not None else None
                system_err = system_err_elem.text if system_err_elem is not None else None

                test_case = TestCase(
                    name=case_name,
                    classname=classname,
                    time=case_time,
                    status=status,
                    failure_message=failure_message,
                    error_message=error_message,
                    system_out=system_out,
                    system_err=system_err
                )
                test_cases.append(test_case)

            return TestSuite(
                name=suite_name,
                tests=tests,
                failures=failures,
                errors=errors,
                skipped=skipped,
                time=time,
                timestamp=timestamp,
                test_cases=test_cases
            )

        except Exception as e:
            print(f"解析 XML 檔案 {xml_file} 時發生錯誤: {e}")
            return None

    def analyze_module_tests(self, module_name: str) -> Optional[ModuleReport]:
        """分析模組測試結果"""
        module_dir = self.reports_dir / module_name
        if not module_dir.exists():
            print(f"模組 {module_name} 的報告目錄不存在: {module_dir}")
            return None

        test_suites = []
        total_tests = 0
        total_failures = 0
        total_errors = 0
        total_skipped = 0
        total_time = 0.0

        # 查找所有 XML 測試報告
        for xml_file in module_dir.glob('*.xml'):
            suite = self.parse_surefire_xml(xml_file)
            if suite:
                test_suites.append(suite)
                total_tests += suite.tests
                total_failures += suite.failures
                total_errors += suite.errors
                total_skipped += suite.skipped
                total_time += suite.time

        if total_tests > 0:
            success_rate = ((total_tests - total_failures - total_errors) / total_tests) * 100
        else:
            success_rate = 0.0

        return ModuleReport(
            name=module_name,
            test_suites=test_suites,
            total_tests=total_tests,
            total_failures=total_failures,
            total_errors=total_errors,
            total_skipped=total_skipped,
            total_time=total_time,
            success_rate=success_rate
        )

    def extract_bdd_scenarios(self, test_case: TestCase) -> Dict[str, Any]:
        """從測試案例中提取 BDD 場景資訊"""
        # 從測試名稱中提取使用者故事和場景描述
        scenario_info = {
            'user_story': '',
            'scenario': '',
            'given': '',
            'when': '',
            'then': ''
        }

        # 解析測試方法名稱
        method_name = test_case.name
        if 'should_' in method_name:
            scenario_info['scenario'] = method_name.replace('should_', '').replace('_', ' ')

        # 從類別名稱提取功能區域
        if 'BDDTest' in test_case.classname:
            feature_area = test_case.classname.replace('BDDTest', '').split('.')[-1]
            scenario_info['feature_area'] = feature_area

        return scenario_info

    def generate_html_report(self, module_reports: List[ModuleReport]) -> str:
        """產生 HTML 報告"""
        template = Template("""
<!DOCTYPE html>
<html lang="zh-TW">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MCP Registry BDD 測試報告</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            margin: 0;
            padding: 20px;
            background-color: #f5f5f5;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            border-radius: 10px;
            margin-bottom: 30px;
            text-align: center;
        }
        .summary {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }
        .summary-card {
            background: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            text-align: center;
        }
        .summary-card h3 {
            margin: 0 0 10px 0;
            color: #333;
        }
        .summary-card .number {
            font-size: 2em;
            font-weight: bold;
            color: #667eea;
        }
        .module-section {
            background: white;
            margin-bottom: 30px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            overflow: hidden;
        }
        .module-header {
            background: #667eea;
            color: white;
            padding: 20px;
            font-size: 1.2em;
            font-weight: bold;
        }
        .module-content {
            padding: 20px;
        }
        .test-suite {
            margin-bottom: 20px;
            border: 1px solid #ddd;
            border-radius: 5px;
        }
        .suite-header {
            background: #f8f9fa;
            padding: 15px;
            border-bottom: 1px solid #ddd;
            font-weight: bold;
        }
        .test-case {
            padding: 10px 15px;
            border-bottom: 1px solid #eee;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .test-case:last-child {
            border-bottom: none;
        }
        .test-case.passed {
            background-color: #d4edda;
            border-left: 4px solid #28a745;
        }
        .test-case.failed {
            background-color: #f8d7da;
            border-left: 4px solid #dc3545;
        }
        .test-case.error {
            background-color: #fff3cd;
            border-left: 4px solid #ffc107;
        }
        .test-case.skipped {
            background-color: #e2e3e5;
            border-left: 4px solid #6c757d;
        }
        .status-badge {
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 0.8em;
            font-weight: bold;
            text-transform: uppercase;
        }
        .badge-passed { background-color: #28a745; color: white; }
        .badge-failed { background-color: #dc3545; color: white; }
        .badge-error { background-color: #ffc107; color: black; }
        .badge-skipped { background-color: #6c757d; color: white; }
        .error-details {
            margin-top: 10px;
            padding: 10px;
            background-color: #f8f9fa;
            border-radius: 4px;
            font-family: monospace;
            font-size: 0.9em;
            white-space: pre-wrap;
        }
        .progress-bar {
            width: 100%;
            height: 20px;
            background-color: #e9ecef;
            border-radius: 10px;
            overflow: hidden;
            margin: 10px 0;
        }
        .progress-fill {
            height: 100%;
            background: linear-gradient(90deg, #28a745, #20c997);
            transition: width 0.3s ease;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>MCP Registry BDD 測試報告</h1>
        <p>產生時間: {{ generation_time }}</p>
    </div>

    <div class="summary">
        <div class="summary-card">
            <h3>總測試數</h3>
            <div class="number">{{ total_tests }}</div>
        </div>
        <div class="summary-card">
            <h3>成功率</h3>
            <div class="number">{{ "%.1f"|format(overall_success_rate) }}%</div>
            <div class="progress-bar">
                <div class="progress-fill" style="width: {{ overall_success_rate }}%"></div>
            </div>
        </div>
        <div class="summary-card">
            <h3>失敗數</h3>
            <div class="number" style="color: #dc3545;">{{ total_failures }}</div>
        </div>
        <div class="summary-card">
            <h3>錯誤數</h3>
            <div class="number" style="color: #ffc107;">{{ total_errors }}</div>
        </div>
    </div>

    {% for module in modules %}
    <div class="module-section">
        <div class="module-header">
            {{ module.name }}
            <span style="float: right;">
                成功率: {{ "%.1f"|format(module.success_rate) }}%
                ({{ module.total_tests - module.total_failures - module.total_errors }}/{{ module.total_tests }})
            </span>
        </div>
        <div class="module-content">
            {% for suite in module.test_suites %}
            <div class="test-suite">
                <div class="suite-header">
                    {{ suite.name }}
                    <span style="float: right; font-weight: normal;">
                        執行時間: {{ "%.3f"|format(suite.time) }}s
                    </span>
                </div>
                {% for test_case in suite.test_cases %}
                <div class="test-case {{ test_case.status }}">
                    <div>
                        <strong>{{ test_case.name }}</strong>
                        <br>
                        <small style="color: #666;">{{ test_case.classname }}</small>
                        {% if test_case.failure_message or test_case.error_message %}
                        <div class="error-details">
                            {{ test_case.failure_message or test_case.error_message }}
                        </div>
                        {% endif %}
                    </div>
                    <div>
                        <span class="status-badge badge-{{ test_case.status }}">{{ test_case.status }}</span>
                        <small style="margin-left: 10px;">{{ "%.3f"|format(test_case.time) }}s</small>
                    </div>
                </div>
                {% endfor %}
            </div>
            {% endfor %}
        </div>
    </div>
    {% endfor %}

    <div style="text-align: center; margin-top: 50px; color: #666;">
        <p>由 MCP Registry BDD 測試報告產生器產生</p>
    </div>
</body>
</html>
        """)

        # 計算總體統計
        total_tests = sum(m.total_tests for m in module_reports)
        total_failures = sum(m.total_failures for m in module_reports)
        total_errors = sum(m.total_errors for m in module_reports)

        if total_tests > 0:
            overall_success_rate = ((total_tests - total_failures - total_errors) / total_tests) * 100
        else:
            overall_success_rate = 0.0

        return template.render(
            generation_time=datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            modules=module_reports,
            total_tests=total_tests,
            total_failures=total_failures,
            total_errors=total_errors,
            overall_success_rate=overall_success_rate
        )

    def generate_json_report(self, module_reports: List[ModuleReport]) -> str:
        """產生 JSON 格式報告"""
        report_data = {
            'generation_time': datetime.now().isoformat(),
            'summary': {
                'total_tests': sum(m.total_tests for m in module_reports),
                'total_failures': sum(m.total_failures for m in module_reports),
                'total_errors': sum(m.total_errors for m in module_reports),
                'total_skipped': sum(m.total_skipped for m in module_reports),
                'total_time': sum(m.total_time for m in module_reports),
            },
            'modules': [asdict(module) for module in module_reports]
        }

        return json.dumps(report_data, indent=2, ensure_ascii=False)

    def generate_reports(self) -> None:
        """產生所有格式的報告"""
        print("開始分析 BDD 測試結果...")

        module_reports = []
        for module_name in self.modules:
            print(f"分析模組: {module_name}")
            module_report = self.analyze_module_tests(module_name)
            if module_report:
                module_reports.append(module_report)
                print(f"  - 測試數: {module_report.total_tests}")
                print(f"  - 成功率: {module_report.success_rate:.1f}%")
            else:
                print(f"  - 無測試結果")

        if not module_reports:
            print("未找到任何測試結果")
            return

        # 產生 HTML 報告
        print("\n產生 HTML 報告...")
        html_report = self.generate_html_report(module_reports)
        html_file = self.reports_dir / 'bdd-test-report.html'
        html_file.write_text(html_report, encoding='utf-8')
        print(f"HTML 報告已儲存至: {html_file}")

        # 產生 JSON 報告
        print("產生 JSON 報告...")
        json_report = self.generate_json_report(module_reports)
        json_file = self.reports_dir / 'bdd-test-report.json'
        json_file.write_text(json_report, encoding='utf-8')
        print(f"JSON 報告已儲存至: {json_file}")

        # 產生簡要統計
        total_tests = sum(m.total_tests for m in module_reports)
        total_failures = sum(m.total_failures for m in module_reports)
        total_errors = sum(m.total_errors for m in module_reports)

        print(f"\n=== BDD 測試結果摘要 ===")
        print(f"總測試數: {total_tests}")
        print(f"成功數: {total_tests - total_failures - total_errors}")
        print(f"失敗數: {total_failures}")
        print(f"錯誤數: {total_errors}")
        if total_tests > 0:
            success_rate = ((total_tests - total_failures - total_errors) / total_tests) * 100
            print(f"成功率: {success_rate:.1f}%")

def main():
    parser = argparse.ArgumentParser(description='MCP Registry BDD 測試報告產生器')
    parser.add_argument('reports_dir', help='測試報告目錄路徑')
    parser.add_argument('--format', choices=['html', 'json', 'both'], default='both',
                       help='報告格式 (預設: both)')

    args = parser.parse_args()

    if not os.path.exists(args.reports_dir):
        print(f"錯誤: 測試報告目錄不存在: {args.reports_dir}")
        sys.exit(1)

    try:
        generator = BDDReportGenerator(args.reports_dir)
        generator.generate_reports()
        print("\n報告產生完成！")
    except Exception as e:
        print(f"產生報告時發生錯誤: {e}")
        sys.exit(1)

if __name__ == '__main__':
    main()