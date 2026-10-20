"""从固定 Nunito 源生成 NET. 圆润子集字体与透明图标；不裁剪参考图。"""
from pathlib import Path
import hashlib
import json
import sys

root = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(root / "target/font-tools"))
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools import subset
from PIL import Image, ImageDraw, ImageFont

folder = root / "app/src/resources/assets/skin/net"
folder.mkdir(parents=True, exist_ok=True)
font = TTFont(root / "vendor/net-font/Nunito-Variable.ttf", recalcTimestamp=False)
font = instantiateVariableFont(font, {"wght": 650}, inplace=True)
options = subset.Options()
options.recalc_timestamp = False
subsetter = subset.Subsetter(options=options)
subsetter.populate(unicodes=range(32, 0x250))
subsetter.subset(font)
for ident, text in {1: "NOPE NET Rounded", 2: "Regular", 3: "NOPE:NETRounded:1.0", 4: "NOPE NET Rounded Regular", 6: "NOPENETRounded-Regular"}.items():
    font["name"].setName(text, ident, 3, 1, 0x409)
font_path = folder / "net-rounded.ttf"
font.save(font_path)

ink = (48, 32, 111, 255)
size = 128
def icon(name, draw):
    image = Image.new("RGBA", (size, size))
    draw(ImageDraw.Draw(image))
    image.save(folder / (name + ".png"), optimize=True)

def path(draw, points, width=8):
    draw.line([(round(x * 4), round(y * 4)) for x, y in points], fill=ink, width=width, joint="curve")

def clipboard(d):
    path(d, [(12, 8), (8, 8), (8, 28), (25, 28), (25, 8), (21, 8)])
    path(d, [(13, 5), (20, 5), (20, 11), (13, 11), (13, 5)])
    path(d, [(12, 16), (20, 16)], 7);path(d, [(12, 21), (20, 21)], 7)

def emoji(d):
    path(d, [(12, 4), (21, 4), (27, 10), (27, 21), (21, 27), (11, 27), (5, 21), (5, 10), (12, 4)])
    for x in (12, 20):d.rounded_rectangle((x*4, 11*4, (x+2)*4, 14*4), radius=2, fill=ink)
    path(d, [(11, 19), (14, 22), (18, 22), (22, 18)], 8)

def settings(d):
    points=[(13,3),(19,3),(20,7),(23,8),(27,7),(30,12),(27,15),(27,18),(30,21),(27,26),(23,25),(20,27),(19,30),(13,30),(12,26),(9,25),(5,26),(2,21),(5,18),(5,15),(2,12),(5,7),(9,8),(12,7),(13,3)]
    # Inset by one pixel unit to keep the full outline away from the asset edge.
    path(d, [(2+x*.87, 1+y*.9) for x,y in points], 7)
    d.ellipse((49, 49, 80, 80), outline=ink, width=7)

def brand(d):
    n=ImageFont.truetype(str(font_path),80)
    ope=ImageFont.truetype(str(font_path),49)
    d.text((21, 3), "N", font=n, fill=ink, anchor="lt", stroke_width=0)
    d.text((21, 72), "ope", font=ope, fill=ink, anchor="lt", stroke_width=0)

for name,draw in (("brand",brand),("clipboard",clipboard),("emoji",emoji),("settings",settings)):
    icon(name, draw)
record={}
for path_ in sorted(folder.iterdir()):
    if path_.is_file() and path_.name!="assets.json":
        record[path_.name]={"bytes":path_.stat().st_size,"sha256":hashlib.sha256(path_.read_bytes()).hexdigest()}
(folder/"assets.json").write_text(json.dumps({"name":"NET.","font_license":"OFL-1.1; see licenses/NET-Nunito-OFL.txt","texture":"imagegen generated fixed pale lavender frosted satin surface; diffuse pink/periwinkle patches, fine even matte grain, no UI/text/frame","icons":"authored transparent raster resources; source in scripts/build_net_assets.py","files":record},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print("NET assets:",[(name,data["bytes"]) for name,data in record.items()])
