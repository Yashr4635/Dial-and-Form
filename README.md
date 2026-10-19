# Sky-Dweller — Time in Motion

A responsive, single-page watch showcase. The hero uses the 240 supplied frames as a scroll-controlled canvas sequence. The page is dependency-free and can be opened directly or served from any static web server.

## Run locally

From this folder, start a static server, for example:

```sh
python3 -m http.server 8000
```

Then open `http://localhost:8000`.

The original 1280 × 720 PNGs are encoded as WebP in `assets/frames/` to reduce the page payload while preserving the supplied sequence. Scroll through the full-height hero to move from the assembled watch, through its exploded view, and back to the finished piece.
