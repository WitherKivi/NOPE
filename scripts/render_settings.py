"""按设置页实际色值和文案生成材质示意，不是 Android 运行截图。"""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import random
root=Path(__file__).resolve().parents[1]
image=Image.new('RGB',(1280,1180),'#f6f5f8');draw=ImageDraw.Draw(image);font='C:/Windows/Fonts/msyh.ttc'
def text(x,y,value,size,color,anchor='lt'):
    draw.text((x,y),value,font=ImageFont.truetype(font,size),fill=color,anchor=anchor)
text(40,20,'NOPE 设置 · 明暗雾面',30,'#28252f')
text(40,68,'实际色值与功能文案的静态示意；非 Android 运行截图。',20,'#69616f')
for night,left in ((False,40),(True,660)):
    top=120;w=580;h=1010;panel=Image.new('RGB',(w,h));pd=ImageDraw.Draw(panel)
    a,b=((55,55,62),(37,37,44)) if night else ((250,249,245),(227,227,233))
    for y in range(h):pd.line((0,y,w,y),fill=tuple(round(v*(1-y/h)+z*y/h) for v,z in zip(a,b)))
    random.seed(41)
    for _ in range(10000):
        x=random.randrange(w);y=random.randrange(h);color=panel.getpixel((x,y));step=random.choice((-2,-1,1,2));pd.point((x,y),fill=tuple(max(0,min(255,c+step)) for c in color))
    ink='#f3f1f6' if night else '#26242b';muted='#c1bbc9' if night else '#68616f';fill='#424249' if night else '#f2f2f3'
    for offset in range(-100,h,250):
        points=[]
        for i in range(100):
            t=i/99;x=-50*(1-t)**3+3*(1-t)**2*t*w*.42+3*(1-t)*t*t*w*.58+t**3*(w+50)
            y=offset+3*(1-t)**2*t*(-135)+3*(1-t)*t*t*270+t**3*105;points.append((x,y))
        pd.line(points,fill='#49454f' if night else '#d8d5df',width=1)
    image.paste(panel,(left,top))
    def label(y,value,size=23,color=ink):text(left+30,top+y,value,size,color)
    def button(y,value):
        draw.rounded_rectangle((left+28,top+y,left+w-28,top+y+78),radius=30,fill=fill,outline='#66636c' if night else '#ffffff',width=2)
        text(left+w/2,top+y+39,value,24,ink,'mm')
    label(22,'NOPE 输入法',38);label(86,'中文 · 日本語 · English · Code',23,muted);label(128,'本地中日文词库已就绪',20,muted)
    button(180,'启用输入法');button(270,'选择输入法');label(385,'输入语言',27)
    button(435,'选择默认输入语言');label(535,'快速切换 · 中文 / English',23)
    button(590,'调整快速切换语言');button(680,'恢复中英快速切换');label(785,'长按空格选择全部键盘。',21,muted);label(875,'键盘高度',27);label(930,'默认 · 283 dp',23)
    text(left+12,1144,'夜间 · 灰色雾面' if night else '日间 · 乳白雾面',20,'#69616f')
out=root/'docs/settings-material-preview.png';image.save(out,optimize=True);print(out)
