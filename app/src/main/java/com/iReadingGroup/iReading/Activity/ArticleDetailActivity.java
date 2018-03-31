package com.iReadingGroup.iReading.Activity;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.iReadingGroup.iReading.AsyncResponse;
import com.iReadingGroup.iReading.Bean.DaoMaster;
import com.iReadingGroup.iReading.Bean.DaoSession;
import com.iReadingGroup.iReading.Bean.OfflineDictBeanDao;
import com.iReadingGroup.iReading.Bean.WordCollectionBean;
import com.iReadingGroup.iReading.Bean.WordCollectionBeanDao;
import com.iReadingGroup.iReading.CollectWordEvent;
//import com.iReadingGroup.iReading.DaoMaster;
//import com.iReadingGroup.iReading.DaoSession;
import com.iReadingGroup.iReading.FetchNewsAsyncTask;
import com.iReadingGroup.iReading.FetchingBriefMeaningAsyncTask;
import com.iReadingGroup.iReading.Bean.OfflineDictBean;
//import com.iReadingGroup.iReading.OfflineDictBeanDao;
import com.iReadingGroup.iReading.R;
//import com.iReadingGroup.iReading.WordCollectionBeanDao;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.greendao.database.Database;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class ArticleDetailActivity extends AppCompatActivity {
    private Context mContext;
    private String articleTitleFromBundle;
    private Toolbar toolBar;
    private String article;
    private String uri;
    private String imageUrl;
    private String category;
    private TextView articleTextView,titleTextView,categoryTextView;
    private ImageView imageView;
    private Spannable spans;
    private PopupWindow popupWindow;
    private String meaning_result="Loading";
    private View ppwContentView;
    private OfflineDictBeanDao daoDictionary;//database instance
    private WordCollectionBeanDao daoCollection;
    private String current_word;
    private ArrayList<String> list_selected_words;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.iReadingGroup.iReading.R.layout.activity_article_detail);
        mContext = this;

        list_selected_words= new ArrayList <String>();
        //get arguments from intent to bundle
        articleTitleFromBundle=getParameterFromBundle();
        uri=getUriFromBundle();

        article=getArticle(uri);


        initializeDatabase();//initialize database


    }

    private void initUI() {
        initializeToolBar();
        initializeStatusBar();
        initializeTextView();       //initialize article's TextView
    }

    private void initializeToolBar(){
        toolBar=(Toolbar) findViewById(com.iReadingGroup.iReading.R.id.toolbar);
        toolBar.setTitle(articleTitleFromBundle);//set corresponding title in toolbar
        setSupportActionBar(toolBar);
    }

    private void initializeStatusBar(){
        //set StatusBar Color
        getWindow().setStatusBarColor(ContextCompat.getColor(this,R.color.colorPrimary));
    }

    private void initializeDatabase(){
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "wordDetail.db");
        Database db = helper.getWritableDb();
        DaoSession daoSession = new DaoMaster(db).newSession();
        daoDictionary = daoSession.getOfflineDictBeanDao();

        DaoMaster.DevOpenHelper helper_collection = new DaoMaster.DevOpenHelper(this, "notes-db");
        Database db_collection = helper_collection.getWritableDb();
        DaoSession daoSession_collection = new DaoMaster(db_collection).newSession();
        daoCollection=daoSession_collection.getWordCollectionBeanDao();
    }

    private void initializeTextView(){


        articleTextView = (TextView) findViewById(com.iReadingGroup.iReading.R.id.text);
        articleTextView.setMovementMethod(LinkMovementMethod.getInstance());
        articleTextView.setText(article, TextView.BufferType.SPANNABLE);
        articleTextView.setHighlightColor(Color.GRAY);//set the color of highlighting
        makeArticleTextViewSpannable();//make every word clickable

        titleTextView=(TextView) findViewById(R.id.title);
        titleTextView.setText(articleTitleFromBundle);

        categoryTextView=findViewById(R.id.category);
        categoryTextView.setText("Category:"+category);

        imageView=findViewById(R.id.img_article);
        Glide.with(this).load(imageUrl).into(imageView);
    }

    private void initializePopupWindow(View view){
        ppwContentView= LayoutInflater.from(mContext).inflate(
                com.iReadingGroup.iReading.R.layout.ppw_tap_search, null);

        //initialize  popupwindow
        popupWindow = new PopupWindow(ppwContentView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(false);
        popupWindow.setTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
                // 这里如果返回true的话，touch事件将被拦截
                // 拦截后 PopupWindow的onTouchEvent不被调用，这样点击外部区域无法dismiss
            }
        });
    }

    private void initializeWordTextView() {
        //initial text in popupwindow
        ((TextView)popupWindow.getContentView().findViewById(com.iReadingGroup.iReading.R.id.meaning)).setText(meaning_result);
    }

    private void initializeCollectButton() {
        ImageButton button = (ImageButton) ppwContentView.findViewById(com.iReadingGroup.iReading.R.id.collect);
        final boolean flag_collected=getWordCollectedStatus(current_word);
        if (flag_collected)
        {   //already collected
            button.setImageDrawable(
                    ContextCompat.getDrawable(getApplicationContext(), R.drawable.collect_true));
        }
        else
        {   //not collected yet
            button.setImageDrawable(
                    ContextCompat.getDrawable(getApplicationContext(), R.drawable.collect_false));
        }
        button.setOnClickListener(new View.OnClickListener() {
            //collect word function.
            @Override
            public void onClick(View v) {
                ImageButton button = (ImageButton) v;
                Log.i("collectdb", current_word);
                if (flag_collected) {
                    removeWordIntoCollection(current_word);
                    removeWordFromCurrentSelectedList(current_word);
                    //already in the db, the user means to removed this word from collection
                    button.setImageDrawable(
                            ContextCompat.getDrawable(getApplicationContext(), R.drawable.collect_false));
                } else {
                    //not in the db, the user means to add this word to collection
                    addWordIntoCollection(current_word);
                    addWordIntoCurrentSelectedList(current_word);
                    button.setImageDrawable(
                            ContextCompat.getDrawable(getApplicationContext(), R.drawable.collect_true));



                }
            }
        });
    }

    private String getParameterFromBundle(){
        Bundle bundle=getIntent().getExtras();
        return bundle.getString("name");
    }
    private String getUriFromBundle(){
        Bundle bundle=getIntent().getExtras();
        return bundle.getString("uri");
    }

    private String getArticle(String uri){
        FetchNewsAsyncTask asyncTask = new FetchNewsAsyncTask(new AsyncResponse() {
            @Override
            public void processFinish(Object output) {
                String b = (String) output;
                String[] buff= b.split("\r\n\r\n");
                imageUrl=buff[0];
                category=buff[1];
                article=buff[2];
                Toast.makeText(mContext, imageUrl, Toast.LENGTH_SHORT).show();
                initUI();
            }
        });
        //using iciba api to search the word, type json is small to transfer
        asyncTask.execute("http://eventregistry.org/json/article?action=getArticle&resultType=info&infoIncludeArticleBasicInfo=false&infoIncludeArticleEventUri=false&infoIncludeArticleCategories=true&infoArticleBodyLen=-1&infoIncludeArticleImage=true&infoIncludeConceptLabel=false&apiKey=19411967-5bfe-4f2a-804e-580654db39c9&articleUri=" +uri);
        return article;
    }

    private void makeArticleTextViewSpannable(){
        spans = (Spannable) articleTextView.getText();
        BreakIterator iterator = BreakIterator.getWordInstance(Locale.US);
        iterator.setText(article);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator
                .next()) {
            String possibleWord = article.substring(start, end);
            if (Character.isLetterOrDigit(possibleWord.charAt(0))) {
                ClickableSpan clickSpan = getClickableSpan(possibleWord,start,end,spans);
                spans.setSpan(clickSpan, start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private ClickableSpan getClickableSpan(final String word,final int start,final int end,final Spannable spans) {
        return new ClickableSpan() {
            final String mWord;
            {
                mWord = word;
            }

            @Override
            public void onClick(final View widget) {
                //if clicked on a word
                //if the asyncTask finishes fetching word meaning from Internet
                current_word=mWord;
                FetchingBriefMeaningAsyncTask asyncTask = new FetchingBriefMeaningAsyncTask(new AsyncResponse() {
                    @Override
                    public void processFinish(Object output) {
                        Log.d("finish", "finsh");
                        meaning_result = (String) output;
                        //show popup window
                        showPopupWindow(widget);
                    }
                });
                //using iciba api to search the word, type json is small to transfer
                List<OfflineDictBean> joes = daoDictionary.queryBuilder()
                        .where(OfflineDictBeanDao.Properties.Word.eq(word))
                        .list();
                if (joes.size()==0)
                {   //find nothing in offline dictionary,using online query
                    asyncTask.execute("https://dict-co.iciba.com/api/dictionary.php?key=341DEFE6E5CA504E62A567082590D0BD&type=json&w=" + word.toLowerCase());

                }
                else{
                    //find meaning in offline dictionary
                    meaning_result=mWord+"[离线]\n"+joes.get(0).getMeaning();
                    //show popup window
                    showPopupWindow(widget);

                }
                //set the selected word color transparent which means highlighted
                spans.setSpan(new BackgroundColorSpan(Color.TRANSPARENT),
                        start, end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                Log.d("tapped on:", mWord);
                //Toast.makeText(widget.getContext(), mWord, Toast.LENGTH_SHORT)
                //.show();

            }

            public void updateDrawState(TextPaint ds) {
                ds.setUnderlineText(false);//no underline for word
            }
        };
    }

    private void showPopupWindow(View view) {
        //loading layOut file
        initializePopupWindow(view);
        initializeWordTextView();
        initializeCollectButton();
        //show the popup window at the bottom
        popupWindow.showAtLocation(view, Gravity.BOTTOM,
                0, 0);



    }
    @Override
    public void onStop() {
        //save to the database and post the event when new word(s) collected.

        if(list_selected_words.size()>0){
            EventBus.getDefault().postSticky(new CollectWordEvent(0));

        }
        super.onStop();
    }

    private boolean getWordCollectedStatus (String word){
        List<WordCollectionBean> l =daoCollection.queryBuilder()
                .where(WordCollectionBeanDao.Properties.Word.eq(word))
                .list();
        if (l.size()==0){
            return false;//not collected yet
        }
        else return true;
    }

    private void addWordIntoCollection (String word){
        WordCollectionBean newWord=new WordCollectionBean();
        newWord.setWord(word);
        daoCollection.insert(newWord);
    }

    private void removeWordIntoCollection (String word){
        List<WordCollectionBean> l =daoCollection.queryBuilder()
                .where(WordCollectionBeanDao.Properties.Word.eq(word))
                .list();
        for (WordCollectionBean existedword : l) {
            daoCollection.delete(existedword);
        }
    }

    private void removeWordFromCurrentSelectedList(String word){
        if (list_selected_words.contains(word)){
            list_selected_words.remove(word);
        }
    }

    private void addWordIntoCurrentSelectedList(String word){
        list_selected_words.add(word);
    }
}