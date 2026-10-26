"use strict";

// No hay precaché, Cache API, modo offline ni cola de peticiones.
self.addEventListener("install", (event) => {
  event.waitUntil(self.skipWaiting());
});
self.addEventListener("activate", (event) => {
  event.waitUntil(self.clients.claim());
});
self.addEventListener("fetch", (event) => {
  event.respondWith(fetch(event.request, { cache: "no-store" }));
});
