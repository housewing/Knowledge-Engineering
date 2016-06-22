package keyword;

import java.io.*;
import java.util.*;

import java.sql.*;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class KeyWord {
    static List<String> stopwords = Arrays.asList("免費下載http","翻攝自YouTube",
            "更多文章","有話要說","即時論壇","食安問題追追追","免費下載","App免費下載","爆廢公社","再生com",
            "西甲戰績表","德甲戰績表","英超戰績表","都在有話要說","食安問題追追追有話要說","myMusic","年月日","肌膚檢測>>>http","googl","http","wwwnbacom",
            "wwwioncecomtw","onlineopinionsappledailycomtw","wwwappledailycomtw","點選http","更多更精采的","py_jumelle_apphtml更多文章","好康優惠我要購買http",
            "bitly","wwwnextmagcomtw","App免費下載http","nextmedia_iphoneAndroid","nextmedia_android","鎖定更多精采報導","請上DailyView","網路溫度計官網",
            "更多專欄文章","boCm更多文章","活動網址","Online報導","擷自Youtube","dailyviewtw","py_jumelle_apphtml","shopdmarketnetnet",
            "台灣時間","文章來源","資料來源","wwwfacebookcom");
    
    static List<String> skipwords = Arrays.asList("ww","VE","on","an","ar","NE","ER","in","州-","MW","RA","AT","AP","AM","EN","CH","ag","as","The","MA",
            "th","CC","ON","MO","Lu","av","AA","黨的","所以","因為","明天","一起","可以","甚至","我們","一般","由於","沒有","中的","可能","不過","不會",
			"只要","為了","因此","一天","是否","以上","總是","雖然","或是","例如","你是","的事","的提示","養了","你的","的人你","讓你","再下","的憲法",
			"病的","桿的","的眼","不要","他命","開出下","條版路","他人","是個","也可","萬美元","個月","其實","大多","年後","不宜","含有","方式","得到",
			"解決","公尺","目前","問題","時間","認為","公里","原因","小人");
    
    public static Map<String, List<Token>> indexTF = new HashMap<String, List<Token>>();
    public static List<Integer> files = new ArrayList<Integer>();
    public static List<String> keyIndex = new ArrayList<String>();

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
    
    public static int EditDistance(String word1, String word2) {
	int len1 = word1.length();
	int len2 = word2.length();
 
	// len1+1, len2+1, because finally return dp[len1][len2]
	int[][] dp = new int[len1 + 1][len2 + 1];
 
	for (int i = 0; i <= len1; i++) {
		dp[i][0] = i;
	}
 
	for (int j = 0; j <= len2; j++) {
		dp[0][j] = j;
	}
 
	//iterate though, and check last char
	for (int i = 0; i < len1; i++) {
            char c1 = word1.charAt(i);
            for (int j = 0; j < len2; j++) {
                char c2 = word2.charAt(j);

                //if last two chars equal
                if (c1 == c2) {
                    //update dp value for +1 length
                    dp[i + 1][j + 1] = dp[i][j];
                } else {
                    int replace = dp[i][j] + 1;
                    int insert = dp[i][j + 1] + 1;
                    int delete = dp[i + 1][j] + 1;

                    int min = replace > insert ? insert : replace;
                    min = delete > min ? min : delete;
                    dp[i + 1][j + 1] = min;
                }
            }
	}

        return dp[len1][len2];
    }
    
    public void indexFile(String line, int fileno) throws IOException {
        int pos = 0;
        line = line.replaceAll("<BR>", "").replaceAll("--", "");
        line = line.replaceAll("[.％%:~@*＊→]", "");
        line = line.replaceAll("[0123456789【】《》╱／/〈〉（\"）‧、。，』『！」「·：…．◎★●&,;；#!？?&(){}\\\\[\\\\]]", " ");
        line = line.trim().replaceAll("  ", " ").replaceAll("  ", " ");
            
        for (String _word : line.split(" ")){
            if (stopwords.contains(_word)) { continue; }
//            System.out.println("this is " + _word);
            
            int len = _word.length();
            if(len <= 1) { continue; }
            else if(len > 11) { len = 11; }
            
            for (int n = 2; n <= len; n++) {
                for (String ngram : ngrams(n, _word)){
                    pos++;
//                    System.out.println("this is " + ngram);
                    
                    List<Token> idx = indexTF.get(ngram);
                    if (idx == null) {
                        idx = new LinkedList<Token>();
                        indexTF.put(ngram, idx);
                    }
                    idx.add(new Token(fileno, pos));    
                }
            }
        }
//        System.out.println("indexed " + fileno + " " + pos + " words");
    }
 
    public static Set<String> searchKeyWord(String _word) {
//        System.out.println("Search keyword: " + _word);
        Set<String> getFile = new HashSet<String>();

        List<Token> idx = indexTF.get(_word);
        if (idx != null) {
            for (Token t : idx) {
                getFile.add(files.get(t.fileno).toString());
            }
        }
//        System.out.print("ts " + _word);
//        for (String f : getFile) {
//            System.out.println(" " + f);
//        }
        return getFile;
    }
    
    public static HashMap<String, Double> calculateTFIDF(int _size){
        HashMap<String, Double> tmpTFIDF = new HashMap<String, Double>();
        for(Object key : indexTF.keySet()){
            if(indexTF.get(key).size() > 5){
//                System.out.println(key + " " + index.get(key).size());
                Set<String> indexDF = new HashSet<String>(); // 紀錄DF
                List<Token> tmpToken = indexTF.get(key);
                for (Token tmp : tmpToken) {
                    indexDF.add(files.get(tmp.fileno).toString());
                }
                
                if(indexDF.size() < _size / 2){
                    double error = Math.log10(indexTF.get(key).size()) * (Math.log10(_size / indexDF.size() + 1));
                    tmpTFIDF.put(key.toString(), new Double(error));
                }
            }
        }
        return tmpTFIDF;
    }
    
    public static void MergeSort(HashMap<String, Double> TFIDF){
        List<String> tmpValueTFIDF = new ArrayList<String>();
        //sort TFIDF by value
        HashMap<String, Double> keyTFIDF = new HashMap<String, Double>();
        ArrayList<Map.Entry<String, Double>> valueList = new ArrayList<Map.Entry<String,Double>>(TFIDF.entrySet());
        Collections.sort(valueList, new Comparator<Map.Entry<String, Double>>(){
            public int compare(Map.Entry<String, Double> value1, Map.Entry<String,Double> value2) {
                return ((value2.getValue() - value1.getValue() == 0) ? 0 : (value2.getValue() - value1.getValue() > 0) ? 1 : -1);
            }
        });
        //output sort TFIDF by value
        for (Map.Entry<String, Double> entry : valueList) {
            if(tmpValueTFIDF.size() < 1000){
                tmpValueTFIDF.add(entry.getKey());
                keyTFIDF.put(entry.getKey(), new Double(entry.getValue()));
            }
//            if(entry.getValue() > 3){
//                tmpValueTFIDF.add(entry.getKey());
//                keyTFIDF.put(entry.getKey(), new Double(entry.getValue()));
//            }
        }
//        System.out.println("valueTFIDF size " + tmpValueTFIDF.size() + " keyTFIDF size " + keyTFIDF.size());
        
        //sort TFIDF by key length
        List<String> deleteTFIDF = new ArrayList<String>();
        ArrayList<Map.Entry<String, Double>> keyList = new ArrayList<Map.Entry<String,Double>>(keyTFIDF.entrySet());
        Collections.sort(keyList, new Comparator<Map.Entry<String, Double>>(){
            public int compare(Map.Entry<String, Double> key1, Map.Entry<String,Double> key2) {
                return ((key2.getKey().length() - key1.getKey().length() == 0) ? 0 : (key2.getKey().length() - key1.getKey().length() > 0) ? 1 : -1);
            }
        });
        //output sort TFIDF by key length
        for (Map.Entry<String, Double> entry : keyList) {
            for(String ts : tmpValueTFIDF){
                int keyLen = entry.getKey().toString().length();
                int valueLen = ts.length();
                
                int error = EditDistance(entry.getKey(), ts);
                if(error != 0){
                    if(error == (keyLen - valueLen)){
                        deleteTFIDF.add(ts);
//                        System.out.println("Between " + entry.getKey() + " & " + ts + " error : " + error );
                    }
                }
            }
        }
        System.out.println("valueTFIDF size " + tmpValueTFIDF.size() + " deleteTFIDF size " + deleteTFIDF.size());
        
        for(String ts : deleteTFIDF){ // 移除子關鍵詞
            tmpValueTFIDF.remove(ts); 
        }
        
        int count = 0;
        for(String ts : tmpValueTFIDF){
            if(keyIndex.contains(ts) || skipwords.contains(ts)){
//                System.out.println("duplicate " + ts);
                continue;
            }
            else{
                keyIndex.add(ts);
                count++;
            }
            
            if(count == 300){
                break;
            }
        }
    }

    public static void searchDB(List<String> _data, String str){
        Connection connDB = null;
        try{
            Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
            String dataSource = "jdbc:ucanaccess://D:/KEDB/ke2016_sample_data.accdb";
            connDB = DriverManager.getConnection(dataSource);
            Statement st = connDB.createStatement();
            st.execute(str);
            ResultSet rs = st.getResultSet();
            Integer count = 0;
            while(rs.next()){
                _data.add(rs.getString("content"));
                files.add(count);
                count++;
//                System.out.println(rs.getString("content"));
            }
            st.close();
            connDB.close();
        }
        catch(ClassNotFoundException e) { System.out.println("Driver loading failed!"); }
        catch(SQLException e) { System.out.println("DB linking failed!"); }     
    }
    
    public static void main(String[] args) throws IOException {
        System.out.println("0: 財經, 1: 體育, 2: 娛樂, 3: 兩岸, 4: 社會, 5: 政治, 6: 保健");
        
        String[] selectMode = {
                "SELECT * FROM ke2016_sample_news WHERE section Like '財經*' OR section Like '產經*'",
                "SELECT * FROM ke2016_sample_news WHERE section Like '體育*' OR section Like '運動*'",
                "SELECT * FROM ke2016_sample_news WHERE section Like '娛樂*' OR section Like '影劇*'",
                "SELECT * FROM ke2016_sample_news WHERE section Like '兩岸*'",
                "SELECT * FROM ke2016_sample_news WHERE section Like '社會*' OR section Like '地方*'",
                "SELECT * FROM ke2016_sample_news WHERE section Like '政治*'",
                "SELECT * FROM ke2016_sample_news WHERE section Like '家庭*'"};
        
        for(String mode : selectMode)
        {
            List<String> iData = new ArrayList<String>(); // 將每篇文章存在List中
            searchDB(iData, mode);
            System.out.println("iData size " + iData.size() + " file size " + files.size());

            KeyWord idx = new KeyWord();
            for (int i = 0; i < iData.size(); i++) {
                idx.indexFile(iData.get(i), files.get(i));
            }
            
            HashMap<String, Double> TFIDF = new HashMap<String, Double>();
            TFIDF = calculateTFIDF(iData.size());
            System.out.println("TFIDF size " + TFIDF.size());
            
            MergeSort(TFIDF);
            
            iData.clear();
            TFIDF.clear();
            files.clear();
            indexTF.clear();
        }
        System.out.println("keyIndex size " + keyIndex.size());
        
        //Store all value into excel fiel
        HSSFWorkbook workbook = new HSSFWorkbook();//建立一個Excel活頁簿
        HSSFSheet sheet = workbook.createSheet("KeyWord"); //在活頁簿中建立一個Sheet

        //取得活頁簿中的第r列第c欄(cell)
        for(int i = 0; i < keyIndex.size(); i++){
            HSSFRow row = sheet.createRow(i);
            HSSFCell cell = row.createCell(0);
            cell.setCellValue(keyIndex.get(i));
        }

        FileOutputStream fOut = new FileOutputStream("D:/KEDB/KeyWord_new300.xls");
        workbook.write(fOut);
        fOut.close();
    }
    
    public class Token {
        int fileno;
        int position;
        
        public Token(int fileno, int position) {
            this.fileno = fileno;
            this.position = position;
        }
    }
}
