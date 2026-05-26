from jinja2 import Environment, FileSystemLoader
from weasyprint import HTML
import os
import time
from concurrent.futures import ProcessPoolExecutor

from utils import format_seconds, format_val, format_time, get_duration_seconds, format_place, format_metric, convert_raw

from charts.meteogram import create_meteogram_chart
from charts.polar_rose import create_polar_rose
from charts.route_map import create_route_map
from charts.timelines import create_timeline_chart, create_astro_timeline_chart

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
                        if v_float > current_maxes[target_key]:
                            current_maxes[target_key] = v_float
                            has_valid[target_key] = True
                    except ValueError:
                        pass
                    break

    for k in max_keys_map:
        if has_valid[k]:
            stats_dict[k] = current_maxes[k]


def generate_report_pdf(report_data: dict) -> bytes:
    print("\n--- ROZPOCZĘCIE GENEROWANIA RAPORTU ---")
    start_total = time.time()

    prefs = report_data.get('preferences', {})
    daily_summaries = report_data.get('dailySummaries', [])
    all_trip_points = report_data.get('points', [])

    display_summaries = []
    current_gap = []

    with ProcessPoolExecutor(max_workers=4) as executor:
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
                meteo_points = points[::max(1, len(points) // 24)] if points else []
                rose_points = points[::max(1, len(points) // 24)] if points else []

                map_future = executor.submit(create_route_map, map_points, 600, 300)
                timeline_future = executor.submit(create_timeline_chart, day.get('date'), events) if events else None
                astro_future = executor.submit(create_astro_timeline_chart, day.get('observedAstroEvents'))

                day['meteogram_chart'] = create_meteogram_chart(meteo_points, prefs)

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

        overall_map_points = all_trip_points[::max(1, len(all_trip_points) // 100)]
        overall_rose_points = all_trip_points[::max(1, len(all_trip_points) // 50)]

        overall_map_future = executor.submit(create_route_map, overall_map_points, 800, 555)

        report_data['overall_wind_rose'] = create_polar_rose(overall_rose_points, 'windDir', 'windSpeed', 'Róża wiatrów', prefs)
        report_data['overall_wave_rose'] = create_polar_rose(overall_rose_points, 'waveDir', 'waveHeight', 'Róża falowania', prefs)

        overall_meteo_points = all_trip_points[::max(1, len(all_trip_points) // 120)] if all_trip_points else []

        if len(overall_meteo_points) > 1:
            try:
                report_data['overall_meteogram_chart'] = create_meteogram_chart(overall_meteo_points, prefs)
            except Exception:
                pass

        report_data['overall_route_map'] = overall_map_future.result()

    template = env.get_template('report_template.html')
    html_content = template.render(data=report_data, prefs=prefs)

    base_dir = os.path.abspath('templates')
    pdf_bytes = HTML(string=html_content, base_url=base_dir).write_pdf()

    total_time = time.time() - start_total

    print(f"✅ RAPORT WYGENEROWANY W: {total_time:.2f} s\n")

    return pdf_bytes