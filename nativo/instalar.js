// Roda sozinho depois do "npx cap sync": copia widget, gato flutuante e chave para o projeto Android
const fs = require('fs'), path = require('path'), os = require('os');
const app = path.join(__dirname, '..', 'android', 'app');
if (!fs.existsSync(app)) { console.log('nativo: pasta android ainda não existe'); process.exit(0); }
fs.cpSync(path.join(__dirname, 'app'), app, { recursive: true });
const mf = path.join(app, 'src', 'main', 'AndroidManifest.xml');
let s = fs.readFileSync(mf, 'utf8');
if (!s.includes('GatoFlutuante')) {
  s = s.replace('<application', fs.readFileSync(path.join(__dirname, 'manifest-permissoes.xml'), 'utf8') + '\n    <application');
  s = s.replace('</application>', fs.readFileSync(path.join(__dirname, 'manifest-app.xml'), 'utf8') + '    </application>');
  fs.writeFileSync(mf, s);
}
// Mesma chave em todo build: atualizações instalam por cima
const ks = path.join(os.homedir(), '.android');
fs.mkdirSync(ks, { recursive: true });
fs.copyFileSync(path.join(__dirname, 'debug.keystore'), path.join(ks, 'debug.keystore'));
console.log('nativo: instalado');
