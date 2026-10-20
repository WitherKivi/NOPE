"""使用随包真实纹理、字体和图标展示 NET. 标准键位；不是 Android 截图。"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

root=Path(__file__).resolve().parents[1]
assets=root/"app/src/resources/assets/skin/net"
scale=3
image=Image.new("RGB",(1280,1130),"#f5f0fb")
draw=ImageDraw.Draw(image)
ui="C:/Windows/Fonts/msyh.ttc"
face=str(assets/"net-rounded.ttf")
frost=Image.open(assets/"frosted-background.png").convert("RGBA")

def text(x,y,value,size=26,fill="#30206f",font=face,anchor="mm"):
    draw.text((x,y),value,font=ImageFont.truetype(font,size),fill=fill,anchor=anchor)

def rounded(box,fill,radius=18,outline=None,width=1):
    draw.rounded_rectangle(tuple(round(v) for v in box),radius,fill=fill,outline=outline,width=width)

def material(box,color="#f0eafa",radius=21):
    x,y,r,b=map(round,box);w,h=r-x,b-y
    rounded((x,y+5,r,b+5),"#c5b2df",radius)
    base=Image.new("RGBA",(w,h),color)
    base=Image.blend(base,frost.resize((w,h),Image.Resampling.LANCZOS),.39)
    veil=Image.new("RGBA",(w,h));v=ImageDraw.Draw(veil)
    for yy in range(h):v.line((0,yy,w,yy),fill=(255,255,255,round(50*(1-yy/max(1,h)))))
    base=Image.alpha_composite(base,veil)
    mask=Image.new("L",(w,h));ImageDraw.Draw(mask).rounded_rectangle((0,0,w-1,h-1),radius,fill=255)
    image.paste(base,(x,y),mask)
    rounded((x,y,r,b),None,radius,"#9484d8",2)
    rounded((x+3,y+3,r-3,b-3),None,max(1,radius-3),"#fffaff",2)

def icon(name,cx,cy,size):
    source=Image.open(assets/(name+".png")).resize((size,size),Image.Resampling.LANCZOS)
    image.paste(source,(round(cx-size/2),round(cy-size/2)),source)

def line(points,width=6,fill="#30206f"):
    draw.line(points,fill=fill,width=width,joint="curve")

def key(x,y,w,h,label,hint="",special=False):
    material((x,y,x+w,y+h),"#e7daf7" if special else "#f0eafa")
    if label=="shift":
        cx=x+w/2;cy=y+h/2;line([(cx-23,cy+1),(cx,cy-24),(cx+23,cy+1),(cx+12,cy+1),(cx+12,cy+24),(cx-12,cy+24),(cx-12,cy+1),(cx-23,cy+1)])
    elif label=="delete":
        cx=x+w/2;cy=y+h/2;line([(cx-10,cy-20),(cx+26,cy-20),(cx+26,cy+20),(cx-10,cy+20),(cx-29,cy),(cx-10,cy-20)]);line([(cx-3,cy-9),(cx+14,cy+9)]);line([(cx+14,cy-9),(cx-3,cy+9)])
    elif label=="return":
        cx=x+w/2;cy=y+h/2;line([(cx+25,cy-18),(cx+25,cy+9),(cx-28,cy+9)]);line([(cx-12,cy-5),(cx-28,cy+9),(cx-12,cy+23)])
    elif label:
        text(x+w/2,y+h/2+(10 if hint else 0),label,68 if len(label)==1 else 44,font=ui if any(ord(c)>0x24f for c in label) else face)
    if hint:text(x+w/2,y+24,hint,27,fill="#6652a0")

text(50,35,"NET.",36,anchor="lm")
text(50,74,"真实资源与标准键位示意 · 非 Android 运行截图",22,font=ui,anchor="lm")
left=50;top=110;width=1179;height=840
image.paste(frost.resize((width,height),Image.Resampling.LANCZOS),(left,top))
rounded((left,top,left+width,top+height),None,15,"#c883ed",9)
rounded((left+7,top+7,left+width-7,top+height-7),None,12,"#fff7ff",3)
# Existing 48dp shortcut region; no additional decorative title bar.
material((left+9,top+7,left+width-9,top+139),"#e9e6f0",15)
for i,name in enumerate(("brand","clipboard","emoji","settings")):icon(name,left+(i+.5)*width/5,top+72,84 if i==0 else 72)
cx=left+4.5*width/5;line([(cx-18,top+63),(cx,top+81),(cx+18,top+63)])
rowheight=168
for row,letters in enumerate(("QWERTYUIOP","ASDFGHJKL","ZXCVBNM")):
    y=top+144+row*rowheight
    available=width-18;unit=available/10
    x=left+9+(unit/2 if row==1 else 0)
    if row==2:key(x+6,y+6,unit*1.5-12,rowheight-12,"shift",special=True);x+=unit*1.5
    for letter in letters:
        hint="1234567890"[letters.index(letter)] if row==0 else ""
        key(x+6,y+6,unit-12,rowheight-12,letter,hint);x+=unit
    if row==2:key(x+6,y+6,unit*1.5-12,rowheight-12,"delete",special=True)
y=top+144+rowheight*3;x=left+9;unit=(width-18)/9.5
for label,weight,special in (("123",1.5,True),("。",1,True),("",4.5,False),("中",1,True),("return",1.5,True)):
    key(x+6,y+6,unit*weight-12,rowheight-12,label,special=special);x+=unit*weight
text(50,990,"输入时，同一顶部区域显示候选；下方仅展示该状态。",22,font=ui,anchor="lm")
material((50,1020,1229,1110),"#eee6fa",15)
for x,value in ((140,"nihao"),(400,"你好"),(650,"你"),(870,"拟好")):text(x,1065,value,42,font=face if value=="nihao" else ui)
line([(1140,1056),(1158,1074),(1176,1056)])
out=root/"docs/net-material-preview.png"
image.save(out,optimize=True)
print(out)
