import io
import base64
from PIL import Image, ImageDraw

from utils import parse_dt, format_place, get_font
import datetime
import io
import base64
import re
from PIL import Image, ImageDraw

from utils import parse_dt, format_place, get_font

def create_astro_timeline_chart(astro_events, base_date_str, min_sec=0, max_sec=86400, events=None, scale=1):
    if not astro_events or not isinstance(astro_events, dict):
        return None

    WIDTH = 1400
    HEIGHT = 130
    MARGIN_SIDE = 30
    PLOT_WIDTH = WIDTH - (2 * MARGIN_SIDE)
    AXIS_Y = 60

    range_sec = max_sec - min_sec
    if range_sec <= 0: range_sec = 3600

    image = Image.new('RGB', (WIDTH, HEIGHT), 'white')
    draw = ImageDraw.Draw(image)

    font_time = get_font(18, bold=True)
    font_legend = get_font(13, bold=False)

    parsed_events = []

    for name, iso_str in astro_events.items():
        if not iso_str: continue
        try:
            safe_iso = iso_str if len(iso_str) >= 19 else iso_str + ":00"
            event_dt = datetime.datetime.strptime(safe_iso[:19], "%Y-%m-%dT%H:%M:%S")

            sec = event_dt.hour * 3600 + event_dt.minute * 60 + event_dt.second
            time_str = event_dt.strftime("%H:%M")

            name_clean = str(name).strip()
            parsed_events.append({"name": name_clean, "sec": sec, "time_str": time_str})
        except Exception as e:
            pass

    if not parsed_events: return None
    parsed_events.sort(key=lambda x: x['sec'])

    gap_intervals = []
    if events:
        for ev in events:
            ev_type = str(ev.get('type', '')).upper()
            if "BRAK" in ev_type:
                dt_s = parse_dt(ev.get('start'))
                dt_e = parse_dt(ev.get('end'))
                if dt_s and dt_e:
                    s_sec = dt_s.hour * 3600 + dt_s.minute * 60 + dt_s.second
                    e_sec = dt_e.hour * 3600 + dt_e.minute * 60 + dt_e.second
                    if e_sec <= s_sec:
                        e_sec = 86400
                    gap_intervals.append((s_sec, e_sec))

    color_map = {
        "Świt astronomiczny": (26, 54, 150), "Świt nautyczny": (66, 170, 245), "Świt cywilny": (245, 80, 180),
        "Wschód Słońca": (255, 220, 0), "Kulminacja Słońca": (255, 255, 255), "Zachód Słońca": (245, 110, 0),
        "Zmierzch cywilny": (210, 30, 30), "Zmierzch nautyczny": (110, 20, 160), "Zmierzch astronomiczny": (8, 12, 25),
        "Wschód Księżyca": (92,213,70),
        "Zachód Księżyca": (18,92,12)
    }

    def get_band_color(sec):

        #brak danych
        for (gs, ge) in gap_intervals:
            if gs <= sec <= ge:
                return (160, 160, 160)

        sun_events = [
            "Świt astronomiczny", "Świt nautyczny", "Świt cywilny",
            "Wschód Słońca", "Kulminacja Słońca", "Zachód Słońca",
            "Zmierzch cywilny", "Zmierzch nautyczny", "Zmierzch astronomiczny"
        ]

        filtered_sun_events = [ev for ev in parsed_events if ev['name'] in sun_events]
        if not filtered_sun_events:
            return (8, 12, 25)

        last_sun_event = None
        for ev in filtered_sun_events:
            if sec >= ev['sec']:
                last_sun_event = ev['name']

        if last_sun_event:
            if last_sun_event in ["Świt astronomiczny"]: return (26, 54, 150)
            if last_sun_event in ["Świt nautyczny"]: return (66, 170, 245)
            if last_sun_event in ["Świt cywilny"]: return (245, 80, 180)
            if last_sun_event in ["Wschód Słońca", "Kulminacja Słońca"]: return (255, 220, 0)
            if last_sun_event in ["Zachód Słońca"]: return (245, 110, 0)
            if last_sun_event in ["Zmierzch cywilny"]: return (210, 30, 30)
            if last_sun_event in ["Zmierzch nautyczny"]: return (110, 20, 160)
            if last_sun_event in ["Zmierzch astronomiczny"]: return (8, 12, 25)

        first_sun_event = filtered_sun_events[0]['name']


        if first_sun_event == "Świt astronomiczny":
            return (8, 12, 25)

        if first_sun_event == "Świt nautyczny":
            return (110, 20, 160)

        if first_sun_event == "Świt cywilny":
            return (210, 30, 30)

        if first_sun_event == "Wschód Słońca":
            return (245, 110, 0)

        if first_sun_event in ["Kulminacja Słońca", "Zachód Słońca"]:
            return (255, 220, 0)

        if first_sun_event == "Zmierzch cywilny":
            return (245, 110, 0)

        if first_sun_event == "Zmierzch nautyczny":
            return (210, 30, 30)

        if first_sun_event == "Zmierzch astronomiczny":
            return (110, 20, 160)

        return (8, 12, 25)

    for x in range(MARGIN_SIDE, WIDTH - MARGIN_SIDE + 1):
        sec = min_sec + ((x - MARGIN_SIDE) / float(PLOT_WIDTH)) * range_sec
        c = get_band_color(sec)
        draw.line([(x, AXIS_Y - 6), (x, AXIS_Y + 6)], fill=c, width=1)

    draw.rectangle([(MARGIN_SIDE, AXIS_Y - 6), (WIDTH - MARGIN_SIDE, AXIS_Y + 6)], outline="#94a3b8", width=2)

    time_labels = []

    for idx, ev in enumerate(parsed_events):
        try:
            x = MARGIN_SIDE + int(((ev['sec'] - min_sec) / range_sec) * PLOT_WIDTH)
            color = color_map.get(ev['name'], (150, 150, 150))
            r = 8
            if ev['name'] == "Kulminacja Słońca":
                outline_color = "#000000"
            else:
                outline_color = "#FFFFFF"
            draw.ellipse([(x - r, AXIS_Y - r), (x + r, AXIS_Y + r)], fill=color, outline=outline_color, width=2)

            time_w = draw.textlength(ev['time_str'], font=font_time)
            time_labels.append({
                'x': x - (time_w / 2),
                'w': time_w,
                'anchor_x': x,
                'time_str': ev['time_str'],
                'radius': r
            })
        except Exception:
            pass


    def resolve_collisions(items, margin_left, margin_right, gap=12):
        if not items: return items
        # odsuwanie w prawo jeśli coś wchodzi z lewej
        for i in range(1, len(items)):
            if items[i]['x'] < items[i-1]['x'] + items[i-1]['w'] + gap:
                items[i]['x'] = items[i-1]['x'] + items[i-1]['w'] + gap

        # Jeżeli całość wyszła za prawy ekran,cofamy wszystko w lewo
        if items[-1]['x'] + items[-1]['w'] > margin_right:
            items[-1]['x'] = margin_right - items[-1]['w']

        # Przejście w lewo
        for i in range(len(items)-2, -1, -1):
            if items[i]['x'] + items[i]['w'] + gap > items[i+1]['x']:
                items[i]['x'] = items[i+1]['x'] - items[i]['w'] - gap

        # Zabezpieczenie przed wyjechaniem za lewy margines
        if items[0]['x'] < margin_left:
            items[0]['x'] = margin_left
            for i in range(1, len(items)):
                if items[i]['x'] < items[i-1]['x'] + items[i-1]['w'] + 5:
                    items[i]['x'] = items[i-1]['x'] + items[i-1]['w'] + 5
        return items

    resolve_collisions(time_labels, MARGIN_SIDE, WIDTH - MARGIN_SIDE)

    for lbl in time_labels:
        actual_x = lbl['x']
        anchor_x = lbl['anchor_x']
        r = lbl['radius']
        text_center = actual_x + (lbl['w'] / 2)

        if abs(text_center - anchor_x) > 5:
            draw.line([(anchor_x, AXIS_Y - r), (text_center, AXIS_Y - 20)], fill="#94a3b8", width=2)
        else:
            draw.line([(anchor_x, AXIS_Y - r), (anchor_x, AXIS_Y - 12)], fill="#94a3b8", width=2)

        draw.text((actual_x, AXIS_Y - 45), lbl['time_str'], fill="black", font=font_time)


    legend_items = []
    for ev in parsed_events:
        if not any(item['name'] == ev['name'] for item in legend_items):
            legend_items.append({'name': ev['name'], 'color': color_map.get(ev['name'], (150, 150, 150))})

    if legend_items:
        item_widths = [10 + 4 + draw.textlength(item['name'], font=font_legend) for item in legend_items]
        total_items_width = sum(item_widths)

        available_width = WIDTH - (2 * MARGIN_SIDE)

        if len(legend_items) > 1:
            gap = (available_width - total_items_width) / (len(legend_items) - 1)
            gap = min(gap, 30)
        else:
            gap = 0

        actual_total_width = total_items_width + (gap * max(0, len(legend_items) - 1))
        start_x = MARGIN_SIDE + (available_width - actual_total_width) / 2

        legend_x = start_x
        legend_y = AXIS_Y + 35

        for i, item in enumerate(legend_items):
            lr = 5
            draw.ellipse([(legend_x, legend_y - lr + 5), (legend_x + 2*lr, legend_y + lr + 5)], fill=item['color'], outline="#000000", width=1)
            draw.text((legend_x + 2*lr + 4, legend_y - 2), item['name'], fill="#334155", font=font_legend)

            legend_x += item_widths[i] + gap

    buf = io.BytesIO()
    if scale != 1:
        image = image.resize((int(WIDTH * scale), int(HEIGHT * scale)), Image.LANCZOS)
    image.save(buf, format="PNG", dpi=(96, 96))
    return base64.b64encode(buf.getvalue()).decode('utf-8')


def create_timeline_chart(date_str, events, min_sec=0, max_sec=86400, scale=1):
    if not events: return None

    WIDTH = 1400
    HEIGHT = 140
    MARGIN_SIDE = 30
    PLOT_WIDTH = WIDTH - (2 * MARGIN_SIDE)
    AXIS_Y = 70

    range_sec = max_sec - min_sec
    if range_sec <= 0: range_sec = 3600

    image = Image.new('RGB', (WIDTH, HEIGHT), 'white')
    draw = ImageDraw.Draw(image)

    font_type = get_font(18, bold=True)
    font_place = get_font(16, italic=True)
    font_time = get_font(18, bold=True)

    valid_events = []

    for ev in events:
        dt_start = parse_dt(ev.get('start'))
        dt_end = parse_dt(ev.get('end'))
        if not dt_start or not dt_end: continue

        start_sec = dt_start.hour * 3600 + dt_start.minute * 60 + dt_start.second
        end_sec = dt_end.hour * 3600 + dt_end.minute * 60 + dt_end.second

        if end_sec <= start_sec:
            end_sec = 86400

        valid_events.append((ev, start_sec, end_sec, dt_start, dt_end))

    if not valid_events: return None


    draw.line([(MARGIN_SIDE, AXIS_Y), (WIDTH - MARGIN_SIDE, AXIS_Y)], fill="lightgray", width=4)

    top_labels = []
    bot_labels = []

    for i, (ev, start_sec, end_sec, dt_start, dt_end) in enumerate(valid_events):
        x1 = MARGIN_SIDE + int(((start_sec - min_sec) / range_sec) * PLOT_WIDTH)
        x2 = MARGIN_SIDE + int(((end_sec - min_sec) / range_sec) * PLOT_WIDTH)
        center_x = (x1 + x2) // 2

        ev_type = ev.get('type', '')
        if ev_type == "RUCH": color = (34, 139, 34)
        elif ev_type == "POSTÓJ": color = (0, 0, 255)
        else: color = (160, 160, 160)

        draw.line([(x1, AXIS_Y), (x2, AXIS_Y)], fill=color, width=24)
        draw.line([(x1, AXIS_Y - 15), (x1, AXIS_Y + 15)], fill="darkgray", width=3)

        type_str = ev_type
        place_str = format_place(ev.get('placeName'))

        w1 = draw.textlength(type_str, font=font_type)
        w2 = draw.textlength(place_str, font=font_place) if place_str else 0
        w = max(w1, w2)

        top_labels.append({
            'x': center_x - w / 2, 'w': w, 'center_x': center_x,
            'type_str': type_str, 'place_str': place_str, 'color': color
        })

        time_str = dt_start.strftime("%H:%M")
        tw = draw.textlength(time_str, font=font_time)
        bot_labels.append({
            'x': x1 - tw / 2, 'w': tw, 'anchor_x': x1, 'time_str': time_str
        })

        if i == len(valid_events) - 1:
            draw.line([(x2, AXIS_Y - 15), (x2, AXIS_Y + 15)], fill="darkgray", width=3)
            if end_sec >= 86400:
                end_time_str = "24:00"
            else:
                end_time_str = dt_end.strftime("%H:%M")
            etw = draw.textlength(end_time_str, font=font_time)
            bot_labels.append({
                'x': x2 - etw / 2, 'w': etw, 'anchor_x': x2, 'time_str': end_time_str
            })

    total_required_width = sum(lbl['w'] for lbl in top_labels) + 15 * (len(top_labels) - 1)

    # Jeśli podpisy postojów się nie zmieszczą, usuwane są dopiski w nawiasach
    if total_required_width > PLOT_WIDTH:
        for lbl in top_labels:
            if lbl['place_str']:
                lbl['place_str'] = re.sub(r'\s*\(.*?\)', '', lbl['place_str']).strip()

                w1 = draw.textlength(lbl['type_str'], font=font_type)
                w2 = draw.textlength(lbl['place_str'], font=font_place)
                lbl['w'] = max(w1, w2)
                lbl['x'] = lbl['center_x'] - lbl['w'] / 2

    def resolve_collisions(items, margin_left, margin_right, gap=15):
        if not items: return items

        for i in range(1, len(items)):
            if items[i]['x'] < items[i-1]['x'] + items[i-1]['w'] + gap:
                items[i]['x'] = items[i-1]['x'] + items[i-1]['w'] + gap

        if items[-1]['x'] + items[-1]['w'] > margin_right:
            items[-1]['x'] = margin_right - items[-1]['w']

        for i in range(len(items)-2, -1, -1):
            if items[i]['x'] + items[i]['w'] + gap > items[i+1]['x']:
                items[i]['x'] = items[i+1]['x'] - items[i]['w'] - gap

        if items[0]['x'] < margin_left:
            items[0]['x'] = margin_left
            for i in range(1, len(items)):
                if items[i]['x'] < items[i-1]['x'] + items[i-1]['w'] + 5:
                    items[i]['x'] = items[i-1]['x'] + items[i-1]['w'] + 5
        return items

    resolve_collisions(top_labels, MARGIN_SIDE, WIDTH - MARGIN_SIDE)
    resolve_collisions(bot_labels, MARGIN_SIDE, WIDTH - MARGIN_SIDE)

    for lbl in top_labels:
        actual_x = lbl['x']
        text_center = actual_x + (lbl['w'] / 2)

        if abs(text_center - lbl['center_x']) > 8:
            draw.line([(lbl['center_x'], AXIS_Y - 15), (text_center, AXIS_Y - 30)], fill="lightgray", width=2)

        draw.text((actual_x, AXIS_Y - 45), lbl['type_str'], fill=lbl['color'], font=font_type)
        if lbl['place_str']:
            draw.text((actual_x, AXIS_Y - 65), lbl['place_str'], fill="black", font=font_place)

    for lbl in bot_labels:
        actual_x = lbl['x']
        text_center = actual_x + (lbl['w'] / 2)

        if abs(text_center - lbl['anchor_x']) > 8:
            draw.line([(lbl['anchor_x'], AXIS_Y + 15), (text_center, AXIS_Y + 25)], fill="lightgray", width=2)

        draw.text((actual_x, AXIS_Y + 30), lbl['time_str'], fill="black", font=font_time)

    buf = io.BytesIO()
    if scale != 1:
        image = image.resize((int(WIDTH * scale), int(HEIGHT * scale)), Image.LANCZOS)
    image.save(buf, format="PNG", dpi=(96, 96))
    return base64.b64encode(buf.getvalue()).decode('utf-8')


