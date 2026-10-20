package app.msime.offline;
import java.util.List;
public final class CandidateExpansionSmoke {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        int[] values={30,30,300,20,40};List<List<CandidateRows.Cell>> rows=CandidateRows.pack(values,70,4);
        check(rows.size()==3,"wrap and long candidate full row");
        int count=0;for(var row:rows){int used=0;for(var cell:row){check(cell.index()==count++,"indices remain in source order");used+=cell.width();}used+=(row.size()-1)*4;check(used<=70,"row fits viewport");}
        check(count==5&&rows.get(1).get(0).width()==70,"long label is retained for multiline");
        check(CandidateRows.pack(new int[0],300,4).isEmpty(),"empty list");
        check(CandidateRows.pack(new int[]{300,0,-3},0,4).size()==3,"zero width before layout is bounded");
        int[] large=new int[10000];java.util.Arrays.fill(large,40);rows=CandidateRows.pack(large,320,4);
        check(rows.stream().mapToInt(List::size).sum()==large.length,"large model loses no candidate");
        CandidateRequest request=new CandidateRequest();long token=request.begin(4,5,6);
        check(request.accepts(token,4,5,6),"same session accepts");
        check(!request.accepts(token,4,5,7)&&!request.accepts(token,4,6,6)&&!request.accepts(token,8,5,6),"generation/editor/session changes refuse");
        request.cancel();check(!request.accepts(token,4,5,6),"collapsed list cancels request");
        long newer=request.begin(4,5,6);check(!request.accepts(token,4,5,6)&&request.accepts(newer,4,5,6),"late request cannot replace reopen");
        System.out.println("CandidateExpansionSmoke: lazy rows, oversized labels, 10k indices and stale session tickets PASS");
    }
}
