package com.flinkinfo.lucene.controller;

import com.alibaba.fastjson.JSONObject;
import com.flinkinfo.lucene.entity.News;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索处理
 *
 * @author nico huangwenzeng1@163.com
 */
@Controller
@RequestMapping("/lucene")
public class LuceneController
{

    //索引
    @Autowired(required = false)
    IndexWriter indexWriter;

    //分词工具
    @Autowired(required = false)
    IKAnalyzer analyzer;

    @Autowired(required = false)
    Analyzer analyzer1;

    private static final String STARTTAG = "<font color='red'>";
    private static final String ENDTAG = "</font>";

    /**
     * 索引列表
     *
     * @return
     * @throws CorruptIndexException
     * @throws IOException
     */
    @ResponseBody
    @RequestMapping("listIndexed")
    public String listIndexed() throws CorruptIndexException, IOException
    {
        //索引搜索
        IndexSearcher indexSearcher = getSearcher();
        int size = indexWriter.maxDoc();
        Document doc = null;
        News news = null;
        List<News> list = new ArrayList<News>();
        for (int i = 0; i < size; i++)
        {
            news = new News();
            doc = indexSearcher.doc(i);
            news.setTitle(doc.get("title"));
            news.setUrl(doc.get("url"));
            list.add(news);
        }
        return JSONObject.toJSONString(list);
    }

    /**
     * 索引文件
     *
     * @return 索引文件列表
     * @throws IOException IO异常
     */
    @ResponseBody
    @RequestMapping("indexFiles")
    public String indexFiles() throws IOException
    {
        Directory d = indexWriter.getDirectory();
        String[] fs = d.listAll();
        return JSONObject.toJSONString(fs);
    }

    /**
     * 删除索引
     *
     * @return
     */
    @ResponseBody
    @RequestMapping("deleteIndexes")
    public String deleteIndexes()
    {

        String flag = "";

        try
        {
            //删除索引
            indexWriter.deleteAll();
            //提交
            indexWriter.commit();
            //成功标识
            flag = "suc";
        }
        catch (IOException e)
        {
            //删除异常
            e.printStackTrace();
            try
            {
                //回滚
                indexWriter.rollback();
            }
            catch (IOException e1)
            {
                e1.printStackTrace();
            }
            //失败标识
            flag = "error";
        }

        return flag;
    }

    @ResponseBody
    @RequestMapping("deleteIndex")
    public String deleteIndex()
    {

        return null;
    }

    @ResponseBody
    @RequestMapping("updateIndex")
    public String updateIndex()
    {

        return null;
    }

    /**
     * 搜索
     *
     * @param text 关键字
     * @return 搜索内容
     * @throws ParseException
     * @throws IOException
     * @throws InvalidTokenOffsetsException
     */
    @ResponseBody
    @RequestMapping("search")
    public String search(String text) throws ParseException, IOException, InvalidTokenOffsetsException
    {

        IndexSearcher searcher = getSearcher();

        QueryParser parser = new MultiFieldQueryParser(Version.LUCENE_48, new String[]{"title", "content"}, analyzer1);

        Query query = parser.parse(text);

        TopDocs td = searcher.search(query, 10);

        ScoreDoc[] sd = td.scoreDocs;

        SimpleHTMLFormatter simpleHtmlFormatter = new SimpleHTMLFormatter(STARTTAG, ENDTAG);

        Highlighter highlighter = new Highlighter(simpleHtmlFormatter, new QueryScorer(query));

        Document doc;

        TokenStream tokenStream = null;

        News news = null;
        List<News> list = new ArrayList<News>();

        String title;
        String content;

        for (int i = 0; i < sd.length; i++)
        {
            news = new News();

            int docId = sd[i].doc;
            doc = searcher.doc(docId);

            title = doc.get("title");
            tokenStream = analyzer1.tokenStream("title", new StringReader(title));
            title = highlighter.getBestFragment(tokenStream, title);
            news.setTitle(title == null ? doc.get("title") : title);

            content = doc.get("content");
            tokenStream = analyzer1.tokenStream("content", new StringReader(content));
            content = highlighter.getBestFragment(tokenStream, content);

            //正文部分，如果没有匹配的关键字，截取前200个字符
            news.setContent(content == null ? (doc.get("content").length() < 200 ? doc.get("content") : doc.get("content").substring(0, 199)) : content);
            news.setUrl(doc.get("url"));
            news.setDate(doc.get("date"));
            news.setOther1(docId + "");
            list.add(news);

        }
        return JSONObject.toJSONString(list);
    }

    @SuppressWarnings("deprecation")
    private IndexSearcher getSearcher() throws IOException
    {
        return new IndexSearcher(IndexReader.open(indexWriter.getDirectory()));
    }

}
