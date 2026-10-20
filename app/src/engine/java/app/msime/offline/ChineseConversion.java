package app.msime.offline;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/** Offline OpenCC s2t phrase-first maximum matching, using the original engine's pinned data. */
public final class ChineseConversion {
    private static final class Node { final Map<Character,Node> children=new HashMap<>(); String value; }
    private final Node normalization=new Node(), conversion=new Node();
    public ChineseConversion(Reader normalize, Reader phrases, Reader regional, Reader characters) throws IOException {
        read(normalization,normalize); read(conversion,phrases); read(conversion,regional); read(conversion,characters);
    }
    private static void read(Node root, Reader source) throws IOException {
        try(BufferedReader rows=new BufferedReader(source)) {
            String line;
            while((line=rows.readLine())!=null) {
                if(line.isEmpty() || line.startsWith("#")) continue;
                String[] parts=line.split("\t",2); if(parts.length!=2) continue;
                Node node=root; for(char c:parts[0].toCharArray()) node=node.children.computeIfAbsent(c,k->new Node());
                if(node.value==null) node.value=parts[1].split(" ",2)[0];
            }
        }
    }
    public String convert(String text) { return pass(conversion,pass(normalization,text)); }
    private static String pass(Node root,String text) {
        StringBuilder output=new StringBuilder();
        for(int start=0;start<text.length();) {
            int ids=idsEnd(text,start,0); if(ids>start) { output.append(text,start,ids); start=ids; continue; }
            Node node=root; String match=null; int end=start;
            for(int i=start;i<text.length();i++) {
                node=node.children.get(text.charAt(i)); if(node==null) break;
                if(node.value!=null) {match=node.value;end=i+1;}
            }
            if(match==null) { int size=Character.charCount(text.codePointAt(start)); output.append(text,start,start+size); start+=size; }
            else { output.append(match); start=end; }
        }
        return output.toString();
    }
    private static int idsEnd(String text,int start,int depth) {
        if(start>=text.length() || depth>16) return -1;
        int cp=text.codePointAt(start), arity=(cp==0x2ff2||cp==0x2ff3)?3:cp>=0x2ff0&&cp<=0x2ffb?2:0;
        if(arity==0) return -1;
        int end=start+Character.charCount(cp);
        for(int i=0;i<arity;i++) { if(end>=text.length()) return -1; int nested=idsEnd(text,end,depth+1); end=nested>end?nested:end+Character.charCount(text.codePointAt(end)); }
        return end-start>128?-1:end;
    }
}
