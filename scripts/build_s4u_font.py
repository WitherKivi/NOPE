"""生成S4U自有像素字体。仅重建字体需fontTools；Android构建直接使用已提交的TTF。"""
from pathlib import Path
import re,sys
root=Path(__file__).resolve().parents[1]
# Optional project-local build-only dependency, never enters APK/source deliveries.
if (root/"target/font-tools").exists():sys.path.insert(0,str(root/"target/font-tools"))
from fontTools.fontBuilder import FontBuilder
from fontTools.pens.ttGlyphPen import TTGlyphPen
source=(root/"app/src/skin/java/app/msime/offline/PixelGlyphs.java").read_text(encoding="utf8")
patterns=re.findall(r'"([01]{5}(?:/[01]{5}){6})"',source)
assert len(patterns)==36
letters="ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
rows=dict(zip(letters,patterns))
lower={
"a":"00000/00000/01110/00001/01111/10001/01111","b":"10000/10000/10110/11001/10001/10001/11110",
"c":"00000/00000/01111/10000/10000/10000/01111","d":"00001/00001/01101/10011/10001/10001/01111",
"e":"00000/00000/01110/10001/11111/10000/01111","f":"00110/01001/01000/11100/01000/01000/01000",
"g":"00000/01111/10001/10001/01111/00001/01110","h":"10000/10000/10110/11001/10001/10001/10001",
"i":"00100/00000/01100/00100/00100/00100/01110","j":"00010/00000/00110/00010/00010/10010/01100",
"k":"10000/10000/10010/10100/11000/10100/10010","l":"01100/00100/00100/00100/00100/00100/01110",
"m":"00000/00000/11010/10101/10101/10101/10101","n":"00000/00000/10110/11001/10001/10001/10001",
"o":"00000/00000/01110/10001/10001/10001/01110","p":"00000/11110/10001/10001/11110/10000/10000",
"q":"00000/01111/10001/10001/01111/00001/00001","r":"00000/00000/10110/11001/10000/10000/10000",
"s":"00000/00000/01111/10000/01110/00001/11110","t":"01000/01000/11100/01000/01000/01001/00110",
"u":"00000/00000/10001/10001/10001/10011/01101","v":"00000/00000/10001/10001/10001/01010/00100",
"w":"00000/00000/10001/10001/10101/10101/01010","x":"00000/00000/10001/01010/00100/01010/10001",
"y":"00000/10001/10001/10001/01111/00001/01110","z":"00000/00000/11111/00010/00100/01000/11111"}
rows.update(lower)
rows.update({"中":"0001000/1111111/1001001/1001001/1111111/0001000/0001000","日":"0111110/0100010/0100010/0111110/0100010/0100010/0111110"})
rows.update({".":"00000/00000/00000/00000/00000/00110/00110","-":"00000/00000/00000/11111/00000/00000/00000","'":"00100/00100/00000/00000/00000/00000/00000"})
fb=FontBuilder(1000,isTTF=True)
names=[".notdef","space"]+[f"uni{ord(c):04X}" for c in rows]
fb.setupGlyphOrder(names);glyphs={};metrics={}
for name in names:
    pen=TTGlyphPen(None)
    if name not in (".notdef","space"):
        char=chr(int(name[3:],16))
        for y,line in enumerate(rows[char].split("/")):
            for x,on in enumerate(line):
                if on=="1":
                    l=100+x*100;b=(6-y)*100
                    pen.moveTo((l,b));pen.lineTo((l+100,b));pen.lineTo((l+100,b+100));pen.lineTo((l,b+100));pen.closePath()
    glyphs[name]=pen.glyph();metrics[name]=((len(rows[char].split("/")[0])+2)*100 if name not in (".notdef","space") else 700,0)
fb.setupGlyf(glyphs);fb.setupHorizontalMetrics(metrics)
fb.setupCharacterMap({32:"space",**{ord(c):f"uni{ord(c):04X}" for c in rows}})
fb.setupHorizontalHeader(ascent=800,descent=-200)
fb.setupNameTable({"familyName":"S4U Pixel","styleName":"Regular","uniqueFontIdentifier":"NOPE:S4UPixel:1.0","fullName":"S4U Pixel Regular","psName":"S4UPixel-Regular","version":"Version1.0","copyright":"NOPE project authors; GPL-3.0; glyph source in build_s4u_font.py and PixelGlyphs.java"})
fb.setupOS2(sTypoAscender=800,sTypoDescender=-200,usWinAscent=800,usWinDescent=200)
fb.setupPost();fb.setupMaxp();fb.font["head"].created=fb.font["head"].modified=3864715200
out=root/"app/src/resources/assets/skin/s4u-keycaps.ttf";out.parent.mkdir(exist_ok=True)
fb.save(out);print(out.name,out.stat().st_size)
