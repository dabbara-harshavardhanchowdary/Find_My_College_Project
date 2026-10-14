self.addEventListener('install', () => {
    self.skipWaiting();
});

self.addEventListener('activate', () => {
    console.log('PWA Service Worker Active');
});

self.addEventListener('fetch', (e) => {
    e.respondWith(fetch(e.request).catch(() => caches.match(e.request)));
});