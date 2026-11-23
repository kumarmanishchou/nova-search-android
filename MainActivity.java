package com.nova.search;

import android.app.Activity;
import android.content.ContentValues;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.graphics.Color;
import android.text.InputType;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import android.database.Cursor;
import android.database.sqlite.*;

public class MainActivity extends Activity {
    private EditText query, seed;
    private TextView status, results;
    private DB db;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private static final int BG = Color.rgb(11,16,32), FG = Color.rgb(238,242,255), MUTED = Color.rgb(160,174,202), BLUE = Color.rgb(139,180,255);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        db = new DB(this);
        LinearLayout page = new LinearLayout(this); page.setOrientation(1); page.setPadding(24,36,24,24); page.setBackgroundColor(BG);
        TextView brand = new TextView(this); brand.setText("NOVA"); brand.setTextColor(BLUE); brand.setTextSize(34); brand.setGravity(Gravity.CENTER); brand.setTypeface(null,1);
        TextView tagline = new TextView(this); tagline.setText("DISCOVER BEYOND THE ORDINARY"); tagline.setTextColor(MUTED); tagline.setTextSize(11); tagline.setGravity(Gravity.CENTER);
        query = new EditText(this); query.setSingleLine(true); query.setHint("Search your independent index"); query.setTextColor(FG); query.setHintTextColor(MUTED); query.setInputType(InputType.TYPE_CLASS_TEXT);
        Button search = button("Search"); search.setOnClickListener(v -> doSearch());
        TextView grow = new TextView(this); grow.setText("GROW NOVA'S INDEX"); grow.setTextColor(BLUE); grow.setTextSize(13); grow.setPadding(0,28,0,8);
        seed = new EditText(this); seed.setSingleLine(true); seed.setHint("https://example.com"); seed.setTextColor(FG); seed.setHintTextColor(MUTED); seed.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        Button crawl = button("Crawl website"); crawl.setOnClickListener(v -> startCrawl());
        status = new TextView(this); status.setTextColor(MUTED); status.setText("Your index starts empty. Add a public website to begin."); status.setPadding(0,12,0,12);
        results = new TextView(this); results.setTextColor(FG); results.setTextSize(14);
        ScrollView scroll = new ScrollView(this); scroll.addView(results);
        page.addView(brand); page.addView(tagline); page.addView(query); page.addView(search); page.addView(grow); page.addView(seed); page.addView(crawl); page.addView(status); page.addView(scroll, new LinearLayout.LayoutParams(-1,0,1));
        setContentView(page);
    }
    private Button button(String t) { Button b=new Button(this); b.setText(t); b.setTextColor(BG); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(BLUE)); return b; }
    private void doSearch() {
        String q=query.getText().toString().trim();
        if(q.isEmpty()) return;
        Cursor c=db.search(q); StringBuilder out=new StringBuilder();
        if(c.getCount()==0) out.append("No matching pages in your index yet.\nTry crawling a website above.");
        while(c.moveToNext()) out.append("\n").append(c.getString(1)).append("\n").append(c.getString(2)).append("\n\n");
        c.close(); results.setText(out.toString());
        status.setText(db.count()+" pages indexed on this device");
    }
    private void startCrawl() {
        String u=seed.getText().toString().trim();
        if(!u.startsWith("https://")) { status.setText("Enter a full HTTPS website address."); return; }
        status.setText("Crawling in the background. Keep NOVA open…");
        worker.execute(() -> {
            int added=0;
            try {
                URL start=new URL(u); String host=start.getHost();
                ArrayDeque<URL> queue=new ArrayDeque<>(); HashSet<String> seen=new HashSet<>(); queue.add(start);
                while(!queue.isEmpty() && seen.size()<40) {
                    URL url=queue.removeFirst(); String key=url.toString();
                    if(!seen.add(key) || !url.getHost().equalsIgnoreCase(host)) continue;
                    HttpURLConnection con=(HttpURLConnection)url.openConnection();
                    con.setConnectTimeout(8000); con.setReadTimeout(10000); con.setRequestProperty("User-Agent","NOVA Search Bot/1.0 (+local prototype)");
                    int code=con.getResponseCode(); String type=con.getContentType();
                    if(code==200 && type!=null && type.toLowerCase().contains("text/html")) {
                        String html=readLimited(con.getInputStream(), 700000);
                        String title=match(html,"(?is)<title[^>]*>(.*?)</title>"); if(title.isEmpty()) title=url.getHost();
                        String text=html.replaceAll("(?is)<script.*?</script>|<style.*?</style>"," ").replaceAll("(?is)<[^>]+>"," ").replaceAll("&nbsp;"," ").replaceAll("&amp;","&").replaceAll("\\s+"," ").trim();
                        db.put(title,url.toString(),text); added++;
                        Matcher m=Pattern.compile("(?is)href\\s*=\\s*['\"]([^'\"]+)['\"]").matcher(html);
                        while(m.find() && queue.size()<200) try {
                            URL next=new URL(url,m.group(1)); if(next.getProtocol().equals("https") && next.getHost().equalsIgnoreCase(host) && !seen.contains(next.toString())) queue.add(next);
                        } catch(Exception ignored) {}
                    }
                    con.disconnect();
                    try { Thread.sleep(900); } catch(InterruptedException ignored) {}
                }
            } catch(Exception e) {
                final String msg=e.getMessage()==null?"Could not crawl this website":e.getMessage();
                main.post(() -> status.setText("Crawl stopped: "+msg));
                return;
            }
            final int n=added; main.post(() -> { status.setText("Crawl finished: "+n+" pages added. Total: "+db.count()); doSearchIfPresent(); });
        });
    }
    private void doSearchIfPresent(){ if(!query.getText().toString().trim().isEmpty()) doSearch(); }
    private String readLimited(InputStream in,int max)throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] b=new byte[8192]; int n,total=0;
        while((n=in.read(b))>0 && total<max){out.write(b,0,Math.min(n,max-total));total+=n;}
        return out.toString("UTF-8");
    }
    private String match(String html,String regex) {
        Matcher m=Pattern.compile(regex).matcher(html); return m.find()?m.group(1).replaceAll("<[^>]+>"," ").replaceAll("\\s+"," ").trim():"";
    }
    @Override protected void onDestroy(){ super.onDestroy(); worker.shutdownNow(); if(db!=null)db.close(); }

    static class DB extends SQLiteOpenHelper {
        DB(Activity a){super(a,"nova.db",null,1);}
        public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE pages(_id INTEGER PRIMARY KEY,title TEXT,url TEXT UNIQUE,body TEXT)");}
        public void onUpgrade(SQLiteDatabase d,int o,int n){}
        void put(String t,String u,String b){SQLiteDatabase d=getWritableDatabase(); ContentValues v=new ContentValues();v.put("title",t);v.put("url",u);v.put("body",b);d.insertWithOnConflict("pages",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
        int count(){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM pages",null);c.moveToFirst();int n=c.getInt(0);c.close();return n;}
        Cursor search(String q){String like="%"+q+"%";return getReadableDatabase().rawQuery("SELECT _id,title,url FROM pages WHERE title LIKE ? OR body LIKE ? ORDER BY CASE WHEN title LIKE ? THEN 0 ELSE 1 END LIMIT 50",new String[]{like,like,like});}
    }
}
