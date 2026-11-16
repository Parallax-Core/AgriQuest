# Art guidelines: the plant reference model

**The tomato plant art in `app/src/main/res/drawable/tomato_stages_<stage>.png` is the reference model for everything that shows the plant.** Use it for the plant's look at every stage, and as the model when making new clip art for cosmetics (outfits, accessories, effects).

| Stage image | Shown for (days since sowing) |
|---|---|
| `tomato_stages_seed.png` | 0–4: seed in the soil only (made from the germination image with the sprout removed) |
| `tomato_stages_germination.png` | 5–9: sprout with its first small leaves |
| `tomato_stages_seedling.png` | 10–27 |
| `tomato_stages_vegetative.png` | 28–55 |
| `tomato_stages_flowering.png` | 56–69 |
| `tomato_stages_fruit.png` | 70–104 (green fruit) |
| `tomato_stages_ripening.png` | 105+ (red tomatoes) |

The day ranges are in `TomatoLook.forDay` (`ui/components/GrowingTomatoPlant.kt`).

## Rules for new plant art and cosmetics

- **Start from the reference:** draw on top of, or next to, the matching `tomato_stages_*.png` image so size and position line up. Don't draw a plant from scratch, and don't switch to a vector or Canvas-drawn plant.
- **Same pixel scale:** the stage images are about 166×210 art pixels and are shown unscaled relative to each other. Draw cosmetics at that same pixel density; don't upscale small sprites. For example, a hat is about 64×64 art pixels, roughly 30% of the image height.
- **Same style:**
  - 1-pixel darker outline around shapes.
  - Flat colors with a lighter highlight band.
  - Transparent background (PNG).
  - No smoothing or anti-aliasing. The app draws them with `FilterQuality.None`.
- **Reuse the palette:** take colors from the stage art, e.g. leaf greens, the yellow of the flowers, the tomato red, and the soil browns.
- **Anchor points:** placement uses the stage images. The soil line is at 86.7% of the height, and the top of the plant per stage is `TomatoLook.topFraction`. Head items sit on the top of the plant, face items just below it, and neck items on the stem (`OutfitLayer` in `ui/components/OutfitArt.kt`).
- **Check every stage:** a cosmetic must look right on all stages, from the seed in the soil to the fully grown plant.
