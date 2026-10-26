"use strict";

const installButton = document.getElementById("install-app");
const installHelp = document.getElementById("install-help");
const standalone = window.matchMedia("(display-mode: standalone)");
let installPrompt;

function showInstallHelp() {
  if (!installHelp) return;
  installHelp.hidden =
    standalone.matches || window.navigator.standalone === true;
  if (!window.isSecureContext) {
    installHelp.textContent =
      "Para instalar la app, accede a la dirección HTTPS del aula.";
  } else if (/iPad|iPhone|iPod/.test(navigator.userAgent)) {
    installHelp.textContent =
      "Para instalar: Compartir → Añadir a pantalla de inicio.";
  } else {
    installHelp.textContent =
      "Puedes instalar la app desde el menú del navegador. Necesita conexión con el aula.";
  }
}

window.addEventListener("beforeinstallprompt", (event) => {
  event.preventDefault();
  installPrompt = event;
  installButton.hidden = standalone.matches;
});
installButton.addEventListener("click", async () => {
  if (!installPrompt) return;
  installButton.disabled = true;
  try {
    await installPrompt.prompt();
    await installPrompt.userChoice;
  } catch (error) {
    if (installHelp) {
      installHelp.textContent =
        "Puedes instalar la app desde el menú del navegador.";
    }
  } finally {
    installPrompt = null;
    installButton.hidden = true;
    installButton.disabled = false;
  }
});
window.addEventListener("appinstalled", () => {
  installPrompt = null;
  installButton.hidden = true;
  if (installHelp) installHelp.hidden = true;
});
standalone.addEventListener("change", showInstallHelp);
showInstallHelp();

if (window.isSecureContext && "serviceWorker" in navigator) {
  navigator.serviceWorker
    .register("/sw.js", { updateViaCache: "none" })
    .catch(() => {
      if (installHelp)
        installHelp.textContent =
          "No se pudo preparar la instalación. Recarga la página con conexión al aula.";
    });
}
