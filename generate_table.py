import re

html_content = open("app/src/test/resources/fubon.html").read()

import requests
from bs4 import BeautifulSoup

soup = BeautifulSoup(html_content, 'html.parser')
rows = soup.select("table.rate-table tbody tr")

official_data = {}
for row in rows:
    cols = row.select("td")
    currency = cols[1].text.strip()
    match = re.search(r'\(([A-Z]{3})\)', currency)
    if not match: continue
    code = match.group(1)

    cashText = cols[4].text.replace('\n', ' ').replace('\r', ' ').strip()
    def parse_pair(text):
        trimmed = text.replace('\u00A0', ' ').strip()
        parts = [p for p in trimmed.split() if p.strip()]
        buy = None if len(parts) > 0 and '---' in parts[0] else parts[0] if len(parts) > 0 else None
        sell = None if len(parts) > 1 and '---' in parts[1] else parts[1] if len(parts) > 1 else None
        return buy, sell

    cash_b, cash_s = parse_pair(cashText)
    official_data[code] = (cash_b or "---", cash_s or "---")

print("| 幣別 | 官方現鈔買入 | 官方現鈔賣出 | App 現鈔買入 | App 現鈔賣出 | 結果 |")
print("|---|---|---|---|---|---|")

currencies = ["USD", "JPY", "CNY", "EUR", "HKD", "AUD", "CAD", "GBP", "SGD", "CHF", "NZD", "ZAR", "SEK", "THB"]
for code in currencies:
    cash_b, cash_s = official_data.get(code, ("---", "---"))

    app_b = "未提供" if cash_b == "---" else cash_b
    app_s = "未提供" if cash_s == "---" else cash_s

    res = "PASS" if ((cash_b == "---" and app_b == "未提供") or (cash_b != "---" and cash_b == app_b)) and \
                    ((cash_s == "---" and app_s == "未提供") or (cash_s != "---" and cash_s == app_s)) else "FAIL"

    print(f"| {code} | {cash_b} | {cash_s} | {app_b} | {app_s} | {res} |")
