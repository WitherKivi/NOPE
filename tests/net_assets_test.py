"""NET. 随包资源、字体覆盖与可追溯许可；无需图像或字体第三方模块。"""
from pathlib import Path
import hashlib
import json
import struct

root=Path(__file__).resolve().parents[1]
folder=root/"app/src/resources/assets/skin/net"
record=json.loads((folder/"assets.json").read_text(encoding="utf-8"))
assert set(record["files"])=={"brand.png","clipboard.png","emoji.png","settings.png","frosted-background.png","net-rounded.ttf"}
for name,info in record["files"].items():
    value=(folder/name).read_bytes()
    assert len(value)==info["bytes"] and hashlib.sha256(value).hexdigest()==info["sha256"],name
    if name.endswith(".png"):
        assert value[:8]==b"\x89PNG\r\n\x1a\n"
        w,h,depth,color=struct.unpack_from(">IIBB",value,16)
        if name!="frosted-background.png":assert (w,h,depth,color)==(128,128,8,6),name
        else:assert w>=1000 and h>=400
source=json.loads((root/"vendor/net-font/provenance.json").read_text(encoding="utf-8"))
assert len(source["commit"])==40 and source["license"]=="OFL-1.1"
for path,info in source["files"].items():
    value=(root/path).read_bytes();assert len(value)==info["bytes"] and hashlib.sha256(value).hexdigest()==info["sha256"]
assert "SIL OPEN FONT LICENSE" in (root/"app/src/resources/assets/licenses/NET-Nunito-OFL.txt").read_text(encoding="utf-8")
f=(folder/"net-rounded.ttf").read_bytes()
u16=lambda p:struct.unpack_from(">H",f,p)[0]
u32=lambda p:struct.unpack_from(">I",f,p)[0]
s16=lambda p:struct.unpack_from(">h",f,p)[0]
tables={f[12+i*16:16+i*16].decode():u32(20+i*16) for i in range(u16(4))}
assert "fvar" not in tables # Static font runs on the full supported Android range.
cmap=tables["cmap"];sub=None
for i in range(u16(cmap+2)):
    p=cmap+u32(cmap+8+i*8)
    if u16(p)==4:sub=p;break
assert sub is not None
count=u16(sub+6)//2;end=sub+14;start=end+2*count+2;delta=start+2*count;offset=delta+2*count
for char in "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789":
    code=ord(char);gid=0
    for i in range(count):
        if u16(start+2*i)<=code<=u16(end+2*i):
            off=u16(offset+2*i)
            gid=(code+s16(delta+2*i))&65535 if not off else u16(offset+2*i+off+2*(code-u16(start+2*i)))
            if off and gid:gid=(gid+s16(delta+2*i))&65535
            break
    assert gid>0,char
    loca=tables["loca"]
    a,b=(u32(loca+gid*4),u32(loca+(gid+1)*4)) if s16(tables["head"]+50) else (u16(loca+gid*2)*2,u16(loca+(gid+1)*2)*2)
    assert b>a,char+" missing outline"
print("NetAssets: frosted texture, transparent PNG icons, static rounded ASCII font and pinned OFL source PASS")
