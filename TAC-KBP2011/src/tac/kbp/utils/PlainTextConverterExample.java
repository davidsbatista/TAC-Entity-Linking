package tac.kbp.utils;

import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;

public class PlainTextConverterExample {
	
        public static final String TEST = "This is a [[Hello World]] '''example'''";

        public static void main(String[] args) {
                WikiModel wikiModel = new WikiModel("http://www.mywiki.com/wiki/${image}", "http://www.mywiki.com/wiki/${title}");
                String plainStr = wikiModel.render(new PlainTextConverter(), TEST);
                System.out.print(plainStr);
        }
}