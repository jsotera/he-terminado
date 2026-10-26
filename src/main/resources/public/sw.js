"use strict";

// No hay precaché, Cache API, modo offline ni cola de peticiones.
self.addEventListener("install", function (event) {
  event.waitUntil(self.skipWaiting());
});
self.addEventListener("activate", function (event) {
  event.waitUntil(self.clients.claim());
});
self.addEventListener("fetch", function (event) {
  event.respondWith(
    fetch(event.request, {
      cache: "no-store",
    }),
  );
});
