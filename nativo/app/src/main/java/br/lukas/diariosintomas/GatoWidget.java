package br.lukas.diariosintomas;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

// Widget da tela inicial (imagem parada, porque widgets não animam)
public class GatoWidget extends AppWidgetProvider {
    static final String TICK = "br.lukas.diariosintomas.GATO_TICK";

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        updateAll(ctx);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        if (TICK.equals(intent.getAction())) updateAll(ctx);
    }

    static void updateAll(Context ctx) {
        GatoFlutuante.atualizarSeLigado();
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, GatoWidget.class));
        if (ids == null || ids.length == 0) return;
        GatoEstado g = GatoEstado.calcular(ctx);
        long now = System.currentTimeMillis();

        Intent open = new Intent(ctx, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        for (int id : ids) {
            RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.gato_widget);
            v.setImageViewResource(R.id.gato_img, g.imagemWidget());
            v.setTextViewText(R.id.gato_txt, g.texto);
            v.setOnClickPendingIntent(R.id.gato_root, pi);
            mgr.updateAppWidget(id, v);
        }

        Intent tick = new Intent(ctx, GatoWidget.class).setAction(TICK);
        PendingIntent tp = PendingIntent.getBroadcast(ctx, 1, tick, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.setAndAllowWhileIdle(AlarmManager.RTC, Math.max(g.proximo, now + GatoEstado.MIN), tp);
    }
}
