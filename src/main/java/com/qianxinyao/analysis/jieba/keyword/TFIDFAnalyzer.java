package com.qianxinyao.analysis.jieba.keyword;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.huaban.analysis.jieba.JiebaSegmenter;

/**
 * @author Tom Qian
 * @email tomqianmaple@outlook.com
 * @github https://github.com/bluemapleman
 * @date Oct 20, 2018
 * tfidf算法原理参考：http://www.cnblogs.com/ywl925/p/3275878.html
 * 部分实现思路参考jieba分词：https://github.com/fxsjy/jieba
 */
public class TFIDFAnalyzer {

    static HashSet<String> stopWordsSet = new HashSet<>();
    static HashMap<String, Double> idfMap = new HashMap<>();
    /*static double idfMedian;*/
    private final String DEFAULT_CHARSET_NAME = "utf-8";

    public TFIDFAnalyzer() {
        synchronized (stopWordsSet) {
            loadStopWords(stopWordsSet, this.getClass().getResourceAsStream("/stop_words.txt"));
        }
        synchronized (idfMap) {
            loadIDFMap(idfMap, this.getClass().getResourceAsStream("/idf_dict.txt"));
            loadIDFMap(idfMap, this.getClass().getResourceAsStream("/idf_user.txt"));
        }
    }

    /**
     * tfidf分析方法
     *
     * @param content 需要分析的文本/文档内容
     * @param topFlag 是否最高排列
     * @param n       需要返回的tfidf值最高的N个关键词，若超过content本身含有的词语上限数目，则默认返回全部
     * @return
     */
    public List<Keyword> analyze(String content, boolean topFlag, int n) {
        List<Keyword> keywordList = analyze(content);
        Collections.sort(keywordList);
        if (!topFlag) {
            Collections.reverse(keywordList);
        }

        if (keywordList.size() > n) {
            int num = keywordList.size() - n;
            for (int i = 0; i < num; i++) {
                keywordList.remove(n);
            }
        }
        return keywordList;
    }

    /**
     * tfidf分析方法
     *
     * @param content 需要分析的文本/文档内容
     * @return
     */
    public List<Keyword> analyze(String content) {
        List<Keyword> keywordList = new ArrayList<>();

        Map<String, Double> tfMap = getTF(content);
        for (String word : tfMap.keySet()) {
            // 若该词不在idf文档中，则使用平均的idf值(可能定期需要对新出现的网络词语进行纳入)
            if (idfMap.containsKey(word)) {
                keywordList.add(new Keyword(word, idfMap.get(word) * tfMap.get(word)));
            } else {
                /*keywordList.add(new Keyword(word, idfMedian * tfMap.get(word)));*/
                /*不在idf中，认为几乎为0*/
                keywordList.add(new Keyword(word, 0.001d * tfMap.get(word)));
            }
        }
        return keywordList;
    }

    /**
     * tf值计算公式
     * tf=N(i,j)/(sum(N(k,j) for all k))
     * N(i,j)表示词语Ni在该文档d（content）中出现的频率，sum(N(k,j))代表所有词语在文档d中出现的频率之和
     *
     * @param content
     * @return
     */
    private Map<String, Double> getTF(String content) {
        Map<String, Double> tfMap = new HashMap<>();
        if (content == null || content.equals("")) {
            return tfMap;
        }

        JiebaSegmenter segmenter = new JiebaSegmenter();
        List<String> segments = segmenter.sentenceProcess(content);
        Map<String, Integer> freqMap = new HashMap<>();

        int wordSum = 0;
        for (String segment : segments) {
            //停用词不予考虑，单字词不予考虑
            if (!stopWordsSet.contains(segment) && segment.length() > 1) {
                wordSum++;
                if (freqMap.containsKey(segment)) {
                    freqMap.put(segment, freqMap.get(segment) + 1);
                } else {
                    freqMap.put(segment, 1);
                }
            }
        }

        // 计算double型的tf值
        for (String word : freqMap.keySet()) {
            tfMap.put(word, freqMap.get(word) * 1.0 / wordSum);
        }

        return tfMap;
    }

    /**
     * 默认jieba分词的停词表
     * url:https://github.com/yanyiwu/nodejieba/blob/master/dict/stop_words.utf8
     *
     * @param set
     * @param in
     */
    private void loadStopWords(Set<String> set, InputStream in) {
        BufferedReader bufr;
        try {
            bufr = new BufferedReader(new InputStreamReader(in, DEFAULT_CHARSET_NAME));
            String line = null;
            while ((line = bufr.readLine()) != null) {
                /*set.add(line.trim());*/
                set.add(line);
            }
            try {
                bufr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * idf值本来需要语料库来自己按照公式进行计算，不过jieba分词已经提供了一份很好的idf字典，所以默认直接使用jieba分词的idf字典
     * url:https://raw.githubusercontent.com/yanyiwu/nodejieba/master/dict/idf.utf8
     *
     * @param map
     * @param in
     */
    private void loadIDFMap(Map<String, Double> map, InputStream in) {
        BufferedReader bufr;
        try {
            bufr = new BufferedReader(new InputStreamReader(in, DEFAULT_CHARSET_NAME));
            String line = null;
            while ((line = bufr.readLine()) != null) {
                String[] kv = line.trim().split(" ");
                if (kv.length != 2) {
                    continue;
                }
                map.put(kv[0], Double.parseDouble(kv[1]));
            }
            try {
                bufr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 计算idf值的中位数
            /*List<Double> idfList = new ArrayList<>(map.values());
            Collections.sort(idfList);
            idfMedian = idfList.get(idfList.size() / 2);*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String content = "孩子上了幼儿园 安全防拐教育要做好";
        int n = 5;
        TFIDFAnalyzer tfidfAnalyzer = new TFIDFAnalyzer();
        List<Keyword> list = tfidfAnalyzer.analyze(content, true, n);
        for (Keyword word : list) {
            System.out.print(word.getName() + ":" + word.getTfidfvalue() + ",");
        }
    }
}

