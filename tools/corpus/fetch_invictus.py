import json, urllib.request, re, html, time

BASE="https://www.projectinvictus.it/wp-json/wp/v2"
UA={"User-Agent":"FitCoach-corpus/1.0 (personal use)"}

def get(u):
    r=urllib.request.Request(u,headers=UA)
    with urllib.request.urlopen(r,timeout=60) as x:
        return json.loads(x.read().decode("utf-8","ignore"))

def clean(h):
    return re.sub(r"\s+"," ",html.unescape(re.sub(r"<[^>]+>"," ",h))).strip()

# mappa categoria -> slug, per etichettare ogni articolo
cats={c["id"]:c["slug"] for c in get(f"{BASE}/categories?per_page=100&_fields=id,slug")}

arts=[]
for page in range(1,15):
    try:
        posts=get(f"{BASE}/posts?per_page=100&page={page}"
                  "&_fields=id,title,link,content,date,categories")
    except Exception as e:
        print("stop a pagina",page,e); break
    if not posts: break
    for p in posts:
        body=clean(p["content"]["rendered"])
        if len(body)<600: continue
        arts.append({"id":p["id"],"title":clean(p["title"]["rendered"]),
                     "url":p["link"],"date":p.get("date","")[:10],
                     "cats":[cats.get(c,c) for c in p.get("categories",[])],
                     "text":body})
    print("pagina",page,"totale",len(arts),flush=True)
    time.sleep(0.3)

json.dump(arts,open("/tmp/invictus_full.json","w"),ensure_ascii=False)
print("ARTICOLI:",len(arts),"| CARATTERI:",sum(len(a["text"]) for a in arts))
