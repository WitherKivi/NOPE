"""分层与区域契约、S4U真实字体表；不以静态图冒充Android验收。"""
from pathlib import Path
import json,struct
root=Path(__file__).resolve().parents[1]
folder=root/"app/src/resources/assets/skins"
ids={"system","cloud","mono","metal","line","color","ios","retro","lavender"}
assert {p.stem for p in folder.glob("*.json") if p.stem!="schema-v1"}==ids
assert set(json.loads((folder/"schema-v1.json").read_text(encoding="utf8"))["properties"]["id"]["enum"])==ids
for id in ids:
    d=json.loads((folder/(id+".json")).read_text(encoding="utf8"));assert d["version"]==1 and d["id"]==id
    assert len({l["id"] for l in d["layers"]})==len(d["layers"])<=64
    for l in d["layers"]:assert l["z"] in (5,30) and l["type"] in {"rect","text","dots","rivet"}
    for v in d["variants"].values():
        assert set(v["palette"])=={"background","foreground","key","special","accent","selectedInk","shadow","outline"}
        assert all(len(c)==7 and c.startswith("#") for c in v["palette"].values())
    assert d["perspective"]["target"]=="background"
    assert d["perspective"]["rotationX"]==(7 if id=="retro" else 0)
retro=json.loads((folder/"retro.json").read_text(encoding="utf8"))
assert {l["z"] for l in retro["layers"]}=={5,30}
# Read the submitted TTF's cmap+loca+glyf, without requiring fontTools for normal testing.
f=(root/"app/src/resources/assets/skin/s4u-keycaps.ttf").read_bytes()
u16=lambda p:struct.unpack_from(">H",f,p)[0]
s16=lambda p:struct.unpack_from(">h",f,p)[0]
u32=lambda p:struct.unpack_from(">I",f,p)[0]
tables={f[12+i*16:16+i*16].decode(): (u32(20+i*16),u32(24+i*16)) for i in range(u16(4))}
cmap=tables["cmap"][0];sub=None
for i in range(u16(cmap+2)):
    p=cmap+u32(cmap+8+i*8)
    if u16(p)==4:sub=p;break
assert sub is not None
count=u16(sub+6)//2;end=sub+14;start=end+2*count+2;delta=start+2*count;offset=delta+2*count
format=s16(tables["head"][0]+50)
for char in "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789中日":
    code=ord(char);gid=0
    for i in range(count):
        if u16(start+2*i)<=code<=u16(end+2*i):
            o=u16(offset+2*i)
            gid=(code+s16(delta+2*i))&65535 if o==0 else u16(offset+2*i+o+2*(code-u16(start+2*i)))
            if o and gid:gid=(gid+s16(delta+2*i))&65535
            break
    assert gid>0,char
    loca=tables["loca"][0]
    a,b=(u32(loca+gid*4),u32(loca+(gid+1)*4)) if format else (u16(loca+gid*2)*2,u16(loca+(gid+1)*2)*2)
    assert b>a and s16(tables["glyf"][0]+a)>0,char+" has no contours"
assert len(f)<20000
print("SkinContract: all skins/layers/roles, background-only perspective, real ASCII+CJK TTF glyph contours PASS")
