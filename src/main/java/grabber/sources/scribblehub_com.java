package grabber.sources;

import grabber.Chapter;
import grabber.GrabberUtils;
import grabber.Novel;
import grabber.NovelMetadata;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class scribblehub_com implements Source {
    private static final String NAME = "Scribble Hub";
    private static final String URL = "https://scribblehub.com";
    private static final boolean CAN_HEADLESS = false;
    private Novel novel;
    private Document toc;

    public scribblehub_com(Novel novel) {
        this.novel = novel;
    }

    public scribblehub_com() {
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean canHeadless() {
        return CAN_HEADLESS;
    }

    @Override
    public String toString() {
        return NAME;
    }

    @Override
    public String getUrl() {
        return URL;
    }

    @Override
    public List<Chapter> getChapterList() {
        List<Chapter> chapterList = new ArrayList<>();
        try {
            toc = Jsoup.connect(novel.novelLink)
                    .userAgent("Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/114.0")
                    .cookies(novel.cookies)
                    .cookie("toc_show", "9999")
                    .timeout(30 * 1000)
                    .get();
            for (Element chapterLink : toc.select("a.toc_a")) {
                chapterList.add(new Chapter(chapterLink.text(), chapterLink.attr("abs:href")));
            }
            Collections.reverse(chapterList);
        } catch (HttpStatusException httpEr) {
            GrabberUtils.err(novel.window, GrabberUtils.getHTMLErrMsg(httpEr));
        } catch (IOException e) {
            GrabberUtils.err(novel.window, "Could not connect to webpage!", e);
        } catch (NullPointerException e) {
            GrabberUtils.err(novel.window, "Could not find expected selectors. Correct novel link?", e);
        }
        return chapterList;
    }

    @Override
    public Element getChapterContent(Chapter chapter) {
        Element chapterBody = null;
        try {
            Document doc = Jsoup.connect(chapter.chapterURL)
                    .cookies(novel.cookies)
                    .get();
            chapterBody = doc.select("#chp_raw").first();
        } catch (HttpStatusException httpEr) {
            GrabberUtils.err(novel.window, GrabberUtils.getHTMLErrMsg(httpEr));
        } catch (IOException e) {
            GrabberUtils.err(novel.window, "Could not connect to webpage!", e);
        }
        return chapterBody;
    }

    @Override
    public NovelMetadata getMetadata() {
        NovelMetadata metadata = new NovelMetadata();
        if (toc != null) {
            metadata.setTitle(toc.select(".fic_title").first().text());
            metadata.setAuthor(toc.select(".auth_name_fic").first().text());
            metadata.setDescription(toc.select(".wi_fic_desc").first().text());
            metadata.setBufferedCover(toc.select(".fic_image img").attr("abs:src"));

            Elements tags = toc.select(".wi_fic_genre a");
            List<String> subjects = new ArrayList<>();
            for (Element tag : tags) {
                subjects.add(tag.text());
            }
            metadata.setSubjects(subjects);
        }
        return metadata;
    }

    @Override
    public List<String> getBlacklistedTags() {
        return new ArrayList<>();
    }
}

