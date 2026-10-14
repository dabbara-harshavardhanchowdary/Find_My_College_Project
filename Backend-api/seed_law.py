import re
import requests
from bs4 import BeautifulSoup
import firebase_admin
from firebase_admin import credentials, firestore

# 1. Initialize Firebase Admin
if not firebase_admin._apps:
    cred = credentials.Certificate("serviceAccountKey.json")
    firebase_admin.initialize_app(cred)
db = firestore.client()

url = "https://www.nirfindia.org/Rankings/2025/LawRanking.html"
headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
}

def clean_text(text):
    return re.sub(r'\s+', ' ', text).strip() if text else ""

def to_float(val):
    try:
        clean = re.sub(r'[^\d.]', '', str(val))
        return float(clean) if clean else 0.0
    except (ValueError, TypeError):
        return 0.0

print(f"Connecting to NIRF 2025 Law table: {url}...")
res = requests.get(url, headers=headers, timeout=30)
res.raise_for_status()

soup = BeautifulSoup(res.content, "html.parser")
table = soup.find("table")
tbody = table.find("tbody") if table.find("tbody") else table
rows = tbody.find_all("tr", recursive=False)

law_colleges = []

for idx, row in enumerate(rows):
    tds = row.find_all("td", recursive=False)
    if len(tds) < 6:
        continue

    # Col 0: Institute ID
    inst_id = clean_text(tds[0].text)

    # Col 1: Institute Name & Nested Metrics Table
    name_td = tds[1]

    metrics = {
        "tlr": 0.0,
        "rpc": 0.0,
        "go": 0.0,
        "oi": 0.0,
        "perception": 0.0
    }

    # Extract sub-metrics from nested table
    metric_table = name_td.find("table")
    if metric_table:
        inner_cells = [clean_text(c.text) for c in metric_table.find_all("td")]
        num_vals = []
        for val in inner_cells:
            if re.match(r'^\d+(\.\d+)?$', val) and val != "100":
                num_vals.append(to_float(val))

        if len(num_vals) >= 5:
            metrics["tlr"] = num_vals[0]
            metrics["rpc"] = num_vals[1]
            metrics["go"] = num_vals[2]
            metrics["oi"] = num_vals[3]
            metrics["perception"] = num_vals[4]

        metric_table.decompose()

    # Clean out extra link tags or 'More Details'
    for tag in name_td.find_all(["div", "a"]):
        tag.decompose()

    college_name = clean_text(name_td.text)
    college_name = re.sub(r'More Details.*', '', college_name, flags=re.IGNORECASE).strip()

    # Outer columns: City, State, Score, Rank
    city = clean_text(tds[2].text)
    state = clean_text(tds[3].text)
    score = to_float(clean_text(tds[4].text))
    rank = clean_text(tds[5].text)

    doc_data = {
        "instituteId": inst_id,
        "name": college_name,
        "city": city,
        "state": state,
        "stream": "Law",
        "score": score,
        "nirfRank": rank,
        "nirfMetrics": metrics
    }

    doc_key = inst_id if inst_id else re.sub(r'[^a-z0-9]+', '-', college_name.lower()).strip('-')
    law_colleges.append((doc_key, doc_data))

    # Print first 2 to terminal to verify
    if idx < 2:
        print(f"\n--- Verified College {idx + 1} ---")
        print(f"Institute ID : {inst_id}")
        print(f"Name         : {college_name}")
        print(f"City         : {city}")
        print(f"State        : {state}")
        print(f"Score        : {score}")
        print(f"Rank         : {rank}")
        print(f"TLR          : {metrics['tlr']}")
        print(f"RPC          : {metrics['rpc']}")
        print(f"GO           : {metrics['go']}")
        print(f"OI           : {metrics['oi']}")
        print(f"PERCEPTION   : {metrics['perception']}")

print(f"\nTotal Law Colleges extracted: {len(law_colleges)}")

# Push to Firestore collection 'colleges_law'
print("Writing clean documents to Firestore collection `colleges_law`...")
batch = db.batch()
count = 0
total = 0

for doc_key, data in law_colleges:
    doc_ref = db.collection("law_colleges").document(doc_key)
    batch.set(doc_ref, data, merge=True)
    count += 1

    if count == 400:
        batch.commit()
        total += count
        batch = db.batch()
        count = 0

if count > 0:
    batch.commit()
    total += count

print(f"\nSuccessfully seeded {total} Law colleges into `colleges_law`!")