/*
 * Copyright (C) 2016 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.android.article;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import xyz.klinker.android.article.data.Article;
import xyz.klinker.android.article.api.ArticleApi;
import xyz.klinker.android.article.data.DataSource;

/**
 * Helper for working with the article apis.
 */
public class ArticleUtils {

    private static final String SELECTOR = "p, h1, h2, h3, h4, h5, h6, img, blockquote, pre";

    private ArticleApi api;

    ArticleUtils(String apiToken) {
        this.api = new ArticleApi(apiToken);
    }

    /**
     * Loads an article from the server.
     *
     * @param url the url to load the article from.
     * @param source the data source.
     * @param callback the callback to receive after loading completes.
     */
    void loadArticle(final String url, final DataSource source,
                     final ArticleLoadedListener callback) {
        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                source.open();
                Article loadedArticle = source.getArticle(url);

                final Article article;
                if (loadedArticle != null) {
                    article = loadedArticle;
                } else {
                    article = api.article().parse(url);

                    // write the url to the article if it isn't there, this will make loading
                    // from the database easier later so that we don't have to worry about loading
                    // non-articles on the server.
                    if (article.url == null) {
                        article.url = url;
                    }

                    source.insertArticle(article);
                }

                source.close();

                if (callback != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onArticleLoaded(article);
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * Preloads an article from the server so that it is cached on the device and immediately
     * available when a user tries to view it without making any network calls. This includes
     * loading the article body along with precaching any images with Glide.
     *
     * @param context the current application context.
     * @param url the url to try and preload.
     */
    public void preloadArticle(Context context, final String url) {
        // TODO(klinker41): preload the article and images. If it is marked as not an article on
        //                  the server, skip the images.
    }

    /**
     * Parses the article content into a elements object using jsoup and the @link{SELECTOR}.
     *
     * @param article the article to parse content from.
     * @param callback the callback to receive after parsing completes.
     */
    void parseArticleContent(final Article article, final ArticleLoadedListener callback) {
        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Document doc = Jsoup.parse(article.content);
                final Elements elements = removeUnnecessaryElements(doc.select(SELECTOR), article);

                if (callback != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onArticleParsed(elements);
                        }
                    });
                }
            }
        }).start();
    }

    @Nullable
    private Elements removeUnnecessaryElements(Elements elements, Article article) {
        for (int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);

            if (i == 0 && (!element.tagName().equals("p") || element.text().contains(article.title))) {
                elements.remove(i--);
                continue;
            }

            if (element.tagName().equals("img")) {
                String src = element.attr("src");
                if (src == null || src.length() == 0 || !isImageUrl(src) ||
                        src.equals(article.image)) {
                    elements.remove(i--);
                }
            } else {
                String text = element.text().trim();
                if (text.length() == 0 || text.equals("Advertisement") || text.equals("Sponsored") ) {
                    elements.remove(i--);
                } else if (i > 0 && text.equals(elements.get(i-1).text().trim())) {
                    elements.remove(i--);
                }
            }
        }

        if (elements.size() > 0) {
            String lastTag = elements.last().tagName();
            while (!lastTag.equals("p") && !lastTag.equals("img")) {
                elements.remove(elements.size() - 1);
                lastTag = elements.last().tagName();
            }

            // if not many paragraphs and text is small, then don't show anything
            if (elements.size() < 7 && elements.text().trim().length() < 100) {
                elements = null;
            }
        }

        return elements;
    }

    private boolean isImageUrl(String src) {
        return src.contains("jpg") || src.contains("png") || src.contains("gif");
    }

}
