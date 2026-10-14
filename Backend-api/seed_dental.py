import re
import requests
from bs4 import BeautifulSoup
import firebase_admin
from firebase_admin import credentials, firestore

# Initialize Firebase Admin
if not firebase_admin._apps:
    cred = credentials.Certificate("serviceAccountKey.json")
    firebase_admin.initialize_app(cred)
db = firestore.client()

# 2025 NIRF Dental Ranking URL
url = "https://www.nirfindia.org/Rankings/2025/DentalRanking.html"
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

print(f"Connecting to NIRF 2025 Dental table: {url}...")
res = requests.get(url, headers=headers, timeout=30)
res.raise_for_status()

soup = BeautifulSoup(res.content, "html.parser")
table = soup.find("table")
tbody = table.find("tbody") if table.find("tbody") else table
rows = tbody.find_all("tr", recursive=False)

dental_colleges = []

for idx, row in enumerate(rows):
    tds = row.find_all("td", recursive=False)
    if len(tds) < 6:
        continue

    # Col 0: Institute ID
    inst_id = clean_text(tds[0].text)

    # Col 1: Institute Name (decompose child tags to remove extra link text)
    name_td = tds[1]
    for tag in name_td.find_all(["table", "div", "a"]):
        tag.decompose()

    raw_name = clean_text(name_td.text)
    college_name = re.sub(r'More Details.*', '', raw_name, flags=re.IGNORECASE).strip()

    # Outer table columns: City, State, Score, Rank
    city = clean_text(tds[2].text)
    state = clean_text(tds[3].text)
    score = to_float(clean_text(tds[4].text))
    rank = clean_text(tds[5].text)

    doc_data = {
        "instituteId": inst_id,
        "name": college_name,
        "city": city,
        "state": state,
        "stream": "Dental",
        "score": score,
        "nirfRank": rank,
        "nirfMetrics": {
            "tlr": 0.0,
            "rpc": 0.0,
            "go": 0.0,
            "oi": 0.0,
            "perception": 0.0
        }
    }

    doc_key = inst_id if inst_id else re.sub(r'[^a-z0-9]+', '-', college_name.lower()).strip('-')
    dental_colleges.append((doc_key, doc_data))

    if idx < 2:
        print(f"\n--- Verified College {idx + 1} ---")
        print(f"Institute ID : {inst_id}")
        print(f"Name         : {college_name}")
        print(f"City         : {city}")
        print(f"State        : {state}")
        print(f"Score        : {score}")
        print(f"Rank         : {rank}")

print(f"\nTotal Dental Colleges parsed: {len(dental_colleges)}")

# Push into the unified 'colleges' collection
print("Writing clean documents to Firestore collection `colleges`...")
batch = db.batch()
for doc_key, data in dental_colleges:
    doc_ref = db.collection("dental_colleges").document(doc_key)
    batch.set(doc_ref, data, merge=True)
batch.commit()

print("Successfully seeded Dental colleges!")