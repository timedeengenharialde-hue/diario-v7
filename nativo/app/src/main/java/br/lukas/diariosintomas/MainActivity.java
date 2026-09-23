package br.lukas.diariosintomas;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(GatoPlugin.class);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Religa o gato flutuante se ele estava ligado (ex.: depois de reiniciar o celular)
        if (getSharedPreferences(GatoEstado.PREFS, MODE_PRIVATE).getBoolean("flutuante", false)
                && GatoFlutuante.temPermissao(this) && !GatoFlutuante.ligado()) {
            GatoFlutuante.ligar(this);
        }
    }
}
