from jinja2 import Environment, FileSystemLoader
from weasyprint import HTML
import os
import io
import zipfile
import base64
import time
import datetime
from concurrent.futures import ProcessPoolExecutor
import matplotlib
import matplotlib.font_manager as font_manager

from utils import format_seconds, format_val, format_time, get_duration_seconds, format_place, format_metric, convert_raw, parse_dt

from charts.meteogram import create_meteogram_chart
from charts.polar_rose import create_polar_rose
from charts.route_map import create_route_map
from charts.timelines import create_timeline_chart, create_astro_timeline_chart


current_dir = os.path.dirname(os.path.abspath(__file__))
font_dir = os.path.join(current_dir, 'fonts')

if os.path.exists(font_dir):
    for font_file in os.listdir(font_dir):
        if font_file.endswith('.ttf'):
            font_path = os.path.join(font_dir, font_file)
            font_manager.fontManager.addfont(font_path)

matplotlib.rcParams['font.family'] = 'sans-serif'
matplotlib.rcParams['font.sans-serif'] = ['Segoe UI', 'sans-serif']
matplotlib.rcParams['axes.unicode_minus'] = False

MAX_WORKERS = int(os.environ.get("MAX_WORKERS", 4))

env = Environment(loader=FileSystemLoader('templates'))
env.filters['format_metric'] = format_metric
env.filters['format_seconds'] = format_seconds
env.filters['format_val'] = format_val
env.filters['format_time'] = format_time
env.filters['get_duration'] = get_duration_seconds
env.filters['format_place'] = format_place

def enrich_with_max(stats_dict, points):
    if not isinstance(stats_dict, dict):
        return

    max_keys_map = {
        'maxTemp': ['temp', 'temperature'], 'maxDewPoint': ['dewPoint'],
        'maxHumidity': ['humidity'], 'maxPressure': ['pressure'],
        'maxWindSpeed': ['windSpeed'], 'maxWindGusts': ['gusts', 'windGusts'],
        'maxCloudCover': ['cloudCover'], 'maxSeaTemperature': ['seaTemp', 'seaTemperature'],
        'maxWaveHeight': ['waveHeight'], 'maxSwellWaveHeight': ['swellWaveH', 'swellWaveHeight'],
        'maxOceanCurrentVelocity': ['oceanCurrentVel', 'currentVelocity'], 'maxRain': ['rain'],
        'maxSnowfall': ['snowfall', 'snow'], 'maxWavePeriod': ['wavePeriod'],
        'maxSwellWavePeriod': ['swellWavePeriod', 'swellWaveP']
    }

    for k in max_keys_map:
        if k not in stats_dict:
            stats_dict[k] = None

    if not points:
        return

    current_maxes = {k: -float('inf') for k in max_keys_map}
    has_valid = {k: False for k in max_keys_map}

    for p in points:
        for target_key, search_keys in max_keys_map.items():
            for sk in search_keys:
                val = p.get(sk)
                if val is not None and str(val).lower() != 'nan':
                    try:
                        v_float = float(val)
                        if target_key == 'maxOceanCurrentVelocity':
                            v_float = v_float / 3.6
                        if v_float > current_maxes[target_key]:
                            current_maxes[target_key] = v_float
                            has_valid[target_key] = True
                    except ValueError:
                        pass
                    break

    for k in max_keys_map:
        if has_valid[k]:
            stats_dict[k] = current_maxes[k]

def get_day_time_bounds(events, astro_events, base_date_str):
    min_sec = 86400
    max_sec = 0
    has_data = False

    for ev in events:
        dt_start = parse_dt(ev.get('start'))
        dt_end = parse_dt(ev.get('end'))
        s = min_sec
        if dt_start:
            s = dt_start.hour * 3600 + dt_start.minute * 60 + dt_start.second
            min_sec = min(min_sec, s)
            has_data = True
        if dt_end:
            e = dt_end.hour * 3600 + dt_end.minute * 60 + dt_end.second
            if e <= s: e = 86400
            max_sec = max(max_sec, e)
            has_data = True

    if astro_events and isinstance(astro_events, dict):
        for name, iso_str in astro_events.items():
            if iso_str:
                try:
                    safe_iso = iso_str if len(iso_str) >= 19 else iso_str + ":00"
                    event_dt = datetime.datetime.strptime(safe_iso[:19], "%Y-%m-%dT%H:%M:%S")

                    s = event_dt.hour * 3600 + event_dt.minute * 60 + event_dt.second
                    min_sec = min(min_sec, s)
                    max_sec = max(max_sec, s)
                    has_data = True
                except Exception:
                    pass

    if not has_data:
        return 0, 86400

    return max(0, min_sec), min(86400, max_sec)

def reduce_points_for_overall_chart(points, target_count=300):
    if not points or len(points) <= target_count:
        return points

    step = max(1, len(points) // target_count)
    reduced = [points[0]]

    for i in range(1, len(points) - 1):
        curr = points[i]
        prev = points[i - 1]
        next_p = points[i + 1]

        curr_speed = float(curr.get('speed') or 0.0)
        prev_speed = float(prev.get('speed') or 0.0)
        next_speed = float(next_p.get('speed') or 0.0)

        curr_moving = curr_speed > 0.5
        prev_moving = prev_speed > 0.5
        next_moving = next_speed > 0.5

        is_edge = (curr_moving != prev_moving) or (curr_moving != next_moving)

        if is_edge or (i % step == 0):
            reduced.append(curr)

    reduced.append(points[-1])
    return reduced

def reassign_astro_events_strictly_by_date(daily_summaries):

    global_astro_events = []
    for day in daily_summaries:
        if day.get('observedAstroEvents'):
            for name, iso_str in day['observedAstroEvents'].items():
                if iso_str:
                    global_astro_events.append({"name": name, "iso": iso_str})

    for day in daily_summaries:
        current_date_str = day.get('date')
        if not current_date_str: continue

        day_astro = {}
        for ev in global_astro_events:
            if str(ev['iso']).startswith(str(current_date_str)):
                name = ev['name']
                while name in day_astro:
                    name += " "
                day_astro[name] = ev['iso']

        day['observedAstroEvents'] = day_astro


def generate_report_pdf(report_data: dict) -> bytes:
    print("\n--- ROZPOCZĘCIE GENEROWANIA RAPORTU ---")
    start_total = time.time()

    prefs = report_data.get('preferences', {})
    modules = report_data.get("modules", [])
    daily_summaries = report_data.get('dailySummaries', [])
    all_trip_points = report_data.get('points', [])

    reassign_astro_events_strictly_by_date(daily_summaries)

    display_summaries = []
    current_gap = []

    with ProcessPoolExecutor(max_workers=MAX_WORKERS) as executor:
        for idx, day in enumerate(daily_summaries):
            day_index = idx + 1
            events = day.get('timelineEvents', [])
            points = day.get('points', [])

            if not report_data.get('points'):
                all_trip_points.extend(points)

            is_gap_day = False
            if len(events) == 1:
                event_type = str(events[0].get('type', '')).lower()
                if 'brak' in event_type or 'gap' in event_type:
                    is_gap_day = True

            if is_gap_day:
                day['day_index'] = day_index
                current_gap.append(day)
            else:
                if current_gap:
                    display_summaries.append({
                        'is_gap': True, 'start_index': current_gap[0]['day_index'],
                        'end_index': current_gap[-1]['day_index'], 'start_date': current_gap[0].get('date'),
                        'end_date': current_gap[-1].get('date'), 'count': len(current_gap)
                    })
                    current_gap = []

                day['is_gap'] = False
                day['day_index'] = day_index

                for ev in events:
                    ev['placeName'] = format_place(ev.get('placeName'))

                map_points = points[::max(1, len(points) // 100)] if points else []
                meteo_points = points if points else []
                rose_points = points[::max(1, len(points) // 24)] if points else []

                day_min_sec, day_max_sec = get_day_time_bounds(events, day.get('observedAstroEvents'), day.get('date'))

                map_future = executor.submit(create_route_map, map_points, 600, 300)
                timeline_future = executor.submit(create_timeline_chart, day.get('date'), events, day_min_sec, day_max_sec) if events else None
                astro_future = executor.submit(create_astro_timeline_chart, day.get('observedAstroEvents'), day.get('date'), day_min_sec, day_max_sec, events)

                day['meteogram_chart'] = create_meteogram_chart(meteo_points, prefs, day_min_sec, day_max_sec, events)

                day['wind_rose'] = create_polar_rose(rose_points, 'windDir', 'windSpeed', 'Róża wiatrów', prefs)
                day['wave_rose'] = create_polar_rose(rose_points, 'waveDir', 'waveHeight', 'Róża falowania', prefs)

                day['route_map'] = map_future.result()

                day['timeline_chart'] = timeline_future.result() if timeline_future else None
                day['astro_timeline_chart'] = astro_future.result()

                enrich_with_max(day.get('overallWeatherStats'), points)
                display_summaries.append(day)

        if current_gap:
            display_summaries.append({
                'is_gap': True, 'start_index': current_gap[0]['day_index'],
                'end_index': current_gap[-1]['day_index'], 'start_date': current_gap[0].get('date'),
                'end_date': current_gap[-1].get('date'), 'count': len(current_gap)
            })

        report_data['display_summaries'] = display_summaries

        enrich_with_max(report_data.get('overallWeather'), all_trip_points)


        overall_map_future = executor.submit(create_route_map, all_trip_points, 800, 555)

        report_data['overall_wind_rose'] = create_polar_rose(all_trip_points, 'windDir', 'windSpeed', 'Róża wiatrów', prefs)
        report_data['overall_wave_rose'] = create_polar_rose(all_trip_points, 'waveDir', 'waveHeight', 'Róża falowania', prefs)

        overall_meteo_points = reduce_points_for_overall_chart(all_trip_points, target_count=80)

        if len(overall_meteo_points) > 1:
            try:
                report_data['overall_meteogram_chart'] = create_meteogram_chart(overall_meteo_points, prefs)
            except Exception:
                pass

        report_data['overall_route_map'] = overall_map_future.result()

    for day in display_summaries:
        if day.get('observedAstroEvents'):
            for k, v in day['observedAstroEvents'].items():
                if 'T' in str(v):
                    day['observedAstroEvents'][k] = v.split('T')[1][:5]

    template = env.get_template('report_template.html')
    html_content = template.render(data=report_data, prefs=prefs, modules=modules)

    base_dir = os.path.abspath('templates')
    pdf_bytes = HTML(string=html_content, base_url=base_dir).write_pdf()

    total_time = time.time() - start_total

    print(f" RAPORT WYGENEROWANY W: {total_time:.2f} s\n")

    return pdf_bytes

def generate_charts_zip(report_data: dict) -> bytes:
    prefs = report_data.get('preferences', {})
    daily_summaries = report_data.get('dailySummaries', [])
    all_trip_points = report_data.get('points', [])

    if not all_trip_points:
        for day in daily_summaries:
            all_trip_points.extend(day.get('points', []))

    futures = {}
    with ProcessPoolExecutor(max_workers=MAX_WORKERS) as executor:

        if all_trip_points:
            futures["Podsumowanie_ogólne/róża_wiatrów.png"] = executor.submit(
                create_polar_rose, all_trip_points, 'windDir', 'windSpeed', 'Róża wiatrów', prefs, figsize=(7.8, 7.8))

            futures["Podsumowanie_ogólne/róża_falowania.png"] = executor.submit(
                create_polar_rose, all_trip_points, 'waveDir', 'waveHeight', 'Róża falowania', prefs, figsize=(7.8, 7.8))

            futures["Podsumowanie_ogólne/mapa_trasy.png"] = executor.submit(
                create_route_map, all_trip_points, 1600, 800)

            overall_meteo_points = reduce_points_for_overall_chart(all_trip_points, target_count=300)
            if len(overall_meteo_points) > 1:
                futures["Podsumowanie_ogólne/meteogram.png"] = executor.submit(
                    create_meteogram_chart, overall_meteo_points, prefs, None, None, None, figsize=(22, 14.8))

        for idx, day in enumerate(daily_summaries):
            day_index = idx + 1
            folder = f"Dzień_{day_index:02d}_{day.get('date', 'BrakDaty')}"

            events = day.get('timelineEvents', [])
            points = day.get('points', [])
            if not points: continue

            day_min, day_max = get_day_time_bounds(events, day.get('observedAstroEvents'))

            futures[f"{folder}/meteogram.png"] = executor.submit(
                create_meteogram_chart, points, prefs, day_min, day_max, events, figsize=(22, 14.8))

            futures[f"{folder}/wykres_astronomiczny.png"] = executor.submit(
                create_astro_timeline_chart, day.get('observedAstroEvents'), day_min, day_max, events, scale=3)

            futures[f"{folder}/mapa_trasy.png"] = executor.submit(
                create_route_map, points, 1800, 900)

            futures[f"{folder}/róża_wiatrów.png"] = executor.submit(
                create_polar_rose, points, 'windDir', 'windSpeed', 'Róża wiatrów', prefs, figsize=(7.8, 7.8))

            futures[f"{folder}/róża_falowania.png"] = executor.submit(
                create_polar_rose, points, 'waveDir', 'waveHeight', 'Róża falowania', prefs, figsize=(7.8, 7.8))

            futures[f"{folder}/wykres_ruchu_i_postojów.png"] = executor.submit(
                create_timeline_chart, day.get('date'), events, day_min, day_max, scale=3)

    zip_buf = io.BytesIO()
    with zipfile.ZipFile(zip_buf, 'w', zipfile.ZIP_DEFLATED) as zf:
        for path_in_zip, future in futures.items():
            try:
                b64_data = future.result()
                if b64_data:
                    zf.writestr(path_in_zip, base64.b64decode(b64_data))
            except Exception as e:
                print(f"Błąd podczas zapisu wykresu {path_in_zip}: {e}")

    return zip_buf.getvalue()