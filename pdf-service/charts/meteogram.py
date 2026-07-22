import io
import base64
import numpy as np
import matplotlib
matplotlib.use('Agg')
from matplotlib.figure import Figure
from matplotlib.backends.backend_agg import FigureCanvasAgg as FigureCanvas
from matplotlib.ticker import FuncFormatter

from utils import parse_dt, convert_raw


def create_meteogram_chart(points, prefs, min_sec=None, max_sec=None, events=None):
    if not points: return None

    is_daily_sync = (min_sec is not None and max_sec is not None)

    # filtrowanie po dacie
    if is_daily_sync:
        first_dt = None
        for p in points:
            dt = parse_dt(p.get('time') or p.get('timeMs'))
            if dt:
                first_dt = dt.date()
                break

        if first_dt:
            filtered = []
            for p in points:
                dt = parse_dt(p.get('time') or p.get('timeMs'))
                if dt and dt.date() == first_dt:
                    filtered.append(p)
            points = filtered

    gap_intervals = []
    if is_daily_sync and events:
        for ev in events:
            ev_type = str(ev.get('type', '')).upper()
            if "BRAK" in ev_type or "GAP" in ev_type:
                dt_s = parse_dt(ev.get('start'))
                dt_e = parse_dt(ev.get('end'))
                if dt_s and dt_e:
                    s_sec = dt_s.hour * 3600 + dt_s.minute * 60 + dt_s.second
                    e_sec = dt_e.hour * 3600 + dt_e.minute * 60 + dt_e.second
                    if e_sec <= s_sec:
                        e_sec = 86400
                    gap_intervals.append((s_sec, e_sec))

    raw_data = []

    for i, p in enumerate(points):
        dt = parse_dt(p.get('time') or p.get('timeMs'))
        s = None
        if is_daily_sync and dt:
            s = dt.hour * 3600 + dt.minute * 60 + dt.second
            for (gs, ge) in gap_intervals:
                if gs <= s <= ge:
                    p = {}
                    break

        if is_daily_sync and s is not None:
            raw_data.append((s, dt, p))
        else:
            raw_data.append((i, dt, p))

    if is_daily_sync:
        for (gs, ge) in gap_intervals:
            raw_data.append((gs + 1, None, {}))
            raw_data.append((ge - 1, None, {}))

    raw_data.sort(key=lambda item: item[0])

    x_vals = []
    parsed_dates = []
    valid_points = []

    for item in raw_data:
        x_vals.append(item[0])
        parsed_dates.append(item[1])
        valid_points.append(item[2])

    points = valid_points
    x = np.array(x_vals)

    if len(x) == 0:
        return None

    def get_data(key, category=None, default=np.nan):
        arr = []
        for p in points:
            val = p.get(key)
            if category:
                converted = convert_raw(val, category, prefs)
                arr.append(converted if not np.isnan(converted) else default)
            else:
                if val is not None and str(val).lower() != 'nan':
                    try:
                        arr.append(float(val))
                    except ValueError:
                        arr.append(default)
                else:
                    arr.append(default)
        return np.array(arr)

    def safe_max(arr, default_val):
        valid = arr[~np.isnan(arr)]
        return np.max(valid) if len(valid) > 0 else default_val

    def safe_min(arr, default_val):
        valid = arr[~np.isnan(arr)]
        return np.min(valid) if len(valid) > 0 else default_val

    pressure = get_data('pressure', 'pressure')
    temp = get_data('temp', 'temp')
    sea_temp = get_data('seaTemp', 'temp')
    wind_spd = get_data('windSpeed', 'wind')
    wind_gst = get_data('gusts', 'wind')
    wind_dir = get_data('windDir')
    wave_hgt = get_data('waveHeight', 'wave')
    wave_per = get_data('wavePeriod')
    wave_dir = get_data('waveDir')
    swell_hgt = get_data('swellWaveH', 'wave')
    swell_per = get_data('swellWaveP')
    swell_dir = get_data('waveDir')
    clouds = get_data('cloudCover', 'clouds')
    rain = get_data('rain', 'rain', default=0.0)
    snow = get_data('snowfall', 'snow', default=0.0)
    speed = get_data('speed', 'speed')

    if np.all(np.isnan(temp)) and np.all(np.isnan(wind_spd)): return None

    has_waves = not np.all(np.isnan(wave_hgt))
    has_swell = not np.all(np.isnan(swell_hgt))

    def comma_fmt(x, pos):
        return f"{x:g}".replace('.', ',')
    comma_formatter = FuncFormatter(comma_fmt)

    num_rows = 3
    if has_waves: num_rows += 1
    if has_swell: num_rows += 1

    fig = Figure(figsize=(11, 7.4))
    canvas = FigureCanvas(fig)
    axes = fig.subplots(nrows=num_rows, ncols=1, sharex=True, gridspec_kw={'hspace': 0.70})

    if not isinstance(axes, (np.ndarray, list)):
        axes = [axes]

    for ax in axes:
        ax.grid(axis='both', linestyle='--', alpha=0.5)
        ax.spines['top'].set_visible(False)
        ax.spines['right'].set_visible(False)
        ax.tick_params(axis='x', direction='in', length=3)
        ax.tick_params(axis='y', labelsize=8)
        ax.yaxis.set_major_formatter(comma_formatter)

        if is_daily_sync:
            ax.set_xlim(min_sec, max_sec)
        elif len(x) > 1:
            ax.set_xlim(0, len(x) - 1)

    legend_kwargs = dict(bbox_to_anchor=(0, 1.02, 1, 0.2), loc="lower left", mode="expand", borderaxespad=0, ncol=4, frameon=False, fontsize=8)

    ax_idx = 0

    # Pobieranie jednostek do etykiet
    u_p = prefs.get('pressure', 'hPa')
    u_t = prefs.get('temp', '°C')
    u_w = prefs.get('wind', 'km/h')
    u_s = prefs.get('speed', 'km/h')
    u_wave = prefs.get('wave', 'm')
    u_rain = prefs.get('rain', 'mm')
    u_snow = prefs.get('snow', 'cm')

    #PANEL 1: Ciśnienie + Temperatura
    ax1 = axes[ax_idx]
    ax_idx += 1
    ax1_twin = ax1.twinx()

    p_min = safe_min(pressure, 1000)
    ax1.plot(x, pressure, color='#172554', lw=2, label=f'Ciśnienie ({u_p})')
    ax1.fill_between(x, p_min - 5, pressure, color='#d9f99d', alpha=0.5)
    ax1.set_ylabel(f'Ciśnienie ({u_p})', fontweight='bold', color='#172554', fontsize=9)
    ax1.set_ylim(p_min - 2, safe_max(pressure, 1020) + 2)

    ax1_twin.plot(x, temp, color='#ef4444', lw=2, label=f'Temperatura powietrza ({u_t})')
    if not np.all(np.isnan(sea_temp)):
        ax1_twin.plot(x, sea_temp, color='#0284c7', lw=2, ls='--', label=f'Temperatura morza ({u_t})')

    ax1_twin.set_ylabel(f'Temperatura ({u_t})', fontweight='bold', color='#ef4444', fontsize=9)
    ax1_twin.spines['top'].set_visible(False)
    ax1_twin.tick_params(axis='y', labelsize=8)
    ax1_twin.yaxis.set_major_formatter(comma_formatter)

    h1, l1 = ax1.get_legend_handles_labels()
    h2, l2 = ax1_twin.get_legend_handles_labels()
    ax1.legend(h1 + h2, l1 + l2, **legend_kwargs)

    def setup_arrow_panel(ax_main, data_arr, color_arr, label_arr, dir_arr, ylabel, color_hex, is_twin=False,
                          secondary_data=None, secondary_label=None, secondary_color=None,
                          tertiary_data=None, tertiary_label=None, tertiary_color=None, tertiary_is_twin=False, tertiary_axis_label=None):

        max_val = safe_max(data_arr, 1.0)

        if secondary_data is not None and not is_twin:
            max_val = max(max_val, safe_max(secondary_data, 1.0))

        if tertiary_data is not None and not tertiary_is_twin:
            max_val = max(max_val, safe_max(tertiary_data, 1.0))

        if max_val == 0:
            max_val = 1.0

        if not np.all(np.isnan(data_arr)):
            ax_main.fill_between(x, 0, data_arr, color=color_arr[0], alpha=0.6)
            ax_main.plot(x, data_arr, color=color_arr[1], lw=2, label=label_arr[0])

        if secondary_data is not None and not is_twin and not np.all(np.isnan(secondary_data)):
            ax_main.plot(x, secondary_data, color=secondary_color, lw=1.5, ls=':', marker='.', markersize=4, label=secondary_label)

        if tertiary_data is not None and not tertiary_is_twin and not np.all(np.isnan(tertiary_data)):
            ax_main.plot(x, tertiary_data, color=tertiary_color, lw=2.5, label=tertiary_label)

        ax_main.set_ylabel(ylabel, fontweight='bold', color=color_arr[1], fontsize=9)
        ax_main.set_ylim(0, max_val * 1.15)
        ax_main.axhline(0, color='#64748b', linewidth=1.0)
        ax_main.yaxis.set_major_formatter(comma_formatter)

        ax_twin = None
        has_sec_twin = is_twin and secondary_data is not None and not np.all(np.isnan(secondary_data))
        has_ter_twin = tertiary_is_twin and tertiary_data is not None and not np.all(np.isnan(tertiary_data))

        if has_sec_twin or has_ter_twin:
            ax_twin = ax_main.twinx()
            ax_twin.spines['top'].set_visible(False)
            ax_twin.tick_params(axis='y', labelsize=8)
            ax_twin.yaxis.set_major_formatter(comma_formatter)

        if has_sec_twin:
            ax_twin.plot(x, secondary_data, color=secondary_color, lw=1.5, ls='-.', label=secondary_label)
            ax_twin.set_ylabel(secondary_label, fontweight='bold', color=secondary_color, fontsize=9)
            sec_min = safe_min(secondary_data, 0)
            sec_max = safe_max(secondary_data, 1)
            margin = (sec_max - sec_min) * 0.15 if sec_max != sec_min else 1.0
            ax_twin.set_ylim(max(0, sec_min - margin), sec_max + margin)

        if has_ter_twin:
            ax_twin.plot(x, tertiary_data, color=tertiary_color, lw=2.5, ls='-', label=tertiary_label)
            if not has_sec_twin:
                axis_lbl = tertiary_axis_label if tertiary_axis_label is not None else tertiary_label
                ax_twin.set_ylabel(axis_lbl, fontweight='bold', color=tertiary_color, fontsize=9)
                ter_min = safe_min(tertiary_data, 0)
                ter_max = safe_max(tertiary_data, 1)
                margin = (ter_max - ter_min) * 0.15 if ter_max != ter_min else 1.0
                upper_limit = max(2.0, ter_max + margin)
                ax_twin.set_ylim(max(0, ter_min - margin), upper_limit)

        valid_dir = ~np.isnan(dir_arr)
        if np.any(valid_dir):
            x_valid = x[valid_dir]
            dir_valid = dir_arr[valid_dir]

            target_arrows = 22
            min_dist = (max_sec - min_sec) / target_arrows if is_daily_sync else len(x) / target_arrows

            x_val, final_dir = [], []
            last_x = -float('inf')

            for xv, dv in zip(x_valid, dir_valid):
                if xv - last_x >= min_dist:
                    x_val.append(xv)
                    final_dir.append(dv)
                    last_x = xv

            rad_dir = np.radians(270 - np.array(final_dir))
            u, v = np.cos(rad_dir), np.sin(rad_dir)
            y_val = np.full_like(x_val, -0.22, dtype=float)

            ax_main.quiver(x_val, y_val, u, v,
                           color=color_hex, scale=45, width=0.003, headwidth=4, pivot='middle',
                           transform=ax_main.get_xaxis_transform(), clip_on=False)

        h_main, l_main = ax_main.get_legend_handles_labels()
        if ax_twin:
            h_twin, l_twin = ax_twin.get_legend_handles_labels()
            ax_main.legend(h_main + h_twin, l_main + l_twin, **legend_kwargs)
        elif h_main:
            ax_main.legend(h_main, l_main, **legend_kwargs)

    #PANEL 2: Wiatr + Porywy + Kierunek
    ax_wind = axes[ax_idx]
    ax_idx += 1
    setup_arrow_panel(
        ax_wind, wind_spd, ['#bae6fd', '#0284c7'], [f'Wiatr ({u_w})'], wind_dir, f'Prędkość ({u_w})', '#1e3a8a',
        is_twin=False,
        secondary_data=wind_gst, secondary_label='Porywy', secondary_color='#1e3a8a',
        tertiary_data=speed, tertiary_label=f'Prędkość jednostki ({u_s})', tertiary_color='#dc2626',
        tertiary_is_twin=True, tertiary_axis_label=f'Prędkość ({u_s})'
    )

    #PANEL 3: Fale Główne
    if has_waves:
        ax_wave = axes[ax_idx]
        ax_idx += 1
        setup_arrow_panel(ax_wave, wave_hgt, ['#99f6e4', '#0d9488'], [f'Wys. fali ({u_wave})'], wave_dir, f'Fala ({u_wave})', '#0f766e', is_twin=True, secondary_data=wave_per, secondary_label='Okres (s)', secondary_color='#ea580c')

    #PANEL 4: Fale Martwe (Swell)
    if has_swell:
        ax_swell = axes[ax_idx]
        ax_idx += 1
        setup_arrow_panel(ax_swell, swell_hgt, ['#e9d5ff', '#9333ea'], [f'Wys. fali martwej ({u_wave})'], swell_dir, f'Fala ({u_wave})', '#7e22ce', is_twin=True, secondary_data=swell_per, secondary_label='Okres (s)', secondary_color='#be185d')


    #PANEL 5: Zachmurzenie + Deszcz + Śnieg
    ax5 = axes[ax_idx]
    ax5_twin = ax5.twinx()

    r_unit = prefs.get('rain', 'mm')
    s_unit = prefs.get('snow', 'cm')

    plot_rain = rain.copy()
    plot_snow = snow.copy()

    if s_unit == 'cm':
        plot_snow = plot_snow * 10
    elif s_unit == 'inch':
        plot_snow = plot_snow * 25.4

    if r_unit == 'inch':
        plot_rain = plot_rain * 25.4


    bar_width = 1800 if is_daily_sync else 0.4

    valid_rain = ~np.isnan(plot_rain)
    ax5.bar(x[valid_rain] - bar_width/2, plot_rain[valid_rain], width=bar_width, color='#3b82f6', alpha=0.8, label=f'Deszcz (mm/h)')

    valid_snow = ~np.isnan(plot_snow)
    ax5.bar(x[valid_snow] + bar_width/2, plot_snow[valid_snow], width=bar_width, color='#2dd4bf', alpha=0.8, label=f'Śnieg (mm/h)')

    ax5.set_ylabel('Opady (mm/h)', fontweight='bold', color='#2563eb', fontsize=9)

    max_precip = safe_max(np.concatenate([plot_rain, plot_snow]), 10.0)
    ax5.set_ylim(0, max(1.0, max_precip * 1.5))

    if not np.all(np.isnan(clouds)):
        ax5_twin.fill_between(x, 0, clouds, color='#cbd5e1', alpha=0.5)
        ax5_twin.plot(x, clouds, color='#64748b', lw=1.5, label='Zachmurzenie (%)')

    ax5_twin.set_ylabel('Chmury (%)', fontweight='bold', color='#475569', fontsize=9)
    ax5_twin.set_ylim(0, 105)
    ax5_twin.spines['top'].set_visible(False)
    ax5_twin.tick_params(axis='y', labelsize=8)
    ax5_twin.yaxis.set_major_formatter(comma_formatter)

    h7, l7 = ax5.get_legend_handles_labels()
    h8, l8 = ax5_twin.get_legend_handles_labels()
    ax5_twin.legend(h7 + h8, l7 + l8, bbox_to_anchor=(0, 1.02, 1, 0.2), loc="lower left", mode="expand", borderaxespad=0, ncol=3, frameon=False, fontsize=8)


    try:
        if is_daily_sync:

            total_seconds = max_sec - min_sec
            steps = [1800, 3600, 7200, 14400, 28800, 43200]

            best_step = 3600
            for s in steps:
                if total_seconds / s <= 8:
                    best_step = s
                    break

            ticks = [int(min_sec)]

            target_next = min_sec + best_step
            current = round(target_next / best_step) * best_step

            while current < max_sec:
                if current > min_sec:
                    ticks.append(int(current))
                current += best_step

            if ticks[-1] != int(max_sec):
                ticks.append(int(max_sec))

            ticks = sorted(list(set(ticks)))

            labels = []
            for t in ticks:
                if t >= 86400:
                    labels.append("24:00")
                else:
                    labels.append(f"{t // 3600 % 24:02d}:{t // 60 % 60:02d}")

            ax5.set_xticks(ticks)
            ax5.set_xticklabels(labels, rotation=0, fontweight='bold', fontsize=8)
        else:
            has_time = any(d is not None for d in parsed_dates)
            if has_time:
                valid_dates = [d for d in parsed_dates if d is not None]
                time_span = valid_dates[-1] - valid_dates[0]
                total_hours = time_span.total_seconds() / 3600.0

                num_ticks = min(8, len(x))
                if num_ticks > 1:
                    ticks = np.linspace(0, len(x) - 1, num_ticks, dtype=int).tolist()
                else:
                    ticks = [0]

                ax5.set_xticks(ticks)
                labels = []
                for i in ticks:
                    dt = parsed_dates[i] if i < len(parsed_dates) else None
                    if dt:
                        if total_hours > 36: labels.append(dt.strftime("%d.%m\n%H:%M"))
                        else: labels.append(dt.strftime("%H:%M"))
                    else:
                        labels.append("")
                ax5.set_xticklabels(labels, rotation=0, fontweight='bold', fontsize=8)
            else:
                ax5.set_xticks([])
    except Exception as e:
        print(f"Błąd formatowania osi X: {e}")
        pass

    fig.subplots_adjust(left=0.06, right=0.94, top=0.92, bottom=0.08)

    buf = io.BytesIO()
    fig.savefig(buf, format='png', bbox_inches='tight', dpi=96, transparent=False)
    return base64.b64encode(buf.getvalue()).decode('utf-8')