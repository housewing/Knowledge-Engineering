package keyword;

import java.sql.*;
import java.io.*;
import java.util.*;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class KeyWord {
    public static List<String> iData = new ArrayList<String>();
    public static HashMap<String, Integer> keyWord = new HashMap<String, Integer>();
    public static HashMap<String, HashMap<String, Integer>> indexTF = new HashMap<String, HashMap<String, Integer>>();
    public static HashMap<String, HashMap<String, Integer>> docVector = new HashMap<String, HashMap<String, Integer>>();
    public static HashMap<String, HashMap<String, Integer>> docClass = new HashMap<String, HashMap<String, Integer>>();
    public static HashMap<String, HashMap<String, Double>> navieBayes = new HashMap<String, HashMap<String, Double>>();
    
    public static HashMap<String, Integer> filesID = new HashMap<String, Integer>();
    public static HashMap<String, Token> filesNO = new HashMap<String, Token>();
    public static List<Integer> dataSet = new ArrayList<Integer>();

    public static List<String> ngrams(int n, String str) {
        List<String> ngrams = new ArrayList<String>();
        String[] words = str.split("");
        for (int i = 0; i < words.length - n + 1; i++){
            ngrams.add(concat(words, i, i+n));
        }
        return ngrams;
    }

    public static String concat(String[] words, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++){
            sb.append(words[i]);
        }
        return sb.toString();
    }
    
    public static void sevenClassify(String classifyStr, int _fileno){
        HashMap<String, Integer> tmpC = docClass.get(classifyStr);
        if (tmpC == null) {
            tmpC = new HashMap<String, Integer>();
            docClass.put(classifyStr, tmpC);
        }
        if(tmpC.get(Integer.toString(_fileno)) == null){
            tmpC.put(Integer.toString(_fileno), 1);
        }
    }
            
    public void indexFile(String line){
        int fileno = -1;
        fileno = iData.indexOf(line);
        
        HashMap<String, Integer> docV = docVector.get(fileno);
        if (docV == null) {
            docV = new HashMap<String, Integer>();
            docVector.put(Integer.toString(fileno), docV);
        }
        
        //docClass
//        String[] classify = {"財經","產經","體育","運動","娛樂","影劇","兩岸","國際","社會","地方","政治","家庭"};
        String[] classify = {"兩岸","政治","家庭"};
        Token to = filesNO.get(Integer.toString(fileno));
        for(String ts : classify){
            if(to.section.indexOf("體育") != -1 || to.section.indexOf("運動") != -1){
                sevenClassify("體育", fileno);
            }
            else if(to.section.indexOf("娛樂") != -1 || to.section.indexOf("影劇") != -1){
                sevenClassify("娛樂", fileno);
            }
            else if(to.section.indexOf("財經") != -1 || to.section.indexOf("產經") != -1){
                sevenClassify("財經", fileno);
            }
            else if(to.section.indexOf("社會") != -1 || to.section.indexOf("地方") != -1){
                sevenClassify("社會", fileno);
            }
            else if(to.section.indexOf(ts) != -1){
                sevenClassify(ts, fileno);
            }
        }
        
        line = line.replaceAll("<BR>", "").replaceAll("--", "");
        line = line.replaceAll("[.％%:~@*＊→]", "");
        line = line.replaceAll("[0123456789【】《》╱／/〈〉（\"）‧、。，』『！」「·：…．◎★●&,;；#!？?&(){}\\\\[\\\\]]", " ");
        line = line.trim().replaceAll("  ", " ").replaceAll("  ", " ");
        for (String _word : line.split(" ")){
            int len = _word.length();
            if(len <= 1) { continue; }
            else if(len > 9) { len = 9; }
            
            for (int n = 2; n <= len; n++) {
                for (String ngram : ngrams(n, _word)){
                    if(keyWord.get(ngram) != null){
                        HashMap<String, Integer> idx = indexTF.get(ngram);
                        if (idx == null) {
                            idx = new HashMap<String, Integer>();
                            indexTF.put(ngram, idx);
                        }
                        
                        //indexTF
                        if(idx.get(Integer.toString(fileno)) == null){
                            idx.put(Integer.toString(fileno), 1); 
                        }
                        else{
                            idx.put(Integer.toString(fileno), idx.get(Integer.toString(fileno)) + 1);
                        }
                        
                        //docVector
                        if(docV.get(ngram) == null){
                            docV.put(ngram, 1); 
                        }
                        else{
                            docV.put(ngram, docV.get(ngram) + 1);
                        }
                    }   
                }
            }
        }
//        System.out.println("indexed " + fileno + " " + pos + " words" + " indexTF " + indexTF.size());
    }
    
    public void calCosineSimilarity(String _queryID){
        HashMap<String, Integer> docNum = new HashMap<String, Integer>();
        HashMap<String, Integer> queryKey = docVector.get(_queryID);
        for(String ts : queryKey.keySet()){
            HashMap<String, Integer> tmpIdx = indexTF.get(ts);
            for(String num : tmpIdx.keySet()){
                if(docNum.get(num) == null){
//                    System.out.println("No." + num + " TF: " + tmpIdx.get(num) + " ts " + ts);
                    docNum.put(num, 1);
                }
            }
        }
//        System.out.println("docNum size " + docNum.size());  

        HashMap<String, Double> CosineSimilarity = new HashMap<String, Double>();
        for (String ts : docNum.keySet()) {
            double dotProduct = 0.0;
            double crossProduct1 = 0.0;
            double crossProduct2 = 0.0;
            double cosineSimilarity = 0.0;
            
            HashMap<String, Integer> dataKey = docVector.get(ts);
            if(dataKey != null){
                Set<String> sameKey = new HashSet<String>(queryKey.keySet());
                sameKey.retainAll(dataKey.keySet());
                for(String key : sameKey){
                    dotProduct += queryKey.get(key) * dataKey.get(key);  //a.b
                }
                
                for(Object queryValue : queryKey.keySet()){
                    crossProduct1 += Math.pow(queryKey.get(queryValue.toString()), 2);  //(a^2)
                }
    //            System.out.println("queryKey " + sameKey.size());

                for(Object dataValue : dataKey.keySet()){
                    crossProduct2 += Math.pow(dataKey.get(dataValue.toString()), 2); //(b^2)
                }
    //            System.out.println("dataKey " + dataKey.size());
                
                crossProduct1 = Math.sqrt(crossProduct1);//sqrt(a^2)
                crossProduct2 = Math.sqrt(crossProduct2);//sqrt(b^2)

                cosineSimilarity = dotProduct / (crossProduct1 * crossProduct2);
                CosineSimilarity.put(ts, cosineSimilarity);
            }
        }
//        System.out.println("CosineSimilarity size " + CosineSimilarity.size());
        
        ArrayList<Map.Entry<String, Double>> valueList = new ArrayList<Map.Entry<String,Double>>(CosineSimilarity.entrySet());
        Collections.sort(valueList, new Comparator<Map.Entry<String, Double>>(){
            public int compare(Map.Entry<String, Double> value1, Map.Entry<String,Double> value2) {
                return ((value2.getValue() - value1.getValue() == 0) ? 0 : (value2.getValue() - value1.getValue() > 0) ? 1 : -1);
            }
        });
        
        List<String> eightClassify = Arrays.asList("財經","兩岸","社會","政治","家庭","體育","娛樂","無法辨識");
        String[] arrSection = new String[7];
        int[] arrClassify = {0,0,0,0,0,0,0};
        for (Map.Entry<String, Double> entry : valueList) {
            if(dataSet.size() < 7){
//                System.out.println(iData.get(Integer.parseInt(entry.getKey())));
                arrSection[dataSet.size()] = filesNO.get(entry.getKey()).section;
                dataSet.add(Integer.parseInt(entry.getKey()));
                System.out.println("ID " + filesNO.get(entry.getKey()).id + " No." + entry.getKey() + " " + filesNO.get(entry.getKey()).section + " " + filesNO.get(entry.getKey()).title + " " + entry.getValue() );
                HashMap<String, Integer> tmpKey = docVector.get(entry.getKey());
                System.out.println(tmpKey.keySet());
            }
        }
        
        for(int i = 0 ; i < arrSection.length; i++){
            if(arrSection[i].indexOf("娛樂") != -1 || arrSection[i].indexOf("影劇") != -1){
                arrClassify[6]++;
            }
            else if(arrSection[i].indexOf("體育") != -1 || arrSection[i].indexOf("運動") != -1){
                arrClassify[5]++;
            }
            else if(arrSection[i].indexOf("財經") != -1 || arrSection[i].indexOf("產經") != -1){
                arrClassify[0]++;
            }
            else if(arrSection[i].indexOf("兩岸") != -1){
                arrClassify[1]++;
            }
            else if(arrSection[i].indexOf("社會") != -1 || arrSection[i].indexOf("地方") != -1){
                arrClassify[2]++;
            }
            else if(arrSection[i].indexOf("政治") != -1){
                arrClassify[3]++;
            }
            else if(arrSection[i].indexOf("家庭") != -1){
                arrClassify[4]++;
            }
        }
        
        int max = 0, count = 0, index = 0;
        for(int i = 0; i < arrClassify.length; i++){
            if( arrClassify[i] > max ){
                max = arrClassify[i];
                index = i;
            }
            count += arrClassify[i];
        }
        
        if((count - max) > max){
            System.out.println("KNN: " + eightClassify.get(7));
        }
        else{
            System.out.println("KNN: " + eightClassify.get(index));
        }
    }

    public void navieBayesTrain(String _queryID){
        HashMap<String, Double> classSize = new HashMap<String, Double>();
        String[] cArray = {"財經","體育","娛樂","兩岸","社會","政治","家庭"};
        for(String _class : cArray){
            HashMap<String, Double> tmpDoc = new HashMap<String, Double>();
            int sum = 0;
            HashMap<String, Integer> docC = docClass.get(_class);
            for(String ts : docC.keySet()){
    //            System.out.println(ts);
                HashMap<String, Integer> dv = docVector.get(ts);
                if(dv != null){
                    for(String t: dv.keySet()){
                        if(tmpDoc.get(t) == null){
                            tmpDoc.put(t, new Double(dv.get(t)));
                        }
                        else{
                            tmpDoc.put(t, tmpDoc.get(t) + dv.get(t));
                        }
                        sum += dv.get(t);
                    }
                }
            }
            classSize.put(_class, new Double(sum + keyWord.size()));
//            System.out.println("docC size " + docC.size() + " tmpDoc size " + tmpDoc.size() + " sum " + classSize.get(_class));

            HashMap<String, Double> tmpNavie = navieBayes.get(_class);
            if (tmpNavie == null) {
                tmpNavie = new HashMap<String, Double>();
                navieBayes.put(_class, tmpNavie);
            }
            
            for(String ts : keyWord.keySet()){
                if(tmpDoc.get(ts) == null){
    //                System.out.println("test " + ts + " " + d);
                    tmpNavie.put(ts, new Double( 1 / (classSize.get(_class))));
                }
                else{
                    tmpNavie.put(ts, new Double((tmpDoc.get(ts) + 1) / (classSize.get(_class))));
                }
            }
        }
//        System.out.println("navieBayes size " + navieBayes.size());

        HashMap<String, Integer> queryKey = docVector.get(_queryID);
        HashMap<String, Double> result = new HashMap<String, Double>();
        for(String c : navieBayes.keySet()){
            double error = 0;
            HashMap<String, Double> tn = navieBayes.get(c);
            for(String ts : queryKey.keySet()){
                error += Math.log10(tn.get(ts)) * queryKey.get(ts);
            }
            double set = classSize.get(c);
            double setAll = 0;
            for(String t : classSize.keySet()){
                setAll += classSize.get(t);
            }
            error = error + Math.log10(set / setAll);
            result.put(c, error);
        }
//        System.out.println("result size " + result.size());
        
        String classifyStr = "";
        double maxValue = -9999;
        for(String ts : result.keySet()){
//            System.out.println(ts + " " + result.get(ts));
            if(result.get(ts) > maxValue){
                maxValue = result.get(ts);
                classifyStr = ts;
            }
        }
        System.out.println("NavieBayes: " + classifyStr);
    }
    
    public void getSummary(int _queryID){
        int count = 0;
        String noBlank = "";
        String replaced = "";
        HashMap<String, Integer> Summary = new HashMap<String, Integer>();
        HashMap<String, Integer> queryData = docVector.get(Integer.toString(_queryID));
        
        for(Integer ts : dataSet){
            noBlank = iData.get(ts).replaceAll("\\s+", "");
            replaced = noBlank.replaceAll("<BR>", "").replaceAll("[（【]", "").replaceAll("[）】]", " ").replaceAll("分享", "分享 ")
                    .replaceAll("！", "！ ").replaceAll("//？", "？ ").replaceAll("。", "。 ");
            for(String contentSplit : replaced.split(" ")){
                for (String k2 : queryData.keySet()) {
                    if (contentSplit.indexOf(k2) > -1) {
                        if (Summary.get(contentSplit) == null) {
                            Summary.put(contentSplit, 1);
                        } else {
                            Summary.put(contentSplit, Summary.get(contentSplit) + 1);
                        }
                    }
                }
            }
        }
        
        List<Map.Entry<String, Integer>> list_Data = new ArrayList<Map.Entry<String, Integer>>(Summary.entrySet());
        Collections.sort(list_Data, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o2.getValue() - o1.getValue();
            }
        });
        System.out.println("---------- Summary ----------");
        for (Map.Entry<String, Integer> entry : list_Data) {
            if (count < 3) {
                System.out.println(entry.getKey() + " " + entry.getValue());
                count++;
            }
        }
    }

    public void searchDB(String str){
        Connection connDB = null;
        try{
            Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
            String dataSource = "jdbc:ucanaccess://D:/KEDB/ke2016_sample_data.accdb";
            connDB = DriverManager.getConnection(dataSource);
            Statement st = connDB.createStatement();
            st.execute(str);
            ResultSet rs = st.getResultSet();
            while(rs.next()){
                iData.add(rs.getString("content"));
                filesID.put(rs.getString("id"), filesID.size());
                filesNO.put(Integer.toString(filesNO.size()), new Token(rs.getString("id"), rs.getString("section"), rs.getString("title")));
            }
            st.close();
            connDB.close();
        }
        catch(ClassNotFoundException e) { System.out.println("Driver loading failed!"); }
        catch(SQLException e) { System.out.println("DB linking failed!"); }     
    }
    
    public void getKeyWord(String str) throws IOException {
        HSSFWorkbook readWorkbook = new HSSFWorkbook(new FileInputStream(str));   //取出excel      
        HSSFSheet readSheet = readWorkbook.getSheetAt(0);   //取得Sheet 可指定sheet的名稱, 參數為sheet名稱
        int row = readSheet.getPhysicalNumberOfRows(); //取得列總數
        for(int i = 0; i < row; i ++) {
            HSSFRow r = readSheet.getRow(i);//先取出列
            int col = r.getPhysicalNumberOfCells();
            for(int j = 0; j < col; j++) {
                keyWord.put(r.getCell(j).toString(), 1);
            }
        }
    }
    
    public static void main(String[] args) throws IOException {
        List<String> selectMode = Arrays.asList(
                "SELECT * FROM ke2016_sample_news WHERE section Like '財經*' OR section Like '產經*'",
                "SELECT * FROM ke2016_sample_news WHERE section Like '體育*' OR section Like '運動*'",
                "SELECT * FROM ke2016_sample_news WHERE section Like '娛樂*' OR section Like '影劇*'",
                "SELECT * FROM ke2016_sample_news WHERE section Like '兩岸*'",
                "SELECT * FROM ke2016_sample_news WHERE section Like '社會*' OR section Like '地方*'",
                "SELECT * FROM ke2016_sample_news WHERE section Like '政治*'",
                "SELECT * FROM ke2016_sample_news WHERE section Like '家庭*'");
        
        KeyWord idx = new KeyWord();
        idx.getKeyWord("D:/KEDB/KeyWord_new300.xls");
        System.out.println("keyWord size " + keyWord.size());
        
//        selectMode.stream().forEach(s -> idx.searchDB(s));
        idx.searchDB("SELECT * FROM ke2016_sample_news");
        System.out.println("iData " + iData.size());
        
        long start = System.currentTimeMillis();
//        iData.stream().forEach(s -> idx.indexFile(s));
        iData.parallelStream().forEach(s -> idx.indexFile(s));
        long end = System.currentTimeMillis();
        System.out.println(" Tag time: " + (double)(end - start) / 1000 + " seconds");
        
        //1456171697538_N01 1457438241681_N01
        //1455562411763_N01 1457146656597_N01 1457561788497_N01 1455913390197_N01 1455913406931_N01 
        //1456086182500_N01 1456345381552_N01 1456863249947_N01 1457381550856_N01 1455653317146_N01
        System.out.println("---------- Please input key word ----------");
        Scanner input = new Scanner(System.in);
        String queryID = "";
        while(queryID != "-1"){
            queryID = input.nextLine();
            idx.calCosineSimilarity(Integer.toString(filesID.get(queryID)));
            idx.navieBayesTrain(Integer.toString(filesID.get(queryID)));
            idx.getSummary(filesID.get(queryID));
            dataSet.clear();
            System.out.println("---------- Please input key word ----------");
        }
    }
    
    public class Token{
        String id;
        String section;
        String title;
        
        Token(String id, String section, String title){
            this.id = id;
            this.section = section;
            this.title = title;
        }
    }
}
