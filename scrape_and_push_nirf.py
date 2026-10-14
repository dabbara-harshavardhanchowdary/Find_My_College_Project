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

# Official NIRF Portal Endpoints
URL_2025 = "https://www.nirfindia.org/Rankings/2025/EngineeringRanking.html"
URL_2024 = "https://www.nirfindia.org/Rankings/2024/EngineeringRanking.html"
PDF_BASE = "https://www.nirfindia.org/nirfpdfcdn/2024/pdf/Engineering/"

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
}

def clean_val(val_str, default=0.0):
    try:
        match = re.search(r"[\d.]+", val_str)
        return float(match.group(0)) if match else default
    except Exception:
        return default

def slugify(name):
    clean = re.sub(r"[^a-zA-Z0-9\s]", "", name).strip().lower()
    return "-".join(clean.split()[:4])

def run():
    print("Connecting to official NIRF Engineering portal...")
    res = requests.get(URL_2025, headers=HEADERS, timeout=15)
    year = 2025
    if res.status_code != 200:
        print("2025 table endpoint not available; falling back to official active rankings table (2024)...")
        res = requests.get(URL_2024, headers=HEADERS, timeout=15)
        year = 2024

    soup = BeautifulSoup(res.text, "html.parser")
    table = soup.find("table")
    if not table:
        print("Failed to locate HTML table on official portal.")
        return

    rows = table.find("tbody").find_all("tr")
    print(f"Discovered {len(rows)} institutional entries. Parsing 5 parameters and generating records...\n")

    batch = db.batch()
    batch_count = 0
    total = 0

    for row in rows:
        cols = row.find_all("td")
        if len(cols) < 6:
            continue

        inst_id = cols[0].get_text(strip=True)
        
        # Name and expandable metric box
        name_col = cols[1]
        name = name_col.contents[0].get_text(strip=True) if name_col.contents else name_col.get_text(strip=True)
        name = name.replace("More DetailsClose", "").replace("More Details", "").strip()

        full_text = name_col.get_text(" ", strip=True)
        
        # Extract the 5 parameters
        tlr_m = re.search(r"TLR\s*\(\d+\)\s*([\d.]+)", full_text)
        rpc_m = re.search(r"RPC\s*\(\d+\)\s*([\d.]+)", full_text)
        go_m  = re.search(r"GO\s*\(\d+\)\s*([\d.]+)", full_text)
        oi_m  = re.search(r"OI\s*\(\d+\)\s*([\d.]+)", full_text)
        pr_m  = re.search(r"PERCEPTION\s*\(\d+\)\s*([\d.]+)", full_text)

        tlr = clean_val(tlr_m.group(1)) if tlr_m else 0.0
        rpc = clean_val(rpc_m.group(1)) if rpc_m else 0.0
        go  = clean_val(go_m.group(1)) if go_m else 0.0
        oi  = clean_val(oi_m.group(1)) if oi_m else 0.0
        pr  = clean_val(pr_m.group(1)) if pr_m else 0.0

        city = cols[2].get_text(strip=True)
        state = cols[3].get_text(strip=True)
        overall_score = clean_val(cols[4].get_text(strip=True))
        
        rank_raw = cols[5].get_text(strip=True)
        try:
            rank = int(rank_raw)
        except ValueError:
            rank = rank_raw

        # Construct official signed report link
        pdf_url = f"{PDF_BASE}{inst_id}.pdf"
        doc_id = slugify(name)

        doc_ref = db.collection("colleges").document(doc_id)
        payload = {
            "id": doc_id,
            "instituteId": inst_id,
            "name": name,
            "city": city,
            "state": state,
            "nirfRank": rank,
            "score": overall_score,
            "rankingYear": year,
            "nirfReportUrl": pdf_url,
            "nirfMetrics": {
                "tlr": tlr,
                "rpc": rpc,
                "go": go,
                "oi": oi,
                "perception": pr
            },
            "source": "Ministry of Education (MoE), Government of India"
        }

        batch.set(doc_ref, payload, merge=True)
        batch_count += 1
        total += 1

        print(f"[{total}] Rank #{rank} | {name[:40]} | Score: {overall_score} | TLR: {tlr} RPC: {rpc} GO: {go} OI: {oi} PR: {pr}")

        if batch_count >= 400:
            batch.commit()
            batch = db.batch()
            batch_count = 0

    if batch_count > 0:
        batch.commit()

    print(f"\nCompleted: {total} colleges pushed to Firestore.")

if __name__ == "__main__":
    run()