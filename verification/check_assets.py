from pathlib import Path
from PIL import Image, ImageDraw
import hashlib, json

root = Path(__file__).resolve().parents[1]
folders = sorted((root / 'app/src/main/assets/solphivia').iterdir())
rows, report = [], []
for folder in folders:
    if not folder.is_dir():
        continue
    frames = sorted(folder.glob('frame_*.png'))
    if not frames:
        continue
    row = Image.new('RGB', (1100, 155), '#ece7ef')
    ImageDraw.Draw(row).text((4, 3), folder.name, fill='black')
    hashes = []
    for i, path in enumerate(frames):
        with Image.open(path) as im:
            im.verify()
        with Image.open(path) as im:
            im.load()
            rgba = im.convert('RGBA')
            assert rgba.getchannel('A').getbbox(), f'Empty sprite: {path}'
            hashes.append(hashlib.sha256(rgba.tobytes()).hexdigest())
            rgba.thumbnail((125, 125))
            row.paste(rgba, (i*135+10, 22), rgba)
            ImageDraw.Draw(row).text((i*135+10, 140), path.stem, fill='black')
    report.append({'animation':folder.name, 'frames':len(frames), 'unique':len(set(hashes))})
    rows.append(row)
sheet=Image.new('RGB',(1100,155*len(rows)),'white')
for i,row in enumerate(rows): sheet.paste(row,(0,i*155))
sheet.save(root/'verification/all-animations.png')
(root/'verification/assets-report.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
print(json.dumps(report,indent=2))
