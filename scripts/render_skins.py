"""Native palette/layout schematic for comparing skins, explicitly not an Android screenshot."""
from pathlib import Path
from project_paths import java_source
from PIL import Image, ImageDraw, ImageFont
import subprocess,json,tempfile,os
root=Path(__file__).resolve().parents[1]
java=Path(os.environ['JAVA_HOME'])/'bin'
with tempfile.TemporaryDirectory(dir=root/'target') as tmp:
    subprocess.run([str(java/'javac.exe'),'--release','17','-encoding','UTF-8','-d',tmp,str(java_source('app/msime/offline/SkinPalette.java')),str(root/'tests/SkinTokens.java')],check=True)
    palettes=json.loads(subprocess.check_output([str(java/'java.exe'),'-Dfile.encoding=UTF-8','-Dstdout.encoding=UTF-8','-cp',tmp,'app.msime.offline.SkinTokens'],encoding='UTF-8'))
for palette in palettes:
    config=json.loads((root/'app/src/resources/assets/skins'/f"{palette['id']}.json").read_text(encoding='utf8'))
    variant=config['variants'].get('dark' if palette['dark'] else 'light',{})
    for role,value in variant.get('palette',{}).items():palette[role]=0xff000000|int(value.lstrip('#'),16)
    palette['radius']=variant.get('radiusDp',palette['radius']);palette['depth']=variant.get('depthDp',palette['depth'])
(root/'docs/skin-tokens.json').write_text(json.dumps(palettes,ensure_ascii=False,indent=2),encoding='utf-8')
scale=2
height=120+((len(palettes)+2)//2)*330
img=Image.new('RGB',(880*scale,height*scale),'#fafafa');d=ImageDraw.Draw(img)
font='C:/Windows/Fonts/msyh.ttc'
def rgb(c):return tuple((c>>s)&255 for s in [16,8,0])
def blend(a,b,t):return tuple(round(x*(1-t)+y*t) for x,y in zip(rgb(a),rgb(b)))
def rect(box,color,radius=0,outline=None,width=1):
    d.rounded_rectangle(tuple(round(v*scale) for v in box),radius=radius*scale,fill=color,outline=outline,width=round(width*scale))
def text(x,y,t,size,color,anchor='mm',pixel=False):
    d.text((x*scale,y*scale),t,font=ImageFont.truetype(str(root/'app/src/resources/assets/skin/s4u-keycaps.ttf') if pixel else font,round(size*scale)),fill=color,anchor=anchor)
def glyph(x,y,kind,color):
    def path(points):d.line([((x+a)*scale,(y+b)*scale) for a,b in points],fill=color,width=3,joint='curve')
    if kind=='shift':path([(-9,1),(0,-9),(9,1),(5,1),(5,9),(-5,9),(-5,1),(-9,1)])
    elif kind=='delete':
        path([(-4,-7),(10,-7),(10,7),(-4,7),(-10,0),(-4,-7)]);path([(0,-3),(6,3)]);path([(6,-3),(0,3)])
    elif kind=='enter':path([(9,-6),(9,3),(-8,3)]);path([(-3,-2),(-8,3),(-3,8)])
    elif kind=='down':path([(-7,-3),(0,4),(7,-3)])
    elif kind=='brand':
        def mark(points,width):d.line([((x+a-12)*scale,(y+b-12)*scale) for a,b in points],fill=color,width=round(width*scale))
        mark([(7,12),(7,2),(17,12),(17,2)],2)
        mark([(2.5,17),(7.5,17),(7.5,22),(2.5,22),(2.5,17)],1.35)
        mark([(10,23),(10,17),(14.5,17),(14.5,20.5),(10,20.5)],1.35)
        mark([(22,22),(17,22),(17,17),(22,17),(22,19.5),(17,19.5)],1.35)
    elif kind=='record':
        rect((x-11,y-11,x+11,y+11),'#eee2c9',1,'#705a43',1)
        d.ellipse(((x-9)*scale,(y-8)*scale,(x+5)*scale,(y+6)*scale),fill='#b0b6ad')
        d.pieslice(((x-9)*scale,(y-8)*scale,(x+5)*scale,(y+6)*scale),210,290,fill='#d9ddd0')
        d.pieslice(((x-9)*scale,(y-8)*scale,(x+5)*scale,(y+6)*scale),40,105,fill='#b0c7c9')
        d.ellipse(((x-4)*scale,(y-3)*scale,x*scale,(y+1)*scale),fill='#eee2c9')
        rect((x+1,y-4,x+10,y+4),'#eee2c9');rect((x+2,y-3,x+9,y+2),'#35483d')
        for xx in [-7,-3,1,5]:rect((x+xx,y+8,x+xx+2,y+9),'#998b76')
    elif kind=='clip':
        path([(-6,-6),(-8,-6),(-8,10),(8,10),(8,-6),(6,-6)]);path([(-4,-9),(4,-9),(4,-4),(-4,-4),(-4,-9)]);path([(-4,1),(4,1)]);path([(-4,5),(4,5)])
    elif kind=='emoji':
        d.ellipse(((x-10)*scale,(y-10)*scale,(x+10)*scale,(y+10)*scale),outline=color,width=3)
        for a in [-3,3]:rect((x+a-1,y-4,x+a+1,y-2),color,1)
        path([(-4,3),(-2,5),(2,5),(4,3)])
    elif kind=='settings':
        import math
        path([(math.cos(i*math.pi/8)*(10 if i%2==0 else 7),math.sin(i*math.pi/8)*(10 if i%2==0 else 7)) for i in range(17)])
        d.ellipse(((x-3)*scale,(y-3)*scale,(x+3)*scale,(y+3)*scale),outline=color,width=3)
def cap(p,x,y,w,h,label,role=False,sheen=False):
    color=p['accent'] if role=='lang' else p['special'] if role else p['key']
    ink=p['selectedInk'] if role=='lang' else p['functionInk'] if role else p['keyInk']
    if p['id']=='retro' and role=='lang':color=p['special'];ink=p['foreground']
    rad=0 if p['id']=='retro' else p['radius'];depth=p['depth']
    rect((x+1,y+depth+1,x+w-1,y+h-1),rgb(p['shadow']),rad)
    box=(x+1,y+1,x+w-1-depth/2,y+h-1-depth)
    rect(box,rgb(color),rad)
    if p['id'] in ['system','cloud','metal','lavender']:
        mask=Image.new('L',img.size);md=ImageDraw.Draw(mask)
        md.rounded_rectangle(tuple(round(v*scale) for v in box),radius=rad*scale,fill=255)
        gradient=Image.new('RGB',img.size);gd=ImageDraw.Draw(gradient)
        for row in range(round(y*scale),round((y+h)*scale)):
            t=(row-y*scale)/(h*scale)
            c=blend(color,0xff000000 if t>.7 else 0xffffffff,.09*abs(t-.7))
            if sheen:c=tuple(round(a*.93+b*.07) for a,b in zip(c,(229,143,194)))
            gd.line((x*scale,row,(x+w)*scale,row),fill=c)
        img.paste(gradient,(0,0),mask)
    if p['id']!='ios':rect(box,None,rad,rgb(p['outline']),1.2 if p['id']=='line' else .7)
    if p['id']=='retro':
        outline=rgb(p['outline']);face=rgb(color);shadow=rgb(p['shadow'])
        rect((x+1,y+1,x+w-1,y+h-1),outline,2)
        rect((x+2,y+2,x+w-2,y+h-2),shadow,2)
        left=x+4;right=x+w-4;top=y+3;bottom=y+h-8
        d.polygon([(v*scale,k*scale) for v,k in [(x+2,y+5),(left,top),(left,bottom),(x+2,y+h-3)]],fill='#fff1d9')
        d.polygon([(v*scale,k*scale) for v,k in [(left,bottom),(right,bottom),(x+w-2,y+h-3),(x+2,y+h-3)]],fill='#b5a184')
        rect((left,top,right,bottom),face,1)
        d.line((left*scale,top*scale,right*scale,top*scale),fill='#fff5df',width=scale)
        d.line((left*scale,(top+2)*scale,left*scale,(bottom-1)*scale),fill='#fff5df',width=scale)
    size=24 if len(label)==1 else 15
    icons={'⇧':'shift','⌫':'delete','↵':'enter'}
    if label in icons:glyph(x+w/2,y+h/2,icons[label],rgb(ink))
    else:text(x+w/2,y+h/2-2 if p['id']=='retro' else y+h/2,label,size,rgb(ink),pixel=p['id'] in ['retro','lavender'] and label not in ['。','、'])
    if len(label)==1 and label in 'QWERTYUIOP':text(x+w-6,y+11,'1234567890'['QWERTYUIOP'.index(label)],8,rgb(ink))
text(34,24,'NOPE 输入法 · 皮肤构成预览',24,'#202124','lm')
text(34,57,'设计示意：使用代码中的实际颜色与键位；不是 Android 运行截图。',13,'#555','lm')
cards=palettes+[dict(next(p for p in palettes if p['id']=='retro'),name='S4U · 快捷栏',preview='idle')]
for i,p in enumerate(cards):
    x=30+(i%2)*430;y=90+(i//2)*330;w=393;h=282
    text(x,y,p['name']+(' · 深色' if p['dark'] else ' · 浅色' if p['id'] in ['system','color','ios'] else ''),17,'#202124','lm')
    y+=24;rect((x,y,x+w,y+h),rgb(p['background']),8)
    if p['id']=='retro':
        # Only the cabinet silhouette narrows at the top. Key/candidate coordinates stay unchanged.
        d.polygon([((x+11)*scale,y*scale),((x+w-11)*scale,y*scale),((x+w)*scale,(y+h)*scale),(x*scale,(y+h)*scale)],fill=rgb(p['background']),outline=rgb(p['outline']),width=scale)

    if p['id']=='lavender':
        rect((x,y,x+w,y+h),None,1,'#c184e9',2)
        rect((x+3,y+3,x+w-3,y+43),'#e9e6f0',0,'#5142ce',1)
    if p['id']=='metal':
        for row in range(y,y+h,2):d.line((x*scale,row*scale,(x+w)*scale,row*scale),fill='#aeb4b9')
    for j,label in enumerate(['brand','clip','emoji','settings','down'] if p['id']!='retro' else []):
        if j==0:glyph(x+(j+.5)*w/5,y+23,'record' if p['id']=='retro' else label,rgb(p['accent']))
        else:glyph(x+(j+.5)*w/5,y+23,label,rgb(p['foreground']))
    if p['id']=='retro':
        glyph(x+23,y+23,'record',rgb(p['foreground']))
        rect((x+45,y+5,x+w-47,y+42),'#705a43',0)
        rect((x+48,y+8,x+w-50,y+39),'#304237',0)
        if p.get('preview')=='idle':
            for j,kind in enumerate(['clip','emoji','settings']):glyph(x+90+j*95,y+24,kind,'#dde8b9')
        else:
            text(x+62,y+24,'nihao',17,'#dde8b9','lm',pixel=True)
            text(x+158,y+24,'你好   你   拟好',17,'#dde8b9','lm')
        glyph(x+w-23,y+23,'down',rgb(p['foreground']))
    for r,letters in enumerate(['QWERTYUIOP' ,'ASDFGHJKL','ZXCVBNM']):
        yy=y+48+r*55
        if p['id']=='color':rect((x+3+(w*.05 if r==1 else 0),yy+2,x+w-3-(w*.05 if r==1 else 0),yy+53),'#080808',18)
        if r==1:xx=x+w*.05;step=w/10
        else:xx=x+3;step=(w-6)/10
        if r==2:
            cap(p,xx,yy,step*1.5-4,52,'⇧',True);xx+=step*1.5
        for letter in letters:cap(p,xx+2,yy,step-4,52,letter,False,p['id']=='system' and letter in 'QWERT');xx+=step
        if r==2:cap(p,xx+2,yy,step*1.5-4,52,'⌫',True)
    yy=y+213;xx=x+3;step=(w-6)/9.5
    for label,weight,role in [('123',1.5,True),('。',1,True),('',4.5,False),('中',1,'lang'),('↵',1.5,True)]:
        cap(p,xx+2,yy,step*weight-4,54,label,role);xx+=step*weight
out=root/'docs/nope-skins-preview.png';img.resize((880,height),Image.Resampling.LANCZOS).save(out)
print(out)
