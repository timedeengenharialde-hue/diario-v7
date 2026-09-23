package br.lukas.diariosintomas;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

// Ponte entre a tela do app (JavaScript) e as partes nativas: widget e gato flutuante
@CapacitorPlugin(name = "GatoWidget")
public class GatoPlugin extends Plugin {

    @PluginMethod
    public void update(PluginCall call) {
        Context ctx = getContext();
        ctx.getSharedPreferences(GatoEstado.PREFS, Context.MODE_PRIVATE).edit()
            .putString("state", call.getString("state", "{}")).apply();
        GatoWidget.updateAll(ctx);
        call.resolve();
    }

    @PluginMethod
    public void flutuante(PluginCall call) {
        Context ctx = getContext();
        boolean ligar = call.getBoolean("ligar", true);
        JSObject r = new JSObject();
        if (ligar && !GatoFlutuante.temPermissao(ctx)) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + ctx.getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            r.put("permissao", false);
            r.put("ligado", false);
            call.resolve(r);
            return;
        }
        ctx.getSharedPreferences(GatoEstado.PREFS, Context.MODE_PRIVATE).edit().putBoolean("flutuante", ligar).apply();
        if (ligar) GatoFlutuante.ligar(ctx); else GatoFlutuante.desligar(ctx);
        r.put("permissao", true);
        r.put("ligado", ligar);
        call.resolve(r);
    }

    @PluginMethod
    public void status(PluginCall call) {
        Context ctx = getContext();
        JSObject r = new JSObject();
        r.put("ligado", ctx.getSharedPreferences(GatoEstado.PREFS, Context.MODE_PRIVATE).getBoolean("flutuante", false));
        r.put("permissao", GatoFlutuante.temPermissao(ctx));
        call.resolve(r);
    }
}
