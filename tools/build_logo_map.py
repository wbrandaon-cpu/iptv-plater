#!/usr/bin/env python3
"""
Gera app/src/main/assets/channel_logos.json: mapa "nome normalizado" -> URL do
logo oficial (colorido) de cada canal, a partir do banco público iptv-org.

Roda na compilação (GitHub Actions). Se a internet falhar, grava um mapa vazio
e o app continua usando só os logos enviados pelo provedor.

IMPORTANTE: normalize() precisa ser idêntica a ChannelLogos.normalize() no app.
"""
import json
import re
import sys
import unicodedata
import urllib.request

BASES = [
    "https://iptv-org.github.io/api/",
    "https://raw.githubusercontent.com/iptv-org/api/gh-pages/",
]

# Só países que costumam aparecer nas listas brasileiras (evita pegar o logo
# de um canal estrangeiro com nome parecido). Em empate, vale a ordem abaixo.
COUNTRY_PRIORITY = ["BR", "PT", "US", "UK", "AR", "MX", "ES", "CA"]

QUALITY = (
    r"fhd|hd|sd|uhd|fullhd|4k|8k|h265|h264|hevc|1080p?|720p?|480p?|2160p?|"
    r"\d+fps|fps|hdr|alt|backup|vip|br|pt|us|uk"
)


def normalize(name: str) -> str:
    s = name.lower()
    s = unicodedata.normalize("NFD", s)
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    s = re.sub(r"\[[^\]]*\]|\([^)]*\)|\{[^}]*\}", " ", s)       # [..] (..) {..}
    s = re.sub(r"^\s*[a-z]{2,3}\s*[:|]\s*", " ", s)             # "br: ", "pt | "
    s = re.sub(r"[^a-z0-9]+", " ", s)                           # só letras/números
    s = re.sub(r"(?<![a-z0-9])(" + QUALITY + r")(?![a-z0-9])", " ", s)
    return re.sub(r"\s+", "", s)


def fetch(name):
    last = None
    for base in BASES:
        try:
            with urllib.request.urlopen(base + name, timeout=60) as r:
                return json.load(r)
        except Exception as e:  # tenta a próxima fonte
            last = e
    raise last


def logo_score(logo):
    """Maior = melhor. Logo atual, do canal principal, colorido e em PNG."""
    tags = [t.lower() for t in (logo.get("tags") or [])]
    score = 0
    if logo.get("in_use"):
        score += 8
    if not logo.get("feed"):
        score += 4
    if "white" not in tags and "dark" not in tags:
        score += 2
    if (logo.get("format") or "").upper() in ("PNG", "JPEG", "WEBP"):
        score += 1
    return score


def main(out_path):
    try:
        channels = fetch("channels.json")
        logos = fetch("logos.json")
    except Exception as e:
        print(f"Aviso: não foi possível baixar o banco de logos ({e}); gerando mapa vazio.")
        with open(out_path, "w") as f:
            f.write("{}")
        return

    best_logo = {}
    for logo in logos:
        cid, url = logo.get("channel"), logo.get("url")
        if not cid or not url:
            continue
        if cid not in best_logo or logo_score(logo) > logo_score(best_logo[cid]):
            best_logo[cid] = logo

    def priority(country):
        return COUNTRY_PRIORITY.index(country) if country in COUNTRY_PRIORITY else len(COUNTRY_PRIORITY)

    result = {}   # nome -> (prioridade, url)
    for ch in channels:
        if ch.get("is_nsfw") or ch.get("closed"):
            continue
        if ch.get("country") not in COUNTRY_PRIORITY:
            continue
        logo = best_logo.get(ch.get("id"))
        if not logo:
            continue
        p = priority(ch.get("country"))
        for n in [ch.get("name") or ""] + list(ch.get("alt_names") or []):
            key = normalize(n)
            if len(key) < 2:
                continue
            if key not in result or p < result[key][0]:
                result[key] = (p, logo["url"])

    # Apelidos: "sbttv" também vale para "sbt", "tvcultura" para "cultura" etc.
    # Só entram quando não existe um nome exato igual.
    # Também remove palavras "de enfeite" comuns: "SBT Nacional" -> "sbt",
    # "Premiere FC 2" -> "premiere2", "Discovery Kids Latin America" -> "discoverykids".
    fillers = ("latinamerica", "latam", "nacional", "brasil", "live", "fc")
    aliases = {}
    for key, value in result.items():
        candidates = [key[:-2] if key.endswith("tv") else None,
                      key[2:] if key.startswith("tv") else None]
        candidates += [key.replace(f, "", 1) for f in fillers if f in key]
        for alias in candidates:
            if alias and len(alias) >= 3 and alias not in result:
                if alias not in aliases or value[0] < aliases[alias][0]:
                    aliases[alias] = value
    result.update(aliases)

    data = {k: v[1] for k, v in sorted(result.items())}
    with open(out_path, "w") as f:
        json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
    print(f"{len(data)} nomes de canais com logo gravados em {out_path}")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "app/src/main/assets/channel_logos.json")
