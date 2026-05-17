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