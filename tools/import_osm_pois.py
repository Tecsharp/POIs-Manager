#!/usr/bin/env python3
"""
Importa POIs reales de OpenStreetMap (vía Overpass API) para México y genera
backend/src/main/resources/seed/pois.json en el formato que espera DataSeeder.

Solo librería estándar (nada que instalar). Uso:

    python3 tools/import_osm_pois.py
    python3 tools/import_osm_pois.py --states MX-VER,MX-PUE   # probar con pocos estados
    python3 tools/import_osm_pois.py --tags fuel,museum       # probar con pocas categorías

Datos: © colaboradores de OpenStreetMap, licencia ODbL — ver https://www.openstreetmap.org/copyright
Overpass es un servicio público gratuito compartido: este script deja ~2s entre
consultas y marca el User-Agent, pero aun así puede tardar bastante con México
completo (horas en el peor caso). Guarda avance parcial en pois.json después de
cada estado, así que si se interrumpe (Ctrl+C) no se pierde todo lo ya bajado;
solo falta volver a correrlo para completar (sin --states/--tags) o acotado a
lo que falte.
"""
import argparse
import json
import ssl
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

try:
    # macOS Python builds a veces no traen el paquete de certificados raíz del sistema
    # (error SSL CERTIFICATE_VERIFY_FAILED). Si certifi está instalado, lo usamos; si no,
    # seguimos con el contexto SSL por defecto.
    import certifi
    SSL_CONTEXT = ssl.create_default_context(cafile=certifi.where())
except ImportError:
    SSL_CONTEXT = ssl.create_default_context()

OVERPASS_URL = "https://overpass-api.de/api/interpreter"
USER_AGENT = "Rutea-POI-Importer/1.0 (+https://github.com/Tecsharp/POIs-Manager)"
REQUEST_TIMEOUT_S = 200
COURTESY_DELAY_S = 2.0
MAX_RETRIES = 2

OUTPUT_PATH = Path(__file__).resolve().parent.parent / "backend/src/main/resources/seed/pois.json"

# Tag de OSM (key, value) -> tipo de POI de Rutea. Deja fuera a propósito lo que satura
# sin aportar mucho a un viajero: parking, atm, bank, gimnasios, escuelas, tiendas genéricas.
TAG_MAP = {
    ("amenity", "fuel"): "GAS_STATION",
    ("amenity", "pharmacy"): "PHARMACY",
    ("amenity", "hospital"): "HOSPITAL",
    ("amenity", "theatre"): "THEATER",
    ("amenity", "bus_station"): "BUS_STATION",
    ("amenity", "restaurant"): "RESTAURANT",
    ("amenity", "cafe"): "CAFE",
    ("amenity", "bar"): "BAR",
    ("amenity", "pub"): "BAR",
    ("amenity", "place_of_worship"): "CHURCH",
    ("tourism", "museum"): "MUSEUM",
    ("tourism", "hotel"): "HOTEL",
    ("tourism", "motel"): "HOTEL",
    ("tourism", "viewpoint"): "PHOTO",
    ("tourism", "attraction"): "SPOT",
    ("natural", "peak"): "SPOT",
    ("natural", "waterfall"): "SPOT",
    ("leisure", "nature_reserve"): "NATURE_RESERVE",
    ("leisure", "park"): "PARK",
    ("boundary", "protected_area"): "NATURE_RESERVE",
    ("shop", "supermarket"): "SUPERMARKET",
    ("shop", "car_repair"): "MECHANICAL_WORKSHOP",
    ("railway", "station"): "TRAIN_STATION",
    ("aeroway", "aerodrome"): "AIRPORT",
}

# Estas se consultan por estado (si no, el país entero en una sola consulta es
# demasiado grande/lenta para el servidor público de Overpass).
HIGH_DENSITY_TAGS = {("amenity", "restaurant"), ("amenity", "cafe"), ("amenity", "bar"),
                     ("amenity", "pub"), ("amenity", "place_of_worship")}

# Los 32 estados por su código ISO 3166-2 (como están etiquetados en las relaciones
# admin_level=4 de OSM) y un nombre legible solo para el progreso en pantalla.
STATES = [
    ("MX-AGU", "Aguascalientes"), ("MX-BCN", "Baja California"), ("MX-BCS", "Baja California Sur"),
    ("MX-CAM", "Campeche"), ("MX-CHP", "Chiapas"), ("MX-CHH", "Chihuahua"), ("MX-COA", "Coahuila"),
    ("MX-COL", "Colima"), ("MX-DIF", "Ciudad de México"), ("MX-DUR", "Durango"), ("MX-GUA", "Guanajuato"),
    ("MX-GRO", "Guerrero"), ("MX-HID", "Hidalgo"), ("MX-JAL", "Jalisco"), ("MX-MEX", "Estado de México"),
    ("MX-MIC", "Michoacán"), ("MX-MOR", "Morelos"), ("MX-NAY", "Nayarit"), ("MX-NLE", "Nuevo León"),
    ("MX-OAX", "Oaxaca"), ("MX-PUE", "Puebla"), ("MX-QUE", "Querétaro"), ("MX-ROO", "Quintana Roo"),
    ("MX-SLP", "San Luis Potosí"), ("MX-SIN", "Sinaloa"), ("MX-SON", "Sonora"), ("MX-TAB", "Tabasco"),
    ("MX-TAM", "Tamaulipas"), ("MX-TLA", "Tlaxcala"), ("MX-VER", "Veracruz"), ("MX-YUC", "Yucatán"),
    ("MX-ZAC", "Zacatecas"),
]


def overpass_query(ql: str) -> dict:
    body = f"[out:json][timeout:180];{ql}".encode("utf-8")
    req = urllib.request.Request(OVERPASS_URL, data=body, headers={"User-Agent": USER_AGENT})
    last_err = None
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            with urllib.request.urlopen(req, timeout=REQUEST_TIMEOUT_S, context=SSL_CONTEXT) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as e:
            last_err = e
            print(f"    intento {attempt}/{MAX_RETRIES} falló ({e}); reintentando…", file=sys.stderr)
            time.sleep(5)
    raise RuntimeError(f"Overpass no respondió tras {MAX_RETRIES} intentos: {last_err}")


def tag_filter_block(tags, area_ref="area.mx"):
    """(node[k=v](area.mx); way[k=v](area.mx); relation[k=v](area.mx); ...)"""
    parts = []
    for k, v in tags:
        parts.append(f'node["{k}"="{v}"]({area_ref});')
        parts.append(f'way["{k}"="{v}"]({area_ref});')
        parts.append(f'relation["{k}"="{v}"]({area_ref});')
    return "(" + "".join(parts) + ");out center tags;"


def element_latlng(el):
    if el.get("type") == "node":
        return el.get("lat"), el.get("lon")
    center = el.get("center")
    if center:
        return center.get("lat"), center.get("lon")
    return None, None


SIMPLE_HOURS = __import__("re").compile(r"^(\d{2}:\d{2})-(\d{2}:\d{2})$")


def to_poi(el, poi_type):
    tags = el.get("tags", {})
    name = (tags.get("name") or tags.get("name:es") or "").strip()
    if not name or len(name) > 200:
        return None
    lat, lng = element_latlng(el)
    if lat is None or lng is None:
        return None
    if not (-90 <= lat <= 90 and -180 <= lng <= 180):
        return None

    addr_parts = [tags.get("addr:street"), tags.get("addr:housenumber")]
    street = " ".join(p for p in addr_parts if p)
    locality = tags.get("addr:suburb") or tags.get("addr:neighbourhood") or tags.get("addr:city")
    address = ", ".join(p for p in [street, locality] if p) or None

    poi = {"name": name[:200], "type": poi_type, "lat": round(lat, 6), "lng": round(lng, 6)}
    if address:
        poi["address"] = address[:300]
    phone = tags.get("phone") or tags.get("contact:phone")
    if phone:
        poi["phone"] = phone[:40]
    website = tags.get("website") or tags.get("contact:website")
    if website and website.startswith(("http://", "https://")):
        poi["website"] = website[:300]
    if tags.get("payment:cards") in ("yes", "no"):
        poi["accepts_cards"] = tags["payment:cards"] == "yes"

    hours_raw = tags.get("opening_hours")
    if hours_raw:
        m = SIMPLE_HOURS.match(hours_raw.strip())
        if m:
            poi["hours"] = {"open_time": m.group(1), "close_time": m.group(2)}
        else:
            # Sintaxis compleja de opening_hours (ej. "Mo-Fr 08:00-18:00; Sa 09:00-14:00"):
            # no la parseamos, se guarda tal cual en comments para no perder el dato.
            poi["hours"] = {"comments": hours_raw[:200]}
    return poi


def fetch_national(tags):
    print(f"Consultando {len(tags)} categorías a nivel nacional…")
    ql = 'area["ISO3166-1"="MX"][admin_level=2]->.mx;' + tag_filter_block(tags)
    data = overpass_query(ql)
    return data.get("elements", [])


def fetch_state_tag(code, tag):
    k, v = tag
    ql = f'area["ISO3166-2"="{code}"]->.mx;' + tag_filter_block([tag])
    data = overpass_query(ql)
    return data.get("elements", [])


def write_output(seen, out_path):
    pois = list(seen.values())
    pois.sort(key=lambda p: (p["type"], p["name"]))
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(pois, ensure_ascii=False, indent=1), encoding="utf-8")
    return len(pois)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--states", help="Códigos ISO 3166-2 separados por coma (default: los 32)")
    parser.add_argument("--tags", help="Nombres de tag de OSM separados por coma, ej. fuel,museum (default: todos)")
    parser.add_argument("--out", default=str(OUTPUT_PATH), help="Ruta de salida")
    args = parser.parse_args()

    states = STATES
    if args.states:
        wanted = set(args.states.split(","))
        states = [s for s in STATES if s[0] in wanted]

    tag_map = TAG_MAP
    if args.tags:
        wanted = set(args.tags.split(","))
        tag_map = {k: v for k, v in TAG_MAP.items() if k[1] in wanted}
    if not tag_map:
        print("Sin categorías que importar (revisa --tags).", file=sys.stderr)
        sys.exit(1)

    national_tags = [t for t in tag_map if t not in HIGH_DENSITY_TAGS]
    state_tags = [t for t in tag_map if t in HIGH_DENSITY_TAGS]
    out_path = Path(args.out)

    seen = {}  # (osm_type, osm_id) -> poi dict, para no duplicar

    try:
        if national_tags:
            elements = fetch_national(national_tags)
            for el in elements:
                tags = el.get("tags", {})
                # Un elemento puede tener varios de nuestros tags a la vez; se queda con el primero que matchea.
                for (k, v), poi_type in tag_map.items():
                    if (k, v) in HIGH_DENSITY_TAGS:
                        continue
                    if tags.get(k) == v:
                        poi = to_poi(el, poi_type)
                        if poi:
                            seen[(el.get("type"), el.get("id"))] = poi
                        break
            print(f"  → {len(seen)} lugares hasta ahora")
            write_output(seen, out_path)  # checkpoint: si se interrumpe lo de abajo, esto no se pierde
            time.sleep(COURTESY_DELAY_S)

        for tag in state_tags:
            poi_type = tag_map[tag]
            k, v = tag
            print(f"Consultando por estado: {k}={v} -> {poi_type}")
            for code, name in states:
                print(f"  {name} ({code})")
                try:
                    elements = fetch_state_tag(code, tag)
                except RuntimeError as e:
                    print(f"    omitido: {e}", file=sys.stderr)
                    elements = []
                for el in elements:
                    key = (el.get("type"), el.get("id"))
                    if key in seen:
                        continue
                    poi = to_poi(el, poi_type)
                    if poi:
                        seen[key] = poi
                time.sleep(COURTESY_DELAY_S)
            write_output(seen, out_path)  # checkpoint tras cada estado
            print(f"  → {len(seen)} lugares hasta ahora (guardado parcial en {out_path})")
    except KeyboardInterrupt:
        print("\nInterrumpido. Se guarda lo que había hasta ahora…", file=sys.stderr)

    total = write_output(seen, out_path)
    print(f"\nTotal de lugares: {total}")
    print(f"Escrito en {out_path}")


if __name__ == "__main__":
    main()
