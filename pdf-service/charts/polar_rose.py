import io
import base64
import numpy as np
import matplotlib
matplotlib.use('Agg')
from matplotlib.figure import Figure
from matplotlib.backends.backend_agg import FigureCanvasAgg as FigureCanvas
import matplotlib.cm as cm

def create_polar_rose(points, dir_key, mag_key, title):
    if not points: return None

    dirs = []
    mags = []
    for p in points:
        d = p.get(dir_key)
        m = p.get(mag_key)
        if d is not None and m is not None and str(d).lower() != 'nan' and str(m).lower() != 'nan':
            try:
                dirs.append(float(d))
                mags.append(float(m))
            except Exception:
                pass

    if not dirs: return None

    try:
        num_bins = 16
        bin_mags = np.zeros(num_bins)
        bin_counts = np.zeros(num_bins)

        for d, m in zip(dirs, mags):
            bin_idx = int(((d + 11.25) % 360) // 22.5)
            if bin_idx >= num_bins: bin_idx = 0
            bin_mags[bin_idx] += m
            bin_counts[bin_idx] += 1

        avg_mags = np.divide(bin_mags, bin_counts, out=np.zeros_like(bin_mags), where=bin_counts!=0)
        angles = np.deg2rad(np.arange(0, 360, 22.5))

        fig = Figure(figsize=(2.6, 2.6))
        canvas = FigureCanvas(fig)
        ax = fig.add_subplot(111, projection='polar')

        ax.set_theta_zero_location("N")
        ax.set_theta_direction(-1)
        ax.set_xticks(np.deg2rad(np.arange(0, 360, 45)))
        ax.set_xticklabels(['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'], color='black', fontsize=7, fontweight='bold')
        ax.tick_params(axis='y', colors='black', labelsize=6)

        max_val = np.max(avg_mags)
        if max_val > 0:
            ax.set_ylim(0, max_val * 1.15)
            norm = matplotlib.colors.Normalize(0, max_val)
            colors = cm.plasma_r(norm(avg_mags))
        else:
            colors = ['#3b82f6'] * num_bins

        ax.bar(angles, avg_mags, width=np.deg2rad(22.5), color=colors, edgecolor='black', linewidth=0.8, alpha=0.9)
        ax.set_title(title, color='black', fontsize=9, fontweight='bold', pad=10)

        buf = io.BytesIO()
        fig.savefig(buf, format='png', bbox_inches='tight', dpi=96, transparent=False)
        return base64.b64encode(buf.getvalue()).decode('utf-8')
    except Exception as e:
        print(f"Błąd podczas rysowania róży wiatrów: {e}")
        return None