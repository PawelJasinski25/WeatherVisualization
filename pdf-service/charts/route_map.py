import io
import base64

try:
    from staticmap import StaticMap, Line
    STATICMAP_AVAILABLE = True
except ImportError:
    STATICMAP_AVAILABLE = False


def create_route_map(points, width=800, height=400):
    if not STATICMAP_AVAILABLE or not points: return None

    segments = {}

    for p in points:
        lat = p.get('latitude')
        lon = p.get('longitude')
        seg_id = p.get('segmentId', 0)

        if lat is not None and lon is not None:
            try:
                if seg_id not in segments:
                    segments[seg_id] = []
                segments[seg_id].append((float(lon), float(lat)))
            except Exception:
                pass

    valid_segments = {k: v for k, v in segments.items() if len(v) >= 2}

    if not valid_segments:
        return None

    try:
        mapa = StaticMap(width, height, url_template='http://a.tile.osm.org/{z}/{x}/{y}.png')

        for seg_id, coords in valid_segments.items():
            line = Line(coords, '#3b82f6', 5)
            mapa.add_line(line)

        image = mapa.render()

        buf = io.BytesIO()
        image.save(buf, format='PNG')
        return base64.b64encode(buf.getvalue()).decode('utf-8')
    except Exception:
        return None