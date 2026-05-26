import re
from datetime import datetime, timezone
from PIL import ImageFont

try:
    from zoneinfo import ZoneInfo
except ImportError:
    import pytz
    ZoneInfo = pytz.timezone

def parse_dt(time_val):
    if not time_val: return None
    if isinstance(time_val, (int, float)):
        try:
            val_float = float(time_val)
            if val_float > 1e11:
                val_float /= 1000.0
            dt = datetime.fromtimestamp(val_float, tz=timezone.utc)
            return dt.astimezone(ZoneInfo("Europe/Warsaw"))
        except Exception:
            pass
    time_str = str(time_val).strip()
    if re.match(r"^\d{2}:\d{2}(:\d{2})?$", time_str): return None
    try:
        clean_str = time_str.replace('Z', '+00:00')
        dt = datetime.fromisoformat(clean_str)
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=timezone.utc)
        return dt.astimezone(ZoneInfo("Europe/Warsaw"))
    except ValueError:
        try:
            if 'T' in time_str:
                core_time = time_str[:19]
                dt = datetime.strptime(core_time, "%Y-%m-%dT%H:%M:%S")
                dt = dt.replace(tzinfo=timezone.utc)
                return dt.astimezone(ZoneInfo("Europe/Warsaw"))
        except Exception:
            pass
    return None

def format_time(time_val):
    if not time_val: return '--:--'
    time_str = str(time_val).strip()
    if re.match(r"^\d{2}:\d{2}(:\d{2})?$", time_str): return time_str[:5]
    dt = parse_dt(time_val)
    if dt: return dt.strftime("%H:%M")
    if 'T' in time_str: return time_str.split('T')[1][:5]
    return time_str[:5]

def format_seconds(total_seconds):
    if total_seconds is None: return "--:--:--"
    total_seconds = int(total_seconds)
    h = total_seconds // 3600
    m = (total_seconds % 3600) // 60
    s = total_seconds % 60
    return f"{h:02d}:{m:02d}:{s:02d}"

def format_val(val, unit='', decimals=1):
    if val is None or str(val).lower() == 'nan':
        return '--'
    try:
        space = "" if unit in ['°C', '%', '°'] else " "
        return f"{float(val):.{decimals}f}{space}{unit}".strip()
    except ValueError:
        return '--'

def get_duration_seconds(start_str, end_str):
    dt1 = parse_dt(start_str)
    dt2 = parse_dt(end_str)
    if dt1 and dt2: return int(abs((dt2 - dt1).total_seconds()))
    return None

def format_place(place_name):
    if not place_name or str(place_name).strip() in ['--', 'None', '']: return ""
    p_str = str(place_name).strip()
    if re.search(r"\d+\.\d{3,}", p_str): return ""
    return p_str

def get_font(size, bold=False, italic=False):
    try:
        if bold: return ImageFont.truetype("arialbd.ttf", size)
        elif italic: return ImageFont.truetype("ariali.ttf", size)
        else: return ImageFont.truetype("arial.ttf", size)
    except IOError:
        return ImageFont.load_default()

def convert_raw(val, category, prefs):
    if val is None or str(val).lower() == 'nan':
        return float('nan')
    try:
        v = float(val)
    except ValueError:
        return float('nan')

    pref = prefs.get(category, '')

    if category == 'temp':
        if pref == '°F': v = (v * 9/5) + 32
    elif category in ['wind', 'speed']:
        if pref == 'm/s': v /= 3.6
        elif pref == 'kt': v /= 1.852
        elif pref == 'mph': v /= 1.60934
        elif pref == 'bft':
            ms = v / 3.6
            if ms < 0.3: return 0
            elif ms < 1.6: return 1
            elif ms < 3.4: return 2
            elif ms < 5.5: return 3
            elif ms < 8.0: return 4
            elif ms < 10.8: return 5
            elif ms < 13.9: return 6
            elif ms < 17.2: return 7
            elif ms < 20.8: return 8
            elif ms < 24.5: return 9
            elif ms < 28.5: return 10
            elif ms < 32.7: return 11
            else: return 12
    elif category == 'currents':
        if pref == 'km/h': v *= 3.6
        elif pref == 'kt': v *= 1.94384
        elif pref == 'mph': v *= 2.23694
    elif category == 'pressure':
        if pref == 'inHg': v *= 0.02953
        elif pref == 'mmHg': v *= 0.75006
    elif category == 'wave':
        if pref == 'ft': v *= 3.28084
    elif category == 'rain':
        if pref == 'inch': v /= 25.4
    elif category == 'snow':
        if pref == 'mm': v *= 10
        elif pref == 'inch': v /= 2.54
    elif category == 'distance':
        if pref == 'NM': v /= 1.852
        elif pref == 'mi': v /= 1.60934

    return v

def format_metric(val, category, prefs, decimals=1):
    v = convert_raw(val, category, prefs)
    import math
    if math.isnan(v):
        return '--'

    unit_str = prefs.get(category, '')

    if not unit_str:
        defaults = {
            'speed': 'km/h', 'wind': 'km/h', 'temp': '°C',
            'distance': 'km', 'pressure': 'hPa', 'rain': 'mm',
            'snow': 'cm', 'wave': 'm', 'currents': 'm/s',
            'clouds': '%', 'humidity': '%'
        }
        unit_str = defaults.get(category, '')

    if unit_str == 'bft':
        return f"{int(v)} bft"

    space = "" if unit_str in ['°C', '°F', '%', '°'] else " "
    return f"{v:.{decimals}f}{space}{unit_str}".strip()