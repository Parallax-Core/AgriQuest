# Tomato outfits: art for the team

The outfit shop (Awards tab) uses placeholder pixel art. To add the final art, replace these files and keep the same names:

| File (`app/src/main/res/drawable-nodpi/`) | Outfit | Slot | Price |
|---|---|---|---|
| `outfit_straw_hat.png` | Straw Hat | Head | 60 |
| `outfit_cap.png` | Garden Cap | Head | 40 |
| `outfit_flower_crown.png` | Flower Crown | Head | 90 |
| `outfit_crown.png` | Harvest Crown | Head | 200 |
| `outfit_sunglasses.png` | Sunglasses | Face | 50 |
| `outfit_scarf.png` | Cozy Scarf | Neck | 70 |

- **Image format:** square PNG with a transparent background. 64×64 or larger works; use nearest-neighbour scaling for pixel art.
- **Placement:** on the plant, each outfit is drawn at 30% of the plant's width. Head items sit on the top of the plant, face items just below it, and neck items on the stem.
- **Adding an outfit:** add a row to `Outfit.CATALOG` (`domain/model/Outfit.kt`) and its image to `outfitDrawable` (`ui/components/OutfitArt.kt`).
- **Points:** points are all XP ever earned minus points spent. Spending doesn't lower XP or level. The wardrobe is saved on the phone only, because the cloud progress table has no field for it yet.
- **Placeholders:** regenerate them with `python3 scripts/generate-outfit-placeholders.py`.
