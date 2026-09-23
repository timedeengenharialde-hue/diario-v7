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
            JSONObject nomes = s.optJSONObject("nomes");
            String pend = null;
            JSONArray chaves = lemb == null ? null : lemb.names();
            for (int m = 0; chaves != null && m < chaves.length(); m++) {
                String med = chaves.optString(m);
                JSONArray hs = lemb.optJSONArray(med);
                long ld = last == null ? 0 : last.optLong(med, 0);
                String nome = nomes == null ? med : nomes.optString(med, med);
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
                        if (now >= r && now < r + 3 * H && ld < r - 90 * MIN && pend == null) pend = nome;
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

    int imagemWidget(Context ctx) {
        int id = ctx.getResources().getIdentifier("gato_" + mood, "drawable", ctx.getPackageName());
        return id != 0 ? id : ctx.getResources().getIdentifier("gato_neutral", "drawable", ctx.getPackageName());
    }
}
