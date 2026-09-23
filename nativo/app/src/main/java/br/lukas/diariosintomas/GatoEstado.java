package br.lukas.diariosintomas;

import android.content.Context;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONObject;

// Decide a cara do gato: remédio pendente tem prioridade; senão, humor do último registro (até 12 h)
final class GatoEstado {
    static final String PREFS = "gato_widget";
    static final long MIN = 60000L, H = 3600000L;

    String mood = "neutral";
    String texto = "Como você está?";
    long proximo;

    static GatoEstado calcular(Context ctx) {
        GatoEstado g = new GatoEstado();
        long now = System.currentTimeMillis();
        g.proximo = now + 30 * MIN;
        try {
            JSONObject s = new JSONObject(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("state", "{}"));
            JSONObject lemb = s.optJSONObject("lemb");
            JSONObject last = s.optJSONObject("last");
            String[] meds = {"rit", "esc"};
            String[] nomes = {"Ritalina", "Escitalopram"};
            String pend = null;
            for (int m = 0; m < 2; m++) {
                JSONArray hs = lemb == null ? null : lemb.optJSONArray(meds[m]);
                long ld = last == null ? 0 : last.optLong(meds[m], 0);
                if (hs == null) continue;
                for (int i = 0; i < hs.length(); i++) {
                    String[] p = hs.optString(i, "").split(":");
                    if (p.length < 2) continue;
                    int hh = Integer.parseInt(p[0].trim()), mm = Integer.parseInt(p[1].trim());
                    for (int d = -1; d <= 1; d++) {
                        Calendar c = Calendar.getInstance();
                        c.setTimeInMillis(now);
                        c.add(Calendar.DAY_OF_YEAR, d);
                        c.set(Calendar.HOUR_OF_DAY, hh);
                        c.set(Calendar.MINUTE, mm);
                        c.set(Calendar.SECOND, 0);
                        c.set(Calendar.MILLISECOND, 0);
                        long r = c.getTimeInMillis();
                        if (now >= r && now < r + 3 * H && ld < r - 90 * MIN && pend == null) pend = nomes[m];
                        if (r > now) g.proximo = Math.min(g.proximo, r);
                        if (now >= r && r + 3 * H > now) g.proximo = Math.min(g.proximo, r + 3 * H);
                    }
                }
            }
            if (pend != null) {
                g.mood = "remedio";
                g.texto = "Hora de tomar " + pend + "!";
            } else {
                String m = s.optString("mood", "neutral");
                long t = s.optLong("moodT", 0);
                if (!"neutral".equals(m) && now - t < 12 * H) {
                    g.mood = m;
                    g.texto = s.optString("frase", "");
                    g.proximo = Math.min(g.proximo, t + 12 * H);
                }
            }
        } catch (Exception e) {
            // estado inválido: gato neutro
        }
        return g;
    }

    int imagemWidget() {
        switch (mood) {
            case "happy": return R.drawable.gato_happy;
            case "alert": return R.drawable.gato_alert;
            case "angry": return R.drawable.gato_angry;
            case "sleep": return R.drawable.gato_sleep;
            case "sad": return R.drawable.gato_sad;
            case "remedio": return R.drawable.gato_remedio;
            default: return R.drawable.gato_neutral;
        }
    }
}
