(() => {
  const FRAME_COUNT = 240;
  const FRAME_ROOT = "assets/frames/frame_";
  const MAX_CACHE = 20;
  const AHEAD = 10;
  const BEHIND = 3;

  const section = document.querySelector(".scroll-story");
  const canvas = document.querySelector(".watch-canvas");
  const context = canvas.getContext("2d", { alpha: false, desynchronized: true });
  const progressBar = document.querySelector(".frame-progress");
  const readout = document.querySelector(".frame-readout");
  const status = document.querySelector("#frame-status");
  const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
  const images = new Map();
  const pending = new Map();

  let targetFrame = reducedMotion.matches ? FRAME_COUNT - 1 : 0;
  let lastDrawnFrame = -1;
  let direction = 1;
  let frameRequest = 0;

  const frameUrl = (index) => `${FRAME_ROOT}${String(index + 1).padStart(4, "0")}.webp`;
  const clamp = (value, low, high) => Math.min(high, Math.max(low, value));

  function resizeCanvas() {
    const rect = canvas.getBoundingClientRect();
    const dpr = Math.min(window.devicePixelRatio || 1, 1.75);
    const width = Math.max(1, Math.round(rect.width * dpr));
    const height = Math.max(1, Math.round(rect.height * dpr));
    if (canvas.width !== width || canvas.height !== height) {
      canvas.width = width;
      canvas.height = height;
      drawBestFrame();
    }
  }

  function loadFrame(index) {
    index = clamp(index, 0, FRAME_COUNT - 1);
    if (images.has(index)) {
      const cached = images.get(index);
      images.delete(index);
      images.set(index, cached);
      return Promise.resolve(cached);
    }
    if (pending.has(index)) return pending.get(index);

    const image = new Image();
    image.decoding = "async";
    const promise = new Promise((resolve) => {
      image.onload = () => {
        pending.delete(index);
        images.set(index, image);
        trimCache();
        if (index === targetFrame || lastDrawnFrame < 0) drawBestFrame();
        resolve(image);
      };
      image.onerror = () => {
        pending.delete(index);
        resolve(null);
      };
    });
    pending.set(index, promise);
    image.src = frameUrl(index);
    return promise;
  }

  function trimCache() {
    if (images.size <= MAX_CACHE) return;
    const protectedFrames = new Set([targetFrame, lastDrawnFrame]);
    const neighbors = [1, 2, 3].map((n) => targetFrame + n * direction);
    neighbors.forEach((n) => protectedFrames.add(clamp(n, 0, FRAME_COUNT - 1)));
    for (const [index, image] of images) {
      if (images.size <= MAX_CACHE) break;
      if (protectedFrames.has(index)) continue;
      images.delete(index);
      image.removeAttribute("src");
    }
  }

  function warmFrames(index) {
    loadFrame(index);
    for (let offset = 1; offset <= AHEAD; offset += 1) {
      loadFrame(index + offset * direction);
    }
    for (let offset = 1; offset <= BEHIND; offset += 1) {
      loadFrame(index - offset * direction);
    }
  }

  function paint(image) {
    if (!image || !image.naturalWidth) return;
    const canvasWidth = canvas.width;
    const canvasHeight = canvas.height;
    const scale = Math.max(canvasWidth / image.naturalWidth, canvasHeight / image.naturalHeight);
    const drawWidth = image.naturalWidth * scale;
    const drawHeight = image.naturalHeight * scale;
    const x = (canvasWidth - drawWidth) / 2;
    const y = (canvasHeight - drawHeight) / 2;
    context.clearRect(0, 0, canvasWidth, canvasHeight);
    context.drawImage(image, x, y, drawWidth, drawHeight);
  }

  function drawBestFrame() {
    let image = images.get(targetFrame);
    let drawnIndex = targetFrame;
    if (!image) {
      for (let distance = 1; distance <= 8 && !image; distance += 1) {
        const forward = clamp(targetFrame + direction * distance, 0, FRAME_COUNT - 1);
        const backward = clamp(targetFrame - direction * distance, 0, FRAME_COUNT - 1);
        if (images.has(forward)) { image = images.get(forward); drawnIndex = forward; }
        else if (images.has(backward)) { image = images.get(backward); drawnIndex = backward; }
      }
    }
    if (!image && images.has(lastDrawnFrame)) {
      image = images.get(lastDrawnFrame);
      drawnIndex = lastDrawnFrame;
    }
    if (!image) return;
    paint(image);
    lastDrawnFrame = drawnIndex;
  }

  function updateFromScroll() {
    frameRequest = 0;
    if (reducedMotion.matches) {
      targetFrame = FRAME_COUNT - 1;
      readout.textContent = String(FRAME_COUNT).padStart(3, "0");
      progressBar.style.width = "100%";
      warmFrames(targetFrame);
      drawBestFrame();
      return;
    }
    const range = Math.max(1, section.offsetHeight - window.innerHeight);
    const progress = clamp(-section.getBoundingClientRect().top / range, 0, 1);
    const nextFrame = Math.round(progress * (FRAME_COUNT - 1));
    if (nextFrame !== targetFrame) direction = nextFrame > targetFrame ? 1 : -1;
    targetFrame = nextFrame;
    progressBar.style.width = `${progress * 100}%`;
    readout.textContent = String(targetFrame + 1).padStart(3, "0");
    warmFrames(targetFrame);
    drawBestFrame();
  }

  function scheduleUpdate() {
    if (!frameRequest) frameRequest = window.requestAnimationFrame(updateFromScroll);
  }

  const menuToggle = document.querySelector(".menu-toggle");
  const mainNav = document.querySelector(".main-nav");
  menuToggle.addEventListener("click", () => {
    const open = menuToggle.getAttribute("aria-expanded") !== "true";
    menuToggle.setAttribute("aria-expanded", String(open));
    menuToggle.setAttribute("aria-label", open ? "Close navigation" : "Open navigation");
    mainNav.classList.toggle("is-open", open);
  });
  mainNav.addEventListener("click", (event) => {
    if (event.target.closest("a")) {
      mainNav.classList.remove("is-open");
      menuToggle.setAttribute("aria-expanded", "false");
      menuToggle.setAttribute("aria-label", "Open navigation");
    }
  });

  window.addEventListener("scroll", scheduleUpdate, { passive: true });
  window.addEventListener("resize", () => { resizeCanvas(); scheduleUpdate(); }, { passive: true });
  reducedMotion.addEventListener?.("change", scheduleUpdate);

  resizeCanvas();
  status.textContent = "Loading the watch animation.";
  warmFrames(targetFrame);
  loadFrame(reducedMotion.matches ? FRAME_COUNT - 1 : 0).then((image) => {
    if (image) {
      drawBestFrame();
      status.textContent = "Watch animation ready.";
    } else {
      status.textContent = "The watch animation could not be loaded.";
    }
  });
  scheduleUpdate();
})();
