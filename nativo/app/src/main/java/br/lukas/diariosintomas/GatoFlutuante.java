package br.lukas.diariosintomas;

import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

// Gato flutuante sobre os outros apps (tipo o botão virtual do iPhone): arrasta, gruda na borda, toque abre o diário
public class GatoFlutuante extends Service {
    private static GatoFlutuante instancia;
    private static final int TIPO_ESPECIAL = 0x40000000; // ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
    private static final int TIPO_OVERLAY = 2038;        // WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private FrameLayout raiz;
    private ImageView img;
    private WindowManager.LayoutParams lp;
    private String animAtual = "";

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            atualizar();
            handler.postDelayed(this, 60000);
        }
    };

    static boolean temPermissao(Context c) {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(c);
    }

    static boolean ligado() {
        return instancia != null;
    }

    static void ligar(Context c) {
        Intent i = new Intent(c, GatoFlutuante.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
        } catch (Exception e) {
            // o Android pode recusar se o app não estiver aberto; tenta de novo ao abrir o app
        }
    }

    static void desligar(Context c) {
        c.stopService(new Intent(c, GatoFlutuante.class));
    }

    static void atualizarSeLigado() {
        final GatoFlutuante g = instancia;
        if (g != null) g.handler.post(new Runnable() {
            @Override
            public void run() {
                g.atualizar();
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (!temPermissao(this) || !primeiroPlano()) {
            stopSelf();
            return;
        }
        instancia = this;
        criarBolha();
        handler.post(tick);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (raiz != null && wm != null) {
            try { wm.removeView(raiz); } catch (Exception e) { /* já removido */ }
        }
        raiz = null;
        instancia = null;
        super.onDestroy();
    }

    private boolean primeiroPlano() {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Notification.Builder b;
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ch = new NotificationChannel("gato_flutuante", "Gato flutuante", NotificationManager.IMPORTANCE_MIN);
                nm.createNotificationChannel(ch);
                b = new Notification.Builder(this, "gato_flutuante");
            } else {
                b = new Notification.Builder(this);
            }
            Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(this, 2, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification n = b.setContentTitle("Gato flutuante ligado")
                .setContentText("Toque para abrir o diário")
                .setSmallIcon(getApplicationInfo().icon)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
            if (Build.VERSION.SDK_INT >= 34) startForeground(7, n, TIPO_ESPECIAL); else startForeground(7, n);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void criarBolha() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        final float d = getResources().getDisplayMetrics().density;
        raiz = new FrameLayout(this);
        img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int w = (int) (96 * d), h = (int) (72 * d);
        raiz.addView(img, new FrameLayout.LayoutParams(w, h));
        raiz.setAlpha(0.95f);

        int tipo = Build.VERSION.SDK_INT >= 26 ? TIPO_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        lp = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, tipo,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = getSharedPreferences(GatoEstado.PREFS, MODE_PRIVATE).getInt("fx", 0);
        lp.y = getSharedPreferences(GatoEstado.PREFS, MODE_PRIVATE).getInt("fy", (int) (220 * d));

        raiz.setOnTouchListener(new View.OnTouchListener() {
            float sx, sy;
            int ox, oy;
            boolean moveu;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        sx = e.getRawX(); sy = e.getRawY(); ox = lp.x; oy = lp.y; moveu = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (e.getRawX() - sx), dy = (int) (e.getRawY() - sy);
                        if (Math.abs(dx) > 10 * d || Math.abs(dy) > 10 * d) moveu = true;
                        if (moveu) {
                            lp.x = ox + dx; lp.y = oy + dy;
                            try { wm.updateViewLayout(raiz, lp); } catch (Exception ex) { /* ignora */ }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (moveu) grudarNaBorda(); else abrirApp();
                        return true;
                    default:
                        return false;
                }
            }
        });
        wm.addView(raiz, lp);
    }

    private void grudarNaBorda() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int largura = raiz.getWidth(), altura = raiz.getHeight();
        int alvo = (lp.x + largura / 2) < dm.widthPixels / 2 ? 0 : dm.widthPixels - largura;
        lp.y = Math.max(0, Math.min(lp.y, dm.heightPixels - altura));
        ValueAnimator va = ValueAnimator.ofInt(lp.x, alvo);
        va.setDuration(200);
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                lp.x = (Integer) a.getAnimatedValue();
                if (raiz != null) {
                    try { wm.updateViewLayout(raiz, lp); } catch (Exception ex) { /* ignora */ }
                }
            }
        });
        va.start();
        getSharedPreferences(GatoEstado.PREFS, MODE_PRIVATE).edit().putInt("fx", alvo).putInt("fy", lp.y).apply();
    }

    private void abrirApp() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try { startActivity(i); } catch (Exception e) { /* ignora */ }
    }

    private void atualizar() {
        if (img == null) return;
        String mood = GatoEstado.calcular(this).mood;
        if (mood.equals(animAtual)) return;
        animAtual = mood;
        AnimationDrawable ad = new AnimationDrawable();
        ad.setOneShot(false);
        // Cada animação é uma folha 8 colunas x N linhas de quadros 160x120
        int id = getResources().getIdentifier("folha_" + mood, "drawable", getPackageName());
        if (id == 0) id = getResources().getIdentifier("folha_neutral", "drawable", getPackageName());
        if (id != 0) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inScaled = false;
            Bitmap folha = BitmapFactory.decodeResource(getResources(), id, o);
            if (folha != null) {
                int total = (folha.getHeight() / 120) * 8;
                for (int i = 0; i < total; i++) {
                    Bitmap q = Bitmap.createBitmap(folha, (i % 8) * 160, (i / 8) * 120, 160, 120);
                    ad.addFrame(new BitmapDrawable(getResources(), q), 125);
                }
            }
        }
        img.setImageDrawable(ad);
        ad.start();
    }
}
